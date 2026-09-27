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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.SuccessColorDark
import kotlin.math.sqrt

/**
 * Apple-inspired, high-performance Mesh Network Discovery Radar Canvas.
 *
 * Core architectural features:
 * - Precomputed layout via [RadarLayoutEngine]: Zero allocations or text measuring in DrawScope.
 * - Guaranteed non-overlapping peer markers and labels.
 * - Protected center "You" node exclusion zone with clear separation.
 * - Restrained, elegant aesthetics: 3 subtle concentric rings, clean cardinal marks, gentle sweep.
 * - Real discovered peers only with peer initial badges and verified profile names.
 * - Real connection lines for actively connected peers only.
 * - Search highlighting with subtle dimming of non-matching peers.
 */
@Composable
fun MeshTopologyCanvas(
    devices: List<BleDevice>,
    modifier: Modifier = Modifier,
    selectedAddress: String? = null,
    searchQuery: String = "",
    isScanning: Boolean = true,
    onNodeSelected: ((BleDevice) -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val connectedColor = SuccessColorDark

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 1. Text styles
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
    val initialTextStyle = remember {
        TextStyle(
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

    // 2. Hardware-accelerated animations (animating purely visual angles / progress)
    val infiniteTransition = rememberInfiniteTransition(label = "RadarCanvasTransitions")

    // Smooth continuous 360-degree rotating sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    // Gentle expanding scanning pulse wave
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulseProgress"
    )

    // Gentle breathing scale for active elements
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarBreathing"
    )

    // Packet flow offset along verified connection lines
    val packetOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPacketOffset"
    )

    // Reusable stroke objects
    val boundaryStroke = remember { Stroke(width = 0.8f) }
    val outerBoundaryStroke = remember { Stroke(width = 1.2f) }
    val pulseStroke = remember { Stroke(width = 1.0f) }

    val connectedCount = remember(devices) { devices.count { it.isConnected } }
    val semanticsDesc = remember(devices.size, connectedCount, isScanning) {
        "Mesh network radar showing ${devices.size} nearby peers, $connectedCount connected. " +
            if (isScanning) "Actively scanning." else "Discovery paused."
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = semanticsDesc }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val canvasSize = remember(widthPx, heightPx) { Size(widthPx, heightPx) }

        // PRECOMPUTE full collision-aware layout outside DrawScope!
        val sceneLayout = remember(devices, selectedAddress, searchQuery, canvasSize, density) {
            RadarLayoutEngine.calculateLayout(
                devices = devices,
                canvasSize = canvasSize,
                density = density,
                textMeasurer = textMeasurer,
                labelTextStyle = nodeLabelTextStyle,
                initialTextStyle = initialTextStyle,
                youTextStyle = youTextStyle,
                selectedAddress = selectedAddress,
                searchQuery = searchQuery
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sceneLayout.nodes) {
                    detectTapGestures { tapOffset ->
                        val tappedNode = sceneLayout.nodes.firstOrNull { node ->
                            val dx = tapOffset.x - node.centerOffset.x
                            val dy = tapOffset.y - node.centerOffset.y
                            sqrt(dx * dx + dy * dy) <= node.nodeRadiusPx * 2.2f
                        }
                        if (tappedNode != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNodeSelected?.invoke(tappedNode.device)
                        }
                    }
                }
        ) {
            val center = sceneLayout.center
            val maxRadarRadius = sceneLayout.maxRadius
            if (maxRadarRadius <= 0f) return@Canvas

            // 1. SUBTLE AMBIENT RADAR BACKGROUND
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.04f),
                        primaryColor.copy(alpha = 0.01f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadarRadius
                ),
                radius = maxRadarRadius,
                center = center
            )

            // 2. RESTRAINED CONCENTRIC RADAR RINGS (3 clean rings)
            val ringFractions = floatArrayOf(0.42f, 0.70f, 1.00f)
            ringFractions.forEachIndexed { index, fraction ->
                val ringRadius = maxRadarRadius * fraction
                val isOuter = index == ringFractions.lastIndex
                val ringColor = if (isOuter) {
                    primaryColor.copy(alpha = 0.18f)
                } else {
                    primaryColor.copy(alpha = 0.09f)
                }

                drawCircle(
                    color = ringColor,
                    radius = ringRadius,
                    center = center,
                    style = if (isOuter) outerBoundaryStroke else boundaryStroke
                )
            }

            // 3. SUBTLE CARDINAL TICK MARKS (Apple-style navigation elegance)
            val tickLen = 6.dp.toPx()
            val outerRadius = maxRadarRadius
            val cardinalAngles = floatArrayOf(0f, 90f, 180f, 270f)
            for (angle in cardinalAngles) {
                rotate(degrees = angle, pivot = center) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.25f),
                        start = Offset(center.x + outerRadius - tickLen, center.y),
                        end = Offset(center.x + outerRadius, center.y),
                        strokeWidth = 1.0f
                    )
                }
            }

            // 4. GENTLE SCANNING PULSE WAVE
            if (isScanning) {
                val pulseRadius = maxRadarRadius * pulseProgress
                val pulseAlpha = (1f - pulseProgress).coerceIn(0f, 0.18f)
                drawCircle(
                    color = primaryColor.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = center,
                    style = pulseStroke
                )
            }

            // 5. SUBTLE CONTINUOUS ROTATING SCANNING SWEEP
            if (isScanning) {
                rotate(degrees = sweepAngle, pivot = center) {
                    // Soft gradient trail (~50 degrees arc behind beam)
                    val trailBrush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.84f to Color.Transparent,
                        0.92f to primaryColor.copy(alpha = 0.02f),
                        0.97f to primaryColor.copy(alpha = 0.06f),
                        1.00f to primaryColor.copy(alpha = 0.14f),
                        center = center
                    )
                    drawCircle(
                        brush = trailBrush,
                        radius = maxRadarRadius,
                        center = center
                    )

                    // Leading sweep line
                    drawLine(
                        brush = Brush.linearGradient(
                            0f to primaryColor.copy(alpha = 0.04f),
                            1f to primaryColor.copy(alpha = 0.45f),
                            start = center,
                            end = Offset(center.x + maxRadarRadius, center.y)
                        ),
                        start = center,
                        end = Offset(center.x + maxRadarRadius, center.y),
                        strokeWidth = 1.2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 6. VERIFIED PEER NODES & ACTIVE CONNECTION LINES
            for (node in sceneLayout.nodes) {
                val nodePos = node.centerOffset
                val isSelected = node.isSelected
                val isDirectConnected = node.isConnected
                val dynamicRadius = node.nodeRadiusPx * (if (isSelected) 1.22f else 1.0f)
                val isDimmed = searchQuery.isNotBlank() && !node.isMatchSearch

                val baseAlpha = if (isDimmed) 0.30f else 1.0f

                // 6a. ACTIVE CONNECTION LINE (Only for verified active connections)
                if (isDirectConnected) {
                    drawLine(
                        color = connectedColor.copy(alpha = 0.55f * baseAlpha),
                        start = center,
                        end = nodePos,
                        strokeWidth = 1.5f
                    )

                    // Subtle packet pulse dot along active connection
                    val packetDx = nodePos.x - center.x
                    val packetDy = nodePos.y - center.y
                    val currentPacketPos = Offset(
                        center.x + packetDx * packetOffset,
                        center.y + packetDy * packetOffset
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = baseAlpha),
                        radius = 2.0f,
                        center = currentPacketPos
                    )
                    drawCircle(
                        color = connectedColor.copy(alpha = 0.35f * baseAlpha),
                        radius = 4.0f,
                        center = currentPacketPos
                    )
                }

                // 6b. SELECTION RING
                if (isSelected) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.20f * baseAlpha),
                        radius = dynamicRadius + 5f,
                        center = nodePos
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = baseAlpha),
                        radius = dynamicRadius + 2.5f,
                        center = nodePos,
                        style = Stroke(width = 1.2f)
                    )
                }

                // 6c. NODE BOUNDARY BORDER
                drawCircle(
                    color = Color(0xFF1E293B).copy(alpha = baseAlpha),
                    radius = dynamicRadius + 1.2f,
                    center = nodePos
                )

                // 6d. NODE BODY COLOR
                val nodeColor = if (isDirectConnected) {
                    connectedColor.copy(alpha = 0.90f * baseAlpha)
                } else {
                    primaryColor.copy(alpha = 0.85f * baseAlpha)
                }
                drawCircle(
                    color = nodeColor,
                    radius = dynamicRadius,
                    center = nodePos
                )

                // 6e. PEER INITIAL (Apple-style initial inside marker)
                if (node.initialLayoutResult != null) {
                    val initLayout = node.initialLayoutResult
                    val initX = nodePos.x - initLayout.size.width / 2f
                    val initY = nodePos.y - initLayout.size.height / 2f
                    drawText(
                        textLayoutResult = initLayout,
                        topLeft = Offset(initX, initY)
                    )
                } else {
                    // Fallback crisp white core dot
                    drawCircle(
                        color = Color.White.copy(alpha = baseAlpha),
                        radius = dynamicRadius * 0.30f,
                        center = nodePos
                    )
                }

                // 6f. PEER DISPLAY NAME BADGE (Collision-free precomputed pill)
                if (node.showLabel && node.nameLayoutResult != null) {
                    val pillLeft = node.labelTopLeft.x
                    val pillTop = node.labelTopLeft.y
                    val pillW = node.labelSize.width
                    val pillH = node.labelSize.height
                    val pillPadH = 5.dp.toPx()
                    val pillPadV = 2.dp.toPx()

                    drawRoundRect(
                        color = Color(0xEE0F172A).copy(alpha = 0.92f * baseAlpha),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = if (isSelected) primaryColor.copy(alpha = baseAlpha) else Color(0x28FFFFFF).copy(alpha = baseAlpha),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 0.8f)
                    )
                    drawText(
                        textLayoutResult = node.nameLayoutResult,
                        topLeft = Offset(pillLeft + pillPadH, pillTop + pillPadV)
                    )
                }
            }

            // 7. CENTRAL "YOU" HUB NODE (Strictly protected, pristine Apple styling)
            val centerNodeRadiusPx = sceneLayout.centerNodeRadiusPx
            val centerBreathingRadius = centerNodeRadiusPx * (1.0f + (breathingScale - 1.0f) * 0.25f)

            // Subtle outer glow halo for "You" node
            drawCircle(
                color = primaryColor.copy(alpha = 0.12f),
                radius = centerBreathingRadius + 6f,
                center = center
            )

            // Dark boundary ring
            drawCircle(
                color = Color(0xFF0F172A),
                radius = centerBreathingRadius + 1.5f,
                center = center
            )

            // Solid primary circle
            drawCircle(
                color = primaryColor,
                radius = centerBreathingRadius,
                center = center
            )

            // Crisp inner white core dot
            drawCircle(
                color = Color.White,
                radius = centerBreathingRadius * 0.32f,
                center = center
            )

            // "You" Label Pill beneath center node
            if (sceneLayout.youLayoutResult != null) {
                val youRect = sceneLayout.youLabelRect
                val youPadH = 6.dp.toPx()
                val youPadV = 1.5.dp.toPx()

                drawRoundRect(
                    color = Color(0xEE0F172A),
                    topLeft = Offset(youRect.left, youRect.top),
                    size = Size(youRect.width, youRect.height),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.55f),
                    topLeft = Offset(youRect.left, youRect.top),
                    size = Size(youRect.width, youRect.height),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 0.8f)
                )
                drawText(
                    textLayoutResult = sceneLayout.youLayoutResult,
                    topLeft = Offset(youRect.left + youPadH, youRect.top + youPadV)
                )
            }
        }
    }
}
