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

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    val usedAppBytes = (uiState.databaseBytes + uiState.mediaCacheBytes + uiState.voiceNotesBytes).coerceAtLeast(1L)
    val storageCategories = listOf(
        StorageCategory("Database (Chats)", uiState.databaseBytes, MaterialTheme.colorScheme.primary),
        StorageCategory("Media Cache", uiState.mediaCacheBytes, MaterialTheme.colorScheme.tertiary),
        StorageCategory("Voice Notes", uiState.voiceNotesBytes, MaterialTheme.colorScheme.secondary),
        StorageCategory("Free Space", uiState.freeSpaceBytes, MaterialTheme.colorScheme.surfaceVariant)
    )

    fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Storage & Data", fontWeight = FontWeight.Bold) },
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
                Text("Storage Usage Overview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                        StorageUsageBar(
                            categories = storageCategories.dropLast(1),
                            totalBytes = usedAppBytes
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
                            subtitle = "Frees up ${formatSize(uiState.mediaCacheBytes)} without deleting message history",
                            icon = Icons.Default.Image,
                            onClick = { showClearDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Optimize Database Size",
                            subtitle = "Reindex message database & compact WAL log (${formatSize(uiState.databaseBytes)} current)",
                            icon = Icons.Default.Storage,
                            onClick = { viewModel.optimizeDatabase() }
                        )
                    }
                }
            }



            item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
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
