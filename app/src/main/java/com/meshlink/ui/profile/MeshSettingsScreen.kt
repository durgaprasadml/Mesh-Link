package com.meshlink.ui.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.settings.MeshSettingsRow
import com.meshlink.ui.settings.MeshSettingsSwitchRow
import com.meshlink.ui.settings.QuickPreferences
import com.meshlink.ui.settings.SecuritySummaryCard
import com.meshlink.ui.settings.SettingsSearchBar
import com.meshlink.ui.settings.SettingsSectionHeader
import com.meshlink.ui.settings.SettingsUiState

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

    val userIdentity = remember(uiState.user, uiState.isOnlineVisible) {
        UserIdentityUi.fromUser(uiState.user, uiState.isOnlineVisible)
    }

    // Build categories dataset
    val allCategories = remember(uiState) {
        listOf(
            SettingsCategoryData(
                title = "Account",
                items = listOf(
                    SettingsRowItemData("acc_profile", "Profile", "Edit display name, bio & avatar", Icons.Default.Person, onClick = onNavigateToProfile),
                    SettingsRowItemData("acc_identity", "Mesh Identity", "Node fingerprint & public keys", Icons.Default.Fingerprint, onClick = onNavigateToProfile),
                    SettingsRowItemData("acc_qr", "Share Identity QR", "Export encrypted QR code", Icons.Default.QrCode, onClick = { onShowToast("QR Code ready to scan") }),
                    SettingsRowItemData("acc_backup", "Backup Identity", "Export identity key package", Icons.Default.Download, onClick = { onShowToast("Backup generated") })
                )
            ),
            SettingsCategoryData(
                title = "Appearance",
                items = listOf(
                    SettingsRowItemData("app_theme", "Theme Mode", "Light, Dark, System or AMOLED", Icons.Default.Palette, trailingText = uiState.themeMode, onClick = onNavigateToAppearance),
                    SettingsRowItemData("app_mat_you", "Dynamic Colors", "Match system wallpaper palette", Icons.Default.Palette, isSwitch = true, isChecked = uiState.isMaterialYouEnabled, onToggle = onSetMaterialYou),
                    SettingsRowItemData("app_amoled", "AMOLED Black Mode", "Pure pitch black background", Icons.Default.Nightlight, isSwitch = true, isChecked = uiState.themeMode == "AMOLED", onToggle = { onSetThemeMode(if (it) "AMOLED" else "SYSTEM") }),
                    SettingsRowItemData("app_contrast", "High Contrast", "Enhanced text & border clarity", Icons.Default.Contrast, isSwitch = true, isChecked = uiState.highContrast, onToggle = onSetHighContrast)
                )
            ),
            SettingsCategoryData(
                title = "Notifications",
                items = listOf(
                    SettingsRowItemData("notif_msg", "Message Notifications", "Alerts for direct mesh messages", Icons.AutoMirrored.Filled.Message, isSwitch = true, isChecked = uiState.messageNotifications, onToggle = { onShowToast("Message notifications updated") }),
                    SettingsRowItemData("notif_sos", "Emergency SOS Alerts", "High-priority emergency broadcasts", Icons.Default.Warning, isSwitch = true, isChecked = uiState.sosAlertsEnabled, onToggle = { onShowToast("SOS alert preference updated") }),
                    SettingsRowItemData("notif_sound", "Notification Sound", uiState.notificationSound, Icons.AutoMirrored.Filled.VolumeUp, onClick = onNavigateToNotifications),
                    SettingsRowItemData("notif_vib", "Vibration Pulse", "Haptic pulse for mesh packets", Icons.Default.Notifications, isSwitch = true, isChecked = uiState.vibrationEnabled, onToggle = { onShowToast("Vibration updated") })
                )
            ),
            SettingsCategoryData(
                title = "Privacy",
                items = listOf(
                    SettingsRowItemData("priv_disc", "Discoverable on Mesh", "Allow nearby nodes to locate ID", Icons.Default.Radar, isSwitch = true, isChecked = uiState.isOnlineVisible, onToggle = onSetOnlineVisible),
                    SettingsRowItemData("priv_e2ee", "End-to-End Encryption", "Signal protocol message cipher", Icons.Default.Lock, isSwitch = true, isChecked = uiState.isEncryptionEnabled, onToggle = onSetEncryptionEnabled),
                    SettingsRowItemData("priv_bio", "Biometric Lock", "Require fingerprint/Face ID", Icons.Default.Fingerprint, isSwitch = true, isChecked = uiState.biometricUnlock, onToggle = { onShowToast("Biometric lock updated") }),
                    SettingsRowItemData("priv_trusted", "Trusted Devices", "${uiState.trustedDevicesCount} Verified Peers", Icons.Default.Devices, onClick = onNavigateToPrivacy)
                )
            ),
            SettingsCategoryData(
                title = "Connectivity",
                items = listOf(
                    SettingsRowItemData("conn_ble", "Bluetooth Low Energy", "Primary discovery transport", Icons.Default.Bluetooth, isSwitch = true, isChecked = uiState.isBleEnabled, onToggle = onSetBleEnabled),
                    SettingsRowItemData("conn_wifi", "Wi-Fi Direct", "High-speed peer file transfers", Icons.Default.Wifi, isSwitch = true, isChecked = uiState.bleScanningEnabled, onToggle = onSetBleScan),
                    SettingsRowItemData("conn_pref", "Preferred Transport", uiState.preferredTransport, Icons.Default.Router, trailingText = uiState.preferredTransport, onClick = onNavigateToNetwork),
                    SettingsRowItemData("conn_relay", "Store-and-Forward Relay", "Relay encrypted packets", Icons.Default.Memory, isSwitch = true, isChecked = uiState.isMeshRelayEnabled, onToggle = onSetRelayEnabled)
                )
            ),
            SettingsCategoryData(
                title = "Diagnostics",
                items = listOf(
                    SettingsRowItemData("diag_analytics", "Network Analytics", "Packet statistics & metrics", Icons.Default.Analytics, onClick = onNavigateToDeveloper),
                    SettingsRowItemData("diag_logs", "Export Diagnostics", "Dump network event logs", Icons.Default.BugReport, onClick = onExportLogs),
                    SettingsRowItemData("diag_dev", "Developer Options", "Advanced protocol tuning", Icons.Default.Code, onClick = onNavigateToDeveloper)
                )
            )
        )
    }

    // Filter categories based on search query
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

    MeshScreen(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile Header
            item(key = "header_profile") {
                ProfileHero(
                    userIdentity = userIdentity,
                    onEditAvatarClick = onNavigateToProfile
                )
            }

            // Security Summary Card
            item(key = "header_security") {
                SecuritySummaryCard(
                    uiState = uiState,
                    onNavigateToPrivacy = onNavigateToPrivacy,
                    onNavigateToNetwork = onNavigateToNetwork
                )
            }

            // Search Bar
            item(key = "header_search") {
                SettingsSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Quick Preferences Row
            if (searchQuery.isBlank()) {
                item(key = "header_quick_prefs") {
                    QuickPreferences(
                        onNavigateToAppearance = onNavigateToAppearance,
                        onNavigateToPrivacy = onNavigateToPrivacy,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToConnectivity = onNavigateToNetwork
                    )
                }
            }

            // Sticky Header Grouped Categories
            filteredCategories.forEach { category ->
                stickyHeader(key = "header_cat_${category.title}") {
                    SettingsSectionHeader(title = category.title)
                }

                items(
                    items = category.items,
                    key = { item -> item.id }
                ) { item ->
                    if (item.isSwitch && item.onToggle != null) {
                        MeshSettingsSwitchRow(
                            icon = item.icon,
                            title = item.title,
                            subtitle = item.subtitle,
                            checked = item.isChecked,
                            onCheckedChange = item.onToggle
                        )
                    } else {
                        MeshSettingsRow(
                            icon = item.icon,
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = item.onClick,
                            trailingContent = if (item.trailingText != null) {
                                {
                                    Text(
                                        text = item.trailingText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // About Section Card
            item(key = "footer_about") {
                Spacer(modifier = Modifier.height(8.dp))
                AboutSection(
                    uiState = uiState,
                    onShowToast = onShowToast
                )
            }
        }
    }
}
