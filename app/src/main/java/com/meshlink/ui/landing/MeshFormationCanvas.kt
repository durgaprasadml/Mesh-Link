package com.meshlink.ui.landing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural GPU-optimized canvas for the Mesh Link Cinematic Landing — v4.
 *
 * Rendering passes (draw order):
 *   1. Deep space atmosphere (nebula haze, dual-layer drift)
 *   2. Volumetric light rays (3 slow crepuscular rays, alpha 0.02–0.04)
 *   3. Scene 1 silence vignette (lifts as progress rises)
 *   4. Background starfield (DoF bloom halos)
 *   5. Background micro-connections
 *   6. Ripple / echo waves
 *   7. Midground plasma connections (gradient trail + spark head)
 *   8. Energy routing packets (cyan, 3-segment fading trail)
 *   9. Midground nodes (3-layer glow, wake-gated, flash bloom for signature)
 *  10. Non-migrating universe stars (60% of text nodes, stay as stars after Scene 7)
 *  11. Foreground blur stars
 *  12. Radial signature wave ring
 *  13. Signature convergence full-screen radial flash
 *  14. Scene 10 home transition vignette (deepens around user node)
 */
@Composable
fun MeshFormationCanvas(
    nodes: List<AnimatedMeshNode>,
    connectionAnimator: MeshConnectionAnimator,
    dataPackets: List<DataPacket>,
    overallProgress: Float,
    timeMs: Long,
    isWelcomeMode: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val timeSec = timeMs / 1000f

        // Pass 1: Deep space dual-layer nebula atmosphere
        drawDeepSpaceAtmosphere(w, h, timeSec)

        // Pass 2: Volumetric light rays
        drawVolumetricRays(w, h, timeSec, overallProgress)

        // Pass 3: Scene 1 silence vignette
        if (overallProgress < AnimationConstants.SCENE_2_END) {
            drawSilenceVignette(overallProgress)
        }

        // Pass 4: Background starfield
        drawBackgroundStarfield(nodes)

        // Pass 5: Background micro-connections
        drawMicroConnections(connectionAnimator.microConnections, nodes)

        // Pass 6: Ripple & echo waves
        drawRippleWaves(connectionAnimator.rippleWave, connectionAnimator.echoWave, w, h)

        // Pass 7: Plasma connections (with progressive letter-order activation)
        drawPlasmaConnections(connectionAnimator.links, overallProgress)

        // Pass 8: Energy routing packets
        drawEnergyPackets(dataPackets, nodes)

        // Pass 9: Midground nodes (wake-gated + signature flash bloom)
        drawMidgroundNodes(nodes, isWelcomeMode)

        // Pass 10: Non-migrating text nodes rendered as persistent universe stars
        drawUniverseStars(nodes, overallProgress)

        // Pass 11: Foreground blur stars
        drawForegroundStars(nodes)

        // Pass 12: Radial signature wave ring
        if (connectionAnimator.signatureWave.isActive) {
            drawRadialSignatureWave(connectionAnimator.signatureWave, w, h)
        }

        // Pass 13: Full-screen convergence flash
        if (connectionAnimator.isSignatureFlashActive) {
            drawSignatureConvergenceFlash(w, h, connectionAnimator.signatureFlashIntensity)
        }

        // Pass 14: Scene 10 home transition vignette
        if (overallProgress > AnimationConstants.SCENE_9_END) {
            drawHomeTransitionVignette(nodes, w, h, overallProgress)
        }
    }
}

// ── Pass 1: Deep space dual-layer atmosphere ──────────────────────────────────

