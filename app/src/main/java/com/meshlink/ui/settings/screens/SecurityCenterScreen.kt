package com.meshlink.ui.settings.screens

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SecurityViewModel

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    onBack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Security Center") },
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
                Text(
                    text = "Manage Active Sessions and Trusted Devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = MeshTheme.spacing.small)
                )
            }

            // ────────── Active Sessions ──────────
            if (uiState.activeSessions.isNotEmpty()) {
                item {
                    SectionHeader("Active Sessions")
                }
                items(uiState.activeSessions.toList()) { peerId ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = MeshTheme.spacing.small),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MeshTheme.shapes.large
                    ) {
                        SettingsItemRow(
                            title = "Peer: ${peerId.take(8)}",
                            subtitle = "Encrypted Connection",
                            icon = Icons.Default.DeviceHub,
                            trailingContent = {
                                IconButton(onClick = { viewModel.terminateSession(peerId) }) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Terminate", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
                item {
                    TextButton(onClick = { viewModel.terminateAllSessions() }) {
                        Text("Terminate All Sessions", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                item {
                    SectionHeader("Active Sessions")
                    Text("No active sessions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ────────── Trusted & Blocked Devices ──────────
            val trusted = uiState.trustedDevices.filter { it.value.name == "TRUSTED" || it.value.name == "VERIFIED" }
            if (trusted.isNotEmpty()) {
                item {
                    SectionHeader("Trusted Devices")
                }
                items(trusted.keys.toList()) { peerId ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = MeshTheme.spacing.small),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MeshTheme.shapes.large
                    ) {
                        SettingsItemRow(
                            title = peerId.take(12),
                            subtitle = trusted[peerId]?.name ?: "UNKNOWN",
                            icon = Icons.Default.VerifiedUser,
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeTrust(peerId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Trust")
                                }
                            }
                        )
                    }
                }
            } else {
                item {
                    SectionHeader("Trusted Devices")
                    Text("No trusted devices", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            val blocked = uiState.trustedDevices.filter { it.value.name == "BLOCKED" }
            if (blocked.isNotEmpty()) {
                item {
                    SectionHeader("Blocked Devices")
                }
                items(blocked.keys.toList()) { peerId ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = MeshTheme.spacing.small),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MeshTheme.shapes.large
                    ) {
                        SettingsItemRow(
                            title = peerId.take(12),
                            subtitle = "Blocked",
                            icon = Icons.Default.Block,
                            textColor = MaterialTheme.colorScheme.onErrorContainer,
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeTrust(peerId) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Unblock", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
}
