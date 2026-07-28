package com.meshlink.security.data

import java.util.Arrays

/**
 * Owns the raw database key material.
 * Ensures the memory is securely wiped when closed.
 */
class SecureDatabaseKey(private val keyBytes: ByteArray) : AutoCloseable {

    fun getBytes(): ByteArray {
        check(!isDestroyed) { "Key has already been destroyed" }
        return keyBytes
    }

    var isDestroyed: Boolean = false
        private set

    override fun close() {
        if (!isDestroyed) {
            Arrays.fill(keyBytes, 0.toByte())
            isDestroyed = true
        }
    }
}
