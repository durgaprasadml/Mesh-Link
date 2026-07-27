package com.meshlink.common.util

import java.util.UUID

interface PacketIdGenerator {
    fun generateId(): String
}

class UuidPacketIdGenerator : PacketIdGenerator {
    override fun generateId(): String = UUID.randomUUID().toString()
}
