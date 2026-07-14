package com.meshlink.database.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RelayPacketEntityTest {

    @Test
    fun `test relay packet entity default values`() {
        val packet = RelayPacketEntity(
            packetId = "packet_1",
            senderId = "user_1",
            targetId = "user_2",
            payload = "payload",
            type = "type_a",
            ttl = 3,
            hopCount = 1,
            encrypted = true
        )
        
        assertEquals(null, packet.transferId)
        assertEquals(0, packet.chunkIndex)
        assertEquals(0, packet.totalChunks)
        assertEquals(null, packet.mimeType)
    }

    @Test
    fun `test relay packet entity equality`() {
        val packet1 = RelayPacketEntity("p1", "s1", "t1", "payload", "text", "NORMAL", "NONE", 1000L, 2000L, 3, 1, true, null, 0, 0, null)
        val packet2 = RelayPacketEntity("p1", "s1", "t1", "payload", "text", "NORMAL", "NONE", 1000L, 2000L, 3, 1, true, null, 0, 0, null)
        val packet3 = RelayPacketEntity("p2", "s1", "t1", "payload", "text", "NORMAL", "NONE", 1000L, 2000L, 3, 1, true, null, 0, 0, null)

        assertEquals(packet1, packet2)
        assertEquals(packet1.hashCode(), packet2.hashCode())
        assertNotEquals(packet1, packet3)
    }
}
