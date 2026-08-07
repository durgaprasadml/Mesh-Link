package com.meshlink.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MeshBackgroundServiceTest {

    @Test
    fun testServiceStates() {
        val states = MeshBackgroundService.ServiceState.values()
        assertEquals(5, states.size)
        assertEquals(MeshBackgroundService.ServiceState.STOPPED, MeshBackgroundService.ServiceState.valueOf("STOPPED"))
    }
}
