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
fun NetworkSettingsScreen(
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
                title = { Text("Network & Transport") },
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
            // Transport Mode
            item {
                Text("Transport Mode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        val modes = listOf("BLE", "WIFI", "HYBRID", "AUTOMATIC")
                        modes.forEachIndexed { index, mode ->
                            SettingsItemRow(
                                title = mode,
                                subtitle = "Prioritize $mode transport",
                                icon = Icons.Default.Transform,
                                trailingContent = {
                                    RadioButton(
                                        selected = uiState.preferredTransport == mode,
                                        onClick = { viewModel.setPreferredTransport(mode) }
                                    )
                                }
                            )
                            if (index < modes.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            }
                        }
                    }
                }
            }

            // Bluetooth
            item {
                Text("Bluetooth Low Energy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Enable BLE",
                            subtitle = "Master switch for Bluetooth transport",
                            icon = Icons.Default.Bluetooth,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.isBleEnabled, 
                                    onCheckedChange = { viewModel.setBleEnabled(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Advertising",
                            subtitle = "Broadcast your presence",
                            icon = Icons.Default.CellTower,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.bleAdvertisingEnabled, 
                                    onCheckedChange = { viewModel.setBleAdvertisingEnabled(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Scanning",
                            subtitle = "Listen for other devices",
                            icon = Icons.Default.Radar,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.bleScanningEnabled, 
                                    onCheckedChange = { viewModel.setBleScanningEnabled(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Auto Restart",
                            subtitle = "Automatically restart BLE if it crashes",
                            icon = Icons.Default.Autorenew,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.bleAutoRestart, 
                                    onCheckedChange = { viewModel.setBleAutoRestart(it) }
                                ) 
                            }
                        )
                    }
                }
            }

            // Wi-Fi Direct
            item {
                Text("Wi-Fi Direct", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Enable Wi-Fi Direct",
                            subtitle = "Master switch for high-bandwidth transport",
                            icon = Icons.Default.Wifi,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.isWifiDirectEnabled, 
                                    onCheckedChange = { viewModel.setWifiDirectEnabled(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Auto Connect",
                            subtitle = "Automatically accept connections",
                            icon = Icons.Default.Link,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.wifiAutoConnect, 
                                    onCheckedChange = { viewModel.setWifiAutoConnect(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Peer Discovery",
                            subtitle = "Actively search for Wi-Fi peers",
                            icon = Icons.Default.Search,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.wifiPeerDiscoveryEnabled, 
                                    onCheckedChange = { viewModel.setWifiPeerDiscoveryEnabled(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Preferred Group Owner",
                            subtitle = "Force Group Owner role during negotiation",
                            icon = Icons.Default.Star,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.wifiPreferredGroupOwner, 
                                    onCheckedChange = { viewModel.setWifiPreferredGroupOwner(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Auto Reconnect",
                            subtitle = "Reconnect if link drops",
                            icon = Icons.Default.Sync,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.wifiReconnectEnabled, 
                                    onCheckedChange = { viewModel.setWifiReconnectEnabled(it) }
                                ) 
                            }
                        )
                    }
                }
            }

            // Mesh Relay
            item {
                Text("Mesh Routing & Relay", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Enable Relay",
                            subtitle = "Help route messages for other peers",
                            icon = Icons.Default.Memory,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.isMeshRelayEnabled, 
                                    onCheckedChange = { viewModel.setMeshRelayEnabled(it) }
                                ) 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        Column(modifier = Modifier.padding(MeshTheme.spacing.medium)) {
                            Text("Max Hops: ${uiState.meshMaxHops}")
                            Slider(
                                value = uiState.meshMaxHops.toFloat(),
                                onValueChange = { viewModel.setMeshMaxHops(it.toInt()) },
                                valueRange = 1f..15f
                            )
                        }
                    }
                }
            }

            // Advanced
            item {
                Text("Advanced Configuration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Strict Encryption",
                            subtitle = "Drop unencrypted packets",
                            icon = Icons.Default.Security,
                            trailingContent = { 
                                Switch(
                                    checked = uiState.advancedEncryptionEnforcement, 
                                    onCheckedChange = { viewModel.setAdvancedEncryptionEnforcement(it) }
                                ) 
                            }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
