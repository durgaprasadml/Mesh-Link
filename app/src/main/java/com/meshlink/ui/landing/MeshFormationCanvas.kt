package com.meshlink.ui.landing

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Procedural GPU-optimized canvas for the minimal 6-node Mesh Link Wireless Discovery landing animation.
 *
 * Renders:
 *   1. Pure dark charcoal background with a subtle radial vignette centered on Node 0
 *   2. 10 growing light beams with neon green aura traveling from source to target
 *   3. 6 star-like glowing nodes with faint idle state (15% opacity) -> bright wake state (100%),
 *      with independent star twinkling and unified breathing pulse
 *   4. Non-linear cinematic center-node (Node 0) zoom-in transition expanding to fill the viewport
 *      as outer nodes dissolve cleanly into the Home Screen
 */
@Composable
fun MeshFormationCanvas(
    overallProgress: Float,
    timeMs: Long,
    isWelcomeMode: Boolean,
    modifier: Modifier = Modifier
) {
    // 6 Logo Nodes
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
            // Stage 2: Center (0) -> Outer 1 (1)
            MeshLogoBeam(id = 0, fromNodeId = 0, toNodeId = 1, discoveryStage = 2),

            // Stage 3: Center (0) -> Outer 2 (2), and Outer 1 (1) -> Outer 2 (2)
            MeshLogoBeam(id = 1, fromNodeId = 0, toNodeId = 2, discoveryStage = 3),
            MeshLogoBeam(id = 2, fromNodeId = 1, toNodeId = 2, discoveryStage = 3),

            // Stage 4: Center (0) -> Outer 3 (3), and Outer 2 (2) -> Outer 3 (3)
            MeshLogoBeam(id = 3, fromNodeId = 0, toNodeId = 3, discoveryStage = 4),
            MeshLogoBeam(id = 4, fromNodeId = 2, toNodeId = 3, discoveryStage = 4),

            // Stage 5: Center (0) -> Outer 4 (4), and Outer 3 (3) -> Outer 4 (4)
            MeshLogoBeam(id = 5, fromNodeId = 0, toNodeId = 4, discoveryStage = 5),
            MeshLogoBeam(id = 6, fromNodeId = 3, toNodeId = 4, discoveryStage = 5),

            // Stage 6: Center (0) -> Outer 5 (5), Outer 4 (4) -> Outer 5 (5), and Outer 5 (5) -> Outer 1 (1)
            MeshLogoBeam(id = 7, fromNodeId = 0, toNodeId = 5, discoveryStage = 6),
            MeshLogoBeam(id = 8, fromNodeId = 4, toNodeId = 5, discoveryStage = 6),
            MeshLogoBeam(id = 9, fromNodeId = 5, toNodeId = 1, discoveryStage = 6)
        )
    }

    // Reuse pre-allocated position buffers to prevent GC allocations in draw loop
    val nodePositions = remember { Array(6) { FloatArray(2) } }
    val tempPos = remember { FloatArray(2) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val cx = w * 0.5f
        val cy = h * 0.5f
        val logoRadius = min(w, h) * 0.24f
        val timeSec = timeMs / 1000f

        // Compute screen coordinates for all 6 nodes
        for (i in 0 until 6) {
            logoNodes[i].computePosition(cx, cy, logoRadius, nodePositions[i])
        }

        // Calculate discovery progress fractions based on overallProgress (0.0f -> 1.0f)
        // normalized discovery window: 0.08f -> 0.62f (each stage is ~0.09f wide)
        val discoveryWindowStart = AnimationConstants.PROGRESS_DISCOVERY_START
        val discoveryWindowEnd = AnimationConstants.PROGRESS_DISCOVERY_END
        val discoveryNorm = ((overallProgress - discoveryWindowStart) / (discoveryWindowEnd - discoveryWindowStart)).coerceIn(0f, 1f)

        // Stage progress (1.0 -> 6.0)
        val currentStageProgress = discoveryNorm * 5f + 1.0f // 1.0 at start, 6.0 at end of discovery

        // Zoom transition factor (occurs during final progress phase 0.80f -> 1.0f)
        val zoomProgress = if (overallProgress > AnimationConstants.PROGRESS_HOLD_END) {
            val rawZoom = ((overallProgress - AnimationConstants.PROGRESS_HOLD_END) / (1.0f - AnimationConstants.PROGRESS_HOLD_END)).coerceIn(0f, 1f)
            // Cubic ease-in-out curve for cinematic camera acceleration
            if (rawZoom < 0.5f) 4f * rawZoom * rawZoom * rawZoom
            else 1f - Math.pow(-2.0 * rawZoom + 2.0, 3.0).toFloat() / 2f
        } else 0f

        // Center node zoom scale expands exponentially from 1.0x to ~40.0x
        val cameraZoomScale = 1.0f + zoomProgress * zoomProgress * 38.0f
        // Outer elements fade out cleanly as camera approaches center node
        val outerElementsAlpha = (1.0f - zoomProgress * 1.4f).coerceIn(0f, 1f)

        // Unified breathing pulse amplitude during logo hold (0.80f hold phase)
        val unifiedPulse = if (overallProgress in discoveryWindowEnd..AnimationConstants.PROGRESS_HOLD_END) {
            val holdNorm = (overallProgress - discoveryWindowEnd) / (AnimationConstants.PROGRESS_HOLD_END - discoveryWindowEnd)
            (sin(holdNorm * Math.PI * 2.0).toFloat() * 0.15f).coerceIn(-0.1f, 0.2f)
        } else 0f

        // ── Pass 1: Background & Subtle Radial Vignette ──────────────────────
        drawBackgroundVignette(w, h, cx, cy, logoRadius, zoomProgress)

        // ── Pass 2 & 3: Canvas Scale Transformation around Center Node ──────
        scale(
            scaleX = cameraZoomScale,
            scaleY = cameraZoomScale,
            pivot = Offset(cx, cy)
        ) {
            // Draw 10 Light Beams
            if (outerElementsAlpha > 0f) {
                drawBeams(
                    beams = logoBeams,
                    nodePositions = nodePositions,
                    currentStageProgress = currentStageProgress,
                    alphaMult = outerElementsAlpha,
                    unifiedPulse = unifiedPulse
                )
            }

            // Draw 6 Glowing Star Nodes
            drawNodes(
                nodes = logoNodes,
                nodePositions = nodePositions,
                currentStageProgress = currentStageProgress,
                timeSec = timeSec,
                outerAlphaMult = outerElementsAlpha,
                zoomProgress = zoomProgress,
                unifiedPulse = unifiedPulse
            )
        }
    }
}