private fun DrawScope.drawDeepSpaceAtmosphere(width: Float, height: Float, timeSec: Float) {
    val cx = width  * (0.50f + sin(timeSec * 0.095f) * 0.022f)
    val cy = height * (0.46f + cos(timeSec * 0.078f) * 0.018f)

    // Primary deep space radial gradient
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AnimationConstants.SpaceDarkCharcoal,
                AnimationConstants.DeepSpaceNavy,
                Color(0xFF010204)
            ),
            center = Offset(cx, cy),
            radius = width.coerceAtLeast(height) * 1.05f
        )
    )

    // Layer 1: cold blue aurora haze drifting
    val hazeOff1 = sin(timeSec * 0.14f) * width * 0.10f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                AnimationConstants.SubtleStarlightBlue.copy(alpha = 0.022f),
                AnimationConstants.StarlightSilver.copy(alpha = 0.010f),
                Color.Transparent
            ),
            start = Offset(hazeOff1, 0f),
            end   = Offset(width * 0.85f + hazeOff1, height)
        )
    )

    // Layer 2: warm purple nebula haze (different drift axis — v4)
    val hazeOff2 = cos(timeSec * 0.11f + 1.2f) * height * 0.08f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                AnimationConstants.NebulaPurple,
                Color.Transparent
            ),
            start = Offset(0f, hazeOff2),
            end   = Offset(width, height * 0.70f + hazeOff2)
        )
    )

    // Layer 3: cosmic dust — very faint scattered cold blue
    val dustX = cos(timeSec * 0.07f + 0.5f) * width * 0.12f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AnimationConstants.CosmicDustAmber,
                Color.Transparent
            ),
            center = Offset(width * 0.30f + dustX, height * 0.65f),
            radius = width * 0.55f
        )
    )
}

// ── Pass 2: Volumetric light rays (crepuscular) ───────────────────────────────

/**
 * 3 faint diverging rays from center-top, alpha 0.02–0.04, animated with slow sine.
 * Invisible during Scene 1 (fade in from 0 after SCENE_1_END).
 */
private fun DrawScope.drawVolumetricRays(
    width: Float,
    height: Float,
    timeSec: Float,
    progress: Float
) {
    val rayAlpha = ((progress - AnimationConstants.SCENE_1_END) / 0.12f).coerceIn(0f, 1f) * 0.04f
    if (rayAlpha < 0.002f) return

    val originX = width * 0.50f
    val originY = -height * 0.12f   // slightly off-screen top

    val rayAngles = floatArrayOf(
        (PI * 0.35f).toFloat() + sin(timeSec * 0.22f) * 0.04f,
        (PI * 0.50f).toFloat() + sin(timeSec * 0.18f + 0.8f) * 0.04f,
        (PI * 0.65f).toFloat() + sin(timeSec * 0.15f + 1.6f) * 0.04f
    )

    rayAngles.forEach { angle ->
        val endX = originX + cos(angle) * width * 1.6f
        val endY = originY + sin(angle) * height * 1.6f
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    AnimationConstants.VolumetricRayWhite.copy(alpha = rayAlpha),
                    Color.Transparent
                ),
                start = Offset(originX, originY),
                end   = Offset(endX, endY)
            ),
            start       = Offset(originX, originY),
            end         = Offset(endX, endY),
            strokeWidth = 28f.dp.toPx(),
            cap         = StrokeCap.Round
        )
    }
}

// ── Pass 3: Scene 1 silence vignette ──────────────────────────────────────────

private fun DrawScope.drawSilenceVignette(progress: Float) {
    val fadeEnd  = AnimationConstants.SCENE_2_END
    val vigAlpha = (1f - (progress / fadeEnd)).coerceIn(0f, 1f) * 0.92f
    drawRect(color = Color(0xFF01030A).copy(alpha = vigAlpha))
}

// ── Pass 4: Background starfield ──────────────────────────────────────────────

private fun DrawScope.drawBackgroundStarfield(nodes: List<AnimatedMeshNode>) {
    nodes.forEach { star ->
        if (star.depthLayer != DepthLayer.BACKGROUND) return@forEach
        val wake = star.wakeProgress
        if (wake <= 0.01f) return@forEach

        val center    = Offset(star.currentX, star.currentY)
        val brightness = star.currentBrightness
        val alpha     = (brightness * 0.52f * wake).coerceIn(0.04f, 0.40f)
        val radius    = star.radiusDp.dp.toPx()

        if (star.dofBlurFactor > 0.15f) {
            drawCircle(
                color  = AnimationConstants.StarGreyDimTransparent.copy(alpha = alpha * 0.35f * wake),
                radius = radius * (1.5f + star.dofBlurFactor * 2.0f),
                center = center
            )
        }
        drawCircle(
            color  = AnimationConstants.StarlightSilver.copy(alpha = alpha),
            radius = radius,
            center = center
        )
    }
}

