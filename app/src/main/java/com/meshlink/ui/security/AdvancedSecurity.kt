package com.meshlink.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun AdvancedSecurityCard(
    advancedSecurity: AdvancedSecurityUi = AdvancedSecurityUi(),
    onToggleScreenshotProtection: ((Boolean) -> Unit)? = null,
    onToggleEncryptedLogs: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshSpacing.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Advanced Security Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hardened protection & memory security",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdvancedSettingRow(
                    icon = Icons.Default.ScreenLockPortrait,
                    title = "Screenshot Protection (FLAG_SECURE)",
                    subtitle = "Block screen recording & app switcher previews",
                    checked = advancedSecurity.screenshotProtectionEnabled,
                    onCheckedChange = onToggleScreenshotProtection
                )

                AdvancedSettingRow(
                    icon = Icons.Default.Timer,
                    title = "Auto-Lock Timeout",
                    subtitle = "Require authentication after ${advancedSecurity.autoLockTimeoutMinutes} min inactivity",
                    checked = true,
                    onCheckedChange = null
                )

                AdvancedSettingRow(
                    icon = Icons.Default.BugReport,
                    title = "Developer Mode Security Filter",
                    subtitle = "Strip sensitive identity material from system logcat output",
                    checked = !advancedSecurity.developerModeAllowed,
                    onCheckedChange = null
                )

                AdvancedSettingRow(
                    icon = Icons.Default.FolderZip,
                    title = "Encrypted Diagnostics Logs",
                    subtitle = "Encrypt debug log files with AES-256 before local saving",
                    checked = advancedSecurity.encryptedLogsEnabled,
                    onCheckedChange = onToggleEncryptedLogs
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = onCheckedChange != null
        )
    }
}
