package com.meshlink

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.meshlink.service.work.BackgroundTaskScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class MeshLinkApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var backgroundTaskScheduler: BackgroundTaskScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        applicationScope.launch {
            // Load SQLCipher native library off the main thread
            try {
                System.loadLibrary("sqlcipher")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Schedule periodic background maintenance off the main thread
            backgroundTaskScheduler.schedulePeriodicWork()
        }
    }
}
