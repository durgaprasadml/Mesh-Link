package com.meshlink.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun EncryptionStatusCard(
    encryptionUi: EncryptionUi,
    modifier: Modifier = Modifier,
    onKeyRotationClick: (() -> Unit)? = null
) {
    MeshGlassCard(
        modifier = modifier.fillMaxWidth(),
        glowColor = MaterialTheme.colorScheme.primary,
        glowRadius = 180f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.mediumLarge)
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
                            imageVector = Icons.Default.Security,
                            contentDescription = "E2EE Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (encryptionUi.isE2eeActive) "End-to-End Encrypted" else "Encryption Restricted",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${encryptionUi.cipherSuite} • ${encryptionUi.keyExchangeAlg}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onKeyRotationClick != null) {
                    IconButton(onClick = onKeyRotationClick) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Rotate Broadcast Key",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = MeshTheme.spacing.medium),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EncryptionAttributeItem(
                    icon = Icons.Default.Lock,
                    label = "Protocol",
                    value = encryptionUi.ratchetProtocol
                )
                EncryptionAttributeItem(
                    icon = Icons.Default.Memory,
                    label = "Keystore",
                    value = if (encryptionUi.isHardwareKeystoreUsed) "Hardware TEE" else "Software Key"
                )
                EncryptionAttributeItem(
                    icon = Icons.Default.VerifiedUser,
                    label = "Forward Secrecy",
                    value = if (encryptionUi.perfectForwardSecrecy) "Active (PFS)" else "Standard"
                )
            }
        }
    }
}

@Composable
private fun EncryptionAttributeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
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
