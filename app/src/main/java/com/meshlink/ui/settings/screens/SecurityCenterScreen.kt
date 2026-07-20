package com.meshlink.ui.settings.screens

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SecurityViewModel
import com.meshlink.ui.settings.SecurityUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    if (showPinDialog) {
        PinConfigurationDialog(
            onDismiss = { showPinDialog = false },
            onPinSet = { pin ->
                viewModel.configurePin(pin)
                showPinDialog = false
            }
        )
    }

    if (showImportDialog) {
        ImportIdentityDialog(
            onDismiss = { showImportDialog = false },
            onImport = { identity ->
                viewModel.importIdentity(identity)
                showImportDialog = false
            }
        )
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
                    text = "End-to-End Encryption Configuration and Identity Management",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = MeshTheme.spacing.small)
                )
            }

            // ────────── Identity Management ──────────
            item {
                SectionHeader("Identity Management")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Mesh ID",
                            subtitle = uiState.meshId,
                            icon = Icons.Default.Badge,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.meshId))
                                Toast.makeText(context, "Mesh ID copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Device Fingerprint",
                            subtitle = uiState.deviceFingerprint,
                            icon = Icons.Default.Fingerprint,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.deviceFingerprint))
                                Toast.makeText(context, "Fingerprint copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Export Identity",
                            subtitle = "Backup keys securely",
                            icon = Icons.Default.Output,
                            onClick = { viewModel.exportIdentity() }
                        )
                        if (uiState.exportedIdentityBase64 != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            SettingsItemRow(
                                title = "Identity Backup (Tap to copy)",
                                subtitle = uiState.exportedIdentityBase64!!.take(20) + "...",
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.exportedIdentityBase64!!))
                                    Toast.makeText(context, "Identity copied to clipboard", Toast.LENGTH_SHORT).show()
                                    viewModel.clearExportedIdentity()
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Import Identity",
                            subtitle = "Restore keys from backup",
                            icon = Icons.Default.Input,
                            onClick = { showImportDialog = true }
                        )
                    }
                }
            }

            // ────────── Encryption & Key Management ──────────
            item {
                SectionHeader("Encryption & Key Management")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "AES-256-GCM",
                            subtitle = "Military-grade encryption",
                            icon = Icons.Default.Shield,
                            trailingContent = { Text("Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Key Age",
                            subtitle = formatDuration(uiState.keyAgeMs),
                            icon = Icons.Default.Timer
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Rotate Identity Keys",
                            subtitle = "Generates new public/private pair",
                            icon = Icons.Default.Refresh,
                            onClick = { viewModel.rotateIdentityKeys() }
                        )
                    }
                }
            }

            // ────────── App Lock & Biometrics ──────────
            item {
                SectionHeader("App Lock & Authentication")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Require PIN to open app",
                            subtitle = if (uiState.hasPinConfigured) "PIN is configured" else "PIN not configured",
                            icon = Icons.Default.Lock,
                            trailingContent = {
                                Switch(
                                    checked = uiState.isAppLockEnabled,
                                    onCheckedChange = { 
                                        if (it) {
                                            if (!uiState.hasPinConfigured) showPinDialog = true
                                            else viewModel.setAppLockEnabled(true)
                                        } else {
                                            viewModel.clearPin()
                                        }
                                    }
                                )
                            }
                        )
                        if (uiState.isAppLockEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            SettingsItemRow(
                                title = "Biometric Authentication",
                                subtitle = "Unlock using fingerprint or face",
                                icon = Icons.Default.Security,
                                trailingContent = {
                                    Switch(
                                        checked = uiState.isBiometricsEnabled,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                authenticateWithBiometrics(context) { success ->
                                                    if (success) viewModel.setBiometricsEnabled(true)
                                                }
                                            } else {
                                                viewModel.setBiometricsEnabled(false)
                                            }
                                        }
                                    )
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            val timeoutText = when (uiState.autoLockTimeoutMs) {
                                0L -> "Immediately"
                                30_000L -> "30 sec"
                                60_000L -> "1 min"
                                300_000L -> "5 min"
                                else -> "Custom"
                            }
                            SettingsItemRow(
                                title = "Auto-Lock Timeout",
                                subtitle = timeoutText,
                                icon = Icons.Default.HourglassEmpty,
                                onClick = {
                                    val next = when (uiState.autoLockTimeoutMs) {
                                        0L -> 30_000L
                                        30_000L -> 60_000L
                                        60_000L -> 300_000L
                                        else -> 0L
                                    }
                                    viewModel.setAutoLockTimeout(next)
                                }
                            )
                        }
                    }
                }
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
                            title = "Peer: \${peerId.take(8)}",
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

            // ────────── Security Logs ──────────
            item {
                SectionHeader("Security Logs")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Export Logs",
                            subtitle = "Save security events to file",
                            icon = Icons.Default.ListAlt,
                            onClick = { viewModel.exportSecurityLogs() }
                        )
                        if (uiState.securityLogs.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
                                uiState.securityLogs.take(5).forEach { log ->
                                    val color = when (log.severity.name) {
                                        "ERROR" -> MaterialTheme.colorScheme.error
                                        "WARNING" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text("[\${log.formattedTime()}] \${log.eventType}: \${log.description}", 
                                         style = MaterialTheme.typography.bodySmall, color = color, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                if (uiState.securityLogs.size > 5) {
                                    Text("... and \${uiState.securityLogs.size - 5} more", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
}

fun formatDuration(ms: Long): String {
    val days = ms / (1000 * 60 * 60 * 24)
    if (days > 0) return "\$days days"
    val hours = ms / (1000 * 60 * 60)
    if (hours > 0) return "\$hours hours"
    val mins = ms / (1000 * 60)
    return "\$mins mins"
}

@Composable
fun PinConfigurationDialog(onDismiss: () -> Unit, onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure App Lock PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
                label = { Text("Enter 4-6 digit PIN") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onPinSet(pin) },
                enabled = pin.length in 4..6
            ) { Text("Set PIN") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportIdentityDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var identity by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Identity") },
        text = {
            OutlinedTextField(
                value = identity,
                onValueChange = { identity = it },
                label = { Text("Paste Base64 Identity") },
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(identity) },
                enabled = identity.isNotBlank()
            ) { Text("Import & Restart") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun authenticateWithBiometrics(context: Context, onResult: (Boolean) -> Unit) {
    val activity = context.findActivity() ?: run {
        Toast.makeText(context, "Cannot find Activity for BiometricPrompt", Toast.LENGTH_SHORT).show()
        onResult(false)
        return
    }

    val biometricManager = BiometricManager.from(context)
    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val executor = ContextCompat.getMainExecutor(context)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Mesh Link Authentication")
                .setSubtitle("Unlock Security Center")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(context, "Auth error: \$errString", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(context, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    onResult(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            })

            biometricPrompt.authenticate(promptInfo)
        }
        else -> {
            Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
            onResult(false)
        }
    }
}
