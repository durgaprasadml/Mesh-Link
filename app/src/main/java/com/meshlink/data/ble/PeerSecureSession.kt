package com.meshlink.data.ble

import java.util.BitSet
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

data class PeerSecureSession(
    val peerId: String,
    val sessionId: String = UUID.randomUUID().toString(),
    val fingerprint: String,
    val sessionStart: Long,
    val sessionVersion: Int,
    val verified: Boolean,
    var lastActivity: Long,
    val packetCounter: AtomicLong = AtomicLong(0),
    val receiveCounter: AtomicLong = AtomicLong(0),
    var expirationTime: Long = sessionStart + 30 * 60 * 1000L,
    val replayWindow: BitSet = BitSet(64),
    var keyVersion: Int = 1,
    var previousKeyVersion: Int = 0,
    var rekeyTimestamp: Long = 0,
    var rotationReason: String = "",
    val totalEncryptedPackets: AtomicLong = AtomicLong(0),
    val totalDecryptedPackets: AtomicLong = AtomicLong(0)
) {
    fun updateActivity(now: Long) {
        lastActivity = now
        expirationTime = now + 30 * 60 * 1000L
    }

    /**
     * Replay protection validation.
     * Sliding window of 64 packets.
     */
    fun isReplay(sequence: Long): Boolean {
        synchronized(replayWindow) {
            val highestReceived = receiveCounter.get()
            
            // Too old (outside window)
            if (sequence <= highestReceived - 64) {
                return true
            }

            // Already received in window
            if (sequence <= highestReceived) {
                val index = (sequence % 64).toInt()
                if (replayWindow.get(index)) {
                    return true
                }
            }
            
            return false
        }
    }

    fun markReceived(sequence: Long) {
        synchronized(replayWindow) {
            val highestReceived = receiveCounter.get()
            
            if (sequence > highestReceived) {
                // Shift window by clearing bits for skipped sequence numbers
                val diff = sequence - highestReceived
                if (diff >= 64) {
                    replayWindow.clear()
                } else {
                    for (i in 1..diff) {
                        replayWindow.clear(((highestReceived + i) % 64).toInt())
                    }
                }
                receiveCounter.set(sequence)
            }
            
            // Mark this packet as received
            val index = (sequence % 64).toInt()
            replayWindow.set(index)
        }
    }
}
