package com.meshlink.database.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuditLogEntityTest {

    @Test
    fun `test audit log entity fields and equality`() {
        val log1 = AuditLogEntity(
            id = 1L,
            timestamp = 100L,
            peerId = "peer",
            eventName = "event",
            severity = 1,
            details = "details",
            actionTaken = "action"
        )
        val log2 = log1.copy()
        val log3 = log1.copy(id = 2L)
        val log4 = AuditLogEntity(
            timestamp = 100L,
            peerId = "peer",
            eventName = "event",
            severity = 1,
            details = "details",
            actionTaken = "action"
        )

        assertEquals(1L, log1.id)
        assertEquals("peer", log1.peerId)
        assertEquals(log1, log2)
        assertNotEquals(log1, log3)
        // log4 has default id 0
        assertEquals(0L, log4.id)
        assertNotEquals(log1, log4)
    }
}
