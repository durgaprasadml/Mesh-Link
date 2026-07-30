package com.meshlink.ui.components.nearby

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import com.meshlink.ui.designsystem.theme.animateMeshGraphColors
import com.meshlink.ui.designsystem.theme.meshGraphColors
import com.meshlink.ui.nearby.ActivePacketEvent
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

    // Smooth Theme Color Transition (300ms)
    val graphColors = animateMeshGraphColors(MaterialTheme.meshGraphColors)

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

        // Clean up removed nodes
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

        // Sort devices by RSSI to place strongest near center
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

                        coroutineScope.launch {
                            newAlpha.animateTo(1f, tween(400))
                            newScale.animateTo(1f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                            newRipple.animateTo(1f, tween(600, easing = FastOutLinearInEasing))
                        }
                    }

                    if (device.isConnected && nodeState.connectionBeamAnim.value == 0f) {
                        coroutineScope.launch {
                            nodeState.connectionBeamAnim.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
                        }
                    }

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
                    PacketType.TEXT, PacketType.DELIVERY_ACK, PacketType.READ_RECEIPT -> graphColors.connections.packetText
                    PacketType.MEDIA_META, PacketType.MEDIA_CHUNK, PacketType.MEDIA_ACK -> graphColors.connections.packetMedia
                    PacketType.LOCATION, PacketType.MAP_SYNC -> graphColors.connections.packetLocation
                    PacketType.VOICE_SIGNAL, PacketType.VOICE_FRAME -> graphColors.connections.packetVoice
                    else -> graphColors.connections.packetText
                }

                val animProgress = Animatable(0f)
                val particleId = latestPacketEvent.id

                val particle = ActivePacketParticle(
                    id = particleId,
                    startPos = Offset.Zero,
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Mesh Topology Network visualization with ${devices.size} nearby devices."
            }
    ) {
        // Responsive Scaling: Adjust radiuses dynamically based on available width & height
        val isCompact = maxWidth < 380.dp
        val baseNodeRadiusPx = with(density) { (if (isCompact) 14.dp else 16.dp).toPx() }
        val hubRadiusPx = with(density) { (if (isCompact) 20.dp else 24.dp).toPx() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(devices, animatedNodesMap) {
                    detectTapGestures { tapOffset ->
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

                // 1. DRAW SUBTLE THEME-AWARE BACKGROUND & RADIAL GLOW VIGNETTE
                drawRect(color = graphColors.background)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            graphColors.backgroundGradientStart,
                            graphColors.backgroundGradientEnd,
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * 1.25f
                    ),
                    center = center,
                    radius = maxRadius * 1.25f
                )

                // 2. DRAW RADAR SCANNING PULSE WAVE (DERIVED FROM PRIMARY THEME COLOR)
                val waveRadius = maxRadius * radarWaveProgress
                val waveAlpha = ((1f - radarWaveProgress) * 0.35f).coerceIn(0f, 0.35f)
                drawCircle(
                    color = graphColors.connections.radarPulse.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // 3. DRAW DEVICE NODES & CONNECTIONS
                if (devices.isEmpty()) {
                    val textPaint = android.graphics.Paint().apply {
                        color = graphColors.nodes.labelText.toArgb()
                        textSize = 13.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        alpha = (180 * pulseOpacity).toInt()
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        "Looking for nearby Mesh Link devices...",
                        center.x,
                        center.y + hubRadiusPx + 44.dp.toPx(),
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

                        val dimFactor = if (hasSelection && !isSelected) 0.35f else 1.0f
                        val currentAlpha = nodeState.alphaAnim.value * dimFactor

                        val isStrong = device.rssi > -70
                        val isWeak = device.rssi < -85

                        val lineColor = when {
                            device.isConnected -> graphColors.connections.connectedLine.copy(alpha = currentAlpha)
                            isRelay -> graphColors.connections.relayLine.copy(alpha = currentAlpha * 0.9f)
                            isWeak -> graphColors.connections.weakLine.copy(alpha = currentAlpha * 0.5f)
                            else -> graphColors.connections.discoveredLine.copy(alpha = currentAlpha * 0.7f)
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
                                color = graphColors.connections.beamSparkCore,
                                radius = 6f,
                                center = currentBeamPos
                            )
                            drawCircle(
                                color = graphColors.connections.beamSparkGlow,
                                radius = 12f,
                                center = currentBeamPos
                            )
                        }

                        // DISCOVERY RIPPLE ANIMATION
                        if (nodeState.rippleAnim.value in 0.01f..0.99f) {
                            val rippleRadius = baseNodeRadiusPx * (1f + nodeState.rippleAnim.value * 1.5f)
                            val rippleAlpha = (1f - nodeState.rippleAnim.value) * currentAlpha
                            drawCircle(
                                color = graphColors.nodes.discoveredGlow.copy(alpha = rippleAlpha),
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
                                color = graphColors.nodes.selectedGlow,
                                radius = nodeRadius + 12f,
                                center = nodePos
                            )
                            drawCircle(
                                color = graphColors.nodes.selectedOutline,
                                radius = nodeRadius + 4f,
                                center = nodePos,
                                style = Stroke(width = 2f)
                            )
                        }

                        // Node Colors based on node state
                        val (glowColor, surfaceColor, coreColor) = when {
                            device.isConnected -> Triple(graphColors.nodes.connectedGlow, graphColors.nodes.connectedSurface, graphColors.nodes.connectedCore)
                            isRelay -> Triple(graphColors.nodes.relayGlow, graphColors.nodes.relaySurface, graphColors.nodes.relayCore)
                            isWeak -> Triple(graphColors.nodes.weakGlow, graphColors.nodes.weakSurface, graphColors.nodes.weakCore)
                            else -> Triple(graphColors.nodes.discoveredGlow, graphColors.nodes.discoveredSurface, graphColors.nodes.discoveredCore)
                        }

                        // Node Glow Aura
                        drawCircle(
                            color = glowColor.copy(alpha = glowColor.alpha * currentAlpha),
                            radius = nodeRadius + 6f,
                            center = nodePos
                        )

                        // Node Body Surface
                        drawCircle(
                            color = surfaceColor.copy(alpha = currentAlpha),
                            radius = nodeRadius,
                            center = nodePos
                        )

                        // Node Inner Core State Circle
                        drawCircle(
                            color = coreColor.copy(alpha = currentAlpha),
                            radius = nodeRadius * 0.7f,
                            center = nodePos
                        )

                        // Relay Double Glowing Ring
                        if (isRelay) {
                            drawCircle(
                                color = graphColors.nodes.relayCore.copy(alpha = 0.8f * currentAlpha),
                                radius = nodeRadius + 4f,
                                center = nodePos,
                                style = Stroke(width = 1.5f)
                            )
                        }

                        // Small Connection Status Indicator Dot
                        val statusDotColor = if (device.isConnected) graphColors.nodes.connectedCore else graphColors.nodes.discoveredCore
                        drawCircle(
                            color = graphColors.background,
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
                            color = graphColors.nodes.labelText.toArgb()
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
                        color = graphColors.connections.beamSparkCore,
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

                // Outer Soft Primary Glow Aura
                drawCircle(
                    color = graphColors.nodes.hubGlow,
                    radius = hubBreathingRadius + 14f,
                    center = center
                )

                // Theme Surface Base Core (primaryContainer)
                drawCircle(
                    color = graphColors.nodes.hubSurface,
                    radius = hubBreathingRadius + 3f,
                    center = center
                )

                // Primary Color Outer Ring
                drawCircle(
                    color = graphColors.nodes.hubOutline,
                    radius = hubBreathingRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // Theme Core Dot (onPrimaryContainer)
                drawCircle(
                    color = graphColors.nodes.hubCoreDot,
                    radius = hubBreathingRadius * 0.5f,
                    center = center
                )
            }
        }
    }
}
