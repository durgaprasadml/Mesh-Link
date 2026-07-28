package com.meshlink.ble.data.gatt

import com.meshlink.common.pool.BufferPool

/**
 * Fragments a full payload byte array into smaller chunks respecting the given MTU.
 *
 * Responsibility: Generates START, CONT, END, and FULL fragments specific to BLE.
 * Thread Ownership: Thread-safe.
 * Lifecycle Ownership: Application scoped.
 * Dependencies: BufferPool.
 */
interface PacketFragmenter {
    /**
     * The callback is invoked for each fragment. Fragments are borrowed from BufferPool
     * and MUST be returned by the caller once processed/sent.
     */
    suspend fun fragment(data: ByteArray, mtu: Int, onFragment: suspend (ByteArray) -> Unit)
}
