package com.meshlink.ble.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BleRestartCoordinatorTest {

    private lateinit var coordinator: BleRestartCoordinator
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        coordinator = BleRestartCoordinator()
        testScope = TestScope(StandardTestDispatcher())
    }

    @Test
    fun `test successful restart transitions state to running and then idle`() = testScope.runTest {
        var actionExecuted = false
        coordinator.scheduleRestart(this, RestartComponent.SCANNER, BleException("test")) {
            actionExecuted = true
        }

        assertEquals(RestartState.SCHEDULED, coordinator.getState(RestartComponent.SCANNER))
        assertEquals(1, coordinator.getAttempts(RestartComponent.SCANNER))

        // Wait for base delay 2000 + jitter (0..1000) = max 3000
        advanceTimeBy(3500)
        
        assertTrue(actionExecuted)
        assertEquals(RestartState.IDLE, coordinator.getState(RestartComponent.SCANNER))
    }

    @Test
    fun `test non-retryable exception does not schedule restart`() = testScope.runTest {
        var actionExecuted = false
        val exception = BleNonRetryableException("test")
        
        coordinator.scheduleRestart(this, RestartComponent.SCANNER, exception) {
            actionExecuted = true
        }

        assertEquals(RestartState.IDLE, coordinator.getState(RestartComponent.SCANNER))
        assertEquals(0, coordinator.getAttempts(RestartComponent.SCANNER))
        
        advanceUntilIdle()
        assertFalse(actionExecuted)
    }

    @Test
    fun `test duplicate scheduling is ignored`() = testScope.runTest {
        var executionCount = 0
        coordinator.scheduleRestart(this, RestartComponent.SCANNER, BleException("test")) {
            executionCount++
        }
        
        // Attempt duplicate while scheduled
        coordinator.scheduleRestart(this, RestartComponent.SCANNER, BleException("test")) {
            executionCount++
        }

        assertEquals(1, coordinator.getAttempts(RestartComponent.SCANNER))
        
        advanceUntilIdle()
        
        assertEquals(1, executionCount)
    }

    @Test
    fun `test reset retry clears state and attempts`() = testScope.runTest {
        coordinator.scheduleRestart(this, RestartComponent.SCANNER, BleException("test")) { }
        
        assertEquals(RestartState.SCHEDULED, coordinator.getState(RestartComponent.SCANNER))
        assertEquals(1, coordinator.getAttempts(RestartComponent.SCANNER))
        
        coordinator.resetRetry(RestartComponent.SCANNER)
        
        assertEquals(RestartState.IDLE, coordinator.getState(RestartComponent.SCANNER))
        assertEquals(0, coordinator.getAttempts(RestartComponent.SCANNER))
    }

    @Test
    fun `test cancel restart stops execution`() = testScope.runTest {
        var actionExecuted = false
        coordinator.scheduleRestart(this, RestartComponent.ADVERTISER, BleException("test")) {
            actionExecuted = true
        }
        
        coordinator.cancelRestart(RestartComponent.ADVERTISER)
        advanceUntilIdle()
        
        assertFalse(actionExecuted)
        assertEquals(RestartState.IDLE, coordinator.getState(RestartComponent.ADVERTISER))
    }

    @Test
    fun `test retry limits are enforced`() = testScope.runTest {
        var executionCount = 0
        
        for (i in 1..15) {
            coordinator.scheduleRestart(this, RestartComponent.SCANNER, BleException("test")) {
                executionCount++
            }
            advanceUntilIdle()
        }
        
        // Max attempts is 10
        assertEquals(10, coordinator.getAttempts(RestartComponent.SCANNER))
        assertEquals(10, executionCount)
        assertEquals(RestartState.IDLE, coordinator.getState(RestartComponent.SCANNER))
    }
}
