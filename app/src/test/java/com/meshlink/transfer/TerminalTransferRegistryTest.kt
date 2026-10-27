package com.meshlink.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalTransferRegistryTest {

    @Test
    fun `records only terminal states`() {
        val registry = TerminalTransferRegistry(maxCapacity = 10)

        registry.record("tx_sending", TransferState.SENDING)
        assertFalse(registry.isTerminal("tx_sending"))
        assertNull(registry.getTerminalState("tx_sending"))

        registry.record("tx_completed", TransferState.COMPLETED)
        assertTrue(registry.isTerminal("tx_completed"))
        assertTrue(registry.isCompleted("tx_completed"))
        assertEquals(TransferState.COMPLETED, registry.getTerminalState("tx_completed"))

        registry.record("tx_failed", TransferState.FAILED)
        assertTrue(registry.isTerminal("tx_failed"))
        assertFalse(registry.isCompleted("tx_failed"))
        assertEquals(TransferState.FAILED, registry.getTerminalState("tx_failed"))
    }

    @Test
    fun `evicts oldest entries beyond capacity`() {
        val registry = TerminalTransferRegistry(maxCapacity = 3)

        registry.record("tx1", TransferState.COMPLETED)
        registry.record("tx2", TransferState.FAILED)
        registry.record("tx3", TransferState.CANCELLED)
        registry.record("tx4", TransferState.COMPLETED)

        // Capacity is 3, tx1 should be evicted
        assertFalse(registry.isTerminal("tx1"))
        assertTrue(registry.isTerminal("tx2"))
        assertTrue(registry.isTerminal("tx3"))
        assertTrue(registry.isTerminal("tx4"))
    }

    @Test
    fun `remove and clear operate correctly`() {
        val registry = TerminalTransferRegistry(maxCapacity = 10)
        registry.record("tx1", TransferState.COMPLETED)
        assertTrue(registry.isTerminal("tx1"))

        registry.remove("tx1")
        assertFalse(registry.isTerminal("tx1"))

        registry.record("tx2", TransferState.COMPLETED)
        registry.clear()
        assertEquals(0, registry.size())
        assertFalse(registry.isTerminal("tx2"))
    }
}
