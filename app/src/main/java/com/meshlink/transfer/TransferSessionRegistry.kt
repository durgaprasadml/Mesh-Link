package com.meshlink.transfer

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight, thread-safe session registry responsible for tracking active transfer sessions.
 * Does not own underlying file streams or memory buffers; provides query and registration interface.
 */
@Singleton
class TransferSessionRegistry @Inject constructor() {

    private val sessions = ConcurrentHashMap<String, TransferSession>()

    /**
     * Registers or updates an active transfer session.
     */
    fun registerSession(session: TransferSession) {
        sessions[session.transferId] = session
    }

    /**
     * Unregisters a transfer session when completed, cancelled, or failed.
     */
    fun unregisterSession(transferId: String): TransferSession? {
        return sessions.remove(transferId)
    }

    /**
     * Looks up an active transfer session by ID.
     */
    fun getSession(transferId: String): TransferSession? {
        return sessions[transferId]
    }

    /**
     * Returns a snapshot list of all registered active transfer sessions.
     */
    fun getActiveSessions(): List<TransferSession> {
        return sessions.values.toList()
    }

    /**
     * Returns the total count of active sessions.
     */
    fun getActiveCount(): Int {
        return sessions.size
    }

    /**
     * Checks if a session with the given ID is registered.
     */
    fun containsSession(transferId: String): Boolean {
        return sessions.containsKey(transferId)
    }

    /**
     * Clears all session entries from the registry.
     */
    fun clearAll() {
        sessions.clear()
    }
}
