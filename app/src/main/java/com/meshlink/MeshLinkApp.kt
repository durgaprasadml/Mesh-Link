package com.meshlink

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.meshlink.service.work.BackgroundTaskScheduler
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.power.AdaptiveMeshPowerManager
import com.meshlink.common.logger.MeshCrashReporter
import com.meshlink.service.MeshBackgroundService
import com.meshlink.service.MeshLifecycleManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class MeshLinkApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var backgroundTaskScheduler: BackgroundTaskScheduler

    @Inject
    lateinit var adaptivePowerManager: AdaptiveMeshPowerManager

    @Inject
    lateinit var meshCrashReporter: MeshCrashReporter

    @Inject
    lateinit var meshLifecycleManager: MeshLifecycleManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun onCreate() {
        // Load SQLCipher native library before Hilt injection (super.onCreate)
        try {
            System.loadLibrary("sqlcipher")
            MeshLogger.d("MeshLinkApp", "SQLCipher native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            MeshLogger.e("MeshLinkApp", "Failed to load SQLCipher native library", e)
        }

        super.onCreate()
        
        // Initialize MeshLogger crash reporter integration (Firebase Crashlytics)
        MeshLogger.init(meshCrashReporter)
        meshCrashReporter.setUserId("durgaprasadmadikeri@gmail.com")
        meshCrashReporter.logBreadcrumb("MeshLinkApp initialized")
        
        // Initialize Mesh Engine Lifecycle Manager
        meshLifecycleManager.initialize()

        // Start MeshBackgroundService
        val serviceIntent = Intent(this, MeshBackgroundService::class.java).apply {
            action = MeshBackgroundService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
            MeshLogger.e("MeshLinkApp", "ForegroundServiceStartNotAllowedException on app start: ${e.message}")
        } catch (e: Exception) {
            MeshLogger.e("MeshLinkApp", "Failed to start MeshBackgroundService on app start: ${e.message}")
        }

        applicationScope.launch {
            // Schedule periodic background maintenance off the main thread
            backgroundTaskScheduler.schedulePeriodicWork()
        }
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                MeshLogger.d("Lifecycle", "Application moved to FOREGROUND")
                adaptivePowerManager.start()
            }

            override fun onStop(owner: LifecycleOwner) {
                MeshLogger.d("Lifecycle", "Application moved to BACKGROUND — Mesh engine continues running persistent background service")
                // Adaptive manager stays alive to listen for Doze/Battery intents
            }
        })
    }
}
