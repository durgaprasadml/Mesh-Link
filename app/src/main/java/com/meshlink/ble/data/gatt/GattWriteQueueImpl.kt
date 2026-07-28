package com.meshlink.ble.data.gatt

import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GattWriteQueueImpl @Inject constructor() : GattWriteQueue {
    private val pendingWrites = mutableListOf<PendingClientWrite>()
    private val activeWrites = mutableMapOf<String, PendingClientWrite>()

    override fun enqueue(write: PendingClientWrite) {
        pendingWrites.add(write)
    }

    override fun dequeueReady(now: Long, isDeviceReady: (String) -> Boolean): PendingClientWrite? {
        val iterator = pendingWrites.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            if (now >= pending.nextAttemptTime && isDeviceReady(pending.address) && !activeWrites.containsKey(pending.address)) {
                iterator.remove()
                return pending
            }
        }
        return null
    }

    override fun removeActive(address: String): PendingClientWrite? {
        return activeWrites.remove(address)
    }

    override fun dropAllForDevice(address: String): List<PendingClientWrite> {
        val dropped = pendingWrites.filter { it.address == address }.toMutableList()
        pendingWrites.removeAll { it.address == address }
        val active = activeWrites.remove(address)
        if (active != null) {
            dropped.add(active)
        }
        return dropped
    }

    override fun hasPendingForDevice(address: String): Boolean {
        return pendingWrites.any { it.address == address } || activeWrites.containsKey(address)
    }

    override fun getActiveWriteAddress(): String? {
        // Return first active write address, mostly for backward compatibility. 
        // With multiple devices, there could be multiple active writes (one per device).
        // Android BLE stack allows concurrent writes to different devices usually, 
        // but often it's safer to have one active write overall for the local GATT client.
        return activeWrites.keys.firstOrNull()
    }

    override fun setActiveWrite(write: PendingClientWrite?) {
        if (write != null) {
            activeWrites[write.address] = write
        }
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
        activeWrites.values.forEach { BufferPool.returnBuffer(it.bytes) }
        activeWrites.clear()
    }
}
