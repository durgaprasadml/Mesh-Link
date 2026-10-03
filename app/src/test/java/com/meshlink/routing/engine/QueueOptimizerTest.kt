package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class QueueOptimizerTest {

    private lateinit var queueOptimizer: QueueOptimizer

    @Before
    fun setUp() {
        queueOptimizer = QueueOptimizer()
    }

    @Test
    fun `MEDIA_ACK is dequeued before MEDIA_CHUNK when priorities are equal`() {
        val chunkPacket = MeshPacket(
            packetId = "chunk_1",
            senderId = "nodeA",
            targetId = "nodeB",
            payload = "chunk_data",
            type = PacketType.MEDIA_CHUNK,
            priority = PacketPriority.NORMAL
        )

        val ackPacket = MeshPacket(
            packetId = "ack_1",
            senderId = "nodeB",
            targetId = "nodeA",
            payload = "0",
            type = PacketType.MEDIA_ACK,
            priority = PacketPriority.NORMAL
        )

        queueOptimizer.enqueue(chunkPacket)
        queueOptimizer.enqueue(ackPacket)

        val firstOut = queueOptimizer.dequeue()
        assertNotNull(firstOut)
        assertEquals("ack_1", firstOut?.packetId)
        assertEquals(PacketType.MEDIA_ACK, firstOut?.type)

        val secondOut = queueOptimizer.dequeue()
        assertNotNull(secondOut)
        assertEquals("chunk_1", secondOut?.packetId)
    }

    @Test
    fun `SOS packets always have absolute precedence over media packets`() {
        val audioPacket = MeshPacket(
            packetId = "audio_1",
            senderId = "nodeA",
            targetId = "nodeB",
            payload = "audio_frame",
            type = PacketType.VOICE_FRAME,
            priority = PacketPriority.HIGH
        )

        val sosPacket = MeshPacket(
            packetId = "sos_1",
            senderId = "nodeA",
            targetId = "",
            payload = "HELP",
            type = PacketType.SOS,
            priority = PacketPriority.CRITICAL
        )

        queueOptimizer.enqueue(audioPacket)
        queueOptimizer.enqueue(sosPacket)

        val first = queueOptimizer.dequeue()
        assertEquals("sos_1", first?.packetId)
    }

    @Test
    fun `HIGH priority audio chunks are dequeued before NORMAL priority image chunks`() {
        val imageChunk = MeshPacket(
            packetId = "img_chunk",
            senderId = "nodeA",
            targetId = "nodeB",
            payload = "img_data",
            type = PacketType.MEDIA_CHUNK,
            priority = PacketPriority.NORMAL
        )

        val voiceChunk = MeshPacket(
            packetId = "voice_chunk",
            senderId = "nodeA",
            targetId = "nodeB",
            payload = "voice_data",
            type = PacketType.MEDIA_CHUNK,
            priority = PacketPriority.HIGH
        )

        queueOptimizer.enqueue(imageChunk)
        queueOptimizer.enqueue(voiceChunk)

        val first = queueOptimizer.dequeue()
        assertEquals("voice_chunk", first?.packetId)

        val second = queueOptimizer.dequeue()
        assertEquals("img_chunk", second?.packetId)
    }
}
