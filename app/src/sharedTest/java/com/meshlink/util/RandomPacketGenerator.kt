package com.meshlink.util

import com.meshlink.ble.data.MeshPacket
import com.meshlink.ble.data.PacketType
import java.util.UUID

object RandomPacketGenerator {
    fun generatePacket(senderId: String = UUID.randomUUID().toString()): MeshPacket {
        return MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = senderId,
            targetId = "target_1",
            payload = "Payload test",
            type = PacketType.TEXT,
            encrypted = false
        )
    }
}
