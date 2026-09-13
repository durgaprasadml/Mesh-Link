package com.meshlink.ui.components.nearby

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.core.data.UserRepositoryImpl
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.SuccessColorDark
import com.meshlink.util.MeshIdNormalizer
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class NodePosition(
    val device: BleDevice,
    val centerOffset: Offset,
    val radiusPx: Float,
    val displayName: String
)

/**
 * Modern, clean Mesh Network Discovery Visualization.
 *
 * Visualizes active peer discovery around the local user ("You"):
 * - Calm, restrained circular discovery boundary rings (without overlapping text markers).
 * - Subtle continuous 360° scanning sweep beam with soft gradient trail when active.
 * - Gentle expanding discovery pulse wave.
 * - Anchored central "You" node with clear legible labeling.
 * - Discovered peer nodes positioned deterministically via canonical Mesh ID hash and real signal.
 * - Verified Mesh-Link profile display names (falling back to "Mesh Peer" while pending).
 * - Clean connection line and subtle packet flow for actively connected peers only.
 * - Quiet, uncluttered representation for discovered unconnected peers.
 * - Efficient: animations run within DrawScope without outer recomposition churn.
 */
@Composable
fun MeshTopologyCanvas(
    devices: List<BleDevice>,
    modifier: Modifier = Modifier,
    selectedAddress: String? = null,
    isScanning: Boolean = true,
    onNodeSelected: ((BleDevice) -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val connectedColor = SuccessColorDark

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 1. Hardware-accelerated animations within DrawScope
    val infiniteTransition = rememberInfiniteTransition(label = "DiscoveryCanvasTransitions")

    // Continuous 360-degree rotating sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscoverySweepAngle"
    )

    // Soft scanning pulse wave expanding outward
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscoveryPulseProgress"
    )

    // Breathing scale for active elements
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DiscoveryBreathing"
    )

    // Packet flow offset along connected lines
    val packetOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscoveryPacketOffset"
    )

    val centerNodeRadiusPx = with(density) { 18.dp.toPx() }
    val peerNodeRadiusPx = with(density) { 11.dp.toPx() }

    // Reusable draw objects to eliminate frame allocations
    val boundaryStroke = remember { Stroke(width = 1.0f) }
    val outerBoundaryStroke = remember { Stroke(width = 1.5f) }
    val pulseStroke = remember { Stroke(width = 1.2f) }

    // Positions cache for tap detection
    val nodePositionsRef = remember { AtomicReference<List<NodePosition>>(emptyList()) }

    // Text styles
    val youTextStyle = remember {
        TextStyle(
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
    val nodeLabelTextStyle = remember {
        TextStyle(
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }

    val connectedCount = remember(devices) { devices.count { it.isConnected } }
    val semanticsDesc = remember(devices.size, connectedCount, isScanning) {
        "Mesh network discovery canvas showing ${devices.size} nearby peers, $connectedCount connected. " +
            if (isScanning) "Actively discovering." else "Discovery paused."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = semanticsDesc }
            .pointerInput(devices) {
                detectTapGestures { tapOffset ->
                    val currentPositions = nodePositionsRef.get()
                    val tappedNode = currentPositions.firstOrNull { nodePos ->
                        val dx = tapOffset.x - nodePos.centerOffset.x
                        val dy = tapOffset.y - nodePos.centerOffset.y
                        sqrt(dx * dx + dy * dy) <= nodePos.radiusPx * 2.5f
                    }

                    if (tappedNode != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNodeSelected?.invoke(tappedNode.device)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxCanvasRadius = (size.width.coerceAtMost(size.height) / 2f) * 0.88f
            if (maxCanvasRadius <= 0f) return@Canvas

            val calculatedPositions = ArrayList<NodePosition>(devices.size)

            // 1. SUBTLE BACKGROUND AMBIENT RADIUS
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.05f),
                        primaryColor.copy(alpha = 0.015f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxCanvasRadius
                ),
                radius = maxCanvasRadius,
                center = center
            )

            // 2. RESTRAINED DISCOVERY BOUNDARY RINGS (3 rings, NO overlapping text labels)
            val ringFractions = floatArrayOf(0.40f, 0.70f, 1.00f)
            ringFractions.forEachIndexed { index, fraction ->
                val ringRadius = maxCanvasRadius * fraction
                val isOuter = index == ringFractions.lastIndex
                val ringColor = if (isOuter) {
                    primaryColor.copy(alpha = 0.22f)
                } else {
                    primaryColor.copy(alpha = 0.10f)
                }

                drawCircle(
                    color = ringColor,
                    radius = ringRadius,
                    center = center,
                    style = if (isOuter) outerBoundaryStroke else boundaryStroke
                )
            }

            // 3. EXPANDING DISCOVERY PULSE WAVE (Restrained and gentle)
            if (isScanning) {
                val pulseRadius = maxCanvasRadius * pulseProgress
                val pulseAlpha = (1f - pulseProgress).coerceIn(0f, 0.25f)
                drawCircle(
                    color = primaryColor.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = center,
                    style = pulseStroke
                )
            }

            // 4. SUBTLE CONTINUOUS ROTATING SCANNING SWEEP
            if (isScanning) {
                rotate(degrees = sweepAngle, pivot = center) {
                    // Soft gradient trail (~60 degrees arc behind beam)
                    val trailBrush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.80f to Color.Transparent,
                        0.90f to primaryColor.copy(alpha = 0.02f),
                        0.97f to primaryColor.copy(alpha = 0.08f),
                        1.00f to primaryColor.copy(alpha = 0.18f),
                        center = center
                    )
                    drawCircle(
                        brush = trailBrush,
                        radius = maxCanvasRadius,
                        center = center
                    )

                    // Leading sweep line
                    drawLine(
                        brush = Brush.linearGradient(
                            0f to primaryColor.copy(alpha = 0.05f),
                            1f to primaryColor.copy(alpha = 0.60f),
                            start = center,
                            end = Offset(center.x + maxCanvasRadius, center.y)
                        ),
                        start = center,
                        end = Offset(center.x + maxCanvasRadius, center.y),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. DETERMINISTIC PEER NODE POSITIONING & CONNECTION VISUALIZATION
            if (devices.isNotEmpty()) {
                devices.forEachIndexed { itemIndex, device ->
                    val canonicalId = MeshIdNormalizer.canonicalize(device.meshId.ifBlank { device.address })

                    // Deterministic Angle: hash canonical Mesh ID to ensure node is visually stable
                    val idHash = abs((canonicalId.hashCode().toLong() and 0xFFFFFFFFL))
                    val baseAngleDeg = ((idHash % 360) + (itemIndex * 43)) % 360
                    val angleRad = Math.toRadians(baseAngleDeg.toDouble()).toFloat()

                    // Radial distance mapped to signal / hop
                    val radiusRatio = when {
                        device.isMeshNode -> 0.88f
                        device.rssi >= -60 -> 0.38f // Strong signal
                        device.rssi >= -75 -> 0.58f // Medium signal
                        device.rssi >= -88 -> 0.76f // Weak signal
                        else -> 0.88f
                    }

                    val ringRadius = maxCanvasRadius * radiusRatio
                    val posX = center.x + ringRadius * cos(angleRad)
                    val posY = center.y + ringRadius * sin(angleRad)
                    val nodePos = Offset(posX, posY)

                    // Strictly resolve Mesh-Link profile display name (never smartphone model)
                    val profName = device.displayName?.trim()?.takeIf {
                        it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
                    }
                    val fallbackName = device.name.trim().takeIf {
                        it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
                    }
                    val resolvedDisplayName = profName ?: fallbackName ?: "Mesh Peer"

                    calculatedPositions.add(
                        NodePosition(
                            device = device,
                            centerOffset = nodePos,
                            radiusPx = peerNodeRadiusPx,
                            displayName = resolvedDisplayName
                        )
                    )

                    val isSelected = device.address == selectedAddress
                    val isDirectConnected = device.isConnected

                    // 5a. CONNECTION LINE (ONLY for real connections!)
                    if (isDirectConnected) {
                        drawLine(
                            color = connectedColor.copy(alpha = 0.70f),
                            start = center,
                            end = nodePos,
                            strokeWidth = 2.0f
                        )

                        // Subtle animated packet dot flowing along the active connection line
                        val packetDx = nodePos.x - center.x
                        val packetDy = nodePos.y - center.y
                        val currentPacketPos = Offset(
                            center.x + packetDx * packetOffset,
                            center.y + packetDy * packetOffset
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = currentPacketPos
                        )
                        drawCircle(
                            color = connectedColor.copy(alpha = 0.45f),
                            radius = 5.0f,
                            center = currentPacketPos
                        )
                    }

                    // 5b. PEER NODE BADGE
                    val dynamicRadius = peerNodeRadiusPx * (if (isSelected) 1.20f else 1.0f)

                    // Outer Selection Ring
                    if (isSelected) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.25f),
                            radius = dynamicRadius + 6f,
                            center = nodePos
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = dynamicRadius + 3f,
                            center = nodePos,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Node Outer Dark Border
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = dynamicRadius + 1.5f,
                        center = nodePos
                    )

                    // Node Body Color
                    val nodeColor = if (isDirectConnected) connectedColor else primaryColor.copy(alpha = 0.85f)
                    drawCircle(
                        color = nodeColor,
                        radius = dynamicRadius,
                        center = nodePos
                    )

                    // Node Core Dot
                    drawCircle(
                        color = Color.White,
                        radius = dynamicRadius * 0.32f,
                        center = nodePos
                    )

                    // 5c. PEER DISPLAY NAME BADGE (Clean, compact, no collisions)
                    val truncatedName = if (resolvedDisplayName.length > 12) {
                        resolvedDisplayName.take(11) + "…"
                    } else {
                        resolvedDisplayName
                    }

                    val nameLayoutResult = textMeasurer.measure(
                        text = truncatedName,
                        style = nodeLabelTextStyle
                    )

                    val pillPaddingH = 5.dp.toPx()
                    val pillPaddingV = 2.dp.toPx()
                    val pillWidth = nameLayoutResult.size.width + pillPaddingH * 2
                    val pillHeight = nameLayoutResult.size.height + pillPaddingV * 2
                    val pillTop = nodePos.y + dynamicRadius + 2.dp.toPx()
                    val pillLeft = nodePos.x - pillWidth / 2f

                    // Compact pill backing
                    drawRoundRect(
                        color = Color(0xDD0F172A),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = if (isSelected) primaryColor else Color(0x28FFFFFF),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 0.8f)
                    )

                    // Text
                    drawText(
                        textLayoutResult = nameLayoutResult,
                        topLeft = Offset(pillLeft + pillPaddingH, pillTop + pillPaddingV)
                    )
                }
            }

            // 6. CENTRAL "YOU" HUB NODE
            val centerBreathingRadius = centerNodeRadiusPx * (1.0f + (breathingScale - 1.0f) * 0.3f)

            // Subtle Outer Glow for "You" node
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = centerBreathingRadius + 8f,
                center = center
            )

            // Node Boundary Border
            drawCircle(
                color = Color(0xFF0F172A),
                radius = centerBreathingRadius + 2f,
                center = center
            )

            // Solid Primary Circle
            drawCircle(
                color = primaryColor,
                radius = centerBreathingRadius,
                center = center
            )

            // Inner Core Dot
            drawCircle(
                color = Color.White,
                radius = centerBreathingRadius * 0.30f,
                center = center
            )

            // "You" Label Pill beneath center node
            val youLayoutResult = textMeasurer.measure(
                text = "You",
                style = youTextStyle
            )
            val youPillPaddingH = 7.dp.toPx()
            val youPillPaddingV = 1.5.dp.toPx()
            val youPillWidth = youLayoutResult.size.width + youPillPaddingH * 2
            val youPillHeight = youLayoutResult.size.height + youPillPaddingV * 2
            val youPillTop = center.y + centerBreathingRadius + 2.dp.toPx()
            val youPillLeft = center.x - youPillWidth / 2f

            drawRoundRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(youPillLeft, youPillTop),
                size = Size(youPillWidth, youPillHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.6f),
                topLeft = Offset(youPillLeft, youPillTop),
                size = Size(youPillWidth, youPillHeight),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 0.8f)
            )
            drawText(
                textLayoutResult = youLayoutResult,
                topLeft = Offset(youPillLeft + youPillPaddingH, youPillTop + youPillPaddingV)
            )

            nodePositionsRef.set(calculatedPositions)
        }
    }
}
