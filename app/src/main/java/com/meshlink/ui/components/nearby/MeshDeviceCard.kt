package com.meshlink.ui.components.nearby

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meshlink.core.data.UserRepositoryImpl
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import com.meshlink.ui.components.UserAvatarImage
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.SuccessColorDark
import com.meshlink.util.MeshIdNormalizer

/**
 * Modern, clean Mesh-Link Peer List Item.
 *
 * Professional list-row design:
 * - Avatar with verified connection state badge.
 * - Strictly displays verified Mesh-Link profile display name (or neutral "Mesh Peer").
 * - Clear semantic status (Connected / Nearby / Connecting) and transport (BLE / Wi-Fi Direct).
 * - Real signal strength (dBm) with semantic signal bars.
 * - Direct, compact action button ("Chat" when connected, "Connect" when discovered).
 * - Free of oversized cards, fake latency formulas, or hardware model leaks.
 */
@Composable
fun MeshDeviceCard(
    device: BleDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false,
    isSelected: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "PeerItemPressScale"
    )

    val canonicalId = remember(device.address, device.meshId) {
        MeshIdNormalizer.canonicalize(device.meshId.ifBlank { device.address })
    }

    // Strictly resolve verified Mesh-Link profile display name; never fall back to hardware model!
    val displayName = remember(device.displayName, device.name, canonicalId) {
        val profName = device.displayName?.trim()?.takeIf {
            it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
        }
        val fallbackName = device.name.trim().takeIf {
            it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
        }
        profName ?: fallbackName ?: "Mesh Peer"
    }

    // Signal evaluation (Real RSSI)
    val isStrongSignal = device.rssi >= -65
    val isMediumSignal = device.rssi >= -80
    val (signalColor, signalIcon) = when {
        isStrongSignal -> SuccessColorDark to Icons.Default.SignalCellularAlt
        isMediumSignal -> MeshTheme.colors.warning to Icons.Default.SignalCellularAlt2Bar
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) to Icons.Default.SignalCellularAlt1Bar
    }

    val transportName = when (device.transport) {
        TransportType.BLE -> "BLE"
        TransportType.WIFI_DIRECT -> "Wi-Fi Direct"
    }

    val statusSubtitle = when {
        isConnecting -> "Connecting…"
        device.isConnected -> "Connected · $transportName"
        device.isMeshNode -> "Mesh · ${device.hopCount} ${if (device.hopCount == 1) "hop" else "hops"}"
        else -> "Nearby · $transportName"
    }

    val itemContentDesc = "$displayName, $statusSubtitle, signal ${device.rssi} dBm"

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                role = Role.Button
                contentDescription = itemContentDesc
            },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            }
        ),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshTheme.spacing.medium, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Avatar with live connection dot
            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatarImage(
                    meshId = device.meshId,
                    displayName = displayName,
                    profilePhotoPath = device.profilePhotoPath,
                    avatarUri = device.avatarUri,
                    size = 44.dp
                )

                if (device.isConnected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(SuccessColorDark)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))

            // 2. Identity & Connection Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (device.isConnected) SuccessColorDark else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (device.isConnected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(MeshTheme.spacing.small))

            // 3. Real Signal (dBm)
            if (device.rssi != 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = MeshTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = signalIcon,
                        contentDescription = "Signal ${device.rssi} dBm",
                        tint = signalColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = signalColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Action Button (Compact, clear touch target)
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .padding(4.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (device.isConnected) {
                FilledTonalButton(
                    onClick = onClick,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
