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
        mockkObject(com.meshlink.util.NotificationHelper)
        every { com.meshlink.util.NotificationHelper.showMessageNotification(any(), any(), any(), any()) } just Runs

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
    fun `receiveBroadcastTextMessage extracts senderName and updates userDao`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userDao.getUser("peer_rahul") } returns null

        val payload = JSONObject().apply {
            put("text", "Need first aid near Gate 2")
            put("senderName", "Rahul")
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val packet = MeshPacket(
            packetId = "p100",
            senderId = "peer_rahul",
            targetId = "BROADCAST",
            payload = payload,
            type = PacketType.TEXT
        )

        broadcastHandler.receiveBroadcastTextMessage(packet)

        val slotUser = slot<UserEntity>()
        coVerify { userDao.insertUser(capture(slotUser)) }
        assertEquals("peer_rahul", slotUser.captured.meshId)
        assertEquals("Rahul", slotUser.captured.name)

        val slotMsg = slot<MessageEntity>()
        coVerify { chatDao.insertMessage(capture(slotMsg)) }
        assertEquals("Need first aid near Gate 2", slotMsg.captured.text)

        verify { com.meshlink.util.NotificationHelper.showMessageNotification(context, "peer_rahul", "📢 Broadcast from Rahul", "Need first aid near Gate 2") }
    }

    @Test
    fun `receiveBroadcastTextMessage falls back to Unknown User when senderName is missing`() = runTest {
        coEvery { chatDao.getMessageByUuid(any()) } returns null
        coEvery { userDao.getUser("peer_anon") } returns null

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

        val slotMsg = slot<MessageEntity>()
        coVerify { chatDao.insertMessage(capture(slotMsg)) }
        assertEquals("Emergency alert", slotMsg.captured.text)

        verify { com.meshlink.util.NotificationHelper.showMessageNotification(context, "peer_anon", "📢 Broadcast from Unknown User", "Emergency alert") }
    }
}
