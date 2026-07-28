package com.meshlink.common.maintenance

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceSchedulerTest {

    @Test
    fun `scheduler executes task after interval`() = runTest {
        var currentTime = 0L
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = MaintenanceScheduler(testDispatcher, applicationScope = backgroundScope).apply {
            timeProvider = { currentTime }
        }
        
        val executionCount = AtomicInteger(0)
        
        scheduler.schedule("TestTask", 60_000L) {
            executionCount.incrementAndGet()
        }
        
        // Advance time to just before trigger
        currentTime += 59_000L
        advanceTimeBy(59_000L)
        assertEquals(0, executionCount.get())
        
        // Advance time past the 60s base interval
        currentTime += 2_000L
        advanceTimeBy(2_000L)
        assertEquals(1, executionCount.get())
        
        // Advance another 60s
        currentTime += 60_000L
        advanceTimeBy(60_000L)
        assertEquals(2, executionCount.get())
        
        scheduler.stop()
    }

    @Test
    fun `multiple tasks are processed correctly`() = runTest {
        var currentTime = 0L
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = MaintenanceScheduler(testDispatcher, applicationScope = backgroundScope).apply {
            timeProvider = { currentTime }
        }
        
        val task1Count = AtomicInteger(0)
        val task2Count = AtomicInteger(0)
        
        scheduler.schedule("Task1", 60_000L) { task1Count.incrementAndGet() }
        scheduler.schedule("Task2", 120_000L) { task2Count.incrementAndGet() }
        
        currentTime += 61_000L
        advanceTimeBy(61_000L)
        assertEquals(1, task1Count.get())
        assertEquals(0, task2Count.get()) // Task 2 needs 120s
        
        currentTime += 60_000L
        advanceTimeBy(60_000L)
        assertEquals(2, task1Count.get())
        assertEquals(1, task2Count.get())
        
        scheduler.stop()
    }
}
