package com.meshlink.transfer

import java.util.Collections
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retains recently finished (terminal) transfer identifiers and their terminal state.
 *
 * When a transfer reaches a terminal state (COMPLETED, FAILED, CANCELLED), its session is
 * removed from [TransferScheduler] to clean up resources and progress tracking.
 * However, late-arriving packets (e.g., duplicated BLE META or CHUNK packets, redundant
 * Wi-Fi Direct stream completion events, or retry races) would previously see a `null`
 * session and recreate an active "recovered_..." session, resurrecting completed transfers,
 * re-running assembly, triggering duplicate delivery acknowledgements, or overwriting
 * the database state back to RECEIVING.
 *
 * This registry acts as a bounded guard against duplicate/resurrected transfer events.
 */
@Singleton
class TerminalTransferRegistry(
    private val maxCapacity: Int
) {
    @Inject
    constructor() : this(maxCapacity = 512)
    private val entries: MutableMap<String, TerminalRecord> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, TerminalRecord>(maxCapacity, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TerminalRecord>?): Boolean {
                    return size > maxCapacity
                }
            }
        )

    data class TerminalRecord(
        val state: TransferState,
        val timestampMs: Long = System.currentTimeMillis()
    )

    fun record(transferId: String, state: TransferState) {
        if (!state.isTerminal()) return
        entries[transferId] = TerminalRecord(state)
    }

    fun getTerminalState(transferId: String): TransferState? = entries[transferId]?.state

    fun isTerminal(transferId: String): Boolean = entries.containsKey(transferId)

    fun isCompleted(transferId: String): Boolean =
        entries[transferId]?.state == TransferState.COMPLETED

    fun remove(transferId: String) {
        entries.remove(transferId)
    }

    fun clear() {
        entries.clear()
    }

    fun size(): Int = entries.size
}