private fun DrawScope.drawBackgroundVignette(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    logoRadius: Float,
    zoomProgress: Float
) {
    // Pure dark charcoal background (#0B0B0B)
    drawRect(color = AnimationConstants.DeepCharcoalBg)

    // Subtle neon green radial vignette centered on Node 0
    val vignetteRadius = w.coerceAtLeast(h) * (0.65f + zoomProgress * 0.5f)
    val vignetteAlpha = (0.25f + zoomProgress * 0.75f).coerceIn(0f, 1f)

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AnimationConstants.RadialVignetteCenter.copy(alpha = vignetteAlpha),
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
    currentStageProgress: Float,
    alphaMult: Float,
    unifiedPulse: Float
) {
    val coreWidth = AnimationConstants.BEAM_CORE_WIDTH_DP.dp.toPx()
    val glowWidth = AnimationConstants.BEAM_GLOW_WIDTH_DP.dp.toPx()

    beams.forEach { beam ->
        val stageThreshold = beam.discoveryStage.toFloat()
        if (currentStageProgress < stageThreshold - 1.0f) return@forEach

        // Beam growth fraction (0.0f -> 1.0f)
        val growthFraction = ((currentStageProgress - (stageThreshold - 1.0f)) / 0.8f).coerceIn(0f, 1f)
        if (growthFraction <= 0f) return@forEach

        val fromPos = nodePositions[beam.fromNodeId]
        val toPos = nodePositions[beam.toNodeId]

        val startX = fromPos[0]
        val startY = fromPos[1]
        val currentEndX = startX + (toPos[0] - startX) * growthFraction
        val currentEndY = startY + (toPos[1] - startY) * growthFraction

        val baseAlpha = (0.75f + unifiedPulse) * alphaMult

        // 1. Soft Outer Glow Aura
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
    }
}

private fun DrawScope.drawNodes(
    nodes: List<MeshLogoNode>,
    nodePositions: Array<FloatArray>,
    currentStageProgress: Float,
    timeSec: Float,
    outerAlphaMult: Float,
    zoomProgress: Float,
    unifiedPulse: Float
) {
    nodes.forEach { node ->
        val pos = nodePositions[node.id]
        val center = Offset(pos[0], pos[1])

        // Stage discovery progress
        val isAwakened = currentStageProgress >= node.discoveryStage.toFloat()
        val wakeFraction = if (isAwakened) {
            ((currentStageProgress - node.discoveryStage.toFloat() + 1.0f) / 0.5f).coerceIn(0f, 1f)
        } else 0f

        // Star twinkling offset
        val twinkle = (sin((timeSec * node.twinkleSpeed + node.twinklePhase).toDouble()).toFloat() * 0.18f)

        // Base opacity: starts at faint ~15% (`FaintStarColor`), ramps to 100% on discovery
        val nodeOpacity = if (isAwakened) {
            (0.82f + twinkle + wakeFraction * 0.18f + unifiedPulse).coerceIn(0.2f, 1.0f)
        } else {
            (0.15f + twinkle * 0.3f).coerceIn(0.08f, 0.22f)
        }

        val alpha = if (node.isCenter) {
            // Center node brightens and stays visible during zoom transition
            (nodeOpacity + zoomProgress * 0.5f).coerceIn(0f, 1f)
        } else {
            (nodeOpacity * outerAlphaMult).coerceIn(0f, 1f)
        }

        if (alpha <= 0.01f) return@forEach

        val baseRadiusDp = if (node.isCenter) AnimationConstants.CENTER_NODE_RADIUS_DP else AnimationConstants.NODE_RADIUS_DP
        val radius = baseRadiusDp.dp.toPx() * (1.0f + unifiedPulse * 0.5f + (if (node.isCenter) zoomProgress * 0.6f else 0f))
        val glowRadius = AnimationConstants.NODE_GLOW_RADIUS_DP.dp.toPx() * (1.0f + zoomProgress * 1.2f)

        // Layer 1: Soft Outer Glow
        drawCircle(
            color = AnimationConstants.SoftNeonGreenGlowOuter.copy(alpha = alpha * 0.35f),
            radius = glowRadius * 1.5f,
            center = center
        )
        drawCircle(
            color = AnimationConstants.SoftNeonGreenGlowInner.copy(alpha = alpha * 0.60f),
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

