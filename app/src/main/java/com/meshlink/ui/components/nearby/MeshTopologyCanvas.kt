package com.meshlink.ui.components.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.meshlink.ui.designsystem.theme.BrandPrimary
import com.meshlink.ui.designsystem.theme.BrandSecondary
import com.meshlink.ui.designsystem.theme.SuccessColorDark
import com.meshlink.util.MeshIdNormalizer
import kotlin.math.PI
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
 * Production-ready Mesh Network Radar Visualization.
 *
 * Visualizes live mesh node discovery around the local user ("You"):
 * - Continuous 360° rotating radar sweep beam with smooth phosphor gradient trail
 * - Concentric tactical radar rings with signal markers (-50dBm, -70dBm, -85dBm)
 * - Soft expanding radar pulse wave
 * - Dedicated central "You" hub node with label (no smartphone hardware name)
 * - Discovered peer nodes positioned deterministically via canonical Mesh ID hash and real RSSI
 * - Display name pill badge showing user's chosen Mesh-Link profile name (or "Mesh Peer" while pending)
 * - Real connection lines and live packet animation for actively connected peers only
 * - Quieter state for unconnected discovered peers (no fake connection lines)
 * - Zero CPU/GPU waste: animations execute strictly inside DrawScope without recomposition loops
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
    val connectedGreen = SuccessColorDark
    val relayBlue = BrandSecondary
    val neutralNodeColor = primaryColor.copy(alpha = 0.85f)

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 1. Hardware-accelerated infinite animation transitions
    val infiniteTransition = rememberInfiniteTransition(label = "RadarMeshAnimations")

    // Continuous 360-degree rotating sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    // Soft scanning pulse wave expanding outward (0.0 to 1.0)
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulseProgress"
    )

    // Breathing node pulse (1.0 to 1.06)
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NodeBreathing"
    )

    // Active connection packet flow animation (0.0 to 1.0)
    val packetOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PacketFlowOffset"
    )

    val centerNodeRadiusPx = with(density) { 22.dp.toPx() }
    val peerNodeRadiusPx = with(density) { 13.dp.toPx() }

    // Pre-allocate Dash and Stroke effects to avoid allocations in drawScope
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    val ringStroke = remember { Stroke(width = 1.2f) }
    val outerRingStroke = remember { Stroke(width = 2.0f) }
    val pulseStroke = remember { Stroke(width = 2.0f) }
    val crosshairDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

    // Keep track of calculated positions for tap detection without mutating Compose state during draw
    val nodePositionsRef = remember { java.util.concurrent.atomic.AtomicReference<List<NodePosition>>(emptyList()) }

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
            color = Color(0xFFF1F5F9),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
    val markerTextStyle = remember {
        TextStyle(
            color = Color(0x6694A3B8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start
        )
    }

    val connectedCount = remember(devices) { devices.count { it.isConnected } }
    val semanticsDesc = remember(devices.size, connectedCount, isScanning) {
        "Radar discovery canvas showing ${devices.size} nearby mesh peers, $connectedCount connected. " +
            if (isScanning) "Continuously scanning." else "Scan paused."
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
                        sqrt(dx * dx + dy * dy) <= nodePos.radiusPx * 2.2f
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
            val maxCanvasRadius = (size.width.coerceAtMost(size.height) / 2f) * 0.86f
            if (maxCanvasRadius <= 0f) return@Canvas

            val localCalculatedPositions = ArrayList<NodePosition>()

            // 1. RADAR BACKGROUND DEPTH & TACTICAL GRID
            // Subtle radial gradient background backing
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.08f),
                        BrandSecondary.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxCanvasRadius
                ),
                radius = maxCanvasRadius,
                center = center
            )

            // 2. CONCENTRIC RADAR RINGS (4 rings)
            val ringFractions = floatArrayOf(0.30f, 0.52f, 0.74f, 1.00f)
            val ringLabels = arrayOf("-50 dBm", "-70 dBm", "-85 dBm", "Mesh Edge")

            ringFractions.forEachIndexed { index, fraction ->
                val ringRadius = maxCanvasRadius * fraction
                val isOuter = index == ringFractions.lastIndex
                val ringColor = if (isOuter) primaryColor.copy(alpha = 0.45f) else primaryColor.copy(alpha = 0.16f)

                drawCircle(
                    color = ringColor,
                    radius = ringRadius,
                    center = center,
                    style = if (isOuter) outerRingStroke else ringStroke
                )

                // Draw distance / signal reference marker label along the horizontal axis
                if (fraction < 1.0f) {
                    val labelResult = textMeasurer.measure(
                        text = ringLabels[index],
                        style = markerTextStyle
                    )
                    drawText(
                        textLayoutResult = labelResult,
                        topLeft = Offset(center.x + ringRadius + 4f, center.y - labelResult.size.height - 2f)
                    )
                }
            }

            // Crosshair tactical axes (North-South, East-West)
            val axisLength = maxCanvasRadius * 0.98f
            drawLine(
                color = primaryColor.copy(alpha = 0.14f),
                start = Offset(center.x - axisLength, center.y),
                end = Offset(center.x + axisLength, center.y),
                strokeWidth = 1f,
                pathEffect = crosshairDashEffect
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.14f),
                start = Offset(center.x, center.y - axisLength),
                end = Offset(center.x, center.y + axisLength),
                strokeWidth = 1f,
                pathEffect = crosshairDashEffect
            )

            // 3. SOFT SCANNING PULSE WAVE
            if (isScanning) {
                val pulseRadius = maxCanvasRadius * pulseProgress
                val pulseAlpha = (1f - pulseProgress).coerceIn(0f, 0.35f)
                drawCircle(
                    color = primaryColor.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = center,
                    style = pulseStroke
                )
            }

            // 4. CONTINUOUS ROTATING RADAR SWEEP BEAM & TRAILING PHOSPHOR GLOW
            val activeSweepAngle = if (isScanning) sweepAngle else 0f
            rotate(degrees = activeSweepAngle, pivot = center) {
                // Trailing phosphor sweep gradient (covers ~75 degrees behind the beam)
                val trailBrush = Brush.sweepGradient(
                    0.00f to Color.Transparent,
                    0.76f to Color.Transparent,
                    0.86f to primaryColor.copy(alpha = 0.03f),
                    0.93f to BrandSecondary.copy(alpha = 0.10f),
                    0.98f to primaryColor.copy(alpha = 0.22f),
                    1.00f to primaryColor.copy(alpha = 0.38f),
                    center = center
                )
                drawCircle(
                    brush = trailBrush,
                    radius = maxCanvasRadius,
                    center = center
                )

                // Leading glowing sweep line
                drawLine(
                    brush = Brush.linearGradient(
                        0f to primaryColor.copy(alpha = 0.1f),
                        1f to primaryColor.copy(alpha = 0.85f),
                        start = center,
                        end = Offset(center.x + maxCanvasRadius, center.y)
                    ),
                    start = center,
                    end = Offset(center.x + maxCanvasRadius, center.y),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round
                )
            }

            // 5. DETERMINISTIC NODE POSITIONING & CONNECTION VISUALIZATION
            if (devices.isNotEmpty()) {
                // Pre-calculate deterministic angles and radii based on canonical Mesh ID & signal
                devices.forEachIndexed { itemIndex, device ->
                    val canonicalId = MeshIdNormalizer.canonicalize(device.meshId.ifBlank { device.address })

                    // Deterministic Angle: hash canonical Mesh ID to ensure node is visually stable
                    val idHash = abs((canonicalId.hashCode().toLong() and 0xFFFFFFFFL))
                    val baseAngleDeg = ((idHash % 360) + (itemIndex * 37)) % 360
                    val angleRad = Math.toRadians(baseAngleDeg.toDouble()).toFloat()

                    // Radial distance mapped from RSSI / hop count
                    val radiusRatio = when {
                        device.isMeshNode -> 0.85f // Multi-hop mesh node on outer ring
                        device.distanceMeters != null -> (0.35f + (device.distanceMeters.toFloat() / 25f).coerceIn(0f, 1f) * 0.45f)
                        device.rssi >= -60 -> 0.34f // Strong signal
                        device.rssi >= -75 -> 0.52f // Medium signal
                        device.rssi >= -88 -> 0.70f // Weak signal
                        else -> 0.82f // Far / edge
                    }

                    val ringRadius = maxCanvasRadius * radiusRatio
                    val posX = center.x + ringRadius * cos(angleRad)
                    val posY = center.y + ringRadius * sin(angleRad)
                    val nodePos = Offset(posX, posY)

                    // Resolve user's chosen display name (fallback to "Mesh Peer")
                    val profName = device.displayName?.trim()?.takeIf {
                        it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
                    }
                    val fallbackName = device.name.trim().takeIf {
                        it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
                    }
                    val resolvedDisplayName = profName ?: fallbackName ?: "Mesh Peer"

                    localCalculatedPositions.add(
                        NodePosition(
                            device = device,
                            centerOffset = nodePos,
                            radiusPx = peerNodeRadiusPx,
                            displayName = resolvedDisplayName
                        )
                    )

                    val isSelected = device.address == selectedAddress
                    val isDirectConnected = device.isConnected && !device.isMeshNode
                    val isRelay = device.isMeshNode || (device.capabilities.toInt() and 0x01 != 0)

                    // 5a. CONNECTION LINE (ONLY for real connections!)
                    if (isDirectConnected) {
                        // Solid illuminated green line from center ("You") to connected peer
                        drawLine(
                            color = connectedGreen.copy(alpha = 0.85f),
                            start = center,
                            end = nodePos,
                            strokeWidth = 2.8f
                        )

                        // Live animated packet dot flowing along the active connection line
                        val packetDx = nodePos.x - center.x
                        val packetDy = nodePos.y - center.y
                        val currentPacketPos = Offset(
                            center.x + packetDx * packetOffset,
                            center.y + packetDy * packetOffset
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 3.5f,
                            center = currentPacketPos
                        )
                        drawCircle(
                            color = connectedGreen.copy(alpha = 0.5f),
                            radius = 6.5f,
                            center = currentPacketPos
                        )
                    } else if (isRelay) {
                        // Subtle dashed relay connection indication
                        drawLine(
                            color = relayBlue.copy(alpha = 0.45f),
                            start = center,
                            end = nodePos,
                            strokeWidth = 1.6f,
                            pathEffect = dashEffect
                        )
                    }
                    // Discovered unconnected peers show NO connection line (quieter state)

                    // 5b. PEER NODE BADGE
                    val nodeBreathing = 1.0f + (breathingScale - 1.0f) * if (itemIndex % 2 == 0) 1.0f else 0.5f
                    val dynamicRadius = peerNodeRadiusPx * nodeBreathing * (if (isSelected) 1.25f else 1.0f)

                    // Outer Selection Halo Ring
                    if (isSelected) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.35f),
                            radius = dynamicRadius + 8f,
                            center = nodePos
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = dynamicRadius + 4f,
                            center = nodePos,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Node Outer Border / Halo
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = dynamicRadius + 2.5f,
                        center = nodePos
                    )

                    // Node Body Color
                    val nodeColor = when {
                        isDirectConnected -> connectedGreen
                        isRelay -> relayBlue
                        else -> neutralNodeColor
                    }

                    drawCircle(
                        color = nodeColor,
                        radius = dynamicRadius,
                        center = nodePos
                    )

                    // Inner Dot / Core Indicator
                    val coreColor = if (isDirectConnected || isRelay) Color.White else Color(0xFF0F172A)
                    drawCircle(
                        color = coreColor,
                        radius = dynamicRadius * 0.35f,
                        center = nodePos
                    )

                    // 5c. PEER DISPLAY NAME PILL BADGE
                    val truncatedName = if (resolvedDisplayName.length > 14) {
                        resolvedDisplayName.take(13) + "…"
                    } else {
                        resolvedDisplayName
                    }

                    val nameLayoutResult = textMeasurer.measure(
                        text = truncatedName,
                        style = nodeLabelTextStyle
                    )

                    val pillPaddingH = 6.dp.toPx()
                    val pillPaddingV = 2.5.dp.toPx()
                    val pillWidth = nameLayoutResult.size.width + pillPaddingH * 2
                    val pillHeight = nameLayoutResult.size.height + pillPaddingV * 2
                    val pillTop = nodePos.y + dynamicRadius + 3.dp.toPx()
                    val pillLeft = nodePos.x - pillWidth / 2f

                    // Semi-transparent dark pill background
                    drawRoundRect(
                        color = Color(0xDD0F172A),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = if (isSelected) primaryColor.copy(alpha = 0.6f) else Color(0x33FFFFFF),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1f)
                    )

                    // Draw text inside pill
                    drawText(
                        textLayoutResult = nameLayoutResult,
                        topLeft = Offset(pillLeft + pillPaddingH, pillTop + pillPaddingV)
                    )
                }
            }

            // 6. CENTRAL "YOU" HUB NODE
            val centerBreathingRadius = centerNodeRadiusPx * (1.0f + (breathingScale - 1.0f) * 0.4f)

            // Outer Glowing Aura for "You" node
            drawCircle(
                color = primaryColor.copy(alpha = 0.18f),
                radius = centerBreathingRadius + 12f,
                center = center
            )

            // Dark Boundary Border
            drawCircle(
                color = Color(0xFF0F172A),
                radius = centerBreathingRadius + 3f,
                center = center
            )

            // Solid Primary Center Circle
            drawCircle(
                color = primaryColor,
                radius = centerBreathingRadius,
                center = center
            )

            // Crisp Inner White Dot
            drawCircle(
                color = Color.White,
                radius = centerBreathingRadius * 0.28f,
                center = center
            )

            // "You" Label Pill directly beneath center node
            val youLayoutResult = textMeasurer.measure(
                text = "You",
                style = youTextStyle
            )
            val youPillPaddingH = 8.dp.toPx()
            val youPillPaddingV = 2.dp.toPx()
            val youPillWidth = youLayoutResult.size.width + youPillPaddingH * 2
            val youPillHeight = youLayoutResult.size.height + youPillPaddingV * 2
            val youPillTop = center.y + centerBreathingRadius + 3.dp.toPx()
            val youPillLeft = center.x - youPillWidth / 2f

            drawRoundRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(youPillLeft, youPillTop),
                size = Size(youPillWidth, youPillHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.7f),
                topLeft = Offset(youPillLeft, youPillTop),
                size = Size(youPillWidth, youPillHeight),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 1f)
            )
            drawText(
                textLayoutResult = youLayoutResult,
                topLeft = Offset(youPillLeft + youPillPaddingH, youPillTop + youPillPaddingV)
            )

            // 7. EMPTY STATE RADAR OVERLAY
            if (devices.isEmpty()) {
                val emptyStatusText = if (isScanning) "Continuously scanning for mesh peers..." else "Mesh discovery paused"
                val statusLayoutResult = textMeasurer.measure(
                    text = emptyStatusText,
                    style = TextStyle(
                        color = Color(0x9994A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
                val statusTop = center.y + maxCanvasRadius * 0.68f
                drawText(
                    textLayoutResult = statusLayoutResult,
                    topLeft = Offset(center.x - statusLayoutResult.size.width / 2f, statusTop)
                )
            }

            nodePositionsRef.set(localCalculatedPositions)
        }
    }
}
