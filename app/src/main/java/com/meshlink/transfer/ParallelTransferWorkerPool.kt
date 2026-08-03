package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.IoDispatcher
import com.meshlink.transfer.scheduler.ParallelTransferWorkerPool as ParallelTransferWorkerPoolInterface
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Reusable, coroutine-based worker pool for parallel chunk processing tasks.
 * Generic and transport-independent (executes caller-supplied chunk preparation pipelines).
 * Reuses worker coroutines rather than allocating a new coroutine per chunk.
 */
@Singleton
class ParallelTransferWorkerPool @Inject constructor(
    private val config: TransferConfiguration,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope
) : ParallelTransferWorkerPoolInterface {

    companion object {
        private const val TAG = "ParallelTransferWorkerPool"
    }

    private var taskChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val workerJobs = mutableListOf<Job>()
    private val activeWorkerCount = AtomicInteger(0)
    private val isRunning = AtomicBoolean(false)

    init {
        startWorkers(config.workerCount)
    }

    @Synchronized
    override fun startWorkers(workerCount: Int) {
        if (isRunning.get() && workerJobs.size == workerCount) {
            return
        }

        stopWorkers()
        taskChannel = Channel(config.queueCapacity)
        isRunning.set(true)

        for (i in 0 until workerCount) {
            val job = applicationScope.launch(ioDispatcher + SupervisorJob()) {
                workerLoop(workerId = i + 1)
            }
            workerJobs.add(job)
        }
        MeshLogger.d(TAG, "Started $workerCount parallel transfer workers")
    }

    private suspend fun workerLoop(workerId: Int) {
        try {
            for (task in taskChannel) {
                if (!isRunning.get()) break
                activeWorkerCount.incrementAndGet()
                try {
                    task()
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Error executing task in worker $workerId: ${e.message}", e)
                } finally {
                    activeWorkerCount.decrementAndGet()
                }
            }
        } catch (e: Exception) {
            MeshLogger.d(TAG, "Worker $workerId terminated: ${e.message}")
        }
    }

    override fun submitChunkTask(task: suspend () -> Unit) {
        if (!isRunning.get()) {
            startWorkers(config.workerCount)
        }
        val result = taskChannel.trySend(task)
        if (result.isFailure) {
            // Channel full or closed; fallback launch on applicationScope to prevent drop
            applicationScope.launch(ioDispatcher + SupervisorJob()) {
                try {
                    taskChannel.send(task)
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to submit chunk task: ${e.message}")
                }
            }
        }
    }

    @Synchronized
    override fun stopWorkers() {
        isRunning.set(false)
        taskChannel.close()
        for (job in workerJobs) {
            job.cancel()
        }
        workerJobs.clear()
        activeWorkerCount.set(0)
    }

    override fun getActiveWorkerCount(): Int {
        return activeWorkerCount.get()
    }

    fun getWorkerPoolSize(): Int {
        return workerJobs.size
    }
}
