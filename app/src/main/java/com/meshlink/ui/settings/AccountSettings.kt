package com.meshlink.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
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
fun AccountSettings(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToTrustedDevices: () -> Unit = {},
    onExportQr: () -> Unit = {},
    onBackupIdentity: () -> Unit = {},
    onExportIdentity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Account & Identity",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.Person,
                    title = "Profile Information",
                    subtitle = "Edit display name, avatar, bio & identity status",
                    onClick = onNavigateToProfile
                )

                SettingsRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Mesh Identity",
                    subtitle = "Ed25519 node fingerprint & public key fingerprints",
                    onClick = onNavigateToProfile
                )

                SettingsRow(
                    icon = Icons.Default.QrCode,
                    title = "Share QR Identity",
                    subtitle = "Generate encrypted QR code for offline pairing",
                    onClick = onExportQr
                )

                SettingsRow(
                    icon = Icons.Default.Devices,
                    title = "Trusted Peers & Devices",
                    subtitle = "Manage cryptographically verified contact keys",
                    onClick = onNavigateToTrustedDevices
                )

                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "Backup Identity",
                    subtitle = "Export identity key package for device recovery",
                    onClick = onBackupIdentity
                )

                SettingsRow(
                    icon = Icons.Default.Share,
                    title = "Export Identity Keys",
                    subtitle = "Securely transfer identity credentials",
                    onClick = onExportIdentity,
                    showDivider = false
                )
            }
        }
    }
}
