package com.meshlink.domain.model

import java.util.BitSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.withLock

enum class SessionState {
    CREATING,
    ACTIVE,
    REKEYING,
    EXPIRING,
    EXPIRED,
    DESTROYED
}

data class PeerSecureSession(
    val peerId: String,
    val sessionId: String,
    val fingerprint: String,
    val sessionStart: Long,
    val sessionVersion: Int,
    val cryptoVersion: Int = 1,
    val verified: Boolean,
    var lastActivity: Long,
    var state: SessionState = SessionState.ACTIVE,
    val packetCounter: AtomicLong = AtomicLong(0),
    val receiveCounter: AtomicLong = AtomicLong(0),
    var expirationTime: Long = sessionStart + 30 * 60 * 1000L,
    val replayWindow: BitSet = BitSet(64),
    var keyVersion: Int = 1,
    var previousKeyVersion: Int = 0,
    var rekeyTimestamp: Long = 0,
    var rotationReason: String = "",
    val totalEncryptedPackets: AtomicLong = AtomicLong(0),
    val totalDecryptedPackets: AtomicLong = AtomicLong(0),
    val processedPacketIds: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
) {
    fun updateActivity(now: Long) {
        lastActivity = now
        expirationTime = now + 30 * 60 * 1000L
        if (state == SessionState.EXPIRING) {
            state = SessionState.ACTIVE
        }
    }

    private val lock = java.util.concurrent.locks.ReentrantLock()

    /**
     * Enhanced Replay Protection Validation.
     * Checks both unique Packet ID and 64-packet sequence sliding window.
     */
    fun isReplay(sequence: Long, packetId: String? = null): Boolean {
        lock.withLock {
            if (packetId != null && processedPacketIds.containsKey(packetId)) {
                return true
            }

            if (sequence <= 0L) {
                return true
            }

            val highestReceived = receiveCounter.get()
            
            // Too old (outside window)
            if (highestReceived >= 64 && sequence <= highestReceived - 64) {
                return true
            }

            // Already received in window
            if (sequence <= highestReceived) {
                val index = Math.floorMod(sequence, 64L).toInt()
                if (replayWindow.get(index)) {
                    return true
                }
            }
            
            return false
        }
    }

    fun markReceived(sequence: Long, packetId: String? = null) {
        lock.withLock {
            if (packetId != null) {
                processedPacketIds[packetId] = System.currentTimeMillis()
                // Evict old packet IDs if map grows too large
                if (processedPacketIds.size > 200) {
                    val cutoff = System.currentTimeMillis() - 300_000L
                    processedPacketIds.entries.removeIf { it.value < cutoff }
                }
            }

            val highestReceived = receiveCounter.get()
            
            if (sequence > highestReceived) {
                val diff = sequence - highestReceived
                if (diff >= 64) {
                    replayWindow.clear()
                } else {
                    for (i in 1..diff) {
                        replayWindow.clear(Math.floorMod(highestReceived + i, 64L).toInt())
                    }
                }
                receiveCounter.set(sequence)
            }
            
            val index = Math.floorMod(sequence, 64L).toInt()
            replayWindow.set(index)
        }
    }
}
