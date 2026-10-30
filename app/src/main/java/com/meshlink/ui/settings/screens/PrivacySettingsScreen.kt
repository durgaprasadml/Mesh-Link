package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                title = { Text("Privacy & Security", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.large)
        ) {
            // Encryption Banner Card
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MeshTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeshTheme.spacing.mediumLarge),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Security Active",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
                        Column {
                            Text(
                                text = "End-to-End Encryption Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                            Text(
                                text = "AES-256-GCM & Curve25519 ratchet protect all mesh payload frames.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }



            // Devices & Keys
            item {
                Text("Trusted Devices & Keys", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Trusted Devices",
                            subtitle = "${uiState.trustedDevicesCount} verified public key fingerprints in keyring",
                            icon = Icons.Default.Devices,
                            onClick = { viewModel.showToast("Keyring details opened") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Session Management",
                            subtitle = "View active BLE identity sessions & reset nonces",
                            icon = Icons.Default.VpnKey,
                            onClick = { viewModel.showToast("Active sessions inspected") }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
        }
    }
}
