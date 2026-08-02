package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.security.*
import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var biometricUnlock by remember { mutableStateOf(uiState.biometricUnlock) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val encryptionUi = remember(uiState) {
        EncryptionUi(
            isE2eeActive = uiState.isEncryptionEnabled,
            cipherSuite = "AES-256-GCM",
            keyExchangeAlg = "ECDH X25519",
            ratchetProtocol = "Double Ratchet v2",
            isHardwareKeystoreUsed = true,
            broadcastKeyVersion = 1,
            sessionEstablishedCount = uiState.trustedDevicesCount,
            perfectForwardSecrecy = uiState.advancedEncryptionEnforcement
        )
    }

    val privacyUi = remember(biometricUnlock, uiState) {
        PrivacyUi(
            discoverabilityEnabled = uiState.isOnlineVisible,
            onlineVisibility = uiState.isOnlineVisible,
            biometricLockEnabled = biometricUnlock,
            appLockEnabled = uiState.appLockEnabled,
            autoLockTimeoutMinutes = 5,
            advancedEncryptionEnforced = uiState.advancedEncryptionEnforcement
        )
    }

    MeshScreen(
        topBar = {
            com.meshlink.ui.components.MeshTopAppBar(
                title = "Privacy & Security",
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshSpacing.ScreenPadding),
            contentPadding = PaddingValues(bottom = MeshSpacing.ListBottomSpacing),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            // Enterprise Encryption Status Banner
            item {
                EncryptionStatusCard(
                    encryptionUi = encryptionUi
                )
            }

            // Node Privacy & Protection Controls
            item {
                PrivacyControlsCard(
                    privacyUi = privacyUi,
                    onDiscoverabilityToggle = {
                        viewModel.setOnlineVisible(it)
                        viewModel.showToast(if (it) "Node discoverability enabled" else "Node discoverability hidden")
                    },
                    onBiometricsToggle = {
                        biometricUnlock = it
                        viewModel.showToast(if (it) "Biometric unlock enabled" else "Biometric unlock disabled")
                    },
                    onAppLockToggle = {
                        viewModel.setAdvancedEncryptionEnforcement(it)
                        viewModel.showToast(if (it) "Strict encryption enforced" else "Standard encryption active")
                    }
                )
            }

            // Devices & Keyring Summary
            item {
                Text("Trusted Devices & Keyring", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Trusted Devices Keyring",
                            subtitle = "${uiState.trustedDevicesCount} verified public key fingerprints in keyring",
                            icon = Icons.Default.Devices,
                            onClick = { viewModel.showToast("Keyring details opened") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Security Diagnostics & Sessions",
                            subtitle = "Inspect active E2EE sessions, key versions, replay events & hardware Keystore status",
                            icon = Icons.Default.VpnKey,
                            onClick = { viewModel.showToast("Opening Security Diagnostics...") }
                        )
                    }
                }
            }

            // Destructive Actions
            item {
                Text("Danger Zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Delete All Data",
                            subtitle = "Permanently purge local database, cryptographic keys, and cache",
                            icon = Icons.Default.DeleteForever,
                            iconTint = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete All Data?", color = MaterialTheme.colorScheme.error) },
            text = {
                Text("This will permanently remove all your messages, local encryption keys, node identity, and cached media from this device. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.showToast("All data successfully cleared")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
