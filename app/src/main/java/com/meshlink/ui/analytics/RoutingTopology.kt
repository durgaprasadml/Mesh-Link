package com.meshlink.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.RouteEntry
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RoutingTopology(
    routes: List<RouteEntry>,
    activeNodes: Set<String>,
    modifier: Modifier = Modifier
) {
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val beamProgress = rememberBeamAnimation()
    val pulseAlpha = rememberPulseAnimation()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val successColor = MeshTheme.colors.success
    val warningColor = MeshTheme.colors.warning
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(MeshTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Polyline,
                        contentDescription = "Routing Topology Map",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                    Text(
                        text = "Tactical Mesh Topology",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${activeNodes.size + 1} Nodes Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MeshTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension / 2.6f).coerceAtLeast(60f)

                    // Draw center local node
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f * pulseAlpha.value),
                        radius = 24f,
                        center = center
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 12f,
                        center = center
                    )

                    // Position outer nodes evenly
                    val nodesList = activeNodes.toList()
                    val totalNodes = nodesList.size.coerceAtLeast(1)
                    val nodeOffsets = mutableMapOf<String, Offset>()

                    for (i in nodesList.indices) {
                        val angle = (2 * Math.PI * i / totalNodes) - (Math.PI / 2)
                        val x = center.x + radius * cos(angle).toFloat()
                        val y = center.y + radius * sin(angle).toFloat()
                        val nodeOffset = Offset(x, y)
                        nodeOffsets[nodesList[i]] = nodeOffset

                        // Draw connection beam line from center
                        val isSelected = selectedNodeId == nodesList[i]
                        val lineColor = if (isSelected) warningColor else secondaryColor.copy(alpha = 0.5f)
                        val strokeWidth = if (isSelected) 4f else 2f

                        drawLine(
                            color = lineColor,
                            start = center,
                            end = nodeOffset,
                            strokeWidth = strokeWidth,
                            pathEffect = if (!isSelected) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), beamProgress.value * 20f) else null
                        )

                        // Draw node point
                        drawCircle(
                            color = successColor.copy(alpha = 0.25f),
                            radius = 16f,
                            center = nodeOffset
                        )
                        drawCircle(
                            color = if (isSelected) warningColor else successColor,
                            radius = 8f,
                            center = nodeOffset
                        )
                    }
                }

                // Interactive touch overlays for node selection
                val nodesList = activeNodes.toList()
                if (nodesList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Local Node (Broadcasting & Listening)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Details for selected node or general summary
            selectedNodeId?.let { nodeId ->
                val associatedRoutes = routes.filter { it.destinationId == nodeId || it.nextHop == nodeId }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = MeshTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeshTheme.spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Node: ${com.meshlink.util.MeshIdNormalizer.canonicalize(nodeId)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active Routes: ${associatedRoutes.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { selectedNodeId = null }) {
                            Text("Clear")
                        }
                    }
                }
            } ?: run {
                Text(
                    text = "Tap nodes on canvas to inspect active dynamic routing paths.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
