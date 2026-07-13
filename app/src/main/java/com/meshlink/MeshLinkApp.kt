package com.meshlink

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.meshlink.service.work.BackgroundTaskScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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

    override fun onCreate() {
        super.onCreate()
        // Load SQLCipher native library
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Schedule periodic background maintenance
        backgroundTaskScheduler.schedulePeriodicWork()
    }
}
