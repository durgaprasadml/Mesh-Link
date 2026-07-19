package com.meshlink.common.diagnostics

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Debug
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class SystemResourceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : ComponentCallbacks2 {

    data class ResourceMetrics(
        val jvmHeapUsageMb: Int = 0,
        val nativeMemoryUsageMb: Int = 0,
        val activeThreadCount: Int = 0,
        val memoryPressureLevel: Int = ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
        val isCriticalMemory: Boolean = false
    )

    private val _metrics = MutableStateFlow(ResourceMetrics())
    val metrics: StateFlow<ResourceMetrics> = _metrics

    init {
        context.registerComponentCallbacks(this)
        updateMetrics() // Initial snapshot
    }

    fun updateMetrics() {
        val runtime = Runtime.getRuntime()
        val jvmUsage = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val nativeUsage = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        val threadCount = Thread.activeCount()

        _metrics.value = _metrics.value.copy(
            jvmHeapUsageMb = jvmUsage.toInt(),
            nativeMemoryUsageMb = nativeUsage.toInt(),
            activeThreadCount = threadCount
        )
    }

    override fun onTrimMemory(level: Int) {
        val isCritical = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        _metrics.value = _metrics.value.copy(
            memoryPressureLevel = level,
            isCriticalMemory = isCritical
        )
        // Ensure we record updated memory usage on pressure
        updateMetrics()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No-op
    }

    override fun onLowMemory() {
        _metrics.value = _metrics.value.copy(
            memoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            isCriticalMemory = true
        )
        updateMetrics()
    }
}