// ── Pass 5: Micro-connections ──────────────────────────────────────────────────

private fun DrawScope.drawMicroConnections(
    microConnections: List<MicroConnection>,
    nodes: List<AnimatedMeshNode>
) {
    microConnections.forEach { micro ->
        if (!micro.isActive || micro.currentAlpha <= 0.01f) return@forEach
        val nA = nodes.getOrNull(micro.nodeAId) ?: return@forEach
        val nB = nodes.getOrNull(micro.nodeBId) ?: return@forEach

        drawLine(
            color       = AnimationConstants.StarlightSilver.copy(alpha = micro.currentAlpha + micro.flashBrightness * 0.22f),
            start       = Offset(nA.currentX, nA.currentY),
            end         = Offset(nB.currentX, nB.currentY),
            strokeWidth = 0.6f.dp.toPx(),
            cap         = StrokeCap.Round
        )
    }
}

// ── Pass 6: Ripple / echo waves ───────────────────────────────────────────────

private fun DrawScope.drawRippleWaves(
    ripple: ConstellationRippleWave,
    echo: ConstellationRippleWave,
    width: Float,
    height: Float
) {
    listOf(ripple, echo).forEach { wave ->
        if (!wave.isActive || wave.alpha <= 0.01f) return@forEach
        drawCircle(
            color  = AnimationConstants.StarlightWhite.copy(alpha = wave.alpha * 0.28f),
            radius = width.coerceAtLeast(height) * wave.radius,
            center = Offset(width * wave.centerX, height * wave.centerY),
            style  = Stroke(width = (if (wave.isEcho) 0.9f else 1.6f).dp.toPx())
        )
    }
}

// ── Pass 7: Plasma connections ────────────────────────────────────────────────

/**
 * Progressive letter-order activation for constellation strokes:
 * each letter group's edges appear in sequence during migration progress.
 * Non-constellation links appear normally based on appearanceDelay.
 */
private fun DrawScope.drawPlasmaConnections(
    links: List<MeshConnectionLink>,
    overallProgress: Float
) {
    links.forEach { link ->
        if (link.alpha <= 0.015f || link.growthProgress <= 0.01f) return@forEach

        val visibility = (link.nodeA.wakeProgress * link.nodeB.wakeProgress).coerceIn(0f, 1f)
        if (visibility < 0.02f) return@forEach

        // Progressive title: constellation strokes fade in letter-by-letter
        val letterAlphaScale: Float = if (link.isConstellationStroke) {
            val migrationProgress = ((overallProgress - AnimationConstants.SCENE_6_END) /
                    (AnimationConstants.SCENE_7_END - AnimationConstants.SCENE_6_END)).coerceIn(0f, 1f)
            val letterThreshold = link.letterIndex.toFloat() / 16f
            if (migrationProgress < letterThreshold) 0f
            else ((migrationProgress - letterThreshold) / 0.065f).coerceIn(0f, 1f)
        } else 1f

        val start   = Offset(link.nodeA.currentX, link.nodeA.currentY)
        val fullEnd = Offset(link.nodeB.currentX, link.nodeB.currentY)
        val currentEnd = Offset(
            start.x + (fullEnd.x - start.x) * link.growthProgress,
            start.y + (fullEnd.y - start.y) * link.growthProgress
        )

        val baseColor = when {
            link.isConstellationStroke -> AnimationConstants.ConnectionConstellation
            link.isMultiHopRelay       -> AnimationConstants.MultiHopRelayColor
            else                       -> AnimationConstants.StarlightSilver
        }

        val energyBoost = link.energyPulseBrightness
        val lineAlpha   = ((link.alpha + energyBoost * 0.40f) * visibility * letterAlphaScale)
            .coerceIn(0f, 1f)

        val strokeWidth = when {
            link.isConstellationStroke -> 1.8f.dp.toPx()
            link.isMultiHopRelay       -> 0.80f.dp.toPx()
            else                       -> 1.1f.dp.toPx()
        }

        // Spark particle at growth head
        if (link.plasmaStage == PlasmaGrowthStage.SPARK || link.plasmaStage == PlasmaGrowthStage.STREAK) {
            drawCircle(
                color  = AnimationConstants.StarlightWhite.copy(alpha = link.growthProgress * visibility * 0.9f),
                radius = strokeWidth * 2.0f,
                center = currentEnd
            )
        }

        // Outer glow
        drawLine(
            color       = baseColor.copy(alpha = lineAlpha * 0.25f),
            start       = start,
            end         = currentEnd,
            strokeWidth = strokeWidth * 2.6f,
            cap         = StrokeCap.Round
        )

        // Core
        drawLine(
            color       = baseColor.copy(alpha = lineAlpha),
            start       = start,
            end         = currentEnd,
            strokeWidth = strokeWidth,
            cap         = StrokeCap.Round
        )
    }
}

