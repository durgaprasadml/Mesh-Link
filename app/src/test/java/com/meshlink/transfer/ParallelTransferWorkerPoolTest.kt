package com.meshlink.transfer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class ParallelTransferWorkerPoolTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var config: TransferConfiguration
    private lateinit var workerPool: ParallelTransferWorkerPool

    @Before
    fun setUp() {
        config = TransferConfiguration().apply { workerCount = 4 }
    }

    @After
    fun tearDown() {
        if (::workerPool.isInitialized) {
            workerPool.stopWorkers()
        }
    }

    @Test
    fun `startWorkers initializes worker pool with correct worker count`() = runTest(testDispatcher) {
        workerPool = ParallelTransferWorkerPool(
            config = config,
            ioDispatcher = testDispatcher,
            applicationScope = this
        )
        assertEquals(4, workerPool.getWorkerPoolSize())
    }

    @Test
    fun `submitChunkTask executes tasks concurrently across reusable workers`() = runTest(testDispatcher) {
        workerPool = ParallelTransferWorkerPool(
            config = config,
            ioDispatcher = testDispatcher,
            applicationScope = this
        )
        val counter = AtomicInteger(0)
        val taskCount = 20

        for (i in 0 until taskCount) {
            workerPool.submitChunkTask {
                counter.incrementAndGet()
            }
        }

        advanceUntilIdle()

        assertEquals(taskCount, counter.get())
    }

    @Test
    fun `stopWorkers cancels workers and clears state`() = runTest(testDispatcher) {
        workerPool = ParallelTransferWorkerPool(
            config = config,
            ioDispatcher = testDispatcher,
            applicationScope = this
        )
        workerPool.stopWorkers()
        assertEquals(0, workerPool.getWorkerPoolSize())
        assertEquals(0, workerPool.getActiveWorkerCount())
    }
}
