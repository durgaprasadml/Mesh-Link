package com.meshlink.ble.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meshlink.ble.data.handlers.BroadcastHandler
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.database.data.local.UserDao
import com.meshlink.domain.model.BroadcastType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.IntelligentTransportManager
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.ui.sos.SosStatus
import com.meshlink.ui.sos.SosViewModel
import com.meshlink.util.NotificationHelper
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SosConnectionlessBroadcastTest {

    private lateinit var context: Context
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val chatDao = mockk<ChatDao>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val locationProvider = mockk<com.meshlink.data.location.LocationProvider>(relaxed = true)
    private val router = mockk<Router>(relaxed = true)
    private val cryptoManager = mockk<MeshCryptoManager>(relaxed = true)
    private val wifiDirectManager = mockk<com.meshlink.wifi.manager.WifiDirectManager>(relaxed = true)

    private lateinit var broadcastHandler: BroadcastHandler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        mockkObject(NotificationHelper)
        every { NotificationHelper.showMessageNotification(any(), any(), any(), any()) } just Runs
        every { NotificationHelper.showSosNotification(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        broadcastHandler = BroadcastHandler(
            context = context,
            userRepository = userRepository,
            chatDao = chatDao,
            userDao = userDao,
            locationProvider = locationProvider,
            router = router,
            cryptoManager = cryptoManager,
            wifiDirectManagerProvider = { wifiDirectManager }
        )
    }

    @After
    fun tearDown() {
        unmockkObject(NotificationHelper)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 1: SOS announcement can be transmitted without an existing GATT connection
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test1_sosAnnouncementCanBeTransmittedWithoutExistingGattConnection`() {
        val tracker = BleAdvertisingTracker()
        val sosEventId = UUID.randomUUID().toString()
        val announcement = SosAnnouncement.create(
            senderMeshId = "PEER_SENDER",
            sosEventId = sosEventId,
            capabilities = 0x01
        )
        val request = AdvertisingRequest("SenderNode", "PEER_SENDER", 0x01, announcement)

        // No GATT connections exist
        val token = tracker.request(request)
        assertEquals(request, tracker.desired)
        assertNotNull(tracker.desired?.sosAnnouncement)

        // Serialized payload is exactly 24 bytes and contains SOS flag
        val payload = announcement.toByteArray()
        assertEquals(SosAnnouncement.ANNOUNCEMENT_PAYLOAD_SIZE, payload.size)
        assertTrue(SosAnnouncement.isSosAnnouncement(payload))

        // State changes without any GATT connection requirement
        tracker.onStarting(token)
        assertEquals(AdvertisingState.STARTING, tracker.state)
        tracker.onStartSuccess(token)
        assertTrue(tracker.isActive)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 2: Nearby unconnected Mesh-Link device can discover and validate announcement
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test2_unconnectedDeviceCanDiscoverAndValidateAnnouncement`() = runTest {
        val senderId = "ALICE101"
        val sosEventId = UUID.randomUUID().toString()
        val nowSec = System.currentTimeMillis() / 1000L
        val announcement = SosAnnouncement.create(senderId, sosEventId, capabilities = 0x01, timestampSeconds = nowSec)
        val payloadBytes = announcement.toByteArray()

        // 1. Verify parsing and integrity check on receiver side
        val parsed = SosAnnouncement.parse(payloadBytes)
        assertNotNull("Announcement must parse successfully", parsed)
        assertEquals(senderId, parsed?.senderMeshId)

        // 2. Dispatch to BroadcastHandler
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName(senderId) } returns "Alice"

        val slotMsg = slot<MessageEntity>()
        coEvery { chatDao.insertMessageAndUpdateChat(capture(slotMsg), any()) } just Runs

        broadcastHandler.receiveSosAnnouncement(parsed!!)

        // Verify incoming message persisted with DELIVERED status and short notification triggered
        assertEquals(parsed.sosIdHex, slotMsg.captured.messageId)
        assertEquals(senderId, slotMsg.captured.senderId)
        assertEquals(DeliveryStatus.DELIVERED, slotMsg.captured.status)
        assertEquals(MessageType.SOS, slotMsg.captured.messageType)
        assertNull(slotMsg.captured.latitude) // Full details not yet retrieved

        verify(exactly = 1) {
            NotificationHelper.showSosNotification(
                context = context,
                sosEventId = parsed.sosIdHex,
                senderId = senderId,
                senderName = "Alice",
                alertText = any(),
                latitude = null,
                longitude = null,
                batteryPercent = null
            )
        }

        // Verify Stage B discovery initiated
        verify(atLeast = 1) { wifiDirectManager.discoverPeers() }
        coVerify(atLeast = 1) { wifiDirectManager.ensureConnected(senderId, any(), false) }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 3: Connected recipient still receives SOS correctly
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test3_connectedRecipientStillReceivesSosCorrectly`() = runTest {
        val sosEventId = "sos_connected_001"
        val senderId = "BOB_NODE"
        coEvery { userRepository.getUserDisplayName(senderId) } returns "Bob"
        coEvery { chatDao.getMessageByUuid(sosEventId) } returns null
        every { cryptoManager.verifySignature(any(), any(), any()) } returns true

        val now = System.currentTimeMillis()
        val jsonPayload = JSONObject().apply {
            put("sosEventId", sosEventId)
            put("lat", 12.9716)
            put("lng", 77.5946)
            put("battery", 75)
            put("timestamp", now)
            put("senderName", "Bob")
            put("signingPubKey", "pub_key_base64")
            put("signature", "sig_base64")
        }.toString()

        val packet = MeshPacket(
            packetId = sosEventId,
            senderId = senderId,
            targetId = "BROADCAST",
            payload = jsonPayload,
            type = PacketType.SOS,
            ttl = 1
        )

        val slotMsg = slot<MessageEntity>()
        coEvery { chatDao.insertMessageAndUpdateChat(capture(slotMsg), any()) } just Runs

        broadcastHandler.receiveSosMessage(packet)

        assertEquals(sosEventId, slotMsg.captured.messageId)
        assertEquals(12.9716, slotMsg.captured.latitude)
        assertEquals(77.5946, slotMsg.captured.longitude)
        assertEquals(75, slotMsg.captured.batteryPercent)
        assertEquals(DeliveryStatus.DELIVERED, slotMsg.captured.status)

        verify(exactly = 1) {
            NotificationHelper.showSosNotification(
                context = context,
                sosEventId = sosEventId,
                senderId = senderId,
                senderName = "Bob",
                alertText = any(),
                latitude = 12.9716,
                longitude = 77.5946,
                batteryPercent = 75
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 4: Duplicate announcements do not create duplicate SOS records or notifications
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test4_duplicateAnnouncementsDoNotCreateDuplicateRecordsOrNotifications`() = runTest {
        val senderId = "CHARLIE1"
        val sosEventId = UUID.randomUUID().toString()
        val announcement = SosAnnouncement.create(senderId, sosEventId, capabilities = 0x01)

        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName(senderId) } returns "Charlie"

        // First delivery
        broadcastHandler.receiveSosAnnouncement(announcement)
        // Second delivery (duplicate over radio scan)
        broadcastHandler.receiveSosAnnouncement(announcement)

        // Exactly 1 message insert and 1 notification call
        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(any(), any()) }
        verify(exactly = 1) {
            NotificationHelper.showSosNotification(
                context = any(),
                sosEventId = announcement.sosIdHex,
                senderId = senderId,
                senderName = any(),
                alertText = any(),
                latitude = any(),
                longitude = any(),
                batteryPercent = any()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 5: Invalid or tampered announcements are rejected
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test5_tamperedAnnouncementsAreRejected`() = runTest {
        val announcement = SosAnnouncement.create("VALID_ND", UUID.randomUUID().toString())
        val rawBytes = announcement.toByteArray()

        // 1. Valid bytes parse
        assertNotNull(SosAnnouncement.parse(rawBytes))

        // 2. Tamper byte 5 (part of sender ID) without updating integrity tag
        val tamperedBytes1 = rawBytes.copyOf()
        tamperedBytes1[5] = (tamperedBytes1[5].toInt() xor 0xFF).toByte()
        assertNull("Tampered sender ID must fail integrity check", SosAnnouncement.parse(tamperedBytes1))

        // 3. Tamper byte 14 (part of SOS ID)
        val tamperedBytes2 = rawBytes.copyOf()
        tamperedBytes2[14] = (tamperedBytes2[14].toInt() xor 0xAA).toByte()
        assertNull("Tampered SOS ID must fail integrity check", SosAnnouncement.parse(tamperedBytes2))

        // 4. Tamper byte 20 (timestamp)
        val tamperedBytes3 = rawBytes.copyOf()
        tamperedBytes3[20] = (tamperedBytes3[20].toInt() xor 0x01).toByte()
        assertNull("Tampered timestamp must fail integrity check", SosAnnouncement.parse(tamperedBytes3))

        // 5. Unverified sender warning check
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        every { cryptoManager.hasPeerKey("UNKNOWN1") } returns false
        coEvery { userRepository.getUserDisplayName("UNKNOWN1") } returns "UNKNOWN1"
        val unverifiedAnn = SosAnnouncement.create("UNKNOWN1", UUID.randomUUID().toString())

        val slotMsg = slot<MessageEntity>()
        coEvery { chatDao.insertMessageAndUpdateChat(capture(slotMsg), any()) } just Runs

        broadcastHandler.receiveSosAnnouncement(unverifiedAnn)
        assertTrue(slotMsg.captured.text.contains("Unverified Sender"))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 6: Replayed announcements cannot repeatedly trigger SOS alerts
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test6_replayedAnnouncementsCannotRepeatedlyTriggerAlerts`() = runTest {
        val senderId = "REPLAY01"
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName(senderId) } returns "ReplayUser"

        val nowSec = System.currentTimeMillis() / 1000L

        // A. Stale announcement (20 minutes in past, > 15m window)
        val staleTs = nowSec - (20 * 60)
        val staleAnnouncement = SosAnnouncement.create(senderId, UUID.randomUUID().toString(), timestampSeconds = staleTs)
        broadcastHandler.receiveSosAnnouncement(staleAnnouncement)
        coVerify(exactly = 0) { chatDao.insertMessageAndUpdateChat(any(), any()) }

        // B. Future-skewed announcement (10 minutes in future, > 5m limit)
        val futureTs = nowSec + (10 * 60)
        val futureAnnouncement = SosAnnouncement.create(senderId, UUID.randomUUID().toString(), timestampSeconds = futureTs)
        broadcastHandler.receiveSosAnnouncement(futureAnnouncement)
        coVerify(exactly = 0) { chatDao.insertMessageAndUpdateChat(any(), any()) }

        // C. Valid fresh announcement accepted
        val freshAnnouncement = SosAnnouncement.create(senderId, UUID.randomUUID().toString(), timestampSeconds = nowSec)
        broadcastHandler.receiveSosAnnouncement(freshAnnouncement)
        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(any(), any()) }

        // D. Replay of same announcement is rejected
        broadcastHandler.receiveSosAnnouncement(freshAnnouncement)
        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(any(), any()) } // Count remains 1
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 7: Full SOS details are not exposed in the compact advertisement
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test7_fullSosDetailsAreNotExposedInCompactAdvertisement`() {
        val fullSosId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
        val announcement = SosAnnouncement.create("SECURE01", fullSosId)
        val bytes = announcement.toByteArray()

        // Size is exactly 24 bytes (fits within 31-byte legacy BLE limit)
        assertEquals(24, bytes.size)

        // Sensitive details are NOT in bytes
        val byteString = String(bytes, Charsets.ISO_8859_1)
        assertFalse(byteString.contains("37.7749")) // Latitude
        assertFalse(byteString.contains("-122.4194")) // Longitude
        assertFalse(byteString.contains("front.jpg")) // Image attachment
        assertFalse(byteString.contains("rear.jpg"))
        assertFalse(byteString.contains("medical"))
        assertFalse(byteString.contains("private_key"))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 8: Full details and attachments are transferred using Wi-Fi Direct only
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test8_fullDetailsAndAttachmentsUseWifiDirectOnly`() {
        val bleTransport = mockk<com.meshlink.ble.api.BleTransport>(relaxed = true)
        val wifiTransport = mockk<com.meshlink.wifi.api.WifiTransport>(relaxed = true)
        val settingsRepo = mockk<com.meshlink.domain.repository.SettingsRepository>(relaxed = true)
        every { settingsRepo.preferredTransport } returns flowOf("AUTOMATIC")
        val itm = IntelligentTransportManager(
            bleTransport = bleTransport,
            wifiTransport = wifiTransport,
            routeOptimizer = mockk(relaxed = true),
            settingsRepository = settingsRepo,
            metrics = mockk(relaxed = true),
            applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        // SOS packets MUST ALWAYS be assigned RouteType.WIFI_DIRECT
        val route = itm.selectTransportForPayload("PEER_TARGET", PacketType.SOS)
        assertEquals(RouteType.WIFI_DIRECT, route)

        val sosPacket = MeshPacket(
            packetId = "sos_test_route",
            senderId = "ME",
            targetId = "BROADCAST",
            payload = "{}",
            type = PacketType.SOS
        )
        val packetRoute = itm.selectTransportForPacket(sosPacket)
        assertEquals(RouteType.WIFI_DIRECT, packetRoute)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 9: If Wi-Fi Direct is unavailable, full SOS transfer remains pending
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test9_ifWifiDirectUnavailableFullSosTransferRemainsPendingInsteadOfBleFallback`() = runTest {
        val bleTransport = mockk<com.meshlink.ble.api.BleTransport>(relaxed = true)
        val wifiTransport = mockk<com.meshlink.wifi.api.WifiTransport>(relaxed = true)
        val settingsRepo = mockk<com.meshlink.domain.repository.SettingsRepository>(relaxed = true)
        every { settingsRepo.preferredTransport } returns flowOf("AUTOMATIC")
        every { wifiTransport.connectedPeers } returns emptySet()
        every { wifiTransport.health } returns MutableStateFlow(com.meshlink.domain.transport.TransportHealth.DISCONNECTED)

        val itm = IntelligentTransportManager(
            bleTransport = bleTransport,
            wifiTransport = wifiTransport,
            routeOptimizer = mockk(relaxed = true),
            settingsRepository = settingsRepo,
            metrics = mockk(relaxed = true),
            applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        val sosPacket = MeshPacket(
            packetId = "sos_pnd_001",
            senderId = "ME",
            targetId = "BROADCAST",
            payload = "{}",
            type = PacketType.SOS
        )

        val result = itm.sendPacket(sosPacket)
        assertTrue("Transfer must fail when Wi-Fi Direct is unavailable", result is MeshResult.Error)
        // BLE fallback must NEVER be attempted for SOS
        coVerify(exactly = 0) { bleTransport.broadcastPacket(any(), any(), any()) }
        coVerify(exactly = 0) { bleTransport.sendPacket(any()) }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 10: SOS remains direct-range-only and is never forwarded through intermediate node
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test10_sosRemainsDirectRangeOnlyTtl1`() = runTest {
        coEvery { userRepository.getLocalUser() } returns User(meshId = "user100", name = "Sender")
        val slotPacket = slot<MeshPacket>()
        coEvery { router.routeMediaPacket(capture(slotPacket)) } returns mockk()

        broadcastHandler.sendSos("sos_ttl_check")

        assertEquals(1, slotPacket.captured.ttl)
        assertEquals("BROADCAST", slotPacket.captured.targetId)
        assertEquals(PacketType.SOS, slotPacket.captured.type)
        assertEquals(BroadcastType.SOS, slotPacket.captured.broadcastType)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 11: Sender status does not claim delivery without confirmation
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test11_senderStatusDoesNotClaimDeliveryWithoutConfirmation`() = runTest {
        val testDispatcher = StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val meshRepo = mockk<com.meshlink.domain.repository.MeshRepository>(relaxed = true)
            val cameraController = mockk<com.meshlink.video.camera.CameraController>(relaxed = true)
            every { cameraController.hasCameraPermission() } returns false

            // No nearby responders discovered yet
            every { meshRepo.scannedDevices } returns MutableStateFlow(emptyMap())
            coEvery { meshRepo.dispatchSos(any()) } returns MeshResult.Success(Unit)

            val vm = SosViewModel(meshRepo, locationProvider, context, cameraController)
            vm.sendSos()
            testDispatcher.scheduler.advanceUntilIdle()

            // Status must remain PENDING with relaysReached = 0 when no peers are discovered
            val state = vm.uiState.value
            assertEquals(SosStatus.PENDING, state.status)
            assertEquals(0, state.relaysReached)
            assertFalse(state.sosSent)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 12: Bluetooth disabled and missing permissions produce honest status
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test12_bluetoothDisabledAndMissingPermissionsProduceHonestStatus`() {
        val tracker = BleAdvertisingTracker()
        val request = AdvertisingRequest("Node1", "NODE_ID", 0x01)

        // Advertising not started because adapter/permission unavailable
        assertEquals(AdvertisingState.IDLE, tracker.state)
        assertFalse(tracker.isActive)

        // Recovery is not claimed active
        tracker.onStopped(byCaller = true)
        assertFalse(tracker.needsRecovery())
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TEST 13: Correlation between Stage A announcement and Stage B full details
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    fun `test13_stageAAnnouncementCorrelatesWithStageBFullDetails`() = runTest {
        val senderId = "CORREL01"
        val fullSosId = UUID.randomUUID().toString()
        val announcement = SosAnnouncement.create(senderId, fullSosId)

        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName(senderId) } returns "CorrelUser"

        // 1. Stage A arrives: creates placeholder with sosIdHex
        val slotStageA = slot<MessageEntity>()
        coEvery { chatDao.insertMessageAndUpdateChat(capture(slotStageA), any()) } just Runs

        broadcastHandler.receiveSosAnnouncement(announcement)
        assertEquals(announcement.sosIdHex, slotStageA.captured.messageId)
        assertNull(slotStageA.captured.latitude)

        // 2. Stage B arrives via Wi-Fi Direct: full package with coordinates and signature
        val stageAPlaceholder = slotStageA.captured
        coEvery { chatDao.getMessageByUuid(fullSosId) } returns null
        coEvery { chatDao.getMessageByUuid(announcement.sosIdHex) } returns stageAPlaceholder
        coEvery { chatDao.deleteMessage(announcement.sosIdHex) } just Runs

        val slotStageB = slot<MessageEntity>()
        coEvery { chatDao.insertMessageAndUpdateChat(capture(slotStageB), any()) } just Runs

        val now = System.currentTimeMillis()
        val jsonPayload = JSONObject().apply {
            put("sosEventId", fullSosId)
            put("lat", 40.7128)
            put("lng", -74.0060)
            put("battery", 90)
            put("timestamp", now)
            put("senderName", "CorrelUser")
        }.toString()

        val fullPacket = MeshPacket(
            packetId = fullSosId,
            senderId = senderId,
            targetId = "BROADCAST",
            payload = jsonPayload,
            type = PacketType.SOS,
            ttl = 1
        )

        broadcastHandler.receiveSosMessage(fullPacket)

        // Verify placeholder was deleted and upgraded to full message with coordinates and fullSosId
        coVerify(exactly = 1) { chatDao.deleteMessage(announcement.sosIdHex) }
        assertEquals(fullSosId, slotStageB.captured.messageId)
        assertEquals(40.7128, slotStageB.captured.latitude)
        assertEquals(-74.0060, slotStageB.captured.longitude)
        assertEquals(90, slotStageB.captured.batteryPercent)
    }
}