// ── Pass 8: Energy routing packets ───────────────────────────────────────────

private fun DrawScope.drawEnergyPackets(
    packets: List<DataPacket>,
    nodes: List<AnimatedMeshNode>
) {
    packets.forEach { packet ->
        if (!packet.isActive || packet.fadeAlpha <= 0.02f) return@forEach

        val fromNode = nodes.getOrNull(packet.fromNodeId) ?: return@forEach
        val toNode   = nodes.getOrNull(packet.toNodeId)   ?: return@forEach
        if (!fromNode.isDiscovered || !toNode.isDiscovered) return@forEach

        val px = fromNode.currentX + (toNode.currentX - fromNode.currentX) * packet.progress
        val py = fromNode.currentY + (toNode.currentY - fromNode.currentY) * packet.progress
        packet.currentX = px
        packet.currentY = py

        val fa = packet.fadeAlpha

        // 3-segment trailing tail
        for (seg in 1..3) {
            val trailT  = (packet.progress - seg * 0.065f).coerceAtLeast(0f)
            val trailX  = fromNode.currentX + (toNode.currentX - fromNode.currentX) * trailT
            val trailY  = fromNode.currentY + (toNode.currentY - fromNode.currentY) * trailT
            val prevT   = (packet.progress - (seg - 1) * 0.065f).coerceAtLeast(0f)
            val prevX   = fromNode.currentX + (toNode.currentX - fromNode.currentX) * prevT
            val prevY   = fromNode.currentY + (toNode.currentY - fromNode.currentY) * prevT
            val segAlpha = (0.42f / seg) * fa
            if (segAlpha > 0.01f) {
                drawLine(
                    color       = AnimationConstants.PacketCyan.copy(alpha = segAlpha),
                    start       = Offset(trailX, trailY),
                    end         = Offset(prevX, prevY),
                    strokeWidth = packet.sizeDp.dp.toPx() * 1.05f,
                    cap         = StrokeCap.Round
                )
            }
        }

        // Core packet glow
        drawCircle(
            color  = AnimationConstants.PacketCyan.copy(alpha = fa),
            radius = packet.sizeDp.dp.toPx(),
            center = Offset(px, py)
        )
        drawCircle(
            color  = AnimationConstants.PacketCyanGlow.copy(alpha = fa * 0.75f),
            radius = packet.sizeDp.dp.toPx() * 2.8f,
            center = Offset(px, py)
        )
    }
}

// ── Pass 9: Midground nodes ────────────────────────────────────────────────────

