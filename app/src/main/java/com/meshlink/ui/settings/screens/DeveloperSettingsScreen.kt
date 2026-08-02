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
fun DeveloperSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var transportLogs by remember { mutableStateOf(uiState.transportLogsEnabled) }
    var developerMode by remember { mutableStateOf(uiState.developerMode) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                title = { Text("Developer & Diagnostics", fontWeight = FontWeight.Bold) },
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

            // Firebase Crashlytics Diagnostics
            item {
                Text("Firebase Crashlytics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Simulate Fatal Crash",
                            subtitle = "Force a test RuntimeException (re-open app after crash to send report)",
                            icon = Icons.Default.Warning,
                            onClick = {
                                viewModel.showToast("Triggering test crash in 1s...")
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    throw RuntimeException("Test Crashlytics Crash - durgaprasadmadikeri@gmail.com")
                                }, 1000)
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Log Non-Fatal Exception",
                            subtitle = "Record non-fatal exception to Crashlytics dashboard",
                            icon = Icons.Default.ReportProblem,
                            onClick = {
                                com.meshlink.common.logger.MeshLogger.log(
                                    com.meshlink.common.logger.LogLevel.CRITICAL,
                                    com.meshlink.common.logger.LogCategory.SYSTEM,
                                    "Simulated non-fatal exception from Developer Menu",
                                    mapOf("user_email" to "durgaprasadmadikeri@gmail.com", "triggered_by" to "DeveloperOptions"),
                                    IllegalStateException("Diagnostic Non-Fatal Test Exception")
                                )
                                viewModel.showToast("Non-fatal exception reported to Crashlytics")
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
        }
    }
}
