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
fun EncryptionStatusCard(
    encryptionUi: EncryptionUi,
    onKeyRotationClick: (() -> Unit)? = null,
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
            // Header Row
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
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Encryption & Protocol Status",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (encryptionUi.isE2eeActive) "End-to-End Encrypted Active" else "Encryption Suspended",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (encryptionUi.isE2eeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (onKeyRotationClick != null) {
                    OutlinedButton(
                        onClick = onKeyRotationClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rotate Keys",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Grid of Protocol details
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EncryptionDetailRow(
                    label = "Symmetric Payload Cipher",
                    value = encryptionUi.cipherSuite,
                    icon = Icons.Default.Security
                )
                EncryptionDetailRow(
                    label = "Key Exchange Algorithm",
                    value = encryptionUi.keyExchangeAlg,
                    icon = Icons.Default.SwapHoriz
                )
                EncryptionDetailRow(
                    label = "Ratchet Protocol",
                    value = encryptionUi.ratchetProtocol,
                    icon = Icons.Default.Sync
                )
                EncryptionDetailRow(
                    label = "Hardware Keystore",
                    value = if (encryptionUi.isHardwareKeystoreUsed) "AndroidKeyStore (TEE/SE)" else "Software Keystore",
                    icon = Icons.Default.Memory,
                    isVerified = encryptionUi.isHardwareKeystoreUsed
                )
                EncryptionDetailRow(
                    label = "Broadcast Key Version",
                    value = "Version ${encryptionUi.broadcastKeyVersion}",
                    icon = Icons.Default.Numbers
                )
                EncryptionDetailRow(
                    label = "Perfect Forward Secrecy",
                    value = if (encryptionUi.perfectForwardSecrecy) "Enabled (Double Ratchet)" else "Disabled",
                    icon = Icons.Default.Verified,
                    isVerified = encryptionUi.perfectForwardSecrecy
                )
            }
        }
    }
}

@Composable
private fun EncryptionDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isVerified: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isVerified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