private fun DrawScope.drawMidgroundNodes(
    nodes: List<AnimatedMeshNode>,
    isWelcomeMode: Boolean
) {
    nodes.forEach { star ->
        if (star.depthLayer != DepthLayer.MIDGROUND) return@forEach
        val wake = star.wakeProgress
        if (wake <= 0.01f) return@forEach

        val center     = Offset(star.currentX, star.currentY)
        val brightness = (star.currentBrightness + star.pulseIntensity).coerceIn(0.05f, 1.00f)
        val rMult      = if (star.isRelayHub) AnimationConstants.RELAY_HUB_RADIUS_FACTOR else 1.0f
        val radius     = star.radiusDp.dp.toPx() * (0.88f + brightness * 0.24f) * rMult

        // Welcome mode: user avatar handled by WelcomeAnimation overlay
        if (star.isUserNode && isWelcomeMode) {
            drawCircle(
                color  = AnimationConstants.StarlightWhiteGlow.copy(alpha = 0.32f * wake),
                radius = radius * 2.0f,
                center = center
            )
            return@forEach
        }

        val starAlpha = if (star.isDiscovered) {
            (brightness * wake).coerceIn(0.35f, 1.00f)
        } else {
            (brightness * wake * 0.40f).coerceIn(0.04f, 0.48f)
        }

        // Layer 1: outer aurora (very faint, only for hubs and discovered nodes)
        if (star.isRelayHub || star.isDiscovered) {
            drawCircle(
                color  = AnimationConstants.StarlightSilverGlow.copy(
                    alpha = (if (star.isRelayHub) 0.11f else 0.06f) * starAlpha
                ),
                radius = radius * 4.0f,
                center = center
            )
            drawCircle(
                color  = AnimationConstants.StarlightSilverGlow.copy(
                    alpha = (if (star.isRelayHub) 0.35f else 0.20f) * starAlpha
                ),
                radius = radius * 2.2f,
                center = center
            )
        }

        // v4: Signature wave flash bloom (4th layer — brief bright spike)
        val flash = star.signatureWaveFlash
        if (flash > 0.02f) {
            drawCircle(
                color  = AnimationConstants.StarlightWhite.copy(alpha = flash * 0.70f),
                radius = radius * (1.5f + flash * 3.0f),
                center = center
            )
            drawCircle(
                color  = AnimationConstants.StarlightWhiteGlow.copy(alpha = flash * 0.35f),
                radius = radius * (2.5f + flash * 5.0f),
                center = center
            )
        }

        // Layer 3: solid core
        drawCircle(
            color  = AnimationConstants.StarlightWhite.copy(alpha = starAlpha),
            radius = radius,
            center = center
        )
    }
}

// ── Pass 10: Non-migrating universe stars (v4) ────────────────────────────────

/**
 * Text-position nodes that did NOT migrate (willMigrate=false) continue to exist
 * as regular stars throughout the animation — universe remains alive after Scene 7.
 * Only rendered as midground stars (not text-position targets).
 */
private fun DrawScope.drawUniverseStars(
    nodes: List<AnimatedMeshNode>,
    overallProgress: Float
) {
    // Only meaningful after Scene 6 (before that, all nodes look the same anyway)
    if (overallProgress < AnimationConstants.SCENE_6_END) return

    nodes.forEach { star ->
        if (star.depthLayer != DepthLayer.MIDGROUND) return@forEach
        if (star.willMigrate || star.isUserNode) return@forEach  // skip migrating nodes
        val wake = star.wakeProgress
        if (wake <= 0.01f) return@forEach

        val center     = Offset(star.currentX, star.currentY)
        val brightness = star.currentBrightness
        val alpha      = (brightness * wake * 0.55f).coerceIn(0.05f, 0.55f)
        val radius     = star.radiusDp.dp.toPx()

        // Soft bloom halo
        drawCircle(
            color  = AnimationConstants.StarlightSilverGlow.copy(alpha = alpha * 0.18f),
            radius = radius * 2.0f,
            center = center
        )
        drawCircle(
            color  = AnimationConstants.StarlightSilver.copy(alpha = alpha),
            radius = radius,
            center = center
        )
    }
}

// ── Pass 11: Foreground blur stars ────────────────────────────────────────────

