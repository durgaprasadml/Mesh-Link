package com.meshlink.ble.data.gatt

import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton

data class PendingApplicationMessage(
    val address: String,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PendingApplicationMessage

        if (address != other.address) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

interface ApplicationMessageQueue {
    fun enqueue(message: PendingApplicationMessage)
    fun dequeueReady(address: String): PendingApplicationMessage?
    fun dropAllForDevice(address: String): List<PendingApplicationMessage>
    fun hasPendingForDevice(address: String): Boolean
    fun clear()
}

@Singleton
class ApplicationMessageQueueImpl @Inject constructor() : ApplicationMessageQueue {
    private val pendingMessages = mutableListOf<PendingApplicationMessage>()

    @Synchronized
    override fun enqueue(message: PendingApplicationMessage) {
        pendingMessages.add(message)
    }

    @Synchronized
    override fun dequeueReady(address: String): PendingApplicationMessage? {
        val iterator = pendingMessages.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            if (pending.address == address) {
                iterator.remove()
                return pending
            }
        }
        return null
    }

    @Synchronized
    override fun dropAllForDevice(address: String): List<PendingApplicationMessage> {
        val dropped = pendingMessages.filter { it.address == address }
        pendingMessages.removeAll { it.address == address }
        return dropped
    }

    @Synchronized
    override fun hasPendingForDevice(address: String): Boolean {
        return pendingMessages.any { it.address == address }
    }

    @Synchronized
    override fun clear() {
        pendingMessages.forEach { BufferPool.returnBuffer(it.payload) }
        pendingMessages.clear()
    }
}
