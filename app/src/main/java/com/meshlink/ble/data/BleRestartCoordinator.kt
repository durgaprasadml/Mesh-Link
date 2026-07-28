package com.meshlink.ble.data

import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

enum class RestartComponent {
    SCANNER,
    ADVERTISER
}

enum class RestartState {
    IDLE,
    SCHEDULED,
    RUNNING
}

open class BleException(message: String, val errorCode: Int? = null) : Exception(message)
class BleNonRetryableException(message: String, errorCode: Int? = null) : BleException(message, errorCode)

@Singleton
class BleRestartCoordinator @Inject constructor() {

    companion object {
        private const val TAG = "BleRestartCoordinator"
        const val MAX_RESTART_ATTEMPTS = 10
        const val BASE_DELAY_MS = 2000L
        const val MAX_DELAY_MS = 60000L
    }

    private val mutex = Mutex()
    private val componentStates = mutableMapOf<RestartComponent, RestartState>()
    private val componentAttempts = mutableMapOf<RestartComponent, Int>()
    private val componentJobs = mutableMapOf<RestartComponent, Job>()

    fun scheduleRestart(
        scope: CoroutineScope,
        component: RestartComponent,
        cause: Throwable? = null,
        action: suspend () -> Unit
    ) {
        if (!shouldRetry(cause)) {
            MeshLogger.w(TAG, "Restart not eligible for $component (cause: ${cause?.message}).")
            return
        }

        scope.launch {
            mutex.withLock {
                val currentState = componentStates[component] ?: RestartState.IDLE
                if (currentState != RestartState.IDLE) {
                    MeshLogger.d(TAG, "Restart already pending/running for $component (State: $currentState). Ignoring duplicate.")
                    return@launch
                }

                val currentAttempts = componentAttempts.getOrDefault(component, 0)
                if (currentAttempts >= MAX_RESTART_ATTEMPTS) {
                    MeshLogger.e(TAG, "Maximum restart attempts ($MAX_RESTART_ATTEMPTS) reached for $component. Giving up.")
                    return@launch
                }

                val attempt = currentAttempts + 1
                componentAttempts[component] = attempt
                componentStates[component] = RestartState.SCHEDULED

                val baseDelay = min(BASE_DELAY_MS * (1 shl (attempt - 1)), MAX_DELAY_MS)
                val jitter = if (baseDelay > 0) Random.nextLong(0, baseDelay / 2 + 1) else 0
                val totalDelay = min(baseDelay + jitter, MAX_DELAY_MS)

                MeshLogger.d(TAG, "BLE $component restart scheduled. Attempt: $attempt, Delay: ${totalDelay / 1000.0} seconds, Reason: ${cause?.message ?: "Unknown"}")

                componentJobs[component] = launch {
                    delay(totalDelay)
                    executeRestart(component, action)
                }
            }
        }
    }

    private suspend fun executeRestart(component: RestartComponent, action: suspend () -> Unit) {
        mutex.withLock {
            componentStates[component] = RestartState.RUNNING
        }
        try {
            action()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Exception during $component restart execution", e)
        } finally {
            mutex.withLock {
                if (componentStates[component] == RestartState.RUNNING) {
                    componentStates[component] = RestartState.IDLE
                }
            }
        }
    }

    suspend fun cancelRestart(component: RestartComponent) {
        mutex.withLock {
            componentJobs[component]?.cancel()
            componentJobs.remove(component)
            componentStates[component] = RestartState.IDLE
            MeshLogger.d(TAG, "Cancelled pending restart for $component")
        }
    }

    suspend fun resetRetry(component: RestartComponent) {
        mutex.withLock {
            componentJobs[component]?.cancel()
            componentJobs.remove(component)
            componentAttempts[component] = 0
            componentStates[component] = RestartState.IDLE
            MeshLogger.d(TAG, "Reset retry state for $component")
        }
    }

    fun shouldRetry(cause: Throwable?): Boolean {
        if (cause == null) return true
        if (cause is SecurityException) return false
        if (cause is CancellationException) return false
        if (cause is BleNonRetryableException) return false
        return true
    }
    
    // For testing/diagnostics
    suspend fun getState(component: RestartComponent): RestartState {
        return mutex.withLock { componentStates[component] ?: RestartState.IDLE }
    }
    
    suspend fun getAttempts(component: RestartComponent): Int {
        return mutex.withLock { componentAttempts[component] ?: 0 }
    }
}