private fun DrawScope.drawForegroundStars(nodes: List<AnimatedMeshNode>) {
    nodes.forEach { star ->
        if (star.depthLayer != DepthLayer.FOREGROUND) return@forEach
        val wake = star.wakeProgress
        if (wake <= 0.01f) return@forEach

        val center     = Offset(star.currentX, star.currentY)
        val brightness = star.currentBrightness
        val alpha      = (brightness * 0.42f * wake).coerceIn(0.06f, 0.44f)
        val radius     = star.radiusDp.dp.toPx()

        drawCircle(
            color  = AnimationConstants.StarlightWhiteGlow.copy(alpha = alpha * 0.28f * wake),
            radius = radius * (1.8f + star.dofBlurFactor * 2.5f),
            center = center
        )
        drawCircle(
            color  = AnimationConstants.StarlightWhite.copy(alpha = alpha),
            radius = radius,
            center = center
        )
    }
}

// ── Pass 12: Radial signature wave ───────────────────────────────────────────

private fun DrawScope.drawRadialSignatureWave(
    wave: RadialSignatureWave,
    width: Float,
    height: Float
) {
    if (!wave.isActive || wave.alpha <= 0.01f) return
    val originPx = Offset(width * wave.originX, height * wave.originY)
    val diagonal = kotlin.math.sqrt((width * width + height * height).toDouble()).toFloat()
    val ringR    = wave.radius * diagonal

    drawCircle(
        color  = AnimationConstants.StarlightWhite.copy(alpha = wave.alpha * 0.16f),
        radius = ringR + 16f.dp.toPx(),
        center = originPx,
        style  = Stroke(width = 20f.dp.toPx())
    )
    drawCircle(
        color  = AnimationConstants.StarlightWhite.copy(alpha = wave.alpha * 0.60f),
        radius = ringR,
        center = originPx,
        style  = Stroke(width = 1.4f.dp.toPx())
    )
    drawCircle(
        color  = AnimationConstants.StarlightWhiteGlow.copy(alpha = wave.alpha * 0.10f),
        radius = (ringR - 7f.dp.toPx()).coerceAtLeast(0f),
        center = originPx,
        style  = Stroke(width = 8f.dp.toPx())
    )
}

// ── Pass 13: Full-screen convergence radial flash ─────────────────────────────

private fun DrawScope.drawSignatureConvergenceFlash(
    width: Float,
    height: Float,
    intensity: Float
) {
    if (intensity <= 0f) return
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AnimationConstants.StarlightWhite.copy(alpha = (intensity * 0.24f).coerceIn(0f, 1f)),
                AnimationConstants.StarlightWhiteGlow.copy(alpha = (intensity * 0.10f).coerceIn(0f, 1f)),
                Color.Transparent
            ),
            center = Offset(width * 0.50f, height * 0.46f),
            radius = width.coerceAtLeast(height) * 0.80f
        )
    )
}

// ── Pass 14: Scene 10 home transition vignette ────────────────────────────────

/**
 * As Scene 10 progresses (0.93 → 1.0):
 * - Screen edges darken proportionally to create a tunnel-focus effect
 * - User node (id=0 or isUserNode) stays bright; distant nodes fade by distance
 */
private fun DrawScope.drawHomeTransitionVignette(
    nodes: List<AnimatedMeshNode>,
    width: Float,
    height: Float,
    progress: Float
) {
    val t = ((progress - AnimationConstants.SCENE_9_END) /
            (1.0f - AnimationConstants.SCENE_9_END)).coerceIn(0f, 1f)

    // Find user/seed node position for vignette center
    val userNode = nodes.firstOrNull { it.isUserNode || it.id == 0 }
    val cx = userNode?.currentX ?: (width * 0.50f)
    val cy = userNode?.currentY ?: (height * 0.46f)

    // Deepening vignette — quadratic to stay gentle at first
    val vigAlpha = t * t * 0.92f

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                AnimationConstants.DeepSpaceNavy.copy(alpha = vigAlpha * 0.55f),
                AnimationConstants.DeepSpaceNavy.copy(alpha = vigAlpha)
            ),
            center = Offset(cx, cy),
            radius = (width.coerceAtLeast(height) * (0.5f - t * 0.25f)).coerceAtLeast(50f)
        )
    )
}
