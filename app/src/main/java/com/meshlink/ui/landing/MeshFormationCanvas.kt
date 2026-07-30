package com.meshlink.ui.landing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.sin

/**
 * Procedural GPU-optimized canvas for the refined Mesh Link Wireless Discovery landing animation.
 *
 * Implements:
 *   1. 6 visually identical nodes (Node 0 center matches outer nodes in radius, glow & core)
 *   2. Accelerating rhythm (180ms -> 140ms per stage) with heavy overlapping node awakenings
 *   3. Traveling pulse of light along connections as lines complete
 *   4. Depth-simulated camera zoom: outer nodes drift outward while center stays locked
 *   5. Subtle +15% glow bloom right before zoom ignition
 *   6. Layered staggered dissolve: Background -> Outer Nodes -> Beams -> Center Node Glow
 */
@Composable
fun MeshFormationCanvas(
    timeMs: Long,
    isWelcomeMode: Boolean,
    modifier: Modifier = Modifier
) {
    // 6 Visually Identical Logo Nodes
    val logoNodes = remember {
        listOf(
            MeshLogoNode(id = 0, isCenter = true, discoveryStage = 1),
            MeshLogoNode(id = 1, isCenter = false, discoveryStage = 2, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[0]),
            MeshLogoNode(id = 2, isCenter = false, discoveryStage = 3, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[1]),
            MeshLogoNode(id = 3, isCenter = false, discoveryStage = 4, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[2]),
            MeshLogoNode(id = 4, isCenter = false, discoveryStage = 5, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[3]),
            MeshLogoNode(id = 5, isCenter = false, discoveryStage = 6, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[4])
        )
    }

    // 10 Light Beams (Radial + Outer Ring)
    val logoBeams = remember {
        listOf(
            MeshLogoBeam(id = 0, fromNodeId = 0, toNodeId = 1, discoveryStage = 2),
            MeshLogoBeam(id = 1, fromNodeId = 0, toNodeId = 2, discoveryStage = 3),
            MeshLogoBeam(id = 2, fromNodeId = 1, toNodeId = 2, discoveryStage = 3),
            MeshLogoBeam(id = 3, fromNodeId = 0, toNodeId = 3, discoveryStage = 4),
            MeshLogoBeam(id = 4, fromNodeId = 2, toNodeId = 3, discoveryStage = 4),
            MeshLogoBeam(id = 5, fromNodeId = 0, toNodeId = 4, discoveryStage = 5),
            MeshLogoBeam(id = 6, fromNodeId = 3, toNodeId = 4, discoveryStage = 5),
            MeshLogoBeam(id = 7, fromNodeId = 0, toNodeId = 5, discoveryStage = 6),
            MeshLogoBeam(id = 8, fromNodeId = 4, toNodeId = 5, discoveryStage = 6),
            MeshLogoBeam(id = 9, fromNodeId = 5, toNodeId = 1, discoveryStage = 6)
        )
    }

    // Stage time boundaries in ms
    val stageStartTimesMs = remember {
        val t = LongArray(7)
        t[0] = AnimationConstants.START_PAUSE_MS // 100ms
        var acc = t[0]
        for (i in 0 until 6) {
            acc += AnimationConstants.NODE_STAGE_DURATIONS_MS[i]
            t[i + 1] = acc
        }
        t
    }

    val discoveryEndTimeMs = stageStartTimesMs[6] // 1040ms
    val holdEndTimeMs = discoveryEndTimeMs + AnimationConstants.LOGO_HOLD_MS + (if (isWelcomeMode) AnimationConstants.WELCOME_TEXT_HOLD_MS else 0L)

    // Camera Zoom Progress (0.0f -> 1.0f)
    val zoomProgress = if (timeMs > holdEndTimeMs) {
        val rawZoom = ((timeMs - holdEndTimeMs).toFloat() / AnimationConstants.CENTER_ZOOM_DURATION_MS.toFloat()).coerceIn(0f, 1f)
        FastOutSlowInEasing.transform(rawZoom)
    } else 0f

    // Layered Alpha Dissolve Schedule
    // 1. Background vignette fades first (0.0 -> 0.35 zoom)
    val bgAlpha = (1.0f - (zoomProgress / 0.35f)).coerceIn(0f, 1f)
    // 2. Outer nodes fade next (0.15 -> 0.55 zoom)
    val outerNodesAlpha = (1.0f - ((zoomProgress - 0.15f) / 0.40f)).coerceIn(0f, 1f)
    // 3. Beams fade (0.25 -> 0.65 zoom)
    val beamsAlpha = (1.0f - ((zoomProgress - 0.25f) / 0.40f)).coerceIn(0f, 1f)
    // 4. Center node glow remains visible longest (0.65 -> 1.00 zoom)
    val centerNodeAlpha = (1.0f - ((zoomProgress - 0.65f) / 0.35f)).coerceIn(0f, 1f)

    // Pre-allocated position buffers for zero GC allocations per frame
    val nodePositions = remember { Array(6) { FloatArray(2) } }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val cx = w * 0.5f
        val cy = h * 0.5f
        val logoRadius = min(w, h) * 0.24f
        val timeSec = timeMs / 1000f

        // Camera Depth Effect: Outer nodes drift radially outward during zoom while center stays locked
        val outerDriftScale = 1.0f + zoomProgress * zoomProgress * 4.2f

        for (i in 0 until 6) {
            val node = logoNodes[i]
            if (node.isCenter) {
                nodePositions[i][0] = cx
                nodePositions[i][1] = cy
            } else {
                val currentRadius = logoRadius * outerDriftScale
                node.computePosition(cx, cy, currentRadius, nodePositions[i])
            }
        }

        // Unified breathing pulse amplitude during hold phase
        val holdNorm = if (timeMs in discoveryEndTimeMs..holdEndTimeMs) {
            ((timeMs - discoveryEndTimeMs).toFloat() / (holdEndTimeMs - discoveryEndTimeMs).toFloat()).coerceIn(0f, 1f)
        } else 0f
        val unifiedPulse = (sin(holdNorm * Math.PI * 2.0).toFloat() * 0.12f).coerceIn(-0.1f, 0.15f)

        // Tiny Glow Bloom (+15% brightness) right before zoom ignition
        val bloomNorm = if (holdNorm > 0.7f && zoomProgress < 0.3f) {
            val raw = if (zoomProgress > 0f) 1.0f - (zoomProgress / 0.3f) else (holdNorm - 0.7f) / 0.3f
            sin(raw * Math.PI * 0.5).toFloat()
        } else 0f
        val centerGlowBloom = 1.0f + bloomNorm * 0.15f

        // ── Pass 1: Background & Vignette ────────────────────────────────────
        drawBackgroundVignette(w, h, cx, cy, bgAlpha)

        // ── Pass 2: Connection Beams with Traveling Pulse ────────────────────
        if (beamsAlpha > 0f) {
            drawBeams(
                beams = logoBeams,
                nodePositions = nodePositions,
                timeMs = timeMs,
                stageStartTimesMs = stageStartTimesMs,
                beamsAlpha = beamsAlpha,
                unifiedPulse = unifiedPulse
            )
        }

        // ── Pass 3: 6 Visually Identical Glowing Star Nodes ──────────────────
        drawNodes(
            nodes = logoNodes,
            nodePositions = nodePositions,
            timeMs = timeMs,
            stageStartTimesMs = stageStartTimesMs,
            timeSec = timeSec,
            outerNodesAlpha = outerNodesAlpha,
            centerNodeAlpha = centerNodeAlpha,
            unifiedPulse = unifiedPulse,
            centerGlowBloom = centerGlowBloom,
            zoomProgress = zoomProgress
        )
    }
}

