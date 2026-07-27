package com.meshlink.common.maintenance

import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.DefaultDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Centralized scheduler for periodic maintenance tasks.
 * Avoids multiple independent 'while(true) delay()' loops waking up the CPU out of sync.
 */
@Singleton
class MaintenanceScheduler @Inject constructor(
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val TAG = "MaintenanceScheduler"
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private var maintenanceJob: Job? = null

    // Base tick interval: 60 seconds
    private val BASE_INTERVAL_MS = 60_000L

    data class MaintenanceTask(
        val name: String,
        val intervalMs: Long,
        val action: suspend () -> Unit,
        var lastExecutionMs: Long = 0L
    )

    private val tasks = CopyOnWriteArrayList<MaintenanceTask>()

    fun schedule(name: String, intervalMs: Long, action: suspend () -> Unit) {
        tasks.add(MaintenanceTask(name, intervalMs, action))
        startIfNeeded()
    }

    private fun startIfNeeded() {
        if (maintenanceJob?.isActive == true) return
        
        MeshLogger.d(TAG, "Starting centralized maintenance scheduler")
        maintenanceJob = scope.launch {
            while (isActive) {
                delay(BASE_INTERVAL_MS)
                
                val now = System.currentTimeMillis()
                for (task in tasks) {
                    if (now - task.lastExecutionMs >= task.intervalMs) {
                        try {
                            task.action()
                        } catch (e: Exception) {
                            MeshLogger.e(TAG, "Error in maintenance task '${task.name}': ${e.message}")
                        }
                        task.lastExecutionMs = now
                    }
                }
            }
        }
    }

    fun stop() {
        maintenanceJob?.cancel()
        maintenanceJob = null
    }
}
