package com.meshlink

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeshLinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load SQLCipher native library
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
