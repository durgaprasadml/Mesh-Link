package com.meshlink.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MeshWatchdogTest {

    @Test
    fun testExponentialBackoffSchedule() {
        val schedule = listOf(1000L, 2000L, 5000L, 10000L, 20000L, 30000L, 60000L)
        assertEquals(7, schedule.size)
        assertEquals(1000L, schedule[0])
        assertEquals(60000L, schedule.last())
    }
}
