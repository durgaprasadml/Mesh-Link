package com.meshlink.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun SecurityDashboardHeader(
    trustUi: TrustUi,
    encryptionUi: EncryptionUi,
    statsUi: SecurityStatsUi,
    modifier: Modifier = Modifier,
    onRotateKeysClick: (() -> Unit)? = null
) {
    MeshGlassCard(
        modifier = modifier.fillMaxWidth(),
        glowColor = MaterialTheme.colorScheme.primary,
        glowRadius = 220f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                ) {
                    SecurityAnimations.ShieldPulseContainer(
                        pulseColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Mission Control Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Enterprise Security Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TrustScoreIndicator(score = trustUi.securityScore)
                            Text(
                                text = "• ${encryptionUi.cipherSuite}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onRotateKeysClick != null) {
                    OutlinedButton(onClick = onRotateKeysClick) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Rotate Keys")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DashboardMetric(
                    icon = Icons.Default.CheckCircle,
                    label = "E2EE Status",
                    value = if (encryptionUi.isE2eeActive) "Enforced" else "Off",
                    tint = MaterialTheme.colorScheme.primary
                )
                DashboardMetric(
                    icon = Icons.Default.Verified,
                    label = "Trusted Keys",
                    value = "${statsUi.verifiedKeys}",
                    tint = MaterialTheme.colorScheme.secondary
                )
                DashboardMetric(
                    icon = Icons.Default.Lock,
                    label = "Active Sessions",
                    value = "${statsUi.activeSessions}",
                    tint = MaterialTheme.colorScheme.tertiary
                )
                DashboardMetric(
                    icon = Icons.Default.Memory,
                    label = "Hardware Keystore",
                    value = if (statsUi.isHardwareKeystoreActive) "TEE Active" else "Software",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
