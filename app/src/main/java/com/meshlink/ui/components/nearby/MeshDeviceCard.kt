package com.meshlink.ui.components.nearby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import java.util.Locale
import com.meshlink.ui.components.UserAvatarImage
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.MeshIdNormalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshDeviceCard(
    device: BleDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false,
    isSelected: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "CardPressScale"
    )

    val isStrongSignal = device.rssi > -70
    val isWeakSignal = device.rssi < -85
    val (signalText, signalColor) = when {
        isStrongSignal -> "Excellent" to MeshTheme.colors.success
        isWeakSignal -> "Weak" to MeshTheme.colors.warning
        else -> "Good" to MaterialTheme.colorScheme.primary
    }

    val distanceText = remember(device.distanceMeters, device.distanceConfidence) {
        if (device.distanceMeters != null) {
            val confStr = device.distanceConfidence?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Estimated"
            "~${String.format(Locale.getDefault(), "%.1f m", device.distanceMeters)} ($confStr)"
        } else {
            "Distance: Calculating..."
        }
    }

    val canonicalId = remember(device.address, device.meshId) {
        MeshIdNormalizer.canonicalize(device.meshId.ifBlank { device.address })
    }

    val displayName = remember(device.displayName, device.name, canonicalId) {
        val profName = device.displayName?.trim()?.takeIf { it.isNotBlank() && !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId) }
        val fallbackName = device.name.trim().takeIf { it.isNotBlank() && !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId) }
        profName ?: fallbackName ?: "Mesh Peer"
    }

    val isRelayCapable = remember(device.capabilities, device.isMeshNode) {
        device.isMeshNode || (device.capabilities.toInt() and 0x01 != 0)
    }

    val estimatedLatencyMs = remember(device.rssi) {
        (-device.rssi * 0.35).toInt().coerceIn(10, 150)
    }

    val containerBorderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            expanded = !expanded
        },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(width = if (isSelected) 2.dp else 0.dp, color = containerBorderColor, shape = RoundedCornerShape(20.dp))
            .animateContentSize()
            .semantics {
                role = Role.Button
                contentDescription = "$displayName, ${if (device.isConnected) "Connected" else "Discovered"}, Signal $signalText, Distance $distanceText"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        interactionSource = interactionSource
    ) {
        Column(modifier = Modifier.padding(MeshTheme.spacing.mediumLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Connection Dot
                Box(contentAlignment = Alignment.BottomEnd) {
                        com.meshlink.ui.components.UserAvatarImage(
                            meshId = device.meshId,
                            displayName = displayName,
                            profilePhotoPath = device.profilePhotoPath,
                            avatarUri = device.avatarUri,
                            size = 52.dp
                        )

                    if (device.isConnected) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))

                // Device Name & Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isRelayCapable) {
                            Spacer(modifier = Modifier.width(MeshTheme.spacing.extraSmall))
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Relay Node",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = distanceText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Signal Strength & Transport Badge
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${device.rssi} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = signalColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isWeakSignal) Icons.Default.SignalCellularOff else Icons.Default.SignalCellularAlt,
                            contentDescription = "Signal $signalText",
                            tint = signalColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val (transportIcon, transportLabel) = Icons.Default.Bluetooth to "BLE"

                    val isDarkTheme = MeshTheme.isDark
                    val (hopContainerColor, hopTextColor) = when (device.hopCount) {
                        0 -> if (isDarkTheme) Color(0xFF14532D) to Color(0xFF86EFAC) else Color(0xFFDCFCE7) to Color(0xFF15803D)
                        1 -> if (isDarkTheme) Color(0xFF1E3A8A) to Color(0xFF93C5FD) else Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
                        else -> if (isDarkTheme) Color(0xFF581C87) to Color(0xFFD8B4FE) else Color(0xFFF3E8FF) to Color(0xFF7E22CE)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            color = hopContainerColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when (device.hopCount) {
                                    0 -> "Direct"
                                    1 -> "Mesh • 1 Hop"
                                    else -> "Mesh • ${device.hopCount} Hops"
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = hopTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = transportIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = transportLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Details Section
            if (expanded) {
                Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                Column(verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Encryption Info
                        DetailChip(
                            icon = Icons.Default.Lock,
                            label = "Security",
                            value = "E2E Encrypted"
                        )

                        // Latency Estimate
                        DetailChip(
                            icon = Icons.Default.Speed,
                            label = "Est. Latency",
                            value = "~$estimatedLatencyMs ms"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Relay capability
                        DetailChip(
                            icon = Icons.Default.Hub,
                            label = "Routing Role",
                            value = if (isRelayCapable) "Relay Ready" else "Leaf Node"
                        )

                        // Canonical ID
                        DetailChip(
                            icon = Icons.Default.Bluetooth,
                            label = "Mesh ID",
                            value = canonicalId.take(12)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                        Text("Connecting...")
                    } else {
                        Text(
                            text = if (device.isConnected) "Open Mesh Chat" else "Connect & Message",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
