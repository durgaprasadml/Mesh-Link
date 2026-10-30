package com.meshlink.ble.data.gatt

import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GattWriteQueueImpl @Inject constructor() : GattWriteQueue {
    var clock: () -> Long = { System.currentTimeMillis() }
    private val pendingWrites = mutableListOf<PendingClientWrite>()
    private val activeWrites = mutableMapOf<String, PendingClientWrite>()
    private val activeWriteTimestamps = mutableMapOf<String, Long>()

    companion object {
        const val ACTIVE_WRITE_TIMEOUT_MS = 3_000L
    }

    override fun enqueue(write: PendingClientWrite) {
        pendingWrites.add(write)
    }

    private fun pruneExpiredActiveWrites(now: Long) {
        val iterator = activeWrites.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val startTime = activeWriteTimestamps[entry.key] ?: 0L
            if (now - startTime > ACTIVE_WRITE_TIMEOUT_MS) {
                MeshLogger.w("GattWriteQueue", "Active write timed out after ${now - startTime}ms for ${entry.key}, auto-clearing")
                BufferPool.returnBuffer(entry.value.bytes)
                activeWriteTimestamps.remove(entry.key)
                iterator.remove()
            }
        }
    }

    override fun dequeueReady(now: Long, isDeviceReady: (String) -> Boolean): PendingClientWrite? {
        pruneExpiredActiveWrites(now)
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
        activeWriteTimestamps.remove(address)
        return activeWrites.remove(address)
    }

    override fun dropAllForDevice(address: String): List<PendingClientWrite> {
        val dropped = pendingWrites.filter { it.address == address }.toMutableList()
        pendingWrites.removeAll { it.address == address }
        activeWriteTimestamps.remove(address)
        val active = activeWrites.remove(address)
        if (active != null) {
            dropped.add(active)
        }
        return dropped
    }

    override fun hasPendingForDevice(address: String): Boolean {
        pruneExpiredActiveWrites(clock())
        return pendingWrites.any { it.address == address } || activeWrites.containsKey(address)
    }

    override fun getActiveWriteAddress(): String? {
        pruneExpiredActiveWrites(clock())
        // Return first active write address, mostly for backward compatibility. 
        // With multiple devices, there could be multiple active writes (one per device).
        // Android BLE stack allows concurrent writes to different devices usually, 
        // but often it's safer to have one active write overall for the local GATT client.
        return activeWrites.keys.firstOrNull()
    }

    override fun setActiveWrite(write: PendingClientWrite?) {
        if (write != null) {
            activeWrites[write.address] = write
            activeWriteTimestamps[write.address] = clock()
        }
    }

    override fun clearIfActiveAndExpired(address: String, write: PendingClientWrite, timeoutMs: Long): PendingClientWrite? {
        val current = activeWrites[address]
        val startTime = activeWriteTimestamps[address] ?: 0L
        val now = clock()
        if (current === write || (current != null && current == write)) {
            if (now - startTime >= timeoutMs) {
                activeWriteTimestamps.remove(address)
                return activeWrites.remove(address)
            }
        }
        return null
    }

    override fun requeueWithBackoff(write: PendingClientWrite, maxRetries: Int, backoffBaseMs: Long): Long {
        write.retryCount++
        if (write.retryCount > maxRetries) {
            return -1L // Permanent failure
        }
        val backoff = backoffBaseMs * (1 shl write.retryCount.coerceAtMost(6))
        write.nextAttemptTime = clock() + backoff
        // Insert at the front so it gets retried soon, but after backoff
        pendingWrites.add(0, write)
        return backoff
    }

    override fun clear() {
        pendingWrites.forEach { BufferPool.returnBuffer(it.bytes) }
        pendingWrites.clear()
        activeWrites.values.forEach { BufferPool.returnBuffer(it.bytes) }
        activeWrites.clear()
        activeWriteTimestamps.clear()
    }
}
