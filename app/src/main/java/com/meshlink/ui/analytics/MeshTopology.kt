package com.meshlink.ui.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.RouteEntry
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshTopology(
    routes: List<RouteEntry>,
    activeNodes: Set<String>,
    modifier: Modifier = Modifier
) {
    val pulseAlpha by rememberPulseAnimation(1800)
    val beamProgress by rememberBeamAnimation(2200)

    val primaryColor = MaterialTheme.colorScheme.primary
    val relayColor = MaterialTheme.colorScheme.tertiary
    val linkColor = MaterialTheme.colorScheme.outlineVariant
    val activeLinkColor = MeshTheme.colors.success

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mesh Topology Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live node graph & dynamic relay routing links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${activeNodes.size.coerceAtLeast(4)} Nodes",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Node Graph Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.height / 2.6f)

                    // Node Positions
                    val nodeCount = activeNodes.size.coerceAtLeast(5)
                    val angles = List(nodeCount) { i -> (i * (2 * Math.PI / nodeCount)).toFloat() }
                    val nodeOffsets = angles.map { angle ->
                        Offset(
                            x = center.x + radius * cos(angle),
                            y = center.y + radius * sin(angle)
                        )
                    }

                    // Draw Orbit Rings
                    drawCircle(
                        color = linkColor.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    )

                    // Draw Link Lines from Center (Local Node) to Peripheral Nodes
                    nodeOffsets.forEachIndexed { idx, targetOffset ->
                        val isDirect = idx % 2 == 0
                        val currentLinkColor = if (isDirect) activeLinkColor else linkColor

                        drawLine(
                            color = currentLinkColor.copy(alpha = 0.6f),
                            start = center,
                            end = targetOffset,
                            strokeWidth = 2.dp.toPx()
                        )

                        // Animated Pulse Beam along link
                        val beamX = center.x + (targetOffset.x - center.x) * beamProgress
                        val beamY = center.y + (targetOffset.y - center.y) * beamProgress
                        drawCircle(
                            color = currentLinkColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(beamX, beamY)
                        )
                    }

                    // Inter-node Relay Lines
                    if (nodeOffsets.size >= 3) {
                        for (i in 0 until nodeOffsets.size - 1) {
                            drawLine(
                                color = relayColor.copy(alpha = 0.4f),
                                start = nodeOffsets[i],
                                end = nodeOffsets[i + 1],
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }
                    }

                    // Draw Local Node Pulse Aura
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f * pulseAlpha),
                        radius = 24.dp.toPx() * (1f + 0.2f * pulseAlpha),
                        center = center
                    )

                    // Draw Local Node Core
                    drawCircle(
                        color = primaryColor,
                        radius = 16.dp.toPx(),
                        center = center
                    )

                    // Draw Peripheral Nodes
                    nodeOffsets.forEachIndexed { idx, pos ->
                        val isRelay = idx % 2 != 0
                        val nodeColor = if (isRelay) relayColor else activeLinkColor

                        drawCircle(
                            color = nodeColor,
                            radius = 10.dp.toPx(),
                            center = pos
                        )
                    }
                }

                // Center Icon overlay
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Local Node",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Topology Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopologyLegendItem(color = primaryColor, label = "Local Device")
                TopologyLegendItem(color = activeLinkColor, label = "Direct Peer")
                TopologyLegendItem(color = relayColor, label = "Relay Node")
            }
        }
    }
}

@Composable
private fun TopologyLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(10.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
