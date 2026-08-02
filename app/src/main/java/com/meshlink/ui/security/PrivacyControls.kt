package com.meshlink.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun PrivacyControlsCard(
    privacyUi: PrivacyUi,
    modifier: Modifier = Modifier,
    onDiscoverabilityToggle: ((Boolean) -> Unit)? = null,
    onVisibilityToggle: ((Boolean) -> Unit)? = null,
    onBiometricsToggle: ((Boolean) -> Unit)? = null,
    onAppLockToggle: ((Boolean) -> Unit)? = null
) {
    MeshGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Privacy Controls",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Node Privacy & Protection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            PrivacyItemRow(
                icon = Icons.Default.Visibility,
                title = "Mesh Node Discoverability",
                subtitle = "Allow nearby peers to discover this device identity over BLE & Wi-Fi",
                checked = privacyUi.discoverabilityEnabled,
                onCheckedChange = onDiscoverabilityToggle
            )

            PrivacyItemRow(
                icon = Icons.Default.VisibilityOff,
                title = "Online Visibility",
                subtitle = "Broadcast online presence status to verified mesh contacts",
                checked = privacyUi.onlineVisibility,
                onCheckedChange = onVisibilityToggle
            )

            PrivacyItemRow(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Lock",
                subtitle = "Require fingerprint / face identification to launch Mesh-Link",
                checked = privacyUi.biometricLockEnabled,
                onCheckedChange = onBiometricsToggle
            )

            PrivacyItemRow(
                icon = Icons.Default.Security,
                title = "Anonymous Mesh Broadcasts",
                subtitle = "Strip node identity signatures from public local emergency broadcasts",
                checked = privacyUi.appLockEnabled,
                onCheckedChange = onAppLockToggle
            )

            PrivacyItemRow(
                icon = Icons.Default.Lock,
                title = "Enforce Strict Payload Encryption",
                subtitle = "Reject plain text or unauthenticated mesh packets automatically",
                checked = privacyUi.advancedEncryptionEnforced,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun PrivacyItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
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
