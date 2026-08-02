package com.meshlink.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.components.glass.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshOverviewSection(
    nearbyDevices: List<BleDevice>,
    onNavigateToNearby: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeCount = nearbyDevices.size
    val maxRssi = nearbyDevices.maxOfOrNull { it.rssi } ?: -100
    val signalQualityText = when {
        nodeCount == 0 -> "Scanning"
        maxRssi >= -65 -> "Optimal (-${kotlin.math.abs(maxRssi)} dBm)"
        maxRssi >= -85 -> "Good (-${kotlin.math.abs(maxRssi)} dBm)"
        else -> "Fair (-${kotlin.math.abs(maxRssi)} dBm)"
    }

    val pulseState = rememberMeshRadarPulse()
    val primaryColor = MeshTheme.colors.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(vertical = 8.dp)
    ) {
        MeshGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .tactileClick(onClick = onNavigateToNearby, pressScale = 0.98f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header telemetry label & status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MESH TELEMETRY CONTROL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryColor,
                                letterSpacing = 1.2.sp
                            )
                        }

                        // Live status badge
                        Surface(
                            shape = MeshTheme.shapes.pill,
                            color = if (nodeCount > 0) Color(0xFF1B5E20).copy(alpha = 0.4f) else MeshTheme.colors.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (nodeCount > 0) Color(0xFF4CAF50) else MeshTheme.colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (nodeCount > 0) Color(0xFF4CAF50) else Color(0xFFFFB300))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (nodeCount > 0) "NETWORK READY" else "SCANNING MESH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (nodeCount > 0) Color(0xFF4CAF50) else Color(0xFFFFB300)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Middle content: Node count big display + Animated radar pulse ring
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = nodeCount,
                                transitionSpec = {
                                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                                },
                                label = "NodeCountAnim"
                            ) { count ->
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$count",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MeshTheme.colors.textPrimary,
                                        letterSpacing = (-1).sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (count == 1) "Active Peer" else "Active Peers",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MeshTheme.colors.textSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Signal: $signalQualityText",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MeshTheme.colors.textSecondary
                            )
                        }

                        // Animated Tactical Radar Pulse Rings
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val center = Offset(size.width / 2, size.height / 2)
                                        val maxRadius = size.minDimension / 2

                                        // Outer Ring
                                        drawCircle(
                                            color = primaryColor.copy(alpha = pulseState.ring1Alpha.value),
                                            radius = maxRadius * pulseState.ring1Scale.value,
                                            center = center,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        // Inner Ring
                                        drawCircle(
                                            color = primaryColor.copy(alpha = pulseState.ring2Alpha.value),
                                            radius = maxRadius * pulseState.ring2Scale.value,
                                            center = center,
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                            )
                            Surface(
                                shape = CircleShape,
                                color = primaryColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CellTower,
                                        contentDescription = "Radar Pulse",
                                        tint = primaryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Telemetry Bar: Transport Modes (BLE 5.3 + Wi-Fi Direct) + Multi-Hop
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MeshTheme.colors.surface.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BLE 5.3",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeshTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Wi-Fi Direct",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeshTheme.colors.textPrimary
                            )
                        }

                        Text(
                            text = "Multi-Hop Enabled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MeshTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}
