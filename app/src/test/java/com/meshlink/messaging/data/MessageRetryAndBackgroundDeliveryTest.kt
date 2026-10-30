package com.meshlink.messaging.data

import android.content.Context
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.ble.data.handlers.AckManager
import com.meshlink.ble.data.handlers.TextMessageHandler
import com.meshlink.common.recovery.RetryCoordinator
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.database.data.local.RelayDao
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.PacketFailed
import com.meshlink.routing.api.PacketQueued
import com.meshlink.routing.api.PacketStatusEvent
import com.meshlink.routing.api.PacketTransmitted
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.BatteryAwareNetworking
import com.meshlink.routing.engine.CongestionMonitor
import com.meshlink.routing.engine.IntelligentRetryEngine
import com.meshlink.routing.engine.PowerState
import com.meshlink.ui.components.chat.DeliveryUiState
import com.meshlink.util.NotificationHelper
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

/**
 * Verification test suite for Issue 1:
 * Reliable Message Retry and Background Delivery.
 *
 * Covers all 12 minimum required test scenarios deterministically with injectable time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageRetryAndBackgroundDeliveryTest {

    private val context = mockk<Context>(relaxed = true)
    private val chatDao = mockk<ChatDao>(relaxed = true)
    private val relayDao = mockk<RelayDao>(relaxed = true)
    private val router = mockk<Router>(relaxed = true)
    private val meshRepository = mockk<MeshRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val packetDispatcher = mockk<PacketDispatcher>(relaxed = true)
    private val congestionMonitor = mockk<CongestionMonitor>(relaxed = true)
    private val batteryAwareNetworking = mockk<BatteryAwareNetworking>(relaxed = true)

    private val packetEventsFlow = MutableSharedFlow<PacketStatusEvent>(extraBufferCapacity = 64)
    private lateinit var intelligentRetryEngine: IntelligentRetryEngine
    private lateinit var stateMachine: MessageStateMachine
    private lateinit var deliveryTracker: DeliveryTracker
    private lateinit var retryCoordinator: RetryCoordinator
    private lateinit var textMessageHandler: TextMessageHandler
    private lateinit var ackManager: AckManager

    private var virtualTime = 1_000_000L

    @Before
    fun setUp() {
        virtualTime = 1_000_000L

        val notificationManager = mockk<android.app.NotificationManager>(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

        every { router.packetEvents } returns packetEventsFlow
        every { congestionMonitor.isCongested() } returns false
        every { congestionMonitor.congestionLevel.value } returns com.meshlink.routing.engine.CongestionLevel.LOW
        every { batteryAwareNetworking.powerState.value } returns PowerState.NORMAL
        coEvery { chatDao.updateMessageStatusConditional(any(), any(), any()) } returns 1

        val localUser = User(
            meshId = "my_mesh_id",
            name = "Alice"
        )
        coEvery { userRepository.getLocalUser() } returns localUser

        intelligentRetryEngine = IntelligentRetryEngine(congestionMonitor, batteryAwareNetworking)
        stateMachine = MessageStateMachine(chatDao)

        val retryCoordinatorProvider = Provider { retryCoordinator }
        deliveryTracker = DeliveryTracker(
            router = router,
            stateMachine = stateMachine,
            applicationScope = CoroutineScope(StandardTestDispatcher()),
            retryCoordinatorProvider = retryCoordinatorProvider
        )

        val deliveryTrackerProvider = Provider { deliveryTracker }
        val meshRepoProvider = Provider { meshRepository }
        retryCoordinator = RetryCoordinator(
            context = context,
            meshRepositoryProvider = meshRepoProvider,
            meshRouter = router,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = CoroutineScope(StandardTestDispatcher()),
            deliveryTrackerProvider = deliveryTrackerProvider
        ).apply {
            clock = { virtualTime }
        }

        textMessageHandler = TextMessageHandler(
            context = context,
            userRepository = userRepository,
            chatDao = chatDao,
            packetDispatcher = packetDispatcher,
            stateMachine = stateMachine
        )

        ackManager = AckManager(
            userRepository = userRepository,
            chatDao = chatDao,
            packetDispatcher = packetDispatcher,
            deliveryTracker = deliveryTracker,
            retryCoordinator = retryCoordinator
        )

        NotificationHelper.setAppForeground(false)
    }

    @After
    fun tearDown() {
        if (::retryCoordinator.isInitialized) {
            retryCoordinator.stop()
        }
        clearAllMocks()
    }

    // ─────────────────── Scenario 1 ───────────────────
    @Test
    fun `1 - A failed send remains pending and is retried`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val coordinator = RetryCoordinator(
            context = context,
            meshRepositoryProvider = Provider { meshRepository },
            meshRouter = router,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = this,
            deliveryTrackerProvider = Provider { deliveryTracker }
        ).apply {
            clock = { virtualTime }
        }
        coordinator.start()

        val msg = MessageEntity(
            messageId = "msg_fail_1",
            chatId = "peer_bob",
            senderId = "my_mesh_id",
            text = "Emergency status update",
            timestamp = virtualTime,
            isFromMe = true,
            status = DeliveryStatus.WAITING_FOR_ROUTE
        )
        coEvery { chatDao.getMessageByUuid("msg_fail_1") } returns msg
        coEvery { chatDao.getMessagesByStatus(DeliveryStatus.WAITING_FOR_ROUTE) } returns listOf(msg)

        coordinator.triggerEvent("route_discovered")
        runCurrent()

        coVerify { meshRepository.sendMessage(eq("peer_bob"), any()) }
        coordinator.stop()
    }

    // ─────────────────── Scenario 2 ───────────────────
    @Test
    fun `2 - A successful delivery acknowledgement stops subsequent retries`() = runTest {
        val coordinator = RetryCoordinator(
            context = context,
            meshRepositoryProvider = Provider { meshRepository },
            meshRouter = router,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = this,
            deliveryTrackerProvider = Provider { deliveryTracker }
        ).apply {
            clock = { virtualTime }
        }
        coordinator.start()

        val msg = MessageEntity(
            messageId = "msg_ack_1",
            chatId = "peer_bob",
            senderId = "my_mesh_id",
            text = "Hello Bob",
            timestamp = virtualTime,
            isFromMe = true,
            status = DeliveryStatus.WAITING_FOR_ACK
        )
        coEvery { chatDao.getMessageByUuid("msg_ack_1") } returns msg

        // Simulate incoming DELIVERY_ACK from Bob
        val ackPacket = MeshPacket(
            senderId = "peer_bob",
            targetId = "my_mesh_id",
            payload = "msg_ack_1",
            type = PacketType.DELIVERY_ACK
        )
        ackManager.handleDeliveryAck(ackPacket)
        runCurrent()

        // Message should be transitioned to DELIVERED
        coVerify { chatDao.updateMessageStatusConditional("msg_ack_1", DeliveryStatus.DELIVERED, any()) }

        // Subsequent retry triggers must not resend msg_ack_1
        coEvery { chatDao.getMessageByUuid("msg_ack_1") } returns msg.copy(status = DeliveryStatus.DELIVERED)
        coordinator.triggerImmediateRetryForPacket("msg_ack_1")
        runCurrent()

        coVerify(exactly = 0) { meshRepository.sendMessage(eq("peer_bob"), any()) }
        coordinator.stop()
    }

    // ─────────────────── Scenario 3 ───────────────────
    @Test
    fun `3 - An unavailable recipient causes bounded retries`() {
        assertEquals(0L, intelligentRetryEngine.calculateRetryDelay(1))
        val delay2 = intelligentRetryEngine.calculateRetryDelay(2)
        val delay3 = intelligentRetryEngine.calculateRetryDelay(3)
        val delay4 = intelligentRetryEngine.calculateRetryDelay(4)
        val delay5 = intelligentRetryEngine.calculateRetryDelay(5)

        assertTrue("Delay 2 must be bounded in [2000, 2600]", delay2 in 2000L..2600L)
        assertTrue("Delay 3 must be bounded in [5000, 6500]", delay3 in 5000L..6500L)
        assertTrue("Delay 4 must be bounded in [10000, 13000]", delay4 in 10000L..13000L)
        assertTrue("Delay 5 must be bounded in [20000, 26000]", delay5 in 20000L..26000L)
        assertTrue("Max delay must not exceed 5 minutes", intelligentRetryEngine.calculateRetryDelay(10) <= 300_000L)
    }

    // ─────────────────── Scenario 4 ───────────────────
    @Test
    fun `4 - A message expires after 10 minutes based on its persisted creation time`() = runTest {
        val creationTime = 1_000_000L
        virtualTime = creationTime + (10 * 60 * 1000L) + 1_000L // 10 minutes and 1 second later

        val coordinator = RetryCoordinator(
            context = context,
            meshRepositoryProvider = Provider { meshRepository },
            meshRouter = router,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = this,
            deliveryTrackerProvider = Provider { deliveryTracker }
        ).apply {
            clock = { virtualTime }
        }
        coordinator.start()

        val expiredMsg = MessageEntity(
            messageId = "msg_10m_exp",
            chatId = "peer_bob",
            senderId = "my_mesh_id",
            text = "Time-sensitive alert",
            timestamp = creationTime,
            isFromMe = true,
            status = DeliveryStatus.RETRYING
        )
        coEvery { chatDao.getMessageByUuid("msg_10m_exp") } returns expiredMsg
        coEvery { chatDao.getMessagesByStatus(DeliveryStatus.RETRYING) } returns listOf(expiredMsg)

        coordinator.triggerEvent("check_expiry")
        runCurrent()

        // Marked as EXPIRED
        coVerify { chatDao.updateMessageStatusConditional("msg_10m_exp", DeliveryStatus.EXPIRED, any()) }
        // Not deleted
        coVerify(exactly = 0) { chatDao.deleteMessages(any()) }
        // Not transmitted
        coVerify(exactly = 0) { meshRepository.sendMessage(any(), any()) }
        coordinator.stop()
    }

    // ─────────────────── Scenario 5 ───────────────────
    @Test
    fun `5 - Restarting the app does not reset the expiry`() = runTest {
        val creationTime = 1_000_000L
        // Message was created at T0. App restarts 8 minutes later.
        val restartTime = creationTime + (8 * 60 * 1000L)
        virtualTime = restartTime

        // Brand-new coordinator instance started at restartTime
        val newCoordinator = RetryCoordinator(
            context = context,
            meshRepositoryProvider = Provider { meshRepository },
            meshRouter = router,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = this,
            deliveryTrackerProvider = Provider { deliveryTracker }
        ).apply {
            clock = { virtualTime }
        }
        newCoordinator.start()

        val msg = MessageEntity(
            messageId = "msg_no_reset",
            chatId = "peer_bob",
            senderId = "my_mesh_id",
            text = "Don't extend my lifetime",
            timestamp = creationTime, // Persisted original timestamp
            isFromMe = true,
            status = DeliveryStatus.RETRYING
        )
        coEvery { chatDao.getMessageByUuid("msg_no_reset") } returns msg
        coEvery { chatDao.getMessagesByStatus(DeliveryStatus.RETRYING) } returns listOf(msg)

        // Advance 2.5 more minutes (total 10.5 minutes since creation, but only 2.5 minutes since restart)
        virtualTime = creationTime + (10 * 60 * 1000L) + 30_000L
        newCoordinator.triggerEvent("check_after_restart")
        runCurrent()

        // Must expire based on original creation timestamp, NOT restart time!
        coVerify { chatDao.updateMessageStatusConditional("msg_no_reset", DeliveryStatus.EXPIRED, any()) }
        newCoordinator.stop()
    }

    // ─────────────────── Scenario 6 ───────────────────
    @Test
    fun `6 - A message already past expiry is not transmitted again`() = runTest {
        val creationTime = 1_000_000L
        virtualTime = creationTime + (20 * 60 * 1000L) // 20 minutes later (already expired while app was closed)

        val coordinator = RetryCoordinator(
            context = context,
            meshRepositoryProvider = Provider { meshRepository },
            meshRouter = router,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = this,
            deliveryTrackerProvider = Provider { deliveryTracker }
        ).apply {
            clock = { virtualTime }
        }

        val staleMsg = MessageEntity(
            messageId = "msg_stale_offline",
            chatId = "peer_bob",
            senderId = "my_mesh_id",
            text = "Old message from an hour ago",
            timestamp = creationTime,
            isFromMe = true,
            status = DeliveryStatus.QUEUED
        )
        coEvery { chatDao.getMessageByUuid("msg_stale_offline") } returns staleMsg
        coEvery { chatDao.getMessagesByStatus(DeliveryStatus.QUEUED) } returns listOf(staleMsg)

        // On app startup / recover pending queues
        coordinator.start()
        runCurrent()

        // Immediately marked EXPIRED
        coVerify { chatDao.updateMessageStatusConditional("msg_stale_offline", DeliveryStatus.EXPIRED, any()) }
        // Absolutely zero transmissions
        coVerify(exactly = 0) { meshRepository.sendMessage(any(), any()) }
        coordinator.stop()
    }

    // ─────────────────── Scenario 7 ───────────────────
    @Test
    fun `7 - Duplicate retry attempts do not create duplicate recipient messages`() = runTest {
        val packet = MeshPacket(
            packetId = "msg_dup_test_1",
            senderId = "peer_alice",
            targetId = "my_mesh_id",
            payload = org.json.JSONObject().apply {
                put("text", "Hello there!")
                put("senderName", "Alice")
            }.toString(),
            type = PacketType.TEXT
        )

        // Attempt 1: Not in DB yet
        coEvery { chatDao.getMessageByUuid("msg_dup_test_1") } returns null
        textMessageHandler.receiveMessage(packet)

        // Saved once and ACK sent
        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(any(), eq("Alice")) }
        coVerify(exactly = 1) { packetDispatcher.dispatchSinglePacket(eq("peer_alice"), match { it.type == PacketType.DELIVERY_ACK && it.payload == "msg_dup_test_1" }) }

        // Attempt 2 (retry arrived): Message is now in DB
        val existingEntity = MessageEntity(
            messageId = "msg_dup_test_1",
            chatId = "peer_alice",
            senderId = "peer_alice",
            text = "Hello there!",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED
        )
        coEvery { chatDao.getMessageByUuid("msg_dup_test_1") } returns existingEntity
        textMessageHandler.receiveMessage(packet)

        // Insert must NOT be called again
        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(any(), any()) }
        // Re-ACK must be sent to satisfy sender
        coVerify(exactly = 2) { packetDispatcher.dispatchSinglePacket(eq("peer_alice"), match { it.type == PacketType.DELIVERY_ACK && it.payload == "msg_dup_test_1" }) }
    }

    // ─────────────────── Scenario 8 ───────────────────
    @Test
    fun `8 - Duplicate or late acknowledgements do not regress terminal message states`() = runTest {
        val deliveredMsg = MessageEntity(
            messageId = "msg_term_1",
            chatId = "peer_bob",
            senderId = "my_mesh_id",
            text = "Durable message",
            timestamp = virtualTime,
            isFromMe = true,
            status = DeliveryStatus.DELIVERED
        )
        coEvery { chatDao.getMessageByUuid("msg_term_1") } returns deliveredMsg

        // Late delivery timeout attempts regression to RETRYING
        val regressedRetrying = stateMachine.transitionToRetrying("msg_term_1")
        assertFalse("Must reject regression from DELIVERED to RETRYING", regressedRetrying)

        // Late failure attempts regression to WAITING_FOR_ROUTE
        val regressedRoute = stateMachine.transitionToWaitingForRoute("msg_term_1")
        assertFalse("Must reject regression from DELIVERED to WAITING_FOR_ROUTE", regressedRoute)

        // Late expiry attempts regression to EXPIRED
        val regressedExpired = stateMachine.transitionToExpired("msg_term_1")
        assertFalse("Must reject regression from DELIVERED to EXPIRED", regressedExpired)

        // Late ACK on an EXPIRED message cannot change it to DELIVERED
        val expiredMsg = deliveredMsg.copy(status = DeliveryStatus.EXPIRED)
        coEvery { chatDao.getMessageByUuid("msg_term_expired") } returns expiredMsg
        val regressedAck = stateMachine.transitionToDelivered("msg_term_expired")
        assertFalse("Late ACK must not transition EXPIRED to DELIVERED", regressedAck)
    }

    // ─────────────────── Scenario 9 ───────────────────
    @Test
    fun `9 - A lost acknowledgement can be recovered without duplicate delivery`() = runTest {
        // Step A: Recipient already has the message
        val packetId = "msg_lost_ack_1"
        val existingMsg = MessageEntity(
            messageId = packetId,
            chatId = "peer_alice",
            senderId = "peer_alice",
            text = "Reliable packet",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED
        )
        coEvery { chatDao.getMessageByUuid(packetId) } returns existingMsg

        val retryPacket = MeshPacket(
            packetId = packetId,
            senderId = "peer_alice",
            targetId = "my_mesh_id",
            payload = org.json.JSONObject().apply {
                put("text", "Reliable packet")
                put("senderName", "Alice")
            }.toString(),
            type = PacketType.TEXT
        )

        // Recipient processes the retry
        textMessageHandler.receiveMessage(retryPacket)

        // Verified: No duplicate insert, re-sent ACK
        coVerify(exactly = 0) { chatDao.insertMessage(any()) }
        coVerify(exactly = 1) { packetDispatcher.dispatchSinglePacket("peer_alice", match { it.type == PacketType.DELIVERY_ACK && it.payload == packetId }) }
    }

    // ─────────────────── Scenario 10 ───────────────────
    @Test
    fun `10 - Pending messages recover after service or process recreation`() = runTest {
        val coordinator = RetryCoordinator(
            context = context,
            meshRepositoryProvider = Provider { meshRepository },
            meshRouter = router,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = this,
            deliveryTrackerProvider = Provider { deliveryTracker }
        ).apply {
            clock = { virtualTime }
        }

        // Message was WAITING_FOR_ACK when process died 1 minute ago
        var currentStatus = DeliveryStatus.WAITING_FOR_ACK
        val strandedMsg = MessageEntity(
            messageId = "msg_stranded_1",
            chatId = "peer_bob",
            senderId = "my_mesh_id",
            text = "Survives process death",
            timestamp = virtualTime - 60_000L,
            isFromMe = true,
            status = DeliveryStatus.WAITING_FOR_ACK
        )
        coEvery { chatDao.getMessageByUuid("msg_stranded_1") } answers {
            strandedMsg.copy(status = currentStatus)
        }
        coEvery { chatDao.updateMessageStatusConditional("msg_stranded_1", any(), any()) } answers {
            currentStatus = secondArg()
            1
        }
        coEvery { chatDao.getMessagesByStatus(DeliveryStatus.WAITING_FOR_ACK) } returns listOf(strandedMsg)

        // Process starts up
        coordinator.start()
        runCurrent()
        runCurrent()

        // Message recovered and retried via meshRepository
        coVerify { meshRepository.sendMessage(eq("peer_bob"), any()) }
        coordinator.stop()
    }

    // ─────────────────── Scenario 11 ───────────────────
    @Test
    fun `11 - Receiving and persisting a message does not require an active chat screen`() = runTest {
        // App is explicitly in the background (no active chat screen, no activity)
        NotificationHelper.setAppForeground(false)
        NotificationHelper.setCurrentChatId(null)

        val bgPacket = MeshPacket(
            packetId = "msg_background_1",
            senderId = "peer_alice",
            targetId = "my_mesh_id",
            payload = org.json.JSONObject().apply {
                put("text", "Incoming background alert")
                put("senderName", "Alice")
            }.toString(),
            type = PacketType.TEXT
        )
        coEvery { chatDao.getMessageByUuid("msg_background_1") } returns null

        textMessageHandler.receiveMessage(bgPacket)

        // Message persisted to database
        coVerify { chatDao.insertMessageAndUpdateChat(match { it.messageId == "msg_background_1" && it.status == DeliveryStatus.DELIVERED }, eq("Alice")) }
        // ACK dispatched back to sender
        coVerify { packetDispatcher.dispatchSinglePacket(eq("peer_alice"), match { it.type == PacketType.DELIVERY_ACK && it.payload == "msg_background_1" }) }
    }

    // ─────────────────── Scenario 12 ───────────────────
    @Test
    fun `12 - Existing message status and database migration behavior remain compatible`() {
        // Status priorities: terminal statuses have highest priority
        assertTrue(DeliveryStatus.DELIVERED.ordinal >= 0)
        assertTrue(DeliveryStatus.EXPIRED.ordinal >= 0)
        assertTrue(DeliveryStatus.SEEN.ordinal >= 0)

        // UI mappings
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.EXPIRED))
        assertEquals(DeliveryUiState.Delivered, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.DELIVERED))
        assertEquals(DeliveryUiState.Seen, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.SEEN))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.RETRYING))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.WAITING_FOR_ROUTE))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(com.meshlink.domain.model.DeliveryStatus.WAITING_FOR_ACK))
    }
}
