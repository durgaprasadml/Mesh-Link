package com.meshlink.ble.data.gatt

import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton

data class PendingApplicationMessage(
    val address: String,
    val payload: ByteArray,
    val priority: Int = 0
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
        var highestPriorityIndex = -1
        var highestPriority = Int.MIN_VALUE
        for (i in pendingMessages.indices) {
            val pending = pendingMessages[i]
            if (pending.address == address) {
                if (pending.priority > highestPriority) {
                    highestPriority = pending.priority
                    highestPriorityIndex = i
                }
            }
        }
        return if (highestPriorityIndex != -1) {
            pendingMessages.removeAt(highestPriorityIndex)
        } else {
            null
        }
    }

    @Synchronized
    override fun dropAllForDevice(address: String): List<PendingApplicationMessage> {
        val dropped = pendingMessages.filter { it.address == address }
        pendingMessages.removeAll { it.address == address }
        dropped.forEach { BufferPool.returnBuffer(it.payload) }
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
