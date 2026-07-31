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
fun NotificationsSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var messageNotifications by remember { mutableStateOf(uiState.messageNotifications) }
    var sosAlertsEnabled by remember { mutableStateOf(uiState.sosAlertsEnabled) }
    var vibrationEnabled by remember { mutableStateOf(uiState.vibrationEnabled) }
    var ledFlashEnabled by remember { mutableStateOf(uiState.ledFlashEnabled) }
    var selectedSound by remember { mutableStateOf(uiState.notificationSound) }
    var selectedPriority by remember { mutableStateOf(uiState.notificationPriority) }
    var showSoundDialog by remember { mutableStateOf(false) }

    MeshScreen(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Notifications") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            // General Notifications
            item {
                Text("Alert Channels", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Message Notifications",
                            subtitle = "Show banners and popups for direct & group messages",
                            icon = Icons.Default.Notifications,
                            trailingContent = {
                                Switch(
                                    checked = messageNotifications,
                                    onCheckedChange = {
                                        messageNotifications = it
                                        viewModel.showToast(if (it) "Notifications enabled" else "Notifications disabled")
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "SOS Emergency Alerts",
                            subtitle = "Always sound loud alert for incoming emergency beacons",
                            icon = Icons.Default.Warning,
                            trailingContent = {
                                Switch(
                                    checked = sosAlertsEnabled,
                                    onCheckedChange = {
                                        sosAlertsEnabled = it
                                        viewModel.showToast(if (it) "SOS alerts enabled" else "SOS alerts disabled")
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Sound & Haptics
            item {
                Text("Sound & Feedback", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Notification Sound",
                            subtitle = selectedSound,
                            icon = Icons.Default.MusicNote,
                            onClick = { showSoundDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Vibration Pattern",
                            subtitle = "Haptic feedback for incoming mesh packets",
                            icon = Icons.Default.Vibration,
                            trailingContent = {
                                Switch(
                                    checked = vibrationEnabled,
                                    onCheckedChange = {
                                        vibrationEnabled = it
                                        viewModel.showToast(if (it) "Vibration enabled" else "Vibration disabled")
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "LED Flash Indicator",
                            subtitle = "Blink device notification LED on packet arrival",
                            icon = Icons.Default.LightMode,
                            trailingContent = {
                                Switch(
                                    checked = ledFlashEnabled,
                                    onCheckedChange = {
                                        ledFlashEnabled = it
                                        viewModel.showToast(if (it) "LED indicator enabled" else "LED indicator disabled")
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Priority
            item {
                Text("Priority & Do Not Disturb", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                        Text("Notification Priority: $selectedPriority", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedPriority == "High",
                                onClick = {
                                    selectedPriority = "High"
                                    viewModel.showToast("Priority set to High")
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) { Text("High") }
                            SegmentedButton(
                                selected = selectedPriority == "Normal",
                                onClick = {
                                    selectedPriority = "Normal"
                                    viewModel.showToast("Priority set to Normal")
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) { Text("Normal") }
                            SegmentedButton(
                                selected = selectedPriority == "Low",
                                onClick = {
                                    selectedPriority = "Low"
                                    viewModel.showToast("Priority set to Low")
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) { Text("Low") }
                        }
                    }
                }
            }
        }
    }

    // Sound Picker Dialog
    if (showSoundDialog) {
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
            title = { Text("Select Notification Sound") },
            text = {
                Column {
                    listOf("Mesh Pulse (Default)", "Chime", "Beacon Signal", "Radar Ping", "System Silent").forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MeshTheme.spacing.small),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (sound == selectedSound),
                                onClick = {
                                    selectedSound = sound
                                    showSoundDialog = false
                                    viewModel.showToast("Sound changed to $sound")
                                }
                            )
                            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                            Text(sound, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
