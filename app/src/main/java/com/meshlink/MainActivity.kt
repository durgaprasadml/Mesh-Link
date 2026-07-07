package com.meshlink

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.meshlink.data.repository.BleRepository
import com.meshlink.service.MeshRelayService
import com.meshlink.ui.navigation.AppNavigation
import com.meshlink.ui.navigation.Screen
import com.meshlink.ui.theme.MeshLinkTheme
import com.meshlink.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import androidx.core.os.bundleOf

// Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bleRepository: BleRepository

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRelayService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

     

        // ✅ Init Firebase
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // ✅ Log app open
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)

        requestNotificationPermissionIfNeeded()

        val startAddress = intent.getStringExtra("address")
        val startName = intent.getStringExtra("name")

        setContent {
            MeshLinkTheme {
                val navController = rememberNavController()

                LaunchedEffect(intent) {
                    if (startAddress != null && startName != null) {

                        firebaseAnalytics.logEvent(
                            "chat_opened",
                            bundleOf("device_name" to startName)
                        )

                        navController.navigate(
                            Screen.ChatDetail.createRoute(startAddress, startName)
                        )
                    }
                }

                AppNavigation(navController = navController)
            }
        }

        startRelayService()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                bleRepository.autoStartMesh()
                firebaseAnalytics.logEvent("mesh_started", null)

            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    // FIX ERROR 1: suppress system notifications while app is in foreground
    override fun onStart() {
        super.onStart()
        NotificationHelper.setAppForeground(true)
    }

    override fun onStop() {
        super.onStop()
        NotificationHelper.setAppForeground(false)
    }

    private fun startRelayService() {
        val intent = Intent(this, MeshRelayService::class.java).apply {
            action = MeshRelayService.ACTION_START
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            firebaseAnalytics.logEvent("relay_service_started", null)

        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notificationPermissionLauncher.launch(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    }
}