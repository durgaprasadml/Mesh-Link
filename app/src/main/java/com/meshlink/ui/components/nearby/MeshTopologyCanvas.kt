package com.meshlink.ui.components.nearby

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.PacketType
import com.meshlink.ui.nearby.ActivePacketEvent
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class CanvasNodeTarget(
    val device: BleDevice,
    val targetAngle: Float,
    val targetRadius: Float,
    val isRelay: Boolean
)

private data class AnimatedNodeState(
    val address: String,
    val animX: Animatable<Float, AnimationVector1D>,
    val animY: Animatable<Float, AnimationVector1D>,
    val alphaAnim: Animatable<Float, AnimationVector1D>,
    val scaleAnim: Animatable<Float, AnimationVector1D>,
    val rippleAnim: Animatable<Float, AnimationVector1D>,
    val connectionBeamAnim: Animatable<Float, AnimationVector1D>
)

private data class ActivePacketParticle(
    val id: String,
    val startPos: Offset,
    val endPos: Offset,
    val color: Color,
    val progress: Animatable<Float, AnimationVector1D>
)

@Composable
fun MeshTopologyCanvas(
    devices: List<BleDevice>,
    modifier: Modifier = Modifier,
    selectedAddress: String? = null,
    latestPacketEvent: ActivePacketEvent? = null,
    onNodeSelected: ((BleDevice) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Detect Accessibility Animator Duration Scale (Reduced Motion)
    val isReducedMotion = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    // Color Palette based on design specification
    val backgroundColor = Color(0xFF0B0B0B)
    val neonGreen = Color(0xFF00E676)
    val relayGreen = Color(0xFF00FF88)
    val discoveredBlue = Color(0xFF00B0FF)
    val searchingGrey = Color(0xFF757575)
    val weakOrange = Color(0xFFFF9800)

    // Infinite Animations: Radar Pulse & Central Hub Breathing
    val infiniteTransition = rememberInfiniteTransition(label = "MeshTopologySubtleAnimations")

    val radarWaveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isReducedMotion) 5000 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarWave"
    )

    val hubBreathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isReducedMotion) 1.01f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HubBreathing"
    )

    val pulseOpacity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseOpacity"
    )

    val baseNodeRadiusPx = with(density) { 16.dp.toPx() }
    val hubRadiusPx = with(density) { 24.dp.toPx() }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) }

    // Map of persistent node animations keyed by device address
    val animatedNodesMap = remember { mutableStateMapOf<String, AnimatedNodeState>() }

    // List of active packet particles
    val activePackets = remember { mutableStateListOf<ActivePacketParticle>() }

    // Track hit areas for tap gestures
    val nodeHitTargets = remember { mutableStateMapOf<String, Pair<BleDevice, Offset>>() }

    // Calculate layout positions & trigger smooth Spring transitions when devices change
    LaunchedEffect(devices) {
        val currentAddresses = devices.map { it.address }.toSet()

        // 1. Clean up removed nodes (fade out & remove)
        val removedAddresses = animatedNodesMap.keys.filter { it !in currentAddresses }
        removedAddresses.forEach { address ->
            val nodeState = animatedNodesMap[address]
            if (nodeState != null) {
                coroutineScope.launch {
                    nodeState.alphaAnim.animateTo(0f, tween(350))
                    nodeState.scaleAnim.animateTo(0f, tween(350))
                    animatedNodesMap.remove(address)
                }
            }
        }

        // 2. Sort devices by RSSI to place strongest near center
        val sortedDevices = devices.sortedByDescending { it.rssi }
        val count = sortedDevices.size

        if (count > 0) {
            val ringCount = when {
                count <= 6 -> 1
                count <= 14 -> 2
                else -> 3
            }
            val itemsPerRing = sortedDevices.chunked((count / ringCount.toFloat()).toInt().coerceAtLeast(1))

            itemsPerRing.forEachIndexed { ringIdx, ringDevices ->
                val ringRadiusFactor = 0.35f + ringIdx * 0.28f
                val angleStep = (2 * PI) / ringDevices.size
                val angleOffset = ringIdx * (PI / 5)

                ringDevices.forEachIndexed { itemIdx, device ->
                    val angle = (angleOffset + itemIdx * angleStep).toFloat()
                    val targetRadiusFactor = ringRadiusFactor

                    val address = device.address

                    var nodeState = animatedNodesMap[address]

                    if (nodeState == null) {
                        // New node discovery animation!
                        val newAnimX = Animatable(0f)
                        val newAnimY = Animatable(0f)
                        val newAlpha = Animatable(0f)
                        val newScale = Animatable(0.8f)
                        val newRipple = Animatable(0f)
                        val newBeam = Animatable(0f)

                        nodeState = AnimatedNodeState(
                            address = address,
                            animX = newAnimX,
                            animY = newAnimY,
                            alphaAnim = newAlpha,
                            scaleAnim = newScale,
                            rippleAnim = newRipple,
                            connectionBeamAnim = newBeam
                        )
                        animatedNodesMap[address] = nodeState

                        // Animate discovery sequence: fade in, scale 0.8->1.0, ripple once
                        coroutineScope.launch {
                            newAlpha.animateTo(1f, tween(400))
                            newScale.animateTo(1f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                            newRipple.animateTo(1f, tween(600, easing = FastOutLinearInEasing))
                        }
                    }

                    // Check if node just connected -> trigger one-shot beam animation!
                    if (device.isConnected && nodeState.connectionBeamAnim.value == 0f) {
                        coroutineScope.launch {
                            nodeState.connectionBeamAnim.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
                        }
                    }

                    // Smoothly animate position change (zero jitter, calm interpolation!)
                    val targetXFactor = targetRadiusFactor * cos(angle)
                    val targetYFactor = targetRadiusFactor * sin(angle)

                    coroutineScope.launch {
                        nodeState.animX.animateTo(
                            targetValue = targetXFactor,
                            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                    coroutineScope.launch {
                        nodeState.animY.animateTo(
                            targetValue = targetYFactor,
                            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        )
                    }
                }
            }
        }
    }

    // Trigger Real Packet Particles when latestPacketEvent arrives
    LaunchedEffect(latestPacketEvent) {
        if (latestPacketEvent != null) {
            val targetNode = animatedNodesMap[latestPacketEvent.senderId] ?: animatedNodesMap.values.firstOrNull()
            if (targetNode != null) {
                val packetColor = when (latestPacketEvent.packetType) {
                    PacketType.TEXT, PacketType.DELIVERY_ACK, PacketType.READ_RECEIPT -> neonGreen
                    PacketType.MEDIA_META, PacketType.MEDIA_CHUNK, PacketType.MEDIA_ACK -> Color(0xFFA855F7)
                    PacketType.LOCATION, PacketType.MAP_SYNC -> Color(0xFF3B82F6)
                    PacketType.VOICE_SIGNAL, PacketType.VOICE_FRAME -> Color(0xFFF97316)
                    else -> neonGreen
                }

                val animProgress = Animatable(0f)
                val particleId = latestPacketEvent.id

                val particle = ActivePacketParticle(
                    id = particleId,
                    startPos = Offset.Zero, // updated during draw relative to center
                    endPos = Offset(targetNode.animX.value, targetNode.animY.value),
                    color = packetColor,
                    progress = animProgress
                )
                activePackets.add(particle)

                coroutineScope.launch {
                    animProgress.animateTo(1f, tween(900, easing = LinearEasing))
                    activePackets.remove(particle)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Mesh Topology Network visualization with ${devices.size} nearby devices."
            }
            .pointerInput(devices, animatedNodesMap) {
                detectTapGestures { tapOffset ->
                    // Hit testing for nodes
                    val hit = nodeHitTargets.values.firstOrNull { (_, pos) ->
                        val dx = tapOffset.x - pos.x
                        val dy = tapOffset.y - pos.y
                        sqrt(dx * dx + dy * dy) <= baseNodeRadiusPx * 2.2f
                    }

                    if (hit != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNodeSelected?.invoke(hit.first)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = (size.width.coerceAtMost(size.height) / 2f) * 0.85f

            nodeHitTargets.clear()

            // 1. DRAW SUBTLE DARK GRADIENT BACKGROUND
            drawRect(color = backgroundColor)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E676).copy(alpha = 0.08f),
                        Color(0xFF00B0FF).copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.2f
                ),
                center = center,
                radius = maxRadius * 1.2f
            )

            // 2. DRAW RADAR SCANNING PULSE WAVE (FROM FIXED HUB)
            val waveRadius = maxRadius * radarWaveProgress
            val waveAlpha = ((1f - radarWaveProgress) * 0.35f).coerceIn(0f, 0.35f)
            drawCircle(
                color = neonGreen.copy(alpha = waveAlpha),
                radius = waveRadius,
                center = center,
                style = Stroke(width = 2f)
            )

            // 3. DRAW DEVICE NODES & CONNECTIONS
            if (devices.isEmpty()) {
                // EMPTY STATE DRAWING IN CANVAS
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 14.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    alpha = (180 * pulseOpacity).toInt()
                }

                drawContext.canvas.nativeCanvas.drawText(
                    "Looking for nearby Mesh Link devices...",
                    center.x,
                    center.y + hubRadiusPx + 48.dp.toPx(),
                    textPaint
                )
            } else {
                val hasSelection = selectedAddress != null

                devices.forEach { device ->
                    val nodeState = animatedNodesMap[device.address] ?: return@forEach

                    val nodeX = center.x + maxRadius * nodeState.animX.value
                    val nodeY = center.y + maxRadius * nodeState.animY.value
                    val nodePos = Offset(nodeX, nodeY)

                    nodeHitTargets[device.address] = Pair(device, nodePos)

                    val isSelected = device.address == selectedAddress
                    val isRelay = (device.capabilities.toInt() and 0x01 != 0) || (device.isConnected && device.rssi > -75)

                    // Node Dimming: if a node is selected, non-selected nodes dim slightly
                    val dimFactor = if (hasSelection && !isSelected) 0.35f else 1.0f
                    val currentAlpha = nodeState.alphaAnim.value * dimFactor

                    // Connection Line Visuals
                    val isStrong = device.rssi > -70
                    val isWeak = device.rssi < -85

                    val lineColor = when {
                        device.isConnected -> neonGreen.copy(alpha = currentAlpha)
                        isRelay -> relayGreen.copy(alpha = currentAlpha * 0.9f)
                        isWeak -> weakOrange.copy(alpha = currentAlpha * 0.5f)
                        else -> discoveredBlue.copy(alpha = currentAlpha * 0.7f)
                    }

                    val strokeWidth = if (isSelected || isStrong) 3.5f else 2f
                    val lineStyle = if (isWeak) dashEffect else null

                    // Draw Connection Line from Center Hub -> Node
                    drawLine(
                        color = lineColor,
                        start = center,
                        end = nodePos,
                        strokeWidth = strokeWidth,
                        pathEffect = lineStyle
                    )

                    // ONE-SHOT CONNECTION BEAM ANIMATION
                    if (nodeState.connectionBeamAnim.value in 0.01f..0.99f) {
                        val beamProgress = nodeState.connectionBeamAnim.value
                        val beamDx = nodePos.x - center.x
                        val beamDy = nodePos.y - center.y
                        val currentBeamPos = Offset(center.x + beamDx * beamProgress, center.y + beamDy * beamProgress)

                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = currentBeamPos
                        )
                        drawCircle(
                            color = neonGreen,
                            radius = 12f,
                            center = currentBeamPos
                        )
                    }

                    // DISCOVERY RIPPLE ANIMATION (EXPANDS ONCE ON DISCOVERY)
                    if (nodeState.rippleAnim.value in 0.01f..0.99f) {
                        val rippleRadius = baseNodeRadiusPx * (1f + nodeState.rippleAnim.value * 1.5f)
                        val rippleAlpha = (1f - nodeState.rippleAnim.value) * currentAlpha
                        drawCircle(
                            color = discoveredBlue.copy(alpha = rippleAlpha),
                            radius = rippleRadius,
                            center = nodePos,
                            style = Stroke(width = 2f)
                        )
                    }

                    // NODE GRAPHICS
                    val nodeRadius = baseNodeRadiusPx * nodeState.scaleAnim.value * (if (isSelected) 1.25f else 1.0f)

                    // Outer Selection Glow / Highlight
                    if (isSelected) {
                        drawCircle(
                            color = neonGreen.copy(alpha = 0.35f),
                            radius = nodeRadius + 12f,
                            center = nodePos
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = nodeRadius + 4f,
                            center = nodePos,
                            style = Stroke(width = 2f)
                        )
                    }

                    // Node Glow Ring
                    val glowColor = when {
                        device.isConnected -> neonGreen
                        isRelay -> relayGreen
                        isWeak -> searchingGrey
                        else -> discoveredBlue
                    }

                    drawCircle(
                        color = glowColor.copy(alpha = 0.25f * currentAlpha),
                        radius = nodeRadius + 6f,
                        center = nodePos
                    )

                    // Node Body Surface
                    drawCircle(
                        color = Color(0xFF181818).copy(alpha = currentAlpha),
                        radius = nodeRadius,
                        center = nodePos
                    )

                    // Node Inner Core State Circle
                    drawCircle(
                        color = glowColor.copy(alpha = currentAlpha),
                        radius = nodeRadius * 0.7f,
                        center = nodePos
                    )

                    // Relay Double Glowing Ring
                    if (isRelay) {
                        drawCircle(
                            color = relayGreen.copy(alpha = 0.8f * currentAlpha),
                            radius = nodeRadius + 4f,
                            center = nodePos,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Small Connection Status Indicator Dot
                    val statusDotColor = if (device.isConnected) neonGreen else Color.Gray
                    drawCircle(
                        color = Color(0xFF0B0B0B),
                        radius = 4.5f,
                        center = Offset(nodePos.x + nodeRadius * 0.65f, nodePos.y - nodeRadius * 0.65f)
                    )
                    drawCircle(
                        color = statusDotColor.copy(alpha = currentAlpha),
                        radius = 3.5f,
                        center = Offset(nodePos.x + nodeRadius * 0.65f, nodePos.y - nodeRadius * 0.65f)
                    )

                    // Device Name Label Text Below Node
                    val displayName = device.name.ifBlank { "Node" }
                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        alpha = (255 * currentAlpha).toInt()
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        if (displayName.length > 10) displayName.take(8) + "…" else displayName,
                        nodePos.x,
                        nodePos.y + nodeRadius + 14.dp.toPx(),
                        labelPaint
                    )
                }
            }

            // 4. DRAW ACTIVE PACKET PARTICLES (REAL TRAFFIC)
            activePackets.forEach { packet ->
                val pVal = packet.progress.value
                val pX = center.x + (packet.endPos.x) * pVal
                val pY = center.y + (packet.endPos.y) * pVal
                val packetPos = Offset(pX, pY)

                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = packetPos
                )
                drawCircle(
                    color = packet.color,
                    radius = 8f,
                    center = packetPos
                )
            }

            // 5. DRAW FIXED CENTRAL HUB (CURRENT DEVICE - YOU)
            val hubBreathingRadius = hubRadiusPx * hubBreathingScale

            // Outer Soft Neon Green Glow Aura
            drawCircle(
                color = neonGreen.copy(alpha = 0.25f),
                radius = hubBreathingRadius + 14f,
                center = center
            )

            // Dark Surface Base
            drawCircle(
                color = Color(0xFF141414),
                radius = hubBreathingRadius + 3f,
                center = center
            )

            // Soft Neon Green Outer Ring
            drawCircle(
                color = neonGreen,
                radius = hubBreathingRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            // Pure White Core Center
            drawCircle(
                color = Color.White,
                radius = hubBreathingRadius * 0.5f,
                center = center
            )
        }
    }
}
