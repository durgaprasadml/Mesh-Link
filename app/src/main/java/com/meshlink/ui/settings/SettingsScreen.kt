package com.meshlink.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 600.dp

        if (isExpandedWidth) {
            // Adaptive Two-Pane Master-Detail Layout for Tablets/Foldables/Desktops
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                ) {
                    MeshSettingsScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onNavigateToProfile = { currentDestination = SettingsDestination.PROFILE },
                        onNavigateToNetwork = { currentDestination = SettingsDestination.NETWORK },
                        onNavigateToMessaging = { currentDestination = SettingsDestination.MESSAGING },
                        onNavigateToEmergency = { currentDestination = SettingsDestination.EMERGENCY },
                        onNavigateToStorage = { currentDestination = SettingsDestination.STORAGE },
                        onNavigateToAppearance = { currentDestination = SettingsDestination.APPEARANCE },
                        onNavigateToNotifications = { currentDestination = SettingsDestination.NOTIFICATIONS },
                        onNavigateToPrivacy = { currentDestination = SettingsDestination.PRIVACY },
                        onNavigateToDeveloper = { currentDestination = SettingsDestination.DEVELOPER },
                        onNavigateToAbout = { currentDestination = SettingsDestination.ABOUT },
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
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    RenderSettingsDestinationContent(
                        destination = if (currentDestination == SettingsDestination.HOME) SettingsDestination.NETWORK else currentDestination,
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackToHome = { currentDestination = SettingsDestination.HOME },
                        onNavigateToWifiDiagnostics = { currentDestination = SettingsDestination.WIFI_DIAGNOSTICS }
                    )
                }
            }
        } else {
            // Single-Pane Navigation for Phones
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
                if (dest == SettingsDestination.HOME) {
                    MeshSettingsScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onNavigateToProfile = { currentDestination = SettingsDestination.PROFILE },
                        onNavigateToNetwork = { currentDestination = SettingsDestination.NETWORK },
                        onNavigateToMessaging = { currentDestination = SettingsDestination.MESSAGING },
                        onNavigateToEmergency = { currentDestination = SettingsDestination.EMERGENCY },
                        onNavigateToStorage = { currentDestination = SettingsDestination.STORAGE },
                        onNavigateToAppearance = { currentDestination = SettingsDestination.APPEARANCE },
                        onNavigateToNotifications = { currentDestination = SettingsDestination.NOTIFICATIONS },
                        onNavigateToPrivacy = { currentDestination = SettingsDestination.PRIVACY },
                        onNavigateToDeveloper = { currentDestination = SettingsDestination.DEVELOPER },
                        onNavigateToAbout = { currentDestination = SettingsDestination.ABOUT },
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
                } else {
                    RenderSettingsDestinationContent(
                        destination = dest,
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackToHome = { currentDestination = SettingsDestination.HOME },
                        onNavigateToWifiDiagnostics = { currentDestination = SettingsDestination.WIFI_DIAGNOSTICS }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RenderSettingsDestinationContent(
    destination: SettingsDestination,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBackToHome: () -> Unit,
    onNavigateToWifiDiagnostics: () -> Unit
) {
    when (destination) {
        SettingsDestination.HOME -> Unit
        SettingsDestination.PROFILE -> ProfileScreen(
            onNavigateBack = onBackToHome
        )
        SettingsDestination.NETWORK -> NetworkSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
        SettingsDestination.MESSAGING -> MessagingSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
        SettingsDestination.EMERGENCY -> EmergencySettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
        SettingsDestination.STORAGE -> StorageSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
        SettingsDestination.APPEARANCE -> AppearanceSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
        SettingsDestination.NOTIFICATIONS -> NotificationsSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
        SettingsDestination.PRIVACY -> PrivacySettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
        SettingsDestination.DEVELOPER -> DeveloperSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome,
            onNavigateToWifiDiagnostics = onNavigateToWifiDiagnostics
        )
        SettingsDestination.WIFI_DIAGNOSTICS -> WifiDiagnosticsScreen(
            onBack = onBackToHome
        )
        SettingsDestination.ABOUT -> AboutSettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBackToHome
        )
    }
}