private fun DrawScope.drawBackgroundVignette(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    bgAlpha: Float
) {
    drawRect(color = AnimationConstants.DeepCharcoalBg)

    if (bgAlpha <= 0f) return

    val vignetteRadius = w.coerceAtLeast(h) * 0.65f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AnimationConstants.RadialVignetteCenter.copy(alpha = 0.30f * bgAlpha),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = vignetteRadius
        )
    )
}

private fun DrawScope.drawBeams(
    beams: List<MeshLogoBeam>,
    nodePositions: Array<FloatArray>,
    timeMs: Long,
    stageStartTimesMs: LongArray,
    beamsAlpha: Float,
    unifiedPulse: Float
) {
    val coreWidth = AnimationConstants.BEAM_CORE_WIDTH_DP.dp.toPx()
    val glowWidth = AnimationConstants.BEAM_GLOW_WIDTH_DP.dp.toPx()

    beams.forEach { beam ->
        val stageIndex = beam.discoveryStage // Stage 2..6
        val startTime = stageStartTimesMs[stageIndex - 1]
        val endTime = stageStartTimesMs[stageIndex]

        if (timeMs < startTime) return@forEach

        // Beam Growth Fraction (0.0f -> 1.0f) using LinearOutSlowInEasing
        val rawFraction = ((timeMs - startTime).toFloat() / (endTime - startTime).toFloat()).coerceIn(0f, 1f)
        val growthFraction = LinearOutSlowInEasing.transform(rawFraction)
        if (growthFraction <= 0f) return@forEach

        val fromPos = nodePositions[beam.fromNodeId]
        val toPos = nodePositions[beam.toNodeId]

        val startX = fromPos[0]
        val startY = fromPos[1]
        val currentEndX = startX + (toPos[0] - startX) * growthFraction
        val currentEndY = startY + (toPos[1] - startY) * growthFraction

        val baseAlpha = (0.75f + unifiedPulse) * beamsAlpha

        // 1. Soft Outer Glow Aura Line
        drawLine(
            color = AnimationConstants.BeamGlowAura.copy(alpha = baseAlpha * 0.45f),
            start = Offset(startX, startY),
            end = Offset(currentEndX, currentEndY),
            strokeWidth = glowWidth,
            cap = StrokeCap.Round
        )

        // 2. Crisp Core Light Line
        drawLine(
            color = AnimationConstants.SoftNeonGreen.copy(alpha = baseAlpha),
            start = Offset(startX, startY),
            end = Offset(currentEndX, currentEndY),
            strokeWidth = coreWidth,
            cap = StrokeCap.Round
        )

        // 3. Traveling Light Pulse along connection (Node ──► Tip spark)
        if (growthFraction in 0.05f..0.98f) {
            val sparkAlpha = (baseAlpha * (1.0f - growthFraction * 0.3f)).coerceIn(0f, 1f)
            val sparkCenter = Offset(currentEndX, currentEndY)

            drawCircle(
                color = AnimationConstants.SoftNeonGreenBright.copy(alpha = sparkAlpha * 0.8f),
                radius = 5.5.dp.toPx(),
                center = sparkCenter
            )
            drawCircle(
                color = AnimationConstants.StarCoreWhite.copy(alpha = sparkAlpha),
                radius = 2.5.dp.toPx(),
                center = sparkCenter
            )
        }
    }
}

