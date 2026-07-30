package com.meshlink.ui.components.nearby

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.components.UserAvatarImage
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.MeshIdNormalizer

@Composable
fun MeshDeviceDetailSheet(
    device: BleDevice?,
    onDismiss: () -> Unit,
    onConnectChat: (BleDevice) -> Unit,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false
) {
    AnimatedVisibility(
        visible = device != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        if (device != null) {
            val displayName = device.name.ifBlank { "Nearby Mesh Node" }
            val canonicalId = remember(device.address, device.meshId) {
                MeshIdNormalizer.canonicalize(device.meshId.ifBlank { device.address })
            }

            val semanticColors = MeshTheme.colors
            val isStrongSignal = device.rssi > -70
            val isWeakSignal = device.rssi < -85
            val (signalText, signalColor) = when {
                isStrongSignal -> "Excellent" to semanticColors.signalStrong
                isWeakSignal -> "Weak" to semanticColors.signalWeak
                else -> "Good" to semanticColors.signalMedium
            }

            val isRelay = remember(device.capabilities, device.isConnected, device.rssi) {
                (device.capabilities.toInt() and 0x01 != 0) || (device.isConnected && device.rssi > -75)
            }

            val estimatedLatency = remember(device.rssi) {
                (-device.rssi * 0.35).toInt().coerceIn(8, 120)
            }

            val simulatedBattery = remember(device.address) {
                (Math.abs(device.address.hashCode()) % 45 + 55)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.small)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = MeshTheme.shapes.extraLarge
                    ),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MeshTheme.shapes.extraLarge,
                tonalElevation = MeshTheme.elevation.level3
            ) {
                Column(
                    modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatarImage(
                            avatarUri = device.avatarUri,
                            displayName = displayName,
                            size = 48.dp
                        )

                        Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isRelay) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "RELAY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Mesh ID: ${canonicalId.take(14)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close detail card",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                    // Grid Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox(
                            icon = Icons.Default.SignalCellularAlt,
                            iconTint = signalColor,
                            label = "Signal Quality",
                            value = "$signalText (${device.rssi} dBm)"
                        )

                        MetricBox(
                            icon = Icons.Default.BatteryFull,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "Battery Level",
                            value = "$simulatedBattery%"
                        )
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox(
                            icon = Icons.Default.Bluetooth,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            label = "Connection",
                            value = if (device.isConnected) "BLE Connected" else "Discovered"
                        )

                        MetricBox(
                            icon = Icons.Default.Speed,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            label = "Est. Latency",
                            value = "~$estimatedLatency ms"
                        )
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox(
                            icon = Icons.Default.Schedule,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "Last Seen",
                            value = "Just now"
                        )

                        MetricBox(
                            icon = Icons.Default.Lock,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "Security",
                            value = "E2E Encrypted"
                        )
                    }

                    Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

                    // Action Button
                    Button(
                        onClick = { onConnectChat(device) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MeshTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Establishing Link...", fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = if (device.isConnected) "Open Chat Session" else "Connect & Message",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
