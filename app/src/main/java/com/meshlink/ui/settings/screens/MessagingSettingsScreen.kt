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
fun MessagingSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var readReceipts by remember { mutableStateOf(uiState.readReceipts) }
    var deliveryStatus by remember { mutableStateOf(uiState.deliveryStatus) }
    var autoRetryCount by remember { mutableFloatStateOf(uiState.autoRetryCount.toFloat()) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(uiState.mediaQuality) }
    var selectedRetention by remember { mutableStateOf(uiState.messageRetention) }

    MeshScreen(
        topBar = {
            com.meshlink.ui.components.MeshTopAppBar(
                title = "Messaging & Media",
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
            // Media Transmission
            item {
                Text("Media Transmission", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Media Quality",
                            subtitle = "Current: $selectedQuality",
                            icon = Icons.Default.HighQuality,
                            onClick = { showQualityDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Auto Download",
                            subtitle = uiState.autoDownload,
                            icon = Icons.Default.CloudDownload,
                            onClick = { viewModel.showToast("Auto Download settings saved") }
                        )
                    }
                }
            }

            // Message Receipts & Delivery
            item {
                Text("Receipts & Delivery", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Read Receipts",
                            subtitle = "Send & request checkmarks when messages are read",
                            icon = Icons.Default.DoneAll,
                            trailingContent = {
                                Switch(
                                    checked = readReceipts,
                                    onCheckedChange = {
                                        readReceipts = it
                                        viewModel.showToast(if (it) "Read receipts enabled" else "Read receipts disabled")
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Delivery Status Notifications",
                            subtitle = "Show tick indicators when peers acknowledge hop delivery",
                            icon = Icons.Default.MarkChatRead,
                            trailingContent = {
                                Switch(
                                    checked = deliveryStatus,
                                    onCheckedChange = {
                                        deliveryStatus = it
                                        viewModel.showToast(if (it) "Delivery status enabled" else "Delivery status disabled")
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Mesh Reliability
            item {
                Text("Reliability & Storage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                            Text("Automatic Message Retries: ${autoRetryCount.toInt()} attempts", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                            Text("Attempts to resend dropped packet frames over mesh hops.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                            Slider(
                                value = autoRetryCount,
                                onValueChange = { autoRetryCount = it },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Message Retention",
                            subtitle = "Auto-delete policy: $selectedRetention",
                            icon = Icons.Default.AutoDelete,
                            onClick = { showRetentionDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Default Chat Wallpaper",
                            subtitle = "Customize chat background theme (Coming Soon)",
                            icon = Icons.Default.Wallpaper,
                            trailingContent = {
                                AssistChip(
                                    onClick = { },
                                    label = { Text("Coming Soon", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            icon = { Icon(Icons.Default.HighQuality, contentDescription = null) },
            title = { Text("Media Quality") },
            text = {
                Column {
                    listOf("Auto (Recommended)", "HD (Original Quality)", "Compressed (Save Mesh Bandwidth)").forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MeshTheme.spacing.small),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (quality == selectedQuality),
                                onClick = {
                                    selectedQuality = quality
                                    showQualityDialog = false
                                    viewModel.showToast("Media quality updated")
                                }
                            )
                            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                            Text(quality, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Retention Selection Dialog
    if (showRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            icon = { Icon(Icons.Default.AutoDelete, contentDescription = null) },
            title = { Text("Message Retention") },
            text = {
                Column {
                    listOf("Forever", "30 Days", "7 Days", "24 Hours").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MeshTheme.spacing.small),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == selectedRetention),
                                onClick = {
                                    selectedRetention = option
                                    showRetentionDialog = false
                                    viewModel.showToast("Message retention set to $option")
                                }
                            )
                            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                            Text(option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
