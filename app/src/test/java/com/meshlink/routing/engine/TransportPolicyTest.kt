package com.meshlink.routing.engine

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportPolicyTest {

    private val classifier = TransportPacketClassifier()
    private val policy = TransportPolicy(classifier)

    @Test
    fun `getPreferredTransport maps CONTROL to BLE and MEDIA to WIFI_DIRECT`() {
        assertEquals(RouteType.BLE, policy.getPreferredTransport(TransportCategory.CONTROL))
        assertEquals(RouteType.WIFI_DIRECT, policy.getPreferredTransport(TransportCategory.MEDIA))
    }

    @Test
    fun `shouldAllowBleFallback allows small payloads and denies large payloads`() {
        val smallTextPacket = MeshPacket(
            senderId = "a", targetId = "b", payload = "Text", type = PacketType.TEXT
        )
        val smallMediaChunk = MeshPacket(
            senderId = "a", targetId = "b", payload = "ChunkData", type = PacketType.MEDIA_CHUNK
        )
        val largeBytes = ByteArray(60_000) { 0x01 }
        val largePacket = MeshPacket(
            senderId = "a", targetId = "b", payload = String(largeBytes, Charsets.ISO_8859_1), type = PacketType.TEXT
        )

        assertTrue(policy.shouldAllowBleFallback(smallTextPacket, TransportCategory.CONTROL))
        assertTrue(policy.shouldAllowBleFallback(smallMediaChunk, TransportCategory.MEDIA))
        assertFalse(policy.shouldAllowBleFallback(largePacket, TransportCategory.MEDIA))
    }
}
