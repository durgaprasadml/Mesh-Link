package com.meshlink.ble.data.handlers

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.messaging.data.DeliveryTracker
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coVerify

class AckManagerTest {

    private lateinit var packetDispatcher: PacketDispatcher
    private lateinit var chatDao: ChatDao
    private lateinit var userRepository: UserRepository
    private lateinit var deliveryTracker: DeliveryTracker
    private lateinit var ackManager: AckManager

    @Before
    fun setUp() {
        packetDispatcher = mockk(relaxed = true)
        chatDao = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        deliveryTracker = mockk(relaxed = true)

        ackManager = AckManager(
            packetDispatcher = packetDispatcher,
            chatDao = chatDao,
            userRepository = userRepository,
            deliveryTracker = deliveryTracker
        )
    }

    @Test
    fun `handleDeliveryAck updates delivery tracker`() = runBlocking {
        val packet = MeshPacket(
            packetId = "p1",
            senderId = "peer",
            targetId = "local",
            payload = "original-packet",
            type = PacketType.DELIVERY_ACK
        )

        ackManager.handleDeliveryAck(packet)

        coVerify { deliveryTracker.onAckReceived("original-packet") }
    }

    @Test
    fun `handleReadReceipt updates delivery tracker`() = runBlocking {
        val packet = MeshPacket(
            packetId = "p2",
            senderId = "peer",
            targetId = "local",
            payload = "original-packet",
            type = PacketType.READ_RECEIPT
        )

        ackManager.handleReadReceipt(packet)

        coVerify { deliveryTracker.onReadReceiptReceived("original-packet") }
    }

    @Test
    fun `sendReadReceipts processes all unread messages`() = runBlocking {
        val localUser = User(meshId = "local", name = "Test User")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { chatDao.getUnreadIncomingMessages("peer") } returns listOf("msg-1", "msg-2")

        ackManager.sendReadReceipts("peer")

        coVerify { chatDao.markMessagesAsSeen(listOf("msg-1", "msg-2")) }
        
        val expectedTarget = MeshIdNormalizer.canonicalize("peer")
        coVerify {
            packetDispatcher.dispatchSinglePacket(
                expectedTarget,
                match { pkt: MeshPacket ->
                    pkt.payload == "msg-1" && pkt.type == PacketType.READ_RECEIPT
                }
            )
        }
        coVerify {
            packetDispatcher.dispatchSinglePacket(
                expectedTarget,
                match { pkt: MeshPacket ->
                    pkt.payload == "msg-2" && pkt.type == PacketType.READ_RECEIPT
                }
            )
        }
    }
}
