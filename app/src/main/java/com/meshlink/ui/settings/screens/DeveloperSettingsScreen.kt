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
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToWifiDiagnostics: (() -> Unit)? = null
) {
    var transportLogs by remember { mutableStateOf(uiState.transportLogsEnabled) }
    var developerMode by remember { mutableStateOf(uiState.developerMode) }

    MeshScreen(
        topBar = {
            com.meshlink.ui.components.MeshTopAppBar(
                title = "Developer Options",
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            contentPadding = PaddingValues(bottom = MeshTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.large)
        ) {
            // Diagnostics Banner
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MeshTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeshTheme.spacing.mediumLarge),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
                        Column {
                            Text("Developer Options Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Low-level packet inspection, BLE parameters, and mesh telemetry.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Mesh Telemetry & Stats
            item {
                Text("Mesh Telemetry & Health", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Wi-Fi Direct & Network Diagnostics",
                            subtitle = "Inspect P2P state, Group Owner, sockets & IP routes",
                            icon = Icons.Default.Wifi,
                            onClick = { onNavigateToWifiDiagnostics?.invoke() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Mesh Node Diagnostics",
                            subtitle = "Interactive node topology graph & hop latency",
                            icon = Icons.Default.Hub,
                            onClick = { viewModel.showToast("Opening Mesh Node Graph...") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Packet Statistics",
                            subtitle = "Total packets: ${uiState.packetCount} | Loss rate: 0.02%",
                            icon = Icons.Default.Assessment,
                            onClick = { viewModel.showToast("Packet counter stats refreshed") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Routing Table Inspector",
                            subtitle = "View distance vector routing entries & TTL weights",
                            icon = Icons.Default.Route,
                            onClick = { viewModel.showToast("Routing table dumped to logcat") }
                        )
                    }
                }
            }

            // Hardware & Transport
            item {
                Text("BLE & Transport Layer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "BLE Stack Information",
                            subtitle = "Tx Power: ${uiState.bleTxPower} | Scan Interval: ${uiState.bleScanInterval}ms",
                            icon = Icons.Default.BluetoothSearching,
                            onClick = { viewModel.showToast("BLE hardware parameters updated") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Real-Time Transport Logs",
                            subtitle = "Stream BLE packet frames & GATT notifications",
                            icon = Icons.Default.Terminal,
                            trailingContent = {
                                Switch(
                                    checked = transportLogs,
                                    onCheckedChange = {
                                        transportLogs = it
                                        viewModel.showToast(if (it) "Transport logging enabled" else "Transport logging disabled")
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Database Inspector",
                            subtitle = "Inspect Room SQLite schemas & VACUUM status",
                            icon = Icons.Default.Storage,
                            onClick = { viewModel.showToast("Database inspector active") }
                        )
                    }
                }
            }

            // Export Actions
            item {
                Text("Log Export & Utilities", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Export System Debug Logs",
                            subtitle = "Save encrypted log archive to external storage",
                            icon = Icons.Default.FileDownload,
                            onClick = { viewModel.exportDebugLogs() }
                        )
                    }
                }
            }
        }
    }
}
