package com.meshlink.messaging.data

import com.meshlink.common.recovery.RetryCoordinator
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.RelayDao
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.BatteryAwareNetworking
import com.meshlink.routing.engine.CongestionMonitor
import com.meshlink.routing.engine.IntelligentRetryEngine
import com.meshlink.routing.engine.PowerState
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutomaticRetryEngineTest {

    private val meshRepository = mockk<MeshRepository>(relaxed = true)
    private val meshRouter = mockk<Router>(relaxed = true)
    private val chatDao = mockk<ChatDao>(relaxed = true)
    private val relayDao = mockk<RelayDao>(relaxed = true)
    private val congestionMonitor = mockk<CongestionMonitor>(relaxed = true)
    private val batteryAwareNetworking = mockk<BatteryAwareNetworking>(relaxed = true)

    private lateinit var intelligentRetryEngine: IntelligentRetryEngine
    private lateinit var stateMachine: MessageStateMachine

    @Before
    fun setUp() {
        every { congestionMonitor.isCongested() } returns false
        every { batteryAwareNetworking.powerState.value } returns PowerState.NORMAL

        intelligentRetryEngine = IntelligentRetryEngine(congestionMonitor, batteryAwareNetworking)
        stateMachine = MessageStateMachine(chatDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createRetryCoordinator(scope: CoroutineScope): RetryCoordinator {
        val meshRepositoryProvider: javax.inject.Provider<MeshRepository> = javax.inject.Provider { meshRepository }
        return RetryCoordinator(
            context = mockk(relaxed = true),
            meshRepositoryProvider = meshRepositoryProvider,
            meshRouter = meshRouter,
            chatDao = chatDao,
            relayDao = relayDao,
            intelligentRetryEngine = intelligentRetryEngine,
            stateMachine = stateMachine,
            applicationScope = scope
        )
    }

    @Test
    fun `calculateRetryDelay adheres to exponential backoff schedule with jitter`() {
        assertEquals(0L, intelligentRetryEngine.calculateRetryDelay(1))
        
        val attempt2Delay = intelligentRetryEngine.calculateRetryDelay(2)
        assertTrue("Attempt 2 delay should be between 2000 and 2600ms, got $attempt2Delay", attempt2Delay in 2000L..2600L)

        val attempt3Delay = intelligentRetryEngine.calculateRetryDelay(3)
        assertTrue("Attempt 3 delay should be between 5000 and 6500ms, got $attempt3Delay", attempt3Delay in 5000L..6500L)

        val attempt4Delay = intelligentRetryEngine.calculateRetryDelay(4)
        assertTrue("Attempt 4 delay should be between 10000 and 13000ms, got $attempt4Delay", attempt4Delay in 10000L..13000L)
    }

    @Test
    fun `triggerEvent immediately processes non-terminal pending messages`() = runTest {
        val sampleMsg = MessageEntity(
            messageId = "msg_retry_1",
            chatId = "peer_A",
            senderId = "me",
            text = "Hello mesh",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.WAITING_FOR_ROUTE
        )

        coEvery { chatDao.getMessagesByStatus(eq(DeliveryStatus.WAITING_FOR_ROUTE)) } returns listOf(sampleMsg)
        coEvery { chatDao.getMessageByUuid(eq("msg_retry_1")) } returns sampleMsg

        val retryCoordinator = createRetryCoordinator(backgroundScope)
        retryCoordinator.start()
        retryCoordinator.triggerEvent("peer_connected")
        runCurrent()

        coVerify { meshRepository.sendMessage(eq("peer_A"), any()) }
    }

    @Test
    fun `cancelRetryForPacket stops pending retry jobs`() = runTest {
        val retryCoordinator = createRetryCoordinator(backgroundScope)
        retryCoordinator.start()
        retryCoordinator.cancelRetryForPacket("msg_retry_1")
        runCurrent()

        coVerify(exactly = 0) { meshRepository.sendMessage(any(), any()) }
    }

    @Test
    fun `messages exceeding 24 hour TTL transition to EXPIRED`() = runTest {
        val oldTimestamp = System.currentTimeMillis() - (25 * 3600 * 1000L) // 25 hours ago
        val expiredMsg = MessageEntity(
            messageId = "msg_old_1",
            chatId = "peer_A",
            senderId = "me",
            text = "Old message",
            timestamp = oldTimestamp,
            isFromMe = true,
            status = DeliveryStatus.RETRYING
        )

        coEvery { chatDao.getMessagesByStatus(eq(DeliveryStatus.RETRYING)) } returns listOf(expiredMsg)
        coEvery { chatDao.getMessageByUuid(eq("msg_old_1")) } returns expiredMsg

        val retryCoordinator = createRetryCoordinator(backgroundScope)
        retryCoordinator.start()
        retryCoordinator.triggerEvent("ttl_check")
        runCurrent()

        coVerify { chatDao.updateMessageStatus(eq("msg_old_1"), eq(DeliveryStatus.EXPIRED)) }
        coVerify(exactly = 0) { meshRepository.sendMessage(any(), any()) }
    }
}
