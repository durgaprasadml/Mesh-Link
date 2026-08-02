package com.meshlink.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.MeshTopAppBar
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

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
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Dashboard, 1 = Trusted Devices, 2 = Audit & Privacy

    MeshScreen(
        modifier = modifier,
        topBar = {
            MeshTopAppBar(
                title = "Security & Trust Control",
                onBackClick = onBackClick,
                actions = {
                    if (onRefreshClick != null) {
                        IconButton(onClick = onRefreshClick) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Diagnostics")
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Enterprise Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Devices & Keys (${connectedDevices.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit & Privacy") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MeshSpacing.ScreenPadding),
                contentPadding = PaddingValues(
                    top = MeshSpacing.CardSpacing,
                    bottom = MeshSpacing.ListBottomSpacing
                ),
                verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Overview Tab
                        item {
                            SecurityDashboardHeader(
                                trustUi = trustUi,
                                encryptionUi = encryptionUi,
                                statsUi = statsUi,
                                onRotateKeysClick = onRotateKeysClick
                            )
                        }

                        item {
                            EncryptionStatusCard(
                                encryptionUi = encryptionUi,
                                onKeyRotationClick = onRotateKeysClick
                            )
                        }

                        item {
                            FingerprintCard(
                                fingerprintUi = fingerprintUi,
                                onCopyClick = onCopyFingerprintClick,
                                onShareClick = onShareFingerprintClick
                            )
                        }

                        if (selectedVerification != null) {
                            item {
                                IdentityVerificationCard(
                                    verificationUi = selectedVerification
                                )
                            }
                        }

                        item {
                            SecurityStatisticsGrid(stats = statsUi)
                        }
                    }

                    1 -> {
                        // Devices & Keys Tab
                        item {
                            ConnectedDevicesSection(
                                connectedDevices = connectedDevices,
                                onVerifyClick = onVerifyDeviceClick
                            )
                        }

                        if (activeSessions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Active E2EE Sessions",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            items(activeSessions) { session ->
                                SessionSecurityCard(
                                    sessionUi = session,
                                    onTriggerRekeyClick = if (onRekeySessionClick != null) { { onRekeySessionClick(session) } } else null
                                )
                            }
                        }

                        if (selectedVerification != null) {
                            item {
                                QRVerificationCard(
                                    verificationUi = selectedVerification
                                )
                            }
                        }
                    }

                    2 -> {
                        // Audit & Privacy Tab
                        item {
                            SecurityAlertsSection(
                                alerts = alerts,
                                onAcknowledgeClick = onAcknowledgeAlertClick
                            )
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
                            SecurityTimelineSection(events = timelineEvents)
                        }
                    }
                }
            }
        }
    }
}
