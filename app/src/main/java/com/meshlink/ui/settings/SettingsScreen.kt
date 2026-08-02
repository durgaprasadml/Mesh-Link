package com.meshlink.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.profile.MeshSettingsScreen
import com.meshlink.ui.profile.ProfileScreen
import com.meshlink.ui.settings.screens.AboutSettingsScreen
import com.meshlink.ui.settings.screens.AppearanceSettingsScreen
import com.meshlink.ui.settings.screens.DeveloperSettingsScreen
import com.meshlink.ui.settings.screens.EmergencySettingsScreen
import com.meshlink.ui.settings.screens.MessagingSettingsScreen
import com.meshlink.ui.settings.screens.NetworkSettingsScreen
import com.meshlink.ui.settings.screens.NotificationsSettingsScreen
import com.meshlink.ui.settings.screens.PrivacySettingsScreen
import com.meshlink.ui.settings.screens.StorageSettingsScreen
import com.meshlink.ui.settings.screens.WifiDiagnosticsScreen
import kotlinx.coroutines.flow.collectLatest

enum class SettingsDestination {
    HOME, PROFILE, NETWORK, MESSAGING, EMERGENCY, STORAGE, APPEARANCE, NOTIFICATIONS, PRIVACY, DEVELOPER, WIFI_DIAGNOSTICS, ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by remember { mutableStateOf(SettingsDestination.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SettingsEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is SettingsEvent.SuccessMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                if (targetState != SettingsDestination.HOME) {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut()
                } else {
                    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut()
                }
            },
            label = "SettingsNav"
        ) { dest ->
            when (dest) {
                SettingsDestination.HOME -> MeshSettingsScreen(
                    uiState = uiState,
                    onBack = onBack,
                    onNavigateToProfile = { currentDestination = SettingsDestination.PROFILE },
                    onSetThemeMode = { viewModel.setThemeMode(it) },
                    onSetMaterialYou = { viewModel.setMaterialYouEnabled(it) },
                    onSetHighContrast = { viewModel.setHighContrast(it) },
                    onSetGlassEffects = { viewModel.setGlassEffectsEnabled(it) },
                    onSetReduceMotion = { viewModel.setReduceMotionEnabled(it) },
                    onSetLargeText = { viewModel.setLargeTextEnabled(it) },
                    onSetEncryptionEnabled = { viewModel.setEncryptionEnabled(it) },
                    onSetOnlineVisible = { viewModel.setOnlineVisible(it) },
                    onSetAdvancedEncryption = { viewModel.setAdvancedEncryptionEnforcement(it) },
                    onSetBleEnabled = { viewModel.setBleEnabled(it) },
                    onSetBleAdv = { viewModel.setBleAdvertisingEnabled(it) },
                    onSetBleScan = { viewModel.setBleScanningEnabled(it) },
                    onSetTransport = { viewModel.setPreferredTransport(it) },
                    onSetRelayEnabled = { viewModel.setMeshRelayEnabled(it) },
                    onSetMaxHops = { viewModel.setMeshMaxHops(it) },
                    onExportLogs = { viewModel.exportDebugLogs() },
                    onShowToast = { viewModel.showToast(it) }
                )
                SettingsDestination.PROFILE -> ProfileScreen(
                    onNavigateBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.NETWORK -> NetworkSettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.MESSAGING -> MessagingSettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.EMERGENCY -> EmergencySettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.STORAGE -> StorageSettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.APPEARANCE -> AppearanceSettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.NOTIFICATIONS -> NotificationsSettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.PRIVACY -> PrivacySettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
                SettingsDestination.DEVELOPER -> DeveloperSettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME },
                    onNavigateToWifiDiagnostics = { currentDestination = SettingsDestination.WIFI_DIAGNOSTICS }
                )
                SettingsDestination.WIFI_DIAGNOSTICS -> WifiDiagnosticsScreen(
                    onBack = { currentDestination = SettingsDestination.DEVELOPER }
                )
                SettingsDestination.ABOUT -> AboutSettingsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { currentDestination = SettingsDestination.HOME }
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
