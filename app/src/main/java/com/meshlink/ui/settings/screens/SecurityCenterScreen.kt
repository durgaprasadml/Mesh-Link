package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme

import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Security Center") },
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
            item {
                Text(
                    text = "All communications on Mesh Link are protected with end-to-end encryption. Your private keys never leave this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = MeshTheme.spacing.small)
                )
            }

            item {
                Text("Encryption Protocol", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "AES-256-GCM",
                            subtitle = "Military-grade encryption active",
                            icon = Icons.Default.Shield,
                            trailingContent = { Text("Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "ECDH Key Exchange",
                            subtitle = "Forward secrecy enabled",
                            icon = Icons.Default.Key,
                            trailingContent = { Text("Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            item {
                Text("Trust Management", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "App Lock",
                            subtitle = "Require authentication to open app",
                            icon = Icons.Default.Lock,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.isAppLockEnabled, 
                                    onCheckedChange = { viewModel.setAppLockEnabled(it) }
                                ) 
                            }
                        )
                        if (uiState.isAppLockEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            SettingsItemRow(
                                title = "Biometric Authentication",
                                subtitle = "Unlock using fingerprint or face",
                                icon = Icons.Default.Security,
                                trailingContent = { 
                                    Switch(
                                        checked = uiState.isBiometricsEnabled, 
                                        onCheckedChange = { viewModel.setBiometricsEnabled(it) }
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
