package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.PeerSecureSession
import com.meshlink.security.data.KeyManager
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.MeshSecurityMonitor
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDiagnosticsScreen(
    cryptoManager: MeshCryptoManager,
    sessionManager: SessionManager,
    rekeyManager: RekeyManager,
    securityMonitor: MeshSecurityMonitor,
    onBack: () -> Unit
) {
    val metricsState by securityMonitor.metricsState.collectAsState()
    val activeSessions = remember { mutableStateListOf<PeerSecureSession>() }
    var hardwareKeystoreUsed by remember { mutableStateOf(false) }
    var localFingerprint by remember { mutableStateOf("") }
    var currentBroadcastVersion by remember { mutableStateOf(1) }

    fun refreshDiagnostics() {
        activeSessions.clear()
        activeSessions.addAll(sessionManager.getAllSessions())
        hardwareKeystoreUsed = cryptoManager.isHardwareKeystoreUsed()
        localFingerprint = cryptoManager.getLocalFingerprint()
        currentBroadcastVersion = cryptoManager.getCurrentBroadcastKeyVersion()
    }

    LaunchedEffect(Unit) {
        refreshDiagnostics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Security Diagnostics", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { refreshDiagnostics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
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
            // Cryptographic Engine Overview Card
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MeshTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
                            Column {
                                Text(
                                    "Security Engine Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "AES-256-GCM E2EE & Ephemeral ECDH Rekeying",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = MeshTheme.spacing.medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Hardware Keystore:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    if (hardwareKeystoreUsed) "Hardware-Backed (AndroidKeyStore)" else "Software Key Fallback",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column {
                                Text("Broadcast Key:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "Version $currentBroadcastVersion",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

                        Text("Identity Key Fingerprint:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            localFingerprint,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Security Metrics
            item {
                Text("Security Events & Metrics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                        MetricRow("Replay Attacks Rejected", metricsState.totalReplayAttempts.toString(), Icons.Default.Shield)
                        MetricRow("Authentication Failures", metricsState.totalAuthFailures.toString(), Icons.Default.Block)
                        MetricRow("Signature Failures", metricsState.totalSignatureFailures.toString(), Icons.Default.ErrorOutline)
                        MetricRow("Tampered Packets Rejected", metricsState.totalTamperedPackets.toString(), Icons.Default.Dangerous)
                        MetricRow("Key Rotations Executed", metricsState.totalKeyRotations.toString(), Icons.Default.Autorenew)
                        MetricRow("Expired Sessions Cleaned", metricsState.totalExpiredSessions.toString(), Icons.Default.TimerOff)
                    }
                }
            }

            // Active Peer Sessions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Sessions (${activeSessions.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = {
                        cryptoManager.rotateBroadcastKey()
                        securityMonitor.recordKeyRotation()
                        refreshDiagnostics()
                    }) {
                        Text("Rotate Broadcast Key")
                    }
                }
            }

            if (activeSessions.isEmpty()) {
                item {
                    Text("No active peer sessions established", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(activeSessions) { session ->
                    SessionCard(session, onRekey = {
                        rekeyManager.manualRekey(session.peerId)
                        refreshDiagnostics()
                    })
                }
            }

            item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MeshTheme.spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SessionCard(session: PeerSecureSession, onRekey: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MeshTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Peer: ${session.peerId}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                AssistChip(
                    onClick = { },
                    label = { Text("State: ${session.state}") }
                )
            }
            Text("Session ID: ${session.sessionId}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text("Key Version: v${session.keyVersion} (Prev: v${session.previousKeyVersion})", style = MaterialTheme.typography.bodySmall)
            Text("Encrypted / Decrypted: ${session.totalEncryptedPackets.get()} / ${session.totalDecryptedPackets.get()}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onRekey) {
                    Text("Trigger Rekey")
                }
            }
        }
    }
}
