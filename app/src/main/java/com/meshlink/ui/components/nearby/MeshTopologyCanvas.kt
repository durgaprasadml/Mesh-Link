package com.meshlink.ui.components.nearby

import com.meshlink.ui.designsystem.theme.MeshTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshTopologyCanvas(
    devices: List<BleDevice>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    
    // Scale animation for the expanding rings (when no devices)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 3.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale"
    )

    // Alpha animation for fading out as rings expand
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha"
    )
    
    val nodeRadius = with(LocalDensity.current) { MeshTheme.spacing.medium.toPx() }
    val centerRadius = with(LocalDensity.current) { MeshTheme.spacing.mediumLarge.toPx() }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    val stroke4 = remember { Stroke(width = 4f) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            if (devices.isEmpty()) {
                // Draw Radar Pulse
                val maxRadius = (size.width.coerceAtMost(size.height) / 2) * 0.8f
                drawCircle(
                    color = primaryColor.copy(alpha = alpha),
                    radius = maxRadius * scale / 3f,
                    center = center,
                    style = stroke4
                )
                drawCircle(
                    color = primaryColor.copy(alpha = (alpha * 1.5f).coerceAtMost(1f)),
                    radius = maxRadius * (scale / 3f) * 0.6f,
                    center = center,
                    style = stroke4
                )
            } else {
                // Draw connections and nodes
                val radius = (size.width.coerceAtMost(size.height) / 2) * 0.65f
                val angleStep = (2 * Math.PI) / devices.size

                devices.forEachIndexed { index, device ->
                    val angle = index * angleStep
                    val x = center.x + radius * cos(angle).toFloat()
                    val y = center.y + radius * sin(angle).toFloat()
                    val nodePos = Offset(x, y)

                    // Draw line from center to node
                    val pathEffect = if (device.rssi < -85) dashEffect else null
                    val lineColor = if (device.rssi < -85) onSurface.copy(alpha = 0.3f) else primaryColor.copy(alpha = 0.6f)
                    
                    drawLine(
                        color = lineColor,
                        start = center,
                        end = nodePos,
                        strokeWidth = 3f,
                        pathEffect = pathEffect
                    )

                    // Draw Node Background
                    drawCircle(
                        color = surfaceVariant,
                        radius = nodeRadius + 4f,
                        center = nodePos
                    )
                    
                    // Draw Node
                    val nodeColor = if (device.rssi > -70) primaryColor else if (device.rssi > -85) primaryColor.copy(alpha=0.5f) else errorColor
                    drawCircle(
                        color = nodeColor,
                        radius = nodeRadius,
                        center = nodePos
                    )
                }
            }

            // Draw Central "Me" Node
            drawCircle(
                color = surfaceVariant,
                radius = centerRadius + 6f,
                center = center
            )
            drawCircle(
                color = primaryColor,
                radius = centerRadius,
                center = center
            )
        }
    }
}
