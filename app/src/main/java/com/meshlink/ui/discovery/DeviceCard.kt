package com.meshlink.ui.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.MeshIdNormalizer

@Composable
fun DeviceCard(
    deviceUi: NearbyDeviceUiState,
    isConnecting: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MeshTheme.colors.primary else MeshTheme.colors.glassBorder
    val glowColor = if (deviceUi.isConnected) MeshTheme.colors.connected else MeshTheme.colors.primary

    MeshGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = MeshSpacing.CardCornerRadius,
        glowColor = glowColor,
        glowRadius = if (isSelected) 180f else 80f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.MD)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar + User Identity & Canonical Mesh ID
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = deviceUi.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MeshTheme.colors.primary
                        )
                        // Online Status Indicator Dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (deviceUi.isConnected) MeshTheme.colors.connected
                                    else MeshTheme.colors.disconnected
                                )
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(MeshSpacing.MD))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deviceUi.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = MeshIdNormalizer.canonicalize(deviceUi.address),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Signal Strength Meter
                SignalMeter(
                    signal = deviceUi.signal,
                    modifier = Modifier.padding(start = MeshSpacing.SM)
                )
            }

            Spacer(modifier = Modifier.height(MeshSpacing.SM))

            // Metadata Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.XS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Transport Badge
                    BadgeChip(
                        icon = when (deviceUi.transportUi) {
                            TransportTypeUi.BLE -> Icons.Default.Bluetooth
                            TransportTypeUi.WIFI_DIRECT -> Icons.Default.Wifi
                            TransportTypeUi.HYBRID -> Icons.Default.Router
                        },
                        label = deviceUi.transportUi.label,
                        color = deviceUi.transportUi.badgeColor
                    )

                    // RSSI Distance Bucket / Real Distance
                    BadgeChip(
                        label = deviceUi.formattedDistance,
                        color = Color(Color(deviceUi.signal.quality.colorHex).value)
                    )

                    // Relay Node Badge if supported
                    if (deviceUi.hasRelayCapability) {
                        BadgeChip(
                            icon = Icons.Default.Router,
                            label = "Relay Node",
                            color = MeshTheme.colors.warning
                        )
                    }
                }

                // Connect / Tactical Chat Action Button
                OutlinedButton(
                    onClick = onConnectClick,
                    enabled = !isConnecting,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (deviceUi.isConnected) MeshTheme.colors.connected.copy(alpha = 0.12f) else Color.Transparent,
                        contentColor = if (deviceUi.isConnected) MeshTheme.colors.connected else MeshTheme.colors.primary
                    )
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MeshTheme.colors.primary
                        )
                    } else if (deviceUi.isConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Connected",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Connect",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connect", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalMeter(
    signal: SignalStrength,
    modifier: Modifier = Modifier
) {
    val qualityColor = Color(signal.quality.colorHex)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (i in 1..4) {
                val height = (4 + i * 3).dp
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(height)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (i <= signal.barCount) qualityColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${signal.rssi} dBm",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = qualityColor
        )
    }
}

@Composable
private fun BadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}
