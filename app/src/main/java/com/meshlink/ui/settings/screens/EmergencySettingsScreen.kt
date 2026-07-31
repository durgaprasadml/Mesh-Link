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
fun EmergencySettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var emergencyNumber by remember { mutableStateOf(uiState.emergencyNumber) }
    var sosAutoBroadcast by remember { mutableStateOf(uiState.sosAutoBroadcast) }
    var shareLiveLocation by remember { mutableStateOf(uiState.shareLiveLocation) }
    var emergencyTemplate by remember { mutableStateOf(uiState.emergencyTemplate) }
    var showNumberDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    MeshScreen(
        topBar = {
            com.meshlink.ui.components.MeshTopAppBar(
                title = "Emergency SOS Settings",
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            contentPadding = PaddingValues(bottom = MeshTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.large)
        ) {
            // Emergency Contacts & Number
            item {
                Text("Emergency Hotline & Contacts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Emergency Hotline Number",
                            subtitle = "Default dialer target: $emergencyNumber",
                            icon = Icons.Default.PhoneInTalk,
                            onClick = { showNumberDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Emergency Contacts",
                            subtitle = "Manage designated ICE contacts for broadcast alerts",
                            icon = Icons.Default.ContactPhone,
                            onClick = { viewModel.showToast("Emergency contacts view opened") }
                        )
                    }
                }
            }

            // SOS Broadcast & Location
            item {
                Text("Broadcast & Location Sharing", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "SOS Auto Broadcast",
                            subtitle = "Automatically trigger mesh emergency beacon when SOS activated",
                            icon = Icons.Default.Warning,
                            trailingContent = {
                                Switch(
                                    checked = sosAutoBroadcast,
                                    onCheckedChange = {
                                        sosAutoBroadcast = it
                                        viewModel.showToast(if (it) "SOS auto broadcast enabled" else "SOS auto broadcast disabled")
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Share Live Location over Mesh",
                            subtitle = "Attach GPS telemetry coordinates to broadcast packets",
                            icon = Icons.Default.MyLocation,
                            trailingContent = {
                                Switch(
                                    checked = shareLiveLocation,
                                    onCheckedChange = {
                                        shareLiveLocation = it
                                        viewModel.showToast(if (it) "Live location sharing enabled" else "Live location sharing disabled")
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Emergency Template & Medical Info
            item {
                Text("Emergency Information & Template", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Emergency Message Template",
                            subtitle = emergencyTemplate,
                            icon = Icons.Default.EditNote,
                            onClick = { showTemplateDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Medical Information & ICE",
                            subtitle = "Blood type, allergies, medical notes (Coming Soon)",
                            icon = Icons.Default.MedicalInformation,
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

    // Number Change Dialog
    if (showNumberDialog) {
        var tempNumber by remember { mutableStateOf(emergencyNumber) }
        AlertDialog(
            onDismissRequest = { showNumberDialog = false },
            icon = { Icon(Icons.Default.Phone, contentDescription = null) },
            title = { Text("Set Emergency Hotline") },
            text = {
                OutlinedTextField(
                    value = tempNumber,
                    onValueChange = { tempNumber = it },
                    label = { Text("Emergency Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        emergencyNumber = tempNumber
                        showNumberDialog = false
                        viewModel.showToast("Emergency number updated to $emergencyNumber")
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNumberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Template Edit Dialog
    if (showTemplateDialog) {
        var tempTemplate by remember { mutableStateOf(emergencyTemplate) }
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
            title = { Text("Emergency Broadcast Template") },
            text = {
                OutlinedTextField(
                    value = tempTemplate,
                    onValueChange = { tempTemplate = it },
                    label = { Text("Template Message") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        emergencyTemplate = tempTemplate
                        showTemplateDialog = false
                        viewModel.showToast("Emergency template updated")
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
