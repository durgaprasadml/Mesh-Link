package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransportPacketClassifierTest {

    private val classifier = TransportPacketClassifier()

    @Test
    fun `classify categorizes signaling and control packet types as CONTROL`() {
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.TEXT))
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.DELIVERY_ACK))
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.READ_RECEIPT))
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.MEDIA_ACK))
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.MEDIA_NACK))
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.LOCATION))
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.SOS))
        assertEquals(TransportCategory.CONTROL, classifier.classify(PacketType.BEACON))
    }

    @Test
    fun `classify categorizes high bandwidth media packet types as MEDIA`() {
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.MEDIA_CHUNK))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.MEDIA_META))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.VOICE_FRAME))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.VIDEO_FRAME))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.RESOURCE_SYNC))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.MAP_SYNC))
    }

    @Test
    fun `classify categorizes media MIME types as MEDIA`() {
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.TEXT, mimeType = "image/jpeg"))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.TEXT, mimeType = "video/mp4"))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.TEXT, mimeType = "audio/aac"))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.TEXT, mimeType = "application/pdf"))
        assertEquals(TransportCategory.MEDIA, classifier.classify(PacketType.TEXT, mimeType = "application/zip"))
    }

    @Test
    fun `classify categorizes large payloads exceeding threshold as MEDIA`() {
        val smallPacket = MeshPacket(
            senderId = "a", targetId = "b", payload = "Hello", type = PacketType.TEXT
        )
        val largeBytes = ByteArray(60_000) { 0x41 }
        val largePacket = MeshPacket(
            senderId = "a", targetId = "b", payload = String(largeBytes, Charsets.ISO_8859_1), type = PacketType.TEXT
        )

        assertEquals(TransportCategory.CONTROL, classifier.classify(smallPacket))
        assertEquals(TransportCategory.MEDIA, classifier.classify(largePacket))
    }
}
