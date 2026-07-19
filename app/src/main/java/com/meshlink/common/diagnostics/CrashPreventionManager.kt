package com.meshlink.common.diagnostics

import com.meshlink.routing.engine.RouteCache
import com.meshlink.transfer.TransferCache
import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class CrashPreventionManager @Inject constructor(
    private val resourceMonitor: SystemResourceMonitor,
    private val routeCache: RouteCache,
    private val transferCache: TransferCache
) {
    companion object {
        private const val TAG = "CrashPreventionManager"
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        scope.launch {
            resourceMonitor.metrics.collectLatest { metrics ->
                if (metrics.isCriticalMemory) {
                    executeDefensiveActions()
                }
            }
        }
    }

    private suspend fun executeDefensiveActions() {
        MeshLogger.w(TAG, "CRITICAL MEMORY: Executing Crash Prevention Actions")
        try {
            // Drop routes that haven't been used in 1 minute to save memory
            val evicted = routeCache.evictStaleRoutes(60_000L)
            MeshLogger.d(TAG, "Evicted $evicted stale routes during memory pressure")
            
            // Clear temporary chunk transfers
            transferCache.clearCache()
            
            // Suggest GC (Dalvik/ART may ignore it, but good practice during critical memory)
            System.gc()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to execute defensive actions: ${e.message}")
        }
    }
}
