package com.meshlink.ble.data.gatt

/**
 * Represents a pending write request to a GATT client.
 */
data class PendingClientWrite(
    val address: String,
    val bytes: ByteArray,
    var retryCount: Int = 0,
    var nextAttemptTime: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PendingClientWrite

        if (address != other.address) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
