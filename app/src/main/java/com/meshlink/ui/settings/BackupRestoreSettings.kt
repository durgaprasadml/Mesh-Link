package com.meshlink.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
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
fun BackupRestoreSettings(
    lastBackupTime: String = "Today, 10:42 AM",
    backupSize: String = "2.4 MB (Encrypted)",
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onExportIdentityKey: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Backup & Recovery",
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
                    icon = Icons.Default.Backup,
                    title = "Local Encrypted Backup",
                    subtitle = "Last generated: $lastBackupTime ($backupSize)",
                    statusChipText = "Encrypted",
                    statusChipColor = MaterialTheme.colorScheme.primaryContainer,
                    statusChipTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    title = "Export Local Backup Package",
                    subtitle = "Save encrypted backup file (.meshbk) to internal storage",
                    onClick = onExportBackup
                )

                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    title = "Restore from Backup File",
                    subtitle = "Decrypt and restore contacts, messages & node identity",
                    onClick = onImportBackup
                )

                SettingsRow(
                    icon = Icons.Default.Key,
                    title = "Export Identity Seed Key",
                    subtitle = "Copy 24-word cryptographic master identity mnemonic",
                    onClick = onExportIdentityKey,
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Zero-knowledge recovery info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 Zero-Knowledge Security Notice",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mesh-Link operates on an offline-first, decentralized model. Your private keys never leave this device. Always keep a backup of your seed phrase in a safe location.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
