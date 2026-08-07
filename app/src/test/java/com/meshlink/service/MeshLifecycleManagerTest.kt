package com.meshlink.service

import android.content.Context
import com.meshlink.common.oem.OemCompatibilityManager
import com.meshlink.common.power.BatteryOptimizationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MeshLifecycleManagerTest {

    private lateinit var context: Context
    private lateinit var meshSupervisor: MeshSupervisor
    private lateinit var meshWatchdog: MeshWatchdog
    private lateinit var oemCompatibilityManager: OemCompatibilityManager
    private lateinit var batteryOptimizationManager: BatteryOptimizationManager
    private lateinit var meshSessionManager: MeshSessionManager

    private lateinit var manager: MeshLifecycleManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        meshSupervisor = mockk(relaxed = true)
        meshWatchdog = mockk(relaxed = true)
        oemCompatibilityManager = mockk(relaxed = true)
        batteryOptimizationManager = mockk(relaxed = true)
        meshSessionManager = mockk(relaxed = true)

        manager = MeshLifecycleManager(
            context,
            meshSupervisor,
            meshWatchdog,
            oemCompatibilityManager,
            batteryOptimizationManager,
            meshSessionManager
        )
    }

    @Test
    fun testLifecycleInitialState() {
        assertFalse(manager.isMeshRunning())
        assertFalse(manager.isPaused.value)
    }

    @Test
    fun testInitializeAfterOnboarding() {
        every { meshSupervisor.isFullyOperational() } returns true
        manager.initializeAfterOnboarding()

        assertTrue(manager.isMeshRunning())
        verify { meshSessionManager.restoreSession() }
        verify { meshSupervisor.forceRestartAllSubsystems() }
        verify { meshWatchdog.start() }
        assertTrue(manager.isFullyOperational())
    }

    @Test
    fun testForceInitialize() {
        manager.forceInitialize()

        assertTrue(manager.isMeshRunning())
        verify { meshSupervisor.forceRestartAllSubsystems() }
        verify { meshWatchdog.start() }
    }
}
