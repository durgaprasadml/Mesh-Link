package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme

import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperOptionsScreen(
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
                title = { Text("Developer Options") },
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MeshTheme.shapes.medium
                ) {
                    Text(
                        text = "WARNING: These settings are intended for development and debugging. They may cause unexpected behavior or data loss.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(MeshTheme.spacing.medium)
                    )
                }
            }

            item {
                Text("Testing & Mocking", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        var mockMesh by remember { mutableStateOf(false) }
                        SettingsItemRow(
                            title = "Mesh Simulator",
                            subtitle = "Mock 100+ nodes to test UI performance.",
                            icon = Icons.Default.Science,
                            trailingContent = { Switch(checked = mockMesh, onCheckedChange = { mockMesh = it }) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        var packetViewer by remember { mutableStateOf(false) }
                        SettingsItemRow(
                            title = "Raw Packet Viewer",
                            subtitle = "Log all incoming/outgoing protocol buffers.",
                            icon = Icons.Default.BugReport,
                            trailingContent = { Switch(checked = packetViewer, onCheckedChange = { packetViewer = it }) }
                        )
                    }
                }
            }
        }
    }
}
