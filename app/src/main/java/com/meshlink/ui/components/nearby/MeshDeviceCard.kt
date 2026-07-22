package com.meshlink.ui.components.nearby

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlin.math.pow

@Composable
fun MeshDeviceCard(
    device: BleDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    val isStrongSignal = device.rssi > -70
    val isWeakSignal = device.rssi < -85
    val statusColor = when {
        isStrongSignal -> MaterialTheme.colorScheme.primary
        isWeakSignal -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onBackground
    }
    
    val distanceText = if (device.transport == TransportType.WIFI_DIRECT) {
        "Distance: N/A"
    } else if (device.distanceConfidence == "LOW") {
        "Approximate Distance"
    } else if (device.distanceMeters != null) {
        val formattedConf = device.distanceConfidence?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        "Distance: ${String.format("%.1f m", device.distanceMeters)} - Confidence: $formattedConf"
    } else {
        "Distance: Calculating..."
    }

    val displayName = device.name.ifBlank { "Unknown Device" }
    val haptic = LocalHapticFeedback.current
    
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            expanded = !expanded
        },
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Online Indicator
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(MeshTheme.spacing.giant)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = displayName.split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString("")
                            .uppercase()
                        
                        Text(
                            text = initials.ifBlank { "?" },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (device.isConnected) {
                        Box(
                            modifier = Modifier
                                .size(MeshTheme.spacing.medium)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
                
                // Device Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = distanceText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Signal and Transport Badge
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        imageVector = if (isWeakSignal) Icons.Default.SignalCellularOff else Icons.Default.SignalCellular4Bar,
                        contentDescription = "Signal",
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                    Badge(
                        containerColor = when(device.transport) {
                            TransportType.BLE -> MaterialTheme.colorScheme.primaryContainer
                            TransportType.WIFI_DIRECT -> MaterialTheme.colorScheme.tertiaryContainer
                            TransportType.HYBRID -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = device.transport.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = MeshTheme.spacing.small, vertical = MeshTheme.spacing.extraSmall)
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Transport type detailed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (device.transport) {
                                TransportType.BLE -> Icons.Default.Bluetooth
                                TransportType.WIFI_DIRECT -> Icons.Default.Wifi
                                TransportType.HYBRID -> Icons.Default.SwapHoriz
                            },
                            contentDescription = "Transport",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MeshTheme.spacing.mediumLarge)
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                        Text(
                            text = when (device.transport) {
                                TransportType.BLE -> "BLE Only"
                                TransportType.WIFI_DIRECT -> "Wi-Fi Direct"
                                TransportType.HYBRID -> "BLE + Wi-Fi Direct"
                            }, 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Connection Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(MeshTheme.spacing.mediumLarge)
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                        Text(
                            text = if (device.isConnected) "Connected & E2E" else "E2E Encrypted", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))
                
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    enabled = !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(MeshTheme.spacing.extraLarge),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = MeshTheme.spacing.extraSmall
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                        Text("Connecting...")
                    } else {
                        Text("Connect & Message")
                    }
                }
            }
        }
    }
}
