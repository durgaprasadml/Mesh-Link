package com.meshlink.ble.data.handlers

import android.content.Context
import com.meshlink.data.location.LocationProvider
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.UserDao
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BroadcastHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val chatDao = mockk<ChatDao>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val locationProvider = mockk<LocationProvider>(relaxed = true)
    private val router = mockk<Router>(relaxed = true)

    private lateinit var broadcastHandler: BroadcastHandler

    @Before
    fun setUp() {
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any<ByteArray>(), any()) } answers {
            val bytes = arg<ByteArray>(0)
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val str = arg<String>(0)
            java.util.Base64.getDecoder().decode(str)
        }

        mockkObject(com.meshlink.util.NotificationHelper)
        every { com.meshlink.util.NotificationHelper.showMessageNotification(any(), any(), any(), any()) } just Runs
        every { com.meshlink.util.NotificationHelper.showSosNotification(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        broadcastHandler = BroadcastHandler(
            context = context,
            userRepository = userRepository,
            chatDao = chatDao,
            userDao = userDao,
            locationProvider = locationProvider,
            router = router
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `broadcastMessage creates packet with senderName and clean text`() = runTest {
        val user = User(meshId = "user123", name = "Durga Prasad")
        coEvery { userRepository.getLocalUser() } returns user

        val slotPacket = slot<MeshPacket>()
        coEvery { router.routeMediaPacket(capture(slotPacket)) } returns mockk()

        broadcastHandler.broadcastMessage("Hello everyone")

        val packet = slotPacket.captured
        assertEquals("BROADCAST", packet.targetId)
        assertEquals(PacketType.TEXT, packet.type)

        val json = JSONObject(packet.payload)
        assertEquals("Hello everyone", json.getString("text"))
        assertEquals("Durga Prasad", json.getString("senderName"))

        val slotEntity = slot<MessageEntity>()
        coEvery { chatDao.insertMessage(capture(slotEntity)) } returns Unit
    }

    @Test
    fun `receiveBroadcastTextMessage extracts senderName and updates userRepository`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName("peer_rahul") } returns "Rahul"

        val expectedTimestamp = 1700000000000L
        val payload = JSONObject().apply {
            put("text", "Need first aid near Gate 2")
            put("senderName", "Rahul")
            put("timestamp", expectedTimestamp)
        }.toString()

        val packet = MeshPacket(
            packetId = "p100",
            senderId = "peer_rahul",
            targetId = "BROADCAST",
            payload = payload,
            type = PacketType.TEXT
        )

        broadcastHandler.receiveBroadcastTextMessage(packet)

        coVerify { userRepository.saveOrUpdatePeerProfile(eq("peer_rahul"), eq("Rahul"), any(), any(), any()) }

        val slotMsg = slot<MessageEntity>()
        coVerify { chatDao.insertMessage(capture(slotMsg)) }
        assertEquals("Need first aid near Gate 2", slotMsg.captured.text)
        assertEquals(expectedTimestamp, slotMsg.captured.timestamp)

        verify { com.meshlink.util.NotificationHelper.showMessageNotification(context, "peer_rahul", "📢 Broadcast from Rahul", "Need first aid near Gate 2") }
    }

    @Test
    fun `receiveBroadcastTextMessage falls back to Mesh Peer when senderName is missing`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName("peer_anon") } returns "Mesh Peer"

        val payload = JSONObject().apply {
            put("text", "Emergency alert")
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val packet = MeshPacket(
            packetId = "p101",
            senderId = "peer_anon",
            targetId = "BROADCAST",
            payload = payload,
            type = PacketType.TEXT
        )

        broadcastHandler.receiveBroadcastTextMessage(packet)

        coVerify(exactly = 0) { userRepository.saveOrUpdatePeerProfile(any(), any(), any(), any(), any()) }

        val slotMsg = slot<MessageEntity>()
        coVerify { chatDao.insertMessage(capture(slotMsg)) }
        assertEquals("Emergency alert", slotMsg.captured.text)

        verify { com.meshlink.util.NotificationHelper.showMessageNotification(context, "peer_anon", "📢 Broadcast from Mesh Peer", "Emergency alert") }
    }

    @Test
    fun `receiveBroadcastTextMessage does not persist generic senderName`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName("peer_anon") } returns "Mesh Peer"

        val payload = JSONObject().apply {
            put("text", "Generic alert")
            put("senderName", "Unknown User")
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val packet = MeshPacket(
            packetId = "p102",
            senderId = "peer_anon",
            targetId = "BROADCAST",
            payload = payload,
            type = PacketType.TEXT
        )

        broadcastHandler.receiveBroadcastTextMessage(packet)

        coVerify(exactly = 0) { userRepository.saveOrUpdatePeerProfile(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `receiveBroadcastTextMessage suppresses replay with mutated packetId`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName("peer_rahul") } returns "Rahul"

        val now = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("text", "Evacuate sector B")
            put("senderName", "Rahul")
            put("timestamp", now)
        }.toString()

        val firstPacket = MeshPacket(
            packetId = "p200_orig",
            senderId = "peer_rahul",
            targetId = "BROADCAST",
            payload = payload,
            type = PacketType.TEXT
        )
        val replayedPacket = firstPacket.copy(packetId = "p200_replayed")

        broadcastHandler.receiveBroadcastTextMessage(firstPacket)
        broadcastHandler.receiveBroadcastTextMessage(replayedPacket)

        coVerify(exactly = 1) { chatDao.insertMessage(any()) }
    }

    @Test
    fun `receiveBroadcastTextMessage rejects stale timestamp replay after newer broadcast from same sender`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName("peer_rahul") } returns "Rahul"

        val now = System.currentTimeMillis()
        val freshPayload = JSONObject().apply {
            put("text", "Current alert")
            put("senderName", "Rahul")
            put("timestamp", now)
        }.toString()

        val staleTimestamp = now - (BroadcastHandler.BROADCAST_REPLAY_WINDOW_MS + 60_000L)
        val stalePayload = JSONObject().apply {
            put("text", "Old captured alert")
            put("senderName", "Rahul")
            put("timestamp", staleTimestamp)
        }.toString()

        broadcastHandler.receiveBroadcastTextMessage(
            MeshPacket(packetId = "p300_fresh", senderId = "peer_rahul", targetId = "BROADCAST", payload = freshPayload, type = PacketType.TEXT)
        )
        broadcastHandler.receiveBroadcastTextMessage(
            MeshPacket(packetId = "p301_stale", senderId = "peer_rahul", targetId = "BROADCAST", payload = stalePayload, type = PacketType.TEXT)
        )

        coVerify(exactly = 1) { chatDao.insertMessage(match { it.messageId == "p300_fresh" }) }
        coVerify(exactly = 0) { chatDao.insertMessage(match { it.messageId == "p301_stale" }) }
    }

    @Test
    fun `receiveSosMessage suppresses replayed SOS event with mutated packetId`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userRepository.getUserDisplayName("peer_sos") } returns "Anita"

        val now = System.currentTimeMillis()
        val sosPayload = JSONObject().apply {
            put("sosEventId", "sos_evt_999")
            put("lat", 12.9716)
            put("lng", 77.5946)
            put("battery", 42)
            put("timestamp", now)
            put("senderName", "Anita")
        }.toString()

        val firstSos = MeshPacket(
            packetId = "sos_pkt_1",
            senderId = "peer_sos",
            targetId = "BROADCAST",
            payload = sosPayload,
            type = PacketType.SOS
        )
        val replayedSos = firstSos.copy(packetId = "sos_pkt_2_mutated")

        broadcastHandler.receiveSosMessage(firstSos)
        broadcastHandler.receiveSosMessage(replayedSos)

        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(any(), any()) }
        verify(exactly = 1) {
            com.meshlink.util.NotificationHelper.showSosNotification(
                context = context,
                sosEventId = "sos_evt_999",
                senderId = "peer_sos",
                senderName = "Anita",
                alertText = any(),
                latitude = 12.9716,
                longitude = 77.5946,
                batteryPercent = 42
            )
        }
    }

    @Test
    fun `receiveSosMessage updates placeholder and triggers showSosNotification`() = runTest {
        val existingPlaceholder = MessageEntity(
            messageId = "sos_placeholder_1",
            chatId = "peer_sos",
            senderId = "peer_sos",
            text = "Placeholder",
            timestamp = 1000L,
            isFromMe = false,
            status = com.meshlink.database.data.local.DeliveryStatus.DELIVERED,
            messageType = com.meshlink.database.data.local.MessageType.SOS,
            latitude = null,
            longitude = null,
            batteryPercent = null
        )
        coEvery { chatDao.getMessageByUuid("sos_placeholder_1") } returns existingPlaceholder
        coEvery { userRepository.getUserDisplayName("peer_sos") } returns "Anita"

        val now = System.currentTimeMillis()
        val sosPayload = JSONObject().apply {
            put("sosEventId", "sos_placeholder_1")
            put("lat", 37.7749)
            put("lng", -122.4194)
            put("battery", 85)
            put("timestamp", now)
            put("senderName", "Anita")
        }.toString()

        val packet = MeshPacket(
            packetId = "sos_pkt_placeholder",
            senderId = "peer_sos",
            targetId = "BROADCAST",
            payload = sosPayload,
            type = PacketType.SOS
        )

        broadcastHandler.receiveSosMessage(packet)

        val slotMsg = slot<MessageEntity>()
        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(capture(slotMsg), "🚨 Anita") }
        assertEquals(37.7749, slotMsg.captured.latitude)
        assertEquals(-122.4194, slotMsg.captured.longitude)
        assertEquals(85, slotMsg.captured.batteryPercent)

        verify(exactly = 1) {
            com.meshlink.util.NotificationHelper.showSosNotification(
                context = context,
                sosEventId = "sos_placeholder_1",
                senderId = "peer_sos",
                senderName = "Anita",
                alertText = any(),
                latitude = 37.7749,
                longitude = -122.4194,
                batteryPercent = 85
            )
        }
    }

    @Test
    fun `sendSos respects shareLiveLocation false and uses persisted emergencyTemplate`() = runTest {
        val settingsRepository = mockk<com.meshlink.domain.repository.SettingsRepository>(relaxed = true)
        every { settingsRepository.shareLiveLocation } returns kotlinx.coroutines.flow.flowOf(false)
        every { settingsRepository.emergencyTemplate } returns kotlinx.coroutines.flow.flowOf("Trapped on 3rd floor")
        coEvery { userRepository.getLocalUser() } returns User(meshId = "user123", name = "Durga")

        val handlerWithSettings = BroadcastHandler(
            context = context,
            userRepository = userRepository,
            chatDao = chatDao,
            userDao = userDao,
            locationProvider = locationProvider,
            router = router,
            settingsRepository = settingsRepository
        )

        val slotPacket = slot<MeshPacket>()
        coEvery { router.routeMediaPacket(capture(slotPacket)) } returns mockk()

        handlerWithSettings.sendSos("sos_test_1")

        coVerify(exactly = 0) { locationProvider.getCurrentLocation() }
        val sosJson = JSONObject(slotPacket.captured.payload)
        assertEquals("Trapped on 3rd floor", sosJson.getString("message"))
        assertEquals(0.0, sosJson.getDouble("lat"), 0.0001)
        assertEquals(0.0, sosJson.getDouble("lng"), 0.0001)
        assertEquals(1, slotPacket.captured.ttl)
    }

    @Test
    fun `broadcastMessage enforces direct-range only ttl of 1`() = runTest {
        coEvery { userRepository.getLocalUser() } returns User(meshId = "user123", name = "Durga")
        val slotPacket = slot<MeshPacket>()
        coEvery { router.routeMediaPacket(capture(slotPacket)) } returns mockk()

        broadcastHandler.broadcastMessage("Direct range alert")

        assertEquals(1, slotPacket.captured.ttl)
        assertEquals("BROADCAST", slotPacket.captured.targetId)
    }

    @Test
    fun `broadcastMessage rejects oversized payload exceeding limit without silent truncation`() = runTest {
        coEvery { userRepository.getLocalUser() } returns User(meshId = "user123", name = "Durga")
        val massiveText = "A".repeat(1000)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                broadcastHandler.broadcastMessage(massiveText)
            }
        }
    }

    @Test
    fun `sendSos signs payload with cryptoManager and verifies signature on receive`() = runTest {
        val cryptoManager = mockk<com.meshlink.security.data.MeshCryptoManager>(relaxed = true)
        every { cryptoManager.getOrCreateSigningKey() } returns "mock_signing_key_pub"
        every { cryptoManager.sign(any()) } returns "mock_sig".toByteArray()
        every { cryptoManager.verifySignature(any(), any(), any()) } returns true
        coEvery { userRepository.getLocalUser() } returns User(meshId = "user_sec", name = "Alice")

        val handlerWithCrypto = BroadcastHandler(
            context = context,
            userRepository = userRepository,
            chatDao = chatDao,
            userDao = userDao,
            locationProvider = locationProvider,
            router = router,
            cryptoManager = cryptoManager
        )

        val slotPacket = slot<MeshPacket>()
        coEvery { router.routeMediaPacket(capture(slotPacket)) } returns mockk()

        handlerWithCrypto.sendSos("sos_sec_001")

        val captured = slotPacket.captured
        assertEquals(1, captured.ttl)
        val json = JSONObject(captured.payload)
        assertEquals("mock_signing_key_pub", json.getString("signingPubKey"))
        assertTrue(json.has("signature"))

        // Receive verified SOS
        coEvery { chatDao.getMessageByUuid("sos_sec_001") } returns null
        coEvery { userRepository.getUserDisplayName("user_sec") } returns "Alice"
        handlerWithCrypto.receiveSosMessage(captured)
        coVerify(exactly = 1) { chatDao.insertMessageAndUpdateChat(any(), any()) }

        // If signature is invalid, drops packet
        every { cryptoManager.verifySignature(any(), any(), any()) } returns false
        handlerWithCrypto.receiveSosMessage(captured.copy(packetId = "sos_sec_002"))
        coVerify(exactly = 0) { chatDao.insertMessageAndUpdateChat(match { it.messageId == "sos_sec_002" }, any()) }
    }
}
