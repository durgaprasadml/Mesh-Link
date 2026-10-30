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
fun EmergencySettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var emergencyNumber by remember(uiState.emergencyNumber) { mutableStateOf(uiState.emergencyNumber) }
    var sosAutoBroadcast by remember(uiState.sosAutoBroadcast) { mutableStateOf(uiState.sosAutoBroadcast) }
    var shareLiveLocation by remember(uiState.shareLiveLocation) { mutableStateOf(uiState.shareLiveLocation) }
    var emergencyTemplate by remember(uiState.emergencyTemplate) { mutableStateOf(uiState.emergencyTemplate) }
    var showNumberDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                },
                title = { Text("Emergency SOS Settings", fontWeight = FontWeight.Bold) },
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
            // Emergency Hotline
            item {
                Text("Emergency Hotline", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                                        viewModel.setSosAutoBroadcast(it)
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
                                        viewModel.setShareLiveLocation(it)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Emergency Template
            item {
                Text("Emergency Message Template", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
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
                        emergencyNumber = tempNumber.trim().ifBlank { "112" }
                        showNumberDialog = false
                        viewModel.setEmergencyNumber(emergencyNumber)
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
                        emergencyTemplate = tempTemplate.trim().ifBlank { uiState.emergencyTemplate }
                        showTemplateDialog = false
                        viewModel.setEmergencyTemplate(emergencyTemplate)
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
