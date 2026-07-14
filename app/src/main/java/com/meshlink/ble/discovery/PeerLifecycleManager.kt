package com.meshlink.ble.discovery

enum class PeerLifecycleState {
    UNKNOWN,
    DISCOVERED,
    VISIBLE,
    CONNECTING,
    CONNECTED,
    AUTHENTICATING,
    READY,
    LOST,
    DISCONNECTED,
    EXPIRED
}

/**
 * Enforces strict, deterministic transitions for a peer's lifecycle.
 */
object PeerLifecycleManager {

    /**
     * Attempts to transition a record to a new state.
     * Prevents invalid state jumps (e.g., DISCONNECTED -> AUTHENTICATING).
     */
    fun transition(record: PeerDiscoveryRecord, newState: PeerLifecycleState): Boolean {
        val current = record.state
        
        // Allowed transitions matrix
        val allowed = when (current) {
            PeerLifecycleState.UNKNOWN -> newState in listOf(PeerLifecycleState.DISCOVERED)
            PeerLifecycleState.DISCOVERED -> newState in listOf(PeerLifecycleState.VISIBLE, PeerLifecycleState.LOST)
            PeerLifecycleState.VISIBLE -> newState in listOf(PeerLifecycleState.CONNECTING, PeerLifecycleState.LOST, PeerLifecycleState.EXPIRED)
            PeerLifecycleState.CONNECTING -> newState in listOf(PeerLifecycleState.CONNECTED, PeerLifecycleState.DISCONNECTED)
            PeerLifecycleState.CONNECTED -> newState in listOf(PeerLifecycleState.AUTHENTICATING, PeerLifecycleState.DISCONNECTED)
            PeerLifecycleState.AUTHENTICATING -> newState in listOf(PeerLifecycleState.READY, PeerLifecycleState.DISCONNECTED)
            PeerLifecycleState.READY -> newState in listOf(PeerLifecycleState.DISCONNECTED)
            PeerLifecycleState.LOST -> newState in listOf(PeerLifecycleState.DISCOVERED, PeerLifecycleState.EXPIRED)
            PeerLifecycleState.DISCONNECTED -> newState in listOf(PeerLifecycleState.VISIBLE, PeerLifecycleState.CONNECTING)
            PeerLifecycleState.EXPIRED -> false // Terminal state until completely recreated
        }
        
        // We can force transitions back to DISCONNECTED/LOST from anywhere in error scenarios
        val isForceInterrupt = newState == PeerLifecycleState.DISCONNECTED || newState == PeerLifecycleState.LOST

        if (allowed || isForceInterrupt) {
            record.state = newState
            return true
        }
        
        return false
    }
}
