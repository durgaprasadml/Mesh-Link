package com.meshlink.transfer

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineCancellationTest {

    @Test
    fun `test supervisor job prevents child failure from cancelling parent scope`() = runBlocking {
        var handlerTriggered = false
        val exceptionHandler = CoroutineExceptionHandler { _, _ ->
            handlerTriggered = true
        }

        val parentJob = SupervisorJob()
        val scope = CoroutineScope(parentJob + Dispatchers.Default + exceptionHandler)

        val child1 = scope.launch {
            throw RuntimeException("Simulated worker failure")
        }

        val child2 = scope.launch {
            delay(100L)
        }

        child1.join()
        child2.join()

        assertTrue(handlerTriggered)
        assertTrue(parentJob.isActive)
        assertFalse(parentJob.isCancelled)

        scope.cancel()
    }

    @Test
    fun `test parent scope cancellation cancels all child jobs`() = runBlocking {
        val parentJob = SupervisorJob()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)

        var child1Running = false
        var child2Running = false

        val child1 = scope.launch {
            child1Running = true
            delay(5000L)
        }

        val child2 = scope.launch {
            child2Running = true
            delay(5000L)
        }

        delay(50L)
        assertTrue(child1Running)
        assertTrue(child2Running)

        scope.cancel()

        assertTrue(child1.isCancelled)
        assertTrue(child2.isCancelled)
    }
}
