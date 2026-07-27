package com.meshlink.ble.data.gatt

import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GattWriteQueueImpl @Inject constructor() : GattWriteQueue {
    private val pendingWrites = mutableListOf<PendingClientWrite>()
    private var activeWriteAddress: String? = null

    override fun enqueue(write: PendingClientWrite) {
        pendingWrites.add(write)
    }

    override fun dequeueReady(now: Long, isDeviceReady: (String) -> Boolean): PendingClientWrite? {
        val iterator = pendingWrites.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            if (now >= pending.nextAttemptTime && isDeviceReady(pending.address)) {
                iterator.remove()
                return pending
            }
        }
        return null
    }

    override fun removeActive(address: String): PendingClientWrite? {
        if (activeWriteAddress == address) {
            val index = pendingWrites.indexOfFirst { it.address == address }
            if (index != -1) {
                val removed = pendingWrites.removeAt(index)
                activeWriteAddress = null
                return removed
            }
            activeWriteAddress = null
        }
        return null
    }

    override fun dropAllForDevice(address: String): List<PendingClientWrite> {
        val dropped = pendingWrites.filter { it.address == address }
        pendingWrites.removeAll { it.address == address }
        if (activeWriteAddress == address) {
            activeWriteAddress = null
        }
        return dropped
    }

    override fun hasPendingForDevice(address: String): Boolean {
        return pendingWrites.any { it.address == address }
    }

    override fun getActiveWriteAddress(): String? {
        return activeWriteAddress
    }

    override fun setActiveWriteAddress(address: String?) {
        activeWriteAddress = address
    }

    override fun requeueWithBackoff(write: PendingClientWrite, maxRetries: Int, backoffBaseMs: Long): Long {
        write.retryCount++
        if (write.retryCount > maxRetries) {
            return -1L // Permanent failure
        }
        val backoff = backoffBaseMs * (1 shl write.retryCount.coerceAtMost(6))
        write.nextAttemptTime = System.currentTimeMillis() + backoff
        // Insert at the front so it gets retried soon, but after backoff
        pendingWrites.add(0, write)
        return backoff
    }

    override fun clear() {
        pendingWrites.forEach { BufferPool.returnBuffer(it.bytes) }
        pendingWrites.clear()
        activeWriteAddress = null
    }
}
