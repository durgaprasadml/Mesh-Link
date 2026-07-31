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
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.components.settings.StorageCategory
import com.meshlink.ui.components.settings.StorageUsageBar
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    val totalStorage = 2L * 1024 * 1024 * 1024 // 2 GB mock
    val storageCategories = listOf(
        StorageCategory("Database (Chats)", 250 * 1024 * 1024L, MaterialTheme.colorScheme.primary),
        StorageCategory("Media Cache", 500 * 1024 * 1024L, MaterialTheme.colorScheme.tertiary),
        StorageCategory("Voice Notes", 120 * 1024 * 1024L, MaterialTheme.colorScheme.secondary),
        StorageCategory("Free Space", totalStorage - 870 * 1024 * 1024L, MaterialTheme.colorScheme.surfaceVariant)
    )

    MeshScreen(
        topBar = {
            com.meshlink.ui.components.MeshTopAppBar(
                title = "Storage & Data",
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
            item {
                Text("Storage Usage Overview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                        StorageUsageBar(
                            categories = storageCategories.dropLast(1),
                            totalBytes = 1000 * 1024 * 1024L
                        )
                    }
                }
            }

            item {
                Text("Manage Cache & Storage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Clear Media Cache",
                            subtitle = "Frees up 500 MB without deleting message history",
                            icon = Icons.Default.Image,
                            onClick = { showClearDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Optimize Database Size",
                            subtitle = "Reindex message database & compact WAL log (250 MB current)",
                            icon = Icons.Default.Storage,
                            onClick = { viewModel.optimizeDatabase() }
                        )
                    }
                }
            }

            item {
                Text("Backup & Export", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Export Messages",
                            subtitle = "Save encrypted HTML/JSON chat archive to local storage",
                            icon = Icons.Default.FileUpload,
                            onClick = { viewModel.showToast("Exporting encrypted chat archive...") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Create Local Backup",
                            subtitle = "Generate secure passphrase-encrypted local backup",
                            icon = Icons.Default.Backup,
                            onClick = { viewModel.backupMessages() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Restore Backup",
                            subtitle = "Import messages from a previous backup file",
                            icon = Icons.Default.Restore,
                            onClick = { viewModel.restoreMessages() }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
            title = { Text("Clear Media Cache?") },
            text = { Text("This will remove temporary media files (images, audio clips). Cached media will re-download when viewed again.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearMediaCache()
                    }
                ) {
                    Text("Clear Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
