package com.meshlink.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var showLicensesDialog by remember { mutableStateOf(false) }

    MeshScreen(
        topBar = {
            com.meshlink.ui.components.MeshTopAppBar(
                title = "About Mesh Link",
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
            // App Banner Header
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeshTheme.spacing.extraLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Mesh Link Logo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
                        Text(
                            text = "Mesh Link",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                        Text(
                            text = "Decentralized Off-Grid Peer-to-Peer Mesh Network",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                        AssistChip(
                            onClick = { },
                            label = { Text("Version 1.4.0 (Build 1042)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Software & Licenses
            item {
                Text("App Details & Licenses", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Open Source Libraries",
                            subtitle = "Jetpack Compose, Kotlin Coroutines, Hilt, Room, Material 3",
                            icon = Icons.Default.Code,
                            onClick = { showLicensesDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Software License",
                            subtitle = "Apache License 2.0",
                            icon = Icons.Default.Gavel,
                            onClick = { viewModel.showToast("Licensed under Apache 2.0") }
                        )
                    }
                }
            }

            // Legal & Social Links
            item {
                Text("Legal & Community", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Privacy Policy",
                            subtitle = "Zero tracking policy - no data leaves local mesh",
                            icon = Icons.Default.Policy,
                            onClick = { viewModel.showToast("Privacy policy opened") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Terms of Service",
                            subtitle = "Usage agreement & liability disclaimers",
                            icon = Icons.Default.Description,
                            onClick = { viewModel.showToast("Terms of service opened") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "GitHub Repository",
                            subtitle = "View source code & contribute",
                            icon = Icons.Default.Public,
                            onClick = { viewModel.showToast("GitHub link opened") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Report an Issue",
                            subtitle = "Submit bug reports or feature requests",
                            icon = Icons.Default.BugReport,
                            onClick = { viewModel.showToast("Report issue form opened") }
                        )
                    }
                }
            }
        }
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            icon = { Icon(Icons.Default.Code, contentDescription = null) },
            title = { Text("Open Source Libraries") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)
                ) {
                    item { Text("• AndroidX Jetpack Compose (Apache 2.0)") }
                    item { Text("• Kotlin Coroutines & Flow (Apache 2.0)") }
                    item { Text("• Dagger Hilt Dependency Injection (Apache 2.0)") }
                    item { Text("• Room Database (Apache 2.0)") }
                    item { Text("• Material Design 3 Components (Apache 2.0)") }
                    item { Text("• Google Protocol Buffers (BSD-3-Clause)") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
