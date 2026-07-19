package com.meshlink.common.diagnostics

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.meshlink.common.logger.MeshLogger

@Singleton
class RuntimeWatchdog @Inject constructor(
    private val selfHealer: SelfHealer
) {
    companion object {
        private const val TAG = "RuntimeWatchdog"
        private const val TIMEOUT_MS = 45_000L // 45 seconds without a ping means stall
    }

    private val monitorScope = CoroutineScope(Dispatchers.Default)
    private val componentPings = ConcurrentHashMap<String, Long>()

    fun start() {
        monitorScope.launch {
            while (true) {
                delay(15_000L) // Check every 15s
                val now = System.currentTimeMillis()
                componentPings.forEach { (component, lastPing) ->
                    if (now - lastPing > TIMEOUT_MS) {
                        MeshLogger.e(TAG, "Watchdog detected stall in component: $component")
                        selfHealer.triggerRecovery(component)
                        // Reset to avoid multiple triggers before recovery completes
                        componentPings[component] = now
                    }
                }
            }
        }
    }

    fun ping(componentName: String) {
        componentPings[componentName] = System.currentTimeMillis()
    }
    
    fun remove(componentName: String) {
        componentPings.remove(componentName)
    }
}
