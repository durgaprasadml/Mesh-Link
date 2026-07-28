package com.meshlink.ble.data.handlers

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MeshIdNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class AckManagerTest {

    private lateinit var userRepository: UserRepository
    private lateinit var chatDao: ChatDao
    private lateinit var packetDispatcher: PacketDispatcher
    private lateinit var ackManager: AckManager

    @Before
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        chatDao = mock(ChatDao::class.java)
        packetDispatcher = mock(PacketDispatcher::class.java)
        ackManager = AckManager(userRepository, chatDao, packetDispatcher)
    }

    @Test
    fun `handleDeliveryAck updates message status to DELIVERED`() = runBlocking {
        val packet = MeshPacket(
            packetId = "ack-id",
            senderId = "peer",
            targetId = "me",
            payload = "msg-123",
            type = PacketType.DELIVERY_ACK,
            encrypted = false
        )

        ackManager.handleDeliveryAck(packet)

        verify(chatDao).updateMessageStatus("msg-123", DeliveryStatus.DELIVERED)
    }

    @Test
    fun `handleReadReceipt updates message status to SEEN`() = runBlocking {
        val packet = MeshPacket(
            packetId = "rr-id",
            senderId = "peer",
            targetId = "me",
            payload = "msg-123",
            type = PacketType.READ_RECEIPT,
            encrypted = false
        )

        ackManager.handleReadReceipt(packet)

        verify(chatDao).updateMessageStatus("msg-123", DeliveryStatus.SEEN)
    }

    @Test
    fun `sendReadReceipts sends packets and updates local status`() = runBlocking {
        val localUser = User(meshId = "me")
        `when`(userRepository.getLocalUser()).thenReturn(localUser)
        `when`(chatDao.getUnreadIncomingMessages("peer")).thenReturn(listOf("msg-1", "msg-2"))

        ackManager.sendReadReceipts("peer")

        verify(chatDao).markMessagesAsSeen(listOf("msg-1", "msg-2"))
        
        val expectedTarget = MeshIdNormalizer.canonicalize("peer")
        verify(packetDispatcher).dispatchSinglePacket(
            org.mockito.kotlin.eq(expectedTarget),
            org.mockito.kotlin.check { pkt ->
                assert(pkt.payload == "msg-1")
                assert(pkt.type == PacketType.READ_RECEIPT)
            }
        )
        verify(packetDispatcher).dispatchSinglePacket(
            org.mockito.kotlin.eq(expectedTarget),
            org.mockito.kotlin.check { pkt ->
                assert(pkt.payload == "msg-2")
                assert(pkt.type == PacketType.READ_RECEIPT)
            }
        )
    }
}
