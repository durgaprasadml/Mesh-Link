package com.meshlink.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacySecurity(
    uiState: SettingsUiState,
    onSetOnlineVisible: (Boolean) -> Unit = {},
    onSetEncryptionEnabled: (Boolean) -> Unit = {},
    onSetBiometricUnlock: (Boolean) -> Unit = {},
    onSetPrivacyMode: (Boolean) -> Unit = {},
    onNavigateToTrustedDevices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Privacy & Security",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SecurityDashboard(uiState = uiState)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.Radar,
                    title = "Discoverable on Mesh",
                    subtitle = "Allow nearby nodes to locate public identity fingerprint",
                    isSwitch = true,
                    isChecked = uiState.isOnlineVisible,
                    onCheckedChange = onSetOnlineVisible,
                    statusChipText = if (uiState.isOnlineVisible) "Visible" else "Hidden",
                    statusChipColor = if (uiState.isOnlineVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    statusChipTextColor = if (uiState.isOnlineVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "End-to-End Encryption (Signal Protocol)",
                    subtitle = "Double-ratchet session key security for all messages",
                    isSwitch = true,
                    isChecked = uiState.isEncryptionEnabled,
                    onCheckedChange = onSetEncryptionEnabled,
                    statusChipText = "AES-256",
                    statusChipColor = MaterialTheme.colorScheme.tertiaryContainer,
                    statusChipTextColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

                SettingsRow(
                    icon = Icons.Default.Devices,
                    title = "Trusted Peers & Key Pins",
                    subtitle = "${uiState.trustedDevicesCount} Verified cryptographically pinned nodes",
                    onClick = onNavigateToTrustedDevices
                )

                SettingsRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Lock",
                    subtitle = "Require Fingerprint/Face Unlock to access Mesh-Link",
                    isSwitch = true,
                    isChecked = uiState.biometricUnlock,
                    onCheckedChange = onSetBiometricUnlock
                )

                SettingsRow(
                    icon = Icons.Default.Pin,
                    title = "App Screen Lock Timeout",
                    subtitle = "Lock app automatically after 5 minutes of inactivity",
                    onClick = {}
                )

                SettingsRow(
                    icon = Icons.Default.VisibilityOff,
                    title = "Stealth Privacy Mode",
                    subtitle = "Hide preview notifications & mask app switcher content",
                    isSwitch = true,
                    isChecked = true,
                    onCheckedChange = onSetPrivacyMode,
                    showDivider = false
                )
            }
        }
    }
}