private fun DrawScope.drawNodes(
    nodes: List<MeshLogoNode>,
    nodePositions: Array<FloatArray>,
    timeMs: Long,
    stageStartTimesMs: LongArray,
    timeSec: Float,
    outerNodesAlpha: Float,
    centerNodeAlpha: Float,
    unifiedPulse: Float,
    centerGlowBloom: Float,
    zoomProgress: Float
) {
    nodes.forEach { node ->
        val pos = nodePositions[node.id]
        val center = Offset(pos[0], pos[1])

        val stageIndex = node.discoveryStage // 1..6
        val startTime = stageStartTimesMs[stageIndex - 1]
        val endTime = stageStartTimesMs[stageIndex]

        // Heavy Overlap Awakening:
        // Center node (Stage 1) awakens from t0 -> t1.
        // Outer nodes (Stage 2..6) begin awakening when previous beam is ~70% complete!
        val wakeStartTime = if (node.isCenter) startTime else (startTime - (endTime - startTime) * 0.30f).toLong()
        val isAwakened = timeMs >= wakeStartTime

        val wakeFraction = if (isAwakened) {
            val raw = ((timeMs - wakeStartTime).toFloat() / (endTime - wakeStartTime).toFloat()).coerceIn(0f, 1f)
            FastOutSlowInEasing.transform(raw)
        } else 0f

        val twinkle = (sin((timeSec * node.twinkleSpeed + node.twinklePhase).toDouble()).toFloat() * 0.15f)

        // Base opacity: starts at faint ~15%, wakes smoothly to 100%
        val nodeOpacity = if (isAwakened) {
            (0.85f + twinkle + wakeFraction * 0.15f + unifiedPulse).coerceIn(0.2f, 1.0f)
        } else {
            (0.15f + twinkle * 0.3f).coerceIn(0.08f, 0.22f)
        }

        val alpha = if (node.isCenter) {
            (nodeOpacity * centerNodeAlpha).coerceIn(0f, 1f)
        } else {
            (nodeOpacity * outerNodesAlpha).coerceIn(0f, 1f)
        }

        if (alpha <= 0.005f) return@forEach

        // Center node uses exact same 7.0dp base radius as outer nodes
        val baseRadiusDp = AnimationConstants.NODE_RADIUS_DP
        val radius = baseRadiusDp.dp.toPx() * (1.0f + unifiedPulse * 0.5f)
        val glowRadius = AnimationConstants.NODE_GLOW_RADIUS_DP.dp.toPx() * (if (node.isCenter) centerGlowBloom + zoomProgress * 0.6f else 1.0f)

        val glowMultiplier = if (node.isCenter) centerGlowBloom else 1.0f

        // Layer 1: Soft Outer Glow Aura
        drawCircle(
            color = AnimationConstants.SoftNeonGreenGlowOuter.copy(alpha = (alpha * 0.35f * glowMultiplier).coerceIn(0f, 1f)),
            radius = glowRadius * 1.5f,
            center = center
        )
        drawCircle(
            color = AnimationConstants.SoftNeonGreenGlowInner.copy(alpha = (alpha * 0.60f * glowMultiplier).coerceIn(0f, 1f)),
            radius = glowRadius * 0.8f,
            center = center
        )

        // Layer 2: Neon Green Halo Rim
        drawCircle(
            color = AnimationConstants.SoftNeonGreenBright.copy(alpha = alpha),
            radius = radius,
            center = center
        )

        // Layer 3: Pure White Star Core
        drawCircle(
            color = AnimationConstants.StarCoreWhite.copy(alpha = alpha),
            radius = radius * 0.45f,
            center = center
        )
    }
}
