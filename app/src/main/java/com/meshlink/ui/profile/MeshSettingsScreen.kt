package com.meshlink.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.settings.AccessibilitySettings
import com.meshlink.ui.settings.AccountSettings
import com.meshlink.ui.settings.BackupRestoreSettings
import com.meshlink.ui.settings.DiagnosticsSettings
import com.meshlink.ui.settings.NotificationSettings
import com.meshlink.ui.settings.PrivacySecurity
import com.meshlink.ui.settings.QuickPreferences
import com.meshlink.ui.settings.SettingsAnimations
import com.meshlink.ui.settings.SettingsProfileCard
import com.meshlink.ui.settings.SettingsRow
import com.meshlink.ui.settings.SettingsSearchBar
import com.meshlink.ui.settings.SettingsSearchResults
import com.meshlink.ui.settings.SettingsSectionHeader
import com.meshlink.ui.settings.SettingsTopBar
import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.StorageSettings

data class SettingsRowItemData(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isSwitch: Boolean = false,
    val isChecked: Boolean = false,
    val trailingText: String? = null,
    val onToggle: ((Boolean) -> Unit)? = null,
    val onClick: () -> Unit = {}
)

data class SettingsCategoryData(
    val title: String,
    val items: List<SettingsRowItemData>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MeshSettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToMessaging: () -> Unit = {},
    onNavigateToEmergency: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToDeveloper: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onSetThemeMode: (String) -> Unit = {},
    onSetMaterialYou: (Boolean) -> Unit = {},
    onSetHighContrast: (Boolean) -> Unit = {},
    onSetGlassEffects: (Boolean) -> Unit = {},
    onSetReduceMotion: (Boolean) -> Unit = {},
    onSetLargeText: (Boolean) -> Unit = {},
    onSetEncryptionEnabled: (Boolean) -> Unit = {},
    onSetOnlineVisible: (Boolean) -> Unit = {},
    onSetAdvancedEncryption: (Boolean) -> Unit = {},
    onSetBleEnabled: (Boolean) -> Unit = {},
    onSetBleAdv: (Boolean) -> Unit = {},
    onSetBleScan: (Boolean) -> Unit = {},
    onSetTransport: (String) -> Unit = {},
    onSetRelayEnabled: (Boolean) -> Unit = {},
    onSetMaxHops: (Int) -> Unit = {},
    onExportLogs: () -> Unit = {},
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val userIdentity = remember(uiState.user, uiState.isOnlineVisible) {
        UserIdentityUi.fromUser(uiState.user, uiState.isOnlineVisible)
    }

    // Comprehensive category dataset for live search filtering
    val allCategories = remember(uiState) {
        listOf(
            SettingsCategoryData(
                title = "Account & Identity",
                items = listOf(
                    SettingsRowItemData("acc_profile", "Profile Information", "Edit display name, bio & avatar", Icons.Default.Person, onClick = onNavigateToProfile),
                    SettingsRowItemData("acc_identity", "Mesh Identity", "Ed25519 node fingerprint & public key", Icons.Default.Fingerprint, onClick = onNavigateToProfile),
                    SettingsRowItemData("acc_qr", "Share Identity QR", "Export encrypted QR pairing code", Icons.Default.QrCode, onClick = { onShowToast("QR Code ready to scan") }),
                    SettingsRowItemData("acc_backup", "Backup Identity", "Export identity key package", Icons.Default.Download, onClick = { onShowToast("Identity backup generated") })
                )
            ),
            SettingsCategoryData(
                title = "Appearance & Themes",
                items = listOf(
                    SettingsRowItemData("app_theme", "Theme Mode", "Light, Dark, System or AMOLED", Icons.Default.Palette, trailingText = uiState.themeMode, onClick = onNavigateToAppearance),
                    SettingsRowItemData("app_mat_you", "Material You Dynamic Colors", "Match system wallpaper palette", Icons.Default.Palette, isSwitch = true, isChecked = uiState.isMaterialYouEnabled, onToggle = onSetMaterialYou),
                    SettingsRowItemData("app_amoled", "AMOLED Black Mode", "Pitch black background for OLED screens", Icons.Default.Nightlight, isSwitch = true, isChecked = uiState.themeMode == "AMOLED", onToggle = { onSetThemeMode(if (it) "AMOLED" else "SYSTEM") }),
                    SettingsRowItemData("app_contrast", "High Contrast Mode", "Increase text & edge contrast", Icons.Default.Contrast, isSwitch = true, isChecked = uiState.highContrast, onToggle = onSetHighContrast)
                )
            ),
            SettingsCategoryData(
                title = "Notifications & Sound",
                items = listOf(
                    SettingsRowItemData("notif_msg", "Message Notifications", "Alerts for direct mesh packets", Icons.AutoMirrored.Filled.Message, isSwitch = true, isChecked = uiState.messageNotifications, onToggle = { onShowToast("Message alerts updated") }),
                    SettingsRowItemData("notif_sos", "Emergency SOS Alerts", "High-priority distress broadcasts", Icons.Default.Warning, isSwitch = true, isChecked = uiState.sosAlertsEnabled, onToggle = { onShowToast("SOS alert preference updated") }),
                    SettingsRowItemData("notif_sound", "Notification Sound", uiState.notificationSound, Icons.AutoMirrored.Filled.VolumeUp, onClick = onNavigateToNotifications),
                    SettingsRowItemData("notif_vib", "Vibration Pulse", "Haptic pulse for mesh events", Icons.Default.Notifications, isSwitch = true, isChecked = uiState.vibrationEnabled, onToggle = { onShowToast("Vibration preference saved") })
                )
            ),
            SettingsCategoryData(
                title = "Privacy & Security",
                items = listOf(
                    SettingsRowItemData("priv_disc", "Discoverable on Mesh", "Allow nearby nodes to locate ID", Icons.Default.Radar, isSwitch = true, isChecked = uiState.isOnlineVisible, onToggle = onSetOnlineVisible),
                    SettingsRowItemData("priv_e2ee", "End-to-End Encryption", "Signal protocol message cipher", Icons.Default.Lock, isSwitch = true, isChecked = uiState.isEncryptionEnabled, onToggle = onSetEncryptionEnabled),
                    SettingsRowItemData("priv_bio", "Biometric Lock", "Require fingerprint/Face ID unlock", Icons.Default.Fingerprint, isSwitch = true, isChecked = uiState.biometricUnlock, onToggle = { onShowToast("Biometric lock updated") }),
                    SettingsRowItemData("priv_trusted", "Trusted Devices & Keys", "${uiState.trustedDevicesCount} Verified Peers", Icons.Default.Devices, onClick = onNavigateToPrivacy)
                )
            ),
            SettingsCategoryData(
                title = "Connectivity & Transports",
                items = listOf(
                    SettingsRowItemData("conn_ble", "Bluetooth Low Energy", "Primary P2P discovery transport", Icons.Default.Bluetooth, isSwitch = true, isChecked = uiState.isBleEnabled, onToggle = onSetBleEnabled),
                    SettingsRowItemData("conn_wifi", "Wi-Fi Direct P2P", "High-speed peer file transfers", Icons.Default.Wifi, isSwitch = true, isChecked = uiState.bleScanningEnabled, onToggle = onSetBleScan),
                    SettingsRowItemData("conn_pref", "Preferred Transport", uiState.preferredTransport, Icons.Default.Router, trailingText = uiState.preferredTransport, onClick = onNavigateToNetwork),
                    SettingsRowItemData("conn_relay", "Store-and-Forward Relay", "Relay encrypted packets for peers", Icons.Default.Memory, isSwitch = true, isChecked = uiState.isMeshRelayEnabled, onToggle = onSetRelayEnabled)
                )
            ),
            SettingsCategoryData(
                title = "Storage & Backup",
                items = listOf(
                    SettingsRowItemData("stor_overview", "Storage & Media Breakdown", "18.4 GB used • 124 MB cache", Icons.Default.Storage, onClick = onNavigateToStorage),
                    SettingsRowItemData("stor_backup", "Backup & Recovery Package", "Local encrypted backup", Icons.Default.Backup, onClick = { onShowToast("Backup file ready") })
                )
            ),
            SettingsCategoryData(
                title = "Diagnostics & Accessibility",
                items = listOf(
                    SettingsRowItemData("diag_analytics", "Network Analytics", "Packet statistics & metrics", Icons.Default.Analytics, onClick = onNavigateToDeveloper),
                    SettingsRowItemData("diag_logs", "Export Diagnostic Logs", "Dump network event logs", Icons.Default.BugReport, onClick = onExportLogs),
                    SettingsRowItemData("diag_access", "Accessibility & Motion", "High contrast, large fonts & reduced motion", Icons.Default.AccessibilityNew, onClick = onNavigateToAppearance),
                    SettingsRowItemData("diag_dev", "Developer Options", "Protocol tuning & beacon rates", Icons.Default.Code, onClick = onNavigateToDeveloper)
                )
            )
        )
    }

    val filteredCategories by remember(searchQuery, allCategories) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allCategories
            } else {
                allCategories.mapNotNull { cat ->
                    val matchingItems = cat.items.filter { item ->
                        item.title.contains(searchQuery, ignoreCase = true) ||
                                item.subtitle.contains(searchQuery, ignoreCase = true)
                    }
                    if (matchingItems.isNotEmpty()) cat.copy(items = matchingItems) else null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                onBack = onBack,
                onSearchClick = { /* Focus search */ },
                onProfileClick = onNavigateToProfile,
                onExportDiagnostics = onExportLogs,
                onResetDefaults = { onShowToast("Preferences reset to defaults") },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Header
            item(key = "header_profile_card") {
                SettingsProfileCard(
                    userIdentity = userIdentity,
                    onEditProfileClick = onNavigateToProfile,
                    onQrClick = { onShowToast("QR Code Identity ready") },
                    onIdentityClick = onNavigateToProfile
                )
            }

            // Search Settings Bar
            item(key = "header_search_bar") {
                SettingsSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholderText = "Search preferences, security, storage..."
                )
            }

            if (searchQuery.isNotBlank()) {
                // Live Search Filtering Results
                item(key = "live_search_results") {
                    SettingsSearchResults(
                        query = searchQuery,
                        filteredCategories = filteredCategories
                    )
                }
            } else {
                // Quick Preferences 4-Card Row
                item(key = "header_quick_preferences") {
                    QuickPreferences(
                        onNavigateToAppearance = onNavigateToAppearance,
                        onNavigateToPrivacy = onNavigateToPrivacy,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToConnectivity = onNavigateToNetwork
                    )
                }

                // Account & Identity Section
                item(key = "sec_account") {
                    AccountSettings(
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToTrustedDevices = onNavigateToPrivacy,
                        onExportQr = { onShowToast("QR Identity generated") },
                        onBackupIdentity = { onShowToast("Backup Identity created") },
                        onExportIdentity = { onShowToast("Identity exported") }
                    )
                }

                // Privacy & Security Section
                item(key = "sec_privacy") {
                    PrivacySecurity(
                        uiState = uiState,
                        onSetOnlineVisible = onSetOnlineVisible,
                        onSetEncryptionEnabled = onSetEncryptionEnabled,
                        onSetBiometricUnlock = { onShowToast("Biometric status updated") },
                        onSetPrivacyMode = { onShowToast("Stealth privacy updated") },
                        onNavigateToTrustedDevices = onNavigateToPrivacy
                    )
                }

                // Notifications Section
                item(key = "sec_notifications") {
                    NotificationSettings(
                        uiState = uiState,
                        onToggleMessageNotifications = { onShowToast("Message alerts updated") },
                        onToggleBroadcastNotifications = { onShowToast("Broadcast alerts updated") },
                        onToggleNearbyNotifications = { onShowToast("Nearby device alerts updated") },
                        onToggleSosAlerts = { onShowToast("SOS alert override updated") },
                        onToggleVibration = { onShowToast("Haptics updated") },
                        onToggleBadges = { onShowToast("Icon badge updated") },
                        onNavigateToSoundPicker = onNavigateToNotifications
                    )
                }

                // Storage Overview Section
                item(key = "sec_storage") {
                    StorageSettings(
                        onClearCache = { onShowToast("Cache cleared (124 MB freed)") },
                        onExportMedia = { onShowToast("Exporting media to Downloads") }
                    )
                }

                // Backup & Recovery Section
                item(key = "sec_backup") {
                    BackupRestoreSettings(
                        onExportBackup = { onShowToast("Backup file exported") },
                        onImportBackup = { onShowToast("Import backup initiated") },
                        onExportIdentityKey = { onShowToast("Mnemonic seed copied to clipboard") }
                    )
                }

                // Diagnostics & Insights Section
                item(key = "sec_diagnostics") {
                    DiagnosticsSettings(
                        onExportLogs = onExportLogs,
                        onNavigateToDeveloperOptions = onNavigateToDeveloper,
                        onNavigateToNetworkDiagnostics = onNavigateToNetwork
                    )
                }

                // Accessibility & Comfort Section
                item(key = "sec_accessibility") {
                    AccessibilitySettings(
                        uiState = uiState,
                        onSetHighContrast = onSetHighContrast,
                        onSetReduceMotion = onSetReduceMotion,
                        onSetLargeText = onSetLargeText
                    )
                }

                // About & Application Information Footer Section
                item(key = "sec_about") {
                    AboutSection(
                        uiState = uiState,
                        onShowToast = onShowToast
                    )
                }
            }
        }
    }
}
