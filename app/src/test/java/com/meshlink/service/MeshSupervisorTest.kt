package com.meshlink.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MeshSupervisorTest {

    @Test
    fun testRadioSubsystemValues() {
        val subsystems = RadioSubsystem.values()
        assertEquals(7, subsystems.size)
    }

    @Test
    fun testRadioStateTransitions() {
        val states = RadioState.values()
        assertNotNull(states)
        assertEquals(5, states.size)
        assertEquals(RadioState.RUNNING, RadioState.valueOf("RUNNING"))
    }
}
