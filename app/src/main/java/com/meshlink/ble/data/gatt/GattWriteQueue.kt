package com.meshlink.ble.data.gatt

/**
 * Manages the queue of pending client writes to characteristic values.
 *
 * Responsibility: Enqueues writes, handles retry backoff, and tracks the active write.
 * Thread Ownership: Must be accessed from a synchronized context (e.g. via Mutex).
 * Lifecycle Ownership: Application scoped.
 * Dependencies: BufferPool (for returning buffers).
 */
interface GattWriteQueue {
    fun enqueue(write: PendingClientWrite)
    fun dequeueReady(now: Long, isDeviceReady: (String) -> Boolean): PendingClientWrite?
    fun removeActive(address: String): PendingClientWrite?
    fun dropAllForDevice(address: String): List<PendingClientWrite>
    fun hasPendingForDevice(address: String): Boolean
    fun getActiveWriteAddress(): String?
    fun setActiveWriteAddress(address: String?)
    fun requeueWithBackoff(write: PendingClientWrite, maxRetries: Int = 10, backoffBaseMs: Long = 50L): Long
    fun clear()
}
