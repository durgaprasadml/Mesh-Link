package com.meshlink.ui.components.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class NodePosition(
    val device: BleDevice,
    val centerOffset: Offset,
    val radiusPx: Float
)

@Composable
fun MeshTopologyCanvas(
    devices: List<BleDevice>,
    modifier: Modifier = Modifier,
    selectedAddress: String? = null,
    onNodeSelected: ((BleDevice) -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val connectedColor = Color(0xFF4CAF50)
    val weakColor = Color(0xFFFF9800)
    val relayColor = Color(0xFF2196F3)

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Infinite Animation Transitions for Radar, Breathing, and Packet Flow
    val infiniteTransition = rememberInfiniteTransition(label = "MeshTopologyAnimations")

    // Radar scan ripples (0.0 to 1.0)
    val radarProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarWave1"
    )

    val radarProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarWave2"
    )

    // Breathing node pulse (1.0 to 1.08)
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NodeBreathing"
    )

    // Packet flow offset (0.0 to 1.0)
    val packetOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PacketFlow"
    )

    val centerNodeRadiusPx = with(density) { 26.dp.toPx() }
    val baseNodeRadiusPx = with(density) { 14.dp.toPx() }

    // Pre-allocate Dash and Stroke effects to avoid allocations in drawScope
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) }
    val stroke2 = remember { Stroke(width = 2f) }

    // Keep track of calculated positions for tap detection without mutating Compose state during draw
    val nodePositionsRef = remember { java.util.concurrent.atomic.AtomicReference<List<NodePosition>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Interactive Mesh Network Topology showing ${devices.size} nearby devices and active connections"
            }
            .pointerInput(devices) {
                detectTapGestures { tapOffset ->
                    val currentPositions = nodePositionsRef.get()
                    val tappedNode = currentPositions.firstOrNull { nodePos ->
                        val dx = tapOffset.x - nodePos.centerOffset.x
                        val dy = tapOffset.y - nodePos.centerOffset.y
                        sqrt(dx * dx + dy * dy) <= nodePos.radiusPx * 1.8f
                    }

                    if (tappedNode != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNodeSelected?.invoke(tappedNode.device)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxCanvasRadius = (size.width.coerceAtMost(size.height) / 2f) * 0.88f
            val localCalculatedPositions = ArrayList<NodePosition>()

            // 1. DRAW RADAR SCANNING RIPPLES
            val wave1Radius = maxCanvasRadius * radarProgress1
            val wave1Alpha = (1f - radarProgress1).coerceIn(0f, 0.4f)
            drawCircle(
                color = primaryColor.copy(alpha = wave1Alpha),
                radius = wave1Radius,
                center = center,
                style = stroke2
            )

            val wave2Radius = maxCanvasRadius * radarProgress2
            val wave2Alpha = (1f - radarProgress2).coerceIn(0f, 0.4f)
            drawCircle(
                color = primaryColor.copy(alpha = wave2Alpha),
                radius = wave2Radius,
                center = center,
                style = stroke2
            )

            // 2. COLLISION-AWARE RADIAL LAYOUT CALCULATION
            if (devices.isNotEmpty()) {
                // Sort devices by RSSI descending (strongest/closest first)
                val sortedDevices = devices.sortedByDescending { it.rssi }

                // Group devices into concentric orbital rings based on count & RSSI
                val ringCount = when {
                    sortedDevices.size <= 6 -> 1
                    sortedDevices.size <= 18 -> 2
                    else -> 3
                }

                val itemsPerRing = sortedDevices.chunked((sortedDevices.size / ringCount.toFloat()).toInt().coerceAtLeast(1))

                itemsPerRing.forEachIndexed { ringIndex, ringDevices ->
                    val ringRadius = maxCanvasRadius * (0.35f + ringIndex * 0.28f)
                    val angleStep = (2 * PI) / ringDevices.size
                    val baseAngleOffset = ringIndex * (PI / 6) // Stagger angles between rings

                    ringDevices.forEachIndexed { itemIndex, device ->
                        val isSelected = device.address == selectedAddress
                        val isRelay = (device.capabilities.toInt() and 0x01 != 0) || (device.isConnected && device.rssi > -75)

                        val angle = baseAngleOffset + itemIndex * angleStep
                        val posX = center.x + ringRadius * cos(angle).toFloat()
                        val posY = center.y + ringRadius * sin(angle).toFloat()
                        val nodePos = Offset(posX, posY)

                        // Store position for tap gesture hit testing
                        localCalculatedPositions.add(NodePosition(device, nodePos, baseNodeRadiusPx))

                        // CONNECTION LINE VISUALIZATION
                        val isWeak = device.rssi < -85
                        val isMedium = device.rssi in -85..-70
                        val isStrong = device.rssi > -70

                        val lineColor = when {
                            device.isConnected -> connectedColor
                            isRelay -> relayColor
                            isWeak -> weakColor.copy(alpha = 0.5f)
                            isMedium -> primaryColor.copy(alpha = 0.6f)
                            else -> primaryColor.copy(alpha = 0.85f)
                        }

                        val linePathEffect = if (isWeak || !device.isConnected) dashEffect else null
                        val strokeWidth = if (isStrong || device.isConnected) 3.5f else 2f

                        // Draw connection line from Center ("YOU") to Node
                        drawLine(
                            color = lineColor,
                            start = center,
                            end = nodePos,
                            strokeWidth = strokeWidth,
                            pathEffect = linePathEffect
                        )

                        // LIVE PACKET FLOW ANIMATION along active connected/relay lines
                        if (device.isConnected || isRelay) {
                            val packetDx = nodePos.x - center.x
                            val packetDy = nodePos.y - center.y
                            val currentPacketPos = Offset(
                                center.x + packetDx * packetOffset,
                                center.y + packetDy * packetOffset
                            )

                            // Glowing packet dot moving along line
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = currentPacketPos
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 7f,
                                center = currentPacketPos
                            )
                        }

                        // NODE VISUALIZATION WITH BREATHING ANIMATION
                        // Apply individual phase offset based on itemIndex so nodes don't pulse in lockstep
                        val nodePhaseScale = 1.0f + (breathingScale - 1.0f) * if (itemIndex % 2 == 0) 1.0f else 0.6f
                        val currentNodeRadius = baseNodeRadiusPx * nodePhaseScale * (if (isSelected) 1.25f else 1.0f)

                        // Outer Glow Ring for Selected / Connected
                        if (isSelected) {
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.35f),
                                radius = currentNodeRadius + 10f,
                                center = nodePos
                            )
                        }

                        // Node Outer Halo Surface
                        drawCircle(
                            color = surfaceVariant,
                            radius = currentNodeRadius + 3f,
                            center = nodePos
                        )

                        // Node Color Body
                        val nodeBodyColor = when {
                            device.isConnected -> connectedColor
                            isRelay -> relayColor
                            isWeak -> weakColor
                            else -> primaryColor
                        }

                        drawCircle(
                            color = nodeBodyColor,
                            radius = currentNodeRadius,
                            center = nodePos
                        )

                        // Inner Core Ring for Relay/Connected
                        if (isRelay || device.isConnected) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                radius = currentNodeRadius * 0.35f,
                                center = nodePos
                            )
                        }
                    }
                }
            }

            // 3. DRAW CENTRAL "YOU" HUB NODE
            val centerBreathingRadius = centerNodeRadiusPx * (1.0f + (breathingScale - 1.0f) * 0.5f)

            // Outer Glowing Aura for Central Node
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = centerBreathingRadius + 14f,
                center = center
            )

            // Outer Border
            drawCircle(
                color = surfaceVariant,
                radius = centerBreathingRadius + 4f,
                center = center
            )

            // Center Primary Circle
            drawCircle(
                color = primaryColor,
                radius = centerBreathingRadius,
                center = center
            )

            // Inner Core Dot
            drawCircle(
                color = Color.White,
                radius = centerBreathingRadius * 0.3f,
                center = center
            )

            nodePositionsRef.set(localCalculatedPositions)
        }
    }
}
