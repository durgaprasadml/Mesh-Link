package com.meshlink.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshSecurityScreen(
    encryptionUi: EncryptionUi,
    trustUi: TrustUi,
    statsUi: SecurityStatsUi,
    fingerprintUi: FingerprintUi,
    privacyUi: PrivacyUi,
    connectedDevices: List<DeviceTrustUi>,
    activeSessions: List<SessionUi>,
    alerts: List<SecurityAlertUi>,
    timelineEvents: List<SecurityTimelineEventUi>,
    selectedVerification: VerificationUi?,
    onBackClick: () -> Unit,
    onRefreshClick: (() -> Unit)? = null,
    onRotateKeysClick: (() -> Unit)? = null,
    onCopyFingerprintClick: (() -> Unit)? = null,
    onShareFingerprintClick: (() -> Unit)? = null,
    onVerifyDeviceClick: ((DeviceTrustUi) -> Unit)? = null,
    onRekeySessionClick: ((SessionUi) -> Unit)? = null,
    onAcknowledgeAlertClick: ((SecurityAlertUi) -> Unit)? = null,
    onPrivacyToggle: ((String, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(SecurityFilterCategory.ALL) }

    val configuration = LocalConfiguration.current
    val isTabletOrExpanded = configuration.screenWidthDp >= 840

    // Sample default permission list for display
    val permissions = remember {
        listOf(
            PermissionItemUi("1", "Nearby Devices", "Nearby", PermissionStatus.GRANTED, "Required for BLE mesh scanning & advertising"),
            PermissionItemUi("2", "Bluetooth", "Bluetooth", PermissionStatus.GRANTED, "Required for peer discovery & transport"),
            PermissionItemUi("3", "Location", "Location", PermissionStatus.GRANTED, "Required by OS for BLE beacon discovery"),
            PermissionItemUi("4", "Notifications", "Notifications", PermissionStatus.GRANTED, "Required for background security alerts"),
            PermissionItemUi("5", "Camera", "Camera", PermissionStatus.GRANTED, "Required for scanning peer verification QR codes")
        )
    }

    // Sample default diagnostics for display
    val diagnostics = remember(statsUi) {
        listOf(
            DiagnosticComponentUi("1", "Encryption Subsystem", "Crypto", DiagnosticHealthStatus.HEALTHY, "AES-256-GCM hardware accelerated", cipherSuiteMetric(encryptionUi)),
            DiagnosticComponentUi("2", "Hardware Keystore", "Storage", if (statsUi.isHardwareKeystoreActive) DiagnosticHealthStatus.HEALTHY else DiagnosticHealthStatus.WARNING, if (statsUi.isHardwareKeystoreActive) "AndroidKeyStore bound to TEE" else "Software keystore fallback", "TEE Active"),
            DiagnosticComponentUi("3", "BLE Security Layer", "Transport", DiagnosticHealthStatus.HEALTHY, "L2CAP encrypted channels active", "${statsUi.activeSessions} links"),
            DiagnosticComponentUi("4", "Key Exchange Engine", "Protocol", DiagnosticHealthStatus.HEALTHY, "ECDH X25519 Double Ratchet", "v2 Active"),
            DiagnosticComponentUi("5", "Replay Protection", "Audit", if (statsUi.replayAttacksRejected > 0) DiagnosticHealthStatus.WARNING else DiagnosticHealthStatus.HEALTHY, "Rejected ${statsUi.replayAttacksRejected} invalid frames", "${statsUi.replayAttacksRejected} blocked")
        )
    }

    MeshScreen(
        modifier = modifier,
        topBar = {
            SecurityTopBar(
                onBackClick = onBackClick,
                isSearchActive = isSearchActive,
                onSearchToggle = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) searchQuery = ""
                },
                onRefreshClick = onRefreshClick,
                onGenerateReportClick = onRefreshClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Filter Chips Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeshSpacing.ScreenPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SecuritySearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClearQuery = { searchQuery = "" }
                    )
                }

                SecurityFilterChipsRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            if (isTabletOrExpanded) {
                // Dual Pane Layout for Tablet / Desktop / Foldables
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MeshSpacing.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Pane: Summary Cards
                    LazyColumn(
                        modifier = Modifier.weight(0.45f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            SecurityScoreCard(
                                trustUi = trustUi,
                                statsUi = statsUi
                            )
                        }

                        item {
                            EncryptionStatusCard(
                                encryptionUi = encryptionUi,
                                onKeyRotationClick = onRotateKeysClick
                            )
                        }

                        item {
                            LocalIdentityCard(
                                fingerprintUi = fingerprintUi,
                                onCopyFingerprint = onCopyFingerprintClick,
                                onShareFingerprint = onShareFingerprintClick
                            )
                        }
                    }

                    // Right Pane: Detailed Sections
                    LazyColumn(
                        modifier = Modifier.weight(0.55f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            TrustedDevicesCard(
                                devices = connectedDevices,
                                onVerifyClick = onVerifyDeviceClick
                            )
                        }

                        item {
                            ActiveSessionsCard(
                                sessions = activeSessions,
                                onTriggerRekey = onRekeySessionClick
                            )
                        }

                        item {
                            PermissionsOverviewCard(permissions = permissions)
                        }

                        item {
                            PrivacyControlsCard(
                                privacyUi = privacyUi,
                                onDiscoverabilityToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("discoverability", it) } } else null,
                                onVisibilityToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("visibility", it) } } else null,
                                onBiometricsToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("biometrics", it) } } else null,
                                onAppLockToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("app_lock", it) } } else null
                            )
                        }

                        item {
                            BackupRecoveryCard(onExportKeysClick = onCopyFingerprintClick)
                        }

                        item {
                            SecurityDiagnosticsCard(diagnostics = diagnostics, statsUi = statsUi)
                        }

                        item {
                            SecurityTimelineSection(events = timelineEvents)
                        }

                        item {
                            AdvancedSecurityCard()
                        }
                    }
                }
            } else {
                // Single Column Layout for Phones
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MeshSpacing.ScreenPadding),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = MeshSpacing.ListBottomSpacing
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.IDENTITY) {
                        item {
                            SecurityScoreCard(
                                trustUi = trustUi,
                                statsUi = statsUi
                            )
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.SESSIONS) {
                        item {
                            EncryptionStatusCard(
                                encryptionUi = encryptionUi,
                                onKeyRotationClick = onRotateKeysClick
                            )
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.IDENTITY) {
                        item {
                            LocalIdentityCard(
                                fingerprintUi = fingerprintUi,
                                onCopyFingerprint = onCopyFingerprintClick,
                                onShareFingerprint = onShareFingerprintClick
                            )
                        }

                        if (selectedVerification != null) {
                            item {
                                IdentityVerificationCard(verificationUi = selectedVerification)
                            }
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.DEVICES) {
                        item {
                            TrustedDevicesCard(
                                devices = connectedDevices,
                                onVerifyClick = onVerifyDeviceClick
                            )
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.SESSIONS) {
                        item {
                            ActiveSessionsCard(
                                sessions = activeSessions,
                                onTriggerRekey = onRekeySessionClick
                            )
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.PERMISSIONS) {
                        item {
                            PermissionsOverviewCard(permissions = permissions)
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.PRIVACY) {
                        item {
                            PrivacyControlsCard(
                                privacyUi = privacyUi,
                                onDiscoverabilityToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("discoverability", it) } } else null,
                                onVisibilityToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("visibility", it) } } else null,
                                onBiometricsToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("biometrics", it) } } else null,
                                onAppLockToggle = if (onPrivacyToggle != null) { { onPrivacyToggle("app_lock", it) } } else null
                            )
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.IDENTITY) {
                        item {
                            BackupRecoveryCard(onExportKeysClick = onCopyFingerprintClick)
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.DIAGNOSTICS) {
                        item {
                            SecurityDiagnosticsCard(diagnostics = diagnostics, statsUi = statsUi)
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.DIAGNOSTICS) {
                        item {
                            SecurityTimelineSection(events = timelineEvents)
                        }
                    }

                    if (selectedCategory == SecurityFilterCategory.ALL || selectedCategory == SecurityFilterCategory.PRIVACY) {
                        item {
                            AdvancedSecurityCard()
                        }
                    }
                }
            }
        }
    }
}

private fun cipherSuiteMetric(ui: EncryptionUi): String = ui.cipherSuite
