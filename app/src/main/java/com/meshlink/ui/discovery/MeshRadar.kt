package com.meshlink.ui.discovery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar node visual offset model.
 */
private data class RadarNodePosition(
    val device: BleDevice,
    val offset: Offset,
    val angleRad: Float,
    val radiusRatio: Float,
    val isConnected: Boolean,
    val isRelay: Boolean
)

/**
 * MeshRadar — High performance Canvas animation engine for live mesh discovery.
 */
@Composable
fun MeshRadar(
    devices: List<BleDevice>,
    selectedAddress: String?,
    isScanning: Boolean,
    onNodeSelected: (BleDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val connectedColor = Color(0xFF00F59B) // Mint accent for connected
    val relayColor = Color(0xFFFFB703)     // Amber accent for relay
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val centerHubColor = MaterialTheme.colorScheme.primary

    val sweepRotation by DiscoveryAnimations.rememberRadarRotation(isScanning = isScanning)
    val rippleProgress by DiscoveryAnimations.rememberRippleExpansion()
    val pulseScale by DiscoveryAnimations.rememberNodePulseScale()
    val beamAlpha by DiscoveryAnimations.rememberBeamAlpha()
    val isReducedMotion = DiscoveryAnimations.isReducedMotion()

    // Calculate node coordinates on radar screen bounds
    val nodePositions = remember(devices) {
        devices.mapIndexed { index, device ->
            val angleDeg = ((index * 137.5f) + 45f) % 360f // Golden ratio distribution
            val angleRad = (angleDeg * (PI / 180f)).toFloat()
            // Radius ratio normalized by RSSI (-40 dBm -> 0.25, -95 dBm -> 0.85)
            val clampedRssi = device.rssi.coerceIn(-95, -40)
            val radiusRatio = 0.25f + (( -40 - clampedRssi ).toFloat() / 55f) * 0.60f
            RadarNodePosition(
                device = device,
                offset = Offset.Zero, // Computed dynamically in Canvas scope
                angleRad = angleRad,
                radiusRatio = radiusRatio,
                isConnected = device.isConnected,
                isRelay = (device.capabilities.toInt() and 0x01) != 0
            )
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodePositions) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = minOf(size.width, size.height) * 0.45f

                        // Find closest node to tap point within 36dp touch target threshold
                        val hit = nodePositions.firstOrNull { pos ->
                            val x = center.x + cos(pos.angleRad) * (maxRadius * pos.radiusRatio)
                            val y = center.y + sin(pos.angleRad) * (maxRadius * pos.radiusRatio)
                            val distSq = (tapOffset.x - x) * (tapOffset.x - x) + (tapOffset.y - y) * (tapOffset.y - y)
                            distSq <= (40.dp.toPx() * 40.dp.toPx())
                        }
                        if (hit != null) {
                            onNodeSelected(hit.device)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.45f

            // 1. Draw Concentric Radar Orbit Circles
            val rings = listOf(0.3f, 0.6f, 0.9f)
            rings.forEach { ratio ->
                drawCircle(
                    color = outlineColor,
                    radius = maxRadius * ratio,
                    center = center,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )
            }

            // 2. Draw Crosshair Axes Lines
            drawLine(
                color = outlineColor.copy(alpha = 0.25f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = outlineColor.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // 3. Draw Expanding Scan Ripple Waves (when scanning)
            if (isScanning && !isReducedMotion) {
                val rippleRadius = maxRadius * rippleProgress
                val rippleAlpha = (1.0f - rippleProgress).coerceIn(0f, 1f) * 0.35f
                drawCircle(
                    color = primaryColor.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 4. Draw Rotating Radar Sweep Gradient Cone
            if (!isReducedMotion) {
                val sweepAngleRad = (sweepRotation * (PI / 180f)).toFloat()
                val sweepPath = Path().apply {
                    moveTo(center.x, center.y)
                    val arcAngleRad = sweepAngleRad - (45f * (PI / 180f)).toFloat()
                    val x = center.x + cos(sweepAngleRad) * maxRadius
                    val y = center.y + sin(sweepAngleRad) * maxRadius
                    lineTo(x, y)
                    arcToRadial(center, maxRadius, arcAngleRad, sweepAngleRad)
                    close()
                }

                drawPath(
                    path = sweepPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    )
                )

                // Leading Sweep Beam Line
                val sweepEndX = center.x + cos(sweepAngleRad) * maxRadius
                val sweepEndY = center.y + sin(sweepAngleRad) * maxRadius
                drawLine(
                    color = primaryColor.copy(alpha = 0.85f),
                    start = center,
                    end = Offset(sweepEndX, sweepEndY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // 5. Draw Central Hub Node
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = 24.dp.toPx() * (if (isScanning) pulseScale else 1f),
                center = center
            )
            drawCircle(
                color = centerHubColor,
                radius = 10.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = center
            )

            // 6. Draw Discovered Node Dots and Connection Beams
            nodePositions.forEach { pos ->
                val nodeX = center.x + cos(pos.angleRad) * (maxRadius * pos.radiusRatio)
                val nodeY = center.y + sin(pos.angleRad) * (maxRadius * pos.radiusRatio)
                val nodeCenter = Offset(nodeX, nodeY)

                val isSelected = pos.device.address == selectedAddress
                val dotColor = when {
                    pos.isConnected -> connectedColor
                    pos.isRelay -> relayColor
                    else -> primaryColor
                }

                // Draw Connection Beam Line to Central Hub if Connected or Selected
                if (pos.isConnected || isSelected) {
                    drawLine(
                        color = dotColor.copy(alpha = if (isSelected) 0.9f else beamAlpha),
                        start = center,
                        end = nodeCenter,
                        strokeWidth = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx(),
                        pathEffect = if (!pos.isConnected) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null
                    )
                }

                // Node Pulse Outer Aura
                drawCircle(
                    color = dotColor.copy(alpha = if (isSelected) 0.4f else 0.2f),
                    radius = (if (isSelected) 18.dp else 12.dp).toPx() * pulseScale,
                    center = nodeCenter
                )

                // Node Core Dot
                drawCircle(
                    color = dotColor,
                    radius = (if (isSelected) 8.dp else 6.dp).toPx(),
                    center = nodeCenter
                )

                // Connected Ring Highlight
                if (pos.isConnected) {
                    drawCircle(
                        color = connectedColor,
                        radius = 10.dp.toPx(),
                        center = nodeCenter,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Path helper drawing arc segment between two radial angles.
 */
private fun Path.arcToRadial(center: Offset, radius: Float, startAngleRad: Float, endAngleRad: Float) {
    val steps = 16
    val angleStep = (endAngleRad - startAngleRad) / steps
    for (i in 0..steps) {
        val a = startAngleRad + i * angleStep
        val px = center.x + cos(a) * radius
        val py = center.y + sin(a) * radius
        lineTo(px, py)
    }
}
