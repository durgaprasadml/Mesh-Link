package com.meshlink.common.util

object Validators {
    fun isValidPeerId(peerId: String): Boolean {
        return peerId.isNotBlank() && peerId.length >= 4
    }

    fun isValidPacketId(packetId: String): Boolean {
        return packetId.isNotBlank()
    }
}
