package com.meshlink

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.service.MeshRelayService
import com.meshlink.ui.components.hasRequiredPermissions
import com.meshlink.ui.navigation.AppNavigation
import com.meshlink.ui.navigation.Screen
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

// Firebase
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var meshRepository: MeshRepository

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    
    private val pendingIntents = kotlinx.coroutines.channels.Channel<Intent>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkAndStartMesh()
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

        setContent {
            val windowSizeClass = @OptIn(ExperimentalMaterial3WindowSizeClassApi::class) calculateWindowSizeClass(this)
            
            val settingsViewModel: com.meshlink.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()

            MeshTheme(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.isMaterialYouEnabled,
                accentColor = uiState.accentColor,
                fontScale = uiState.fontScale,
                largeTextEnabled = uiState.largeTextEnabled,
                cornerRadiusScale = uiState.cornerRadiusScale,
                animationsEnabled = uiState.animationsEnabled,
                glassEffectsEnabled = uiState.glassEffectsEnabled,
                highContrast = uiState.highContrast,
                reduceMotionEnabled = uiState.reduceMotionEnabled
            ) {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    for (newIntent in pendingIntents) {
                        val address = newIntent.getStringExtra("address")
                        val name = newIntent.getStringExtra("name")
                        if (address != null && name != null) {
                            firebaseAnalytics.logEvent(
                                "chat_opened",
                                bundleOf("device_name" to name)
                            )
                            navController.navigate(Screen.ChatDetail.createRoute(address, name)) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
                
                LaunchedEffect(intent) {
                    val address = intent.getStringExtra("address")
                    val name = intent.getStringExtra("name")
                    if (address != null && name != null) {
                        firebaseAnalytics.logEvent(
                            "chat_opened",
                            bundleOf("device_name" to name)
                        )
                        navController.navigate(Screen.ChatDetail.createRoute(address, name)) {
                            launchSingleTop = true
                        }
                    }
                }

                AppNavigation(navController = navController, windowSizeClass = windowSizeClass)
            }
        }

        lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            checkAndStartMesh()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntents.trySend(intent)
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
    
    private fun checkAndStartMesh() {
        if (hasRequiredPermissions(this)) {
            startRelayService()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    meshRepository.autoStartMesh()
                    firebaseAnalytics.logEvent("mesh_started", null)
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
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
