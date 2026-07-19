package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.components.settings.StorageCategory
import com.meshlink.ui.components.settings.StorageUsageBar
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(onBack: () -> Unit) {
    val totalStorage = 2L * 1024 * 1024 * 1024 // 2 GB mock
    val storageCategories = listOf(
        StorageCategory("Database (Chats)", 250 * 1024 * 1024L, MaterialTheme.colorScheme.primary),
        StorageCategory("Media Cache", 500 * 1024 * 1024L, MaterialTheme.colorScheme.tertiary),
        StorageCategory("Voice Notes", 120 * 1024 * 1024L, MaterialTheme.colorScheme.secondary),
        StorageCategory("Free Space", totalStorage - 870 * 1024 * 1024L, MaterialTheme.colorScheme.surfaceVariant)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Storage & Data") },
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
                Text("Usage Overview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                        StorageUsageBar(
                            categories = storageCategories.dropLast(1), // Exclude free space from bar segments
                            totalBytes = 1000 * 1024 * 1024L
                        )
                    }
                }
            }

            item {
                Text("Manage Storage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Clear Media Cache",
                            subtitle = "Frees up space without deleting chats.",
                            icon = Icons.Default.Image,
                            onClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Optimize Database",
                            subtitle = "Reindexes messages to save space.",
                            icon = Icons.Default.Storage,
                            onClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Delete All Data",
                            subtitle = "Erase all chats and settings.",
                            icon = Icons.Default.DeleteOutline,
                            iconTint = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.error,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}
