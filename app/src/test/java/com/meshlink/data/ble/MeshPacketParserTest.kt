package com.meshlink.data.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MeshPacketParserTest {

    @Test
    fun `test toJson and fromJson for standard text packet`() {
        val packet = MeshPacket(
            packetId = "12345",
            senderId = "sender1",
            targetId = "target1",
            payload = "hello world",
            type = PacketType.TEXT,
            encrypted = true,
            ttl = 5,
            hopCount = 2,
            visitedPath = mutableListOf("node1", "node2")
        )

        val jsonString = MeshPacketParser.toJson(packet)
        val parsedPacket = MeshPacketParser.fromJson(jsonString)

        assertNotNull("Parsed packet should not be null", parsedPacket)
        assertEquals("12345", parsedPacket?.packetId)
        assertEquals("sender1", parsedPacket?.senderId)
        assertEquals("target1", parsedPacket?.targetId)
        assertEquals("hello world", parsedPacket?.payload)
        assertEquals(PacketType.TEXT, parsedPacket?.type)
        assertEquals(true, parsedPacket?.encrypted)
        assertEquals(5, parsedPacket?.ttl)
        assertEquals(2, parsedPacket?.hopCount)
        assertEquals(listOf("node1", "node2"), parsedPacket?.visitedPath)
    }

    @Test
    fun `test toJson and fromJson for media chunk packet`() {
        val packet = MeshPacket(
            packetId = "media123",
            senderId = "sender1",
            targetId = "target1",
            payload = "base64data",
            type = PacketType.MEDIA_CHUNK,
            transferId = "transfer123",
            chunkIndex = 1,
            totalChunks = 10,
            mimeType = "image/jpeg",
            encrypted = false
        )

        val jsonString = MeshPacketParser.toJson(packet)
        val parsedPacket = MeshPacketParser.fromJson(jsonString)

        assertNotNull(parsedPacket)
        assertEquals(PacketType.MEDIA_CHUNK, parsedPacket?.type)
        assertEquals("transfer123", parsedPacket?.transferId)
        assertEquals(1, parsedPacket?.chunkIndex)
        assertEquals(10, parsedPacket?.totalChunks)
        assertEquals("image/jpeg", parsedPacket?.mimeType)
    }

    @Test
    fun `test fromJson with invalid json returns null`() {
        val invalidJson = "{ invalid json }"
        val parsedPacket = MeshPacketParser.fromJson(invalidJson)
        assertNull(parsedPacket)
    }
}
