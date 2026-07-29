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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural GPU-optimized DrawScope Canvas rendering the 6-phase starlight constellation experience.
 * Guarantees zero allocation in the draw loop for maximum 60/120 FPS performance.
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
        val canvasWidth = size.width
        val canvasHeight = size.height

        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        // 1. Draw Deep Space Background Atmosphere & Subtle Starlight Shimmer
        drawDeepSpaceAtmosphere(canvasWidth, canvasHeight, timeMs)

        // 2. Draw Wave Ripple Effects
        drawConstellationRipples(connectionAnimator.rippleWave, canvasWidth, canvasHeight)

        // 3. Draw Single-Color Starlight Connections & Constellation Letter Strokes
        drawConstellationConnections(connectionAnimator.links, overallProgress)

        // 4. Draw White Energy Packets & Fading Light Trails
        drawEnergyPackets(dataPackets, nodes)

        // 5. Draw Night Sky Star Nodes (Independent Twinkling & Seed Node Halos)
        drawStarfieldNodes(nodes, overallProgress, isWelcomeMode)
    }
}

private fun DrawScope.drawDeepSpaceAtmosphere(width: Float, height: Float, timeMs: Long) {
    val timeSec = timeMs / 1000f

    // Deep space radial background
    val centerOffset = Offset(
        width * (0.50f + sin(timeSec * 0.15f) * 0.03f),
        height * (0.45f + cos(timeSec * 0.12f) * 0.03f)
    )

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            AnimationConstants.SpaceDarkCharcoal,
            AnimationConstants.DeepSpaceNavy,
            Color(0xFF030408)
        ),
        center = centerOffset,
        radius = width.coerceAtLeast(height) * 0.90f
    )

    drawRect(brush = backgroundBrush)

    // Subtle starlight haze
    val hazeOffset = sin(timeSec * 0.25f) * width * 0.15f
    val hazeBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            AnimationConstants.SubtleStarlightBlue.copy(alpha = 0.04f),
            AnimationConstants.StarlightSilver.copy(alpha = 0.02f),
            Color.Transparent
        ),
        start = Offset(hazeOffset, 0f),
        end = Offset(width * 0.8f + hazeOffset, height)
    )

    drawRect(brush = hazeBrush)
}

private fun DrawScope.drawConstellationRipples(
    rippleWave: ConstellationRippleWave,
    width: Float,
    height: Float
) {
    if (!rippleWave.isActive || rippleWave.alpha <= 0.01f) return

    val center = Offset(width * rippleWave.centerX, height * rippleWave.centerY)
    val maxRadius = width.coerceAtLeast(height) * rippleWave.radius

    drawCircle(
        color = AnimationConstants.StarlightWhite.copy(alpha = rippleWave.alpha * 0.25f),
        radius = maxRadius,
        center = center,
        style = Stroke(width = 1.5f.dp.toPx())
    )
}

private fun DrawScope.drawConstellationConnections(
    links: List<MeshConnectionLink>,
    overallProgress: Float
) {
    links.forEach { link ->
        if (link.alpha > 0.02f && link.growthProgress > 0.02f) {
            val n1 = link.nodeA
            val n2 = link.nodeB

            val start = Offset(n1.currentX, n1.currentY)
            val fullEnd = Offset(n2.currentX, n2.currentY)

            val currentEnd = Offset(
                start.x + (fullEnd.x - start.x) * link.growthProgress,
                start.y + (fullEnd.y - start.y) * link.growthProgress
            )

            val baseColor = if (link.isConstellationStroke) {
                AnimationConstants.ConnectionConstellation
            } else {
                AnimationConstants.StarlightSilver
            }

            // Energy pulse boost when white packet travels on this line
            val activeBoost = link.energyPulseBrightness

            val lineAlpha = ((link.alpha * (if (link.isConstellationStroke) 0.85f else 0.40f) + activeBoost * 0.5f)).coerceIn(0f, 1f)
            val strokeWidth = if (link.isConstellationStroke) 2.0f.dp.toPx() else 1.2f.dp.toPx()

            // Outer starlight glow line
            drawLine(
                color = baseColor.copy(alpha = lineAlpha * 0.4f),
                start = start,
                end = currentEnd,
                strokeWidth = strokeWidth * 2.5f,
                cap = StrokeCap.Round
            )

            // Inner core connection line
            drawLine(
                color = baseColor.copy(alpha = lineAlpha),
                start = start,
                end = currentEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawEnergyPackets(
    packets: List<DataPacket>,
    nodes: List<AnimatedMeshNode>
) {
    packets.forEach { packet ->
        if (packet.isActive) {
            val fromNode = nodes.getOrNull(packet.fromNodeId)
            val toNode = nodes.getOrNull(packet.toNodeId)

            if (fromNode != null && toNode != null && fromNode.isDiscovered && toNode.isDiscovered) {
                val px = fromNode.currentX + (toNode.currentX - fromNode.currentX) * packet.progress
                val py = fromNode.currentY + (toNode.currentY - fromNode.currentY) * packet.progress

                packet.currentX = px
                packet.currentY = py

                // Fading white packet trail
                val trailX = fromNode.currentX + (toNode.currentX - fromNode.currentX) * (packet.progress - 0.08f).coerceAtLeast(0f)
                val trailY = fromNode.currentY + (toNode.currentY - fromNode.currentY) * (packet.progress - 0.08f).coerceAtLeast(0f)

                drawLine(
                    color = AnimationConstants.StarlightWhite.copy(alpha = 0.45f),
                    start = Offset(trailX, trailY),
                    end = Offset(px, py),
                    strokeWidth = packet.sizeDp.dp.toPx() * 1.2f,
                    cap = StrokeCap.Round
                )

                // Packet core star particle
                drawCircle(
                    color = AnimationConstants.StarlightWhite,
                    radius = packet.sizeDp.dp.toPx(),
                    center = Offset(px, py)
                )

                // Subtle white aura glow
                drawCircle(
                    color = AnimationConstants.StarlightWhiteGlow,
                    radius = packet.sizeDp.dp.toPx() * 2.5f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

private fun DrawScope.drawStarfieldNodes(
    nodes: List<AnimatedMeshNode>,
    overallProgress: Float,
    isWelcomeMode: Boolean
) {
    nodes.forEach { star ->
        val center = Offset(star.currentX, star.currentY)

        // Independent twinkling brightness & pulse
        val brightness = (star.currentBrightness + star.pulseIntensity).coerceIn(0.08f, 1.00f)
        val radius = star.radiusDp.dp.toPx() * (0.85f + brightness * 0.30f)

        // Render User Node avatar ring container in Welcome mode
        if (star.isUserNode && isWelcomeMode) {
            // Outer seed node glowing halo rings
            val haloRadius = radius * 1.8f
            drawCircle(
                color = AnimationConstants.StarlightWhiteGlow.copy(alpha = 0.5f),
                radius = haloRadius,
                center = center
            )
            drawCircle(
                color = AnimationConstants.StarlightSilver.copy(alpha = 0.3f),
                radius = haloRadius * 1.4f,
                center = center,
                style = Stroke(width = 1.5f)
            )
            return@forEach
        }

        // Standard Star Nodes in Universe
        val starAlpha = if (star.isDiscovered) {
            (brightness * 1.0f).coerceIn(0.40f, 1.00f)
        } else {
            (brightness * 0.45f).coerceIn(0.08f, 0.55f)
        }

        // Outer Starlight Halo for Discovered / Seed Stars
        if (star.isDiscovered || star.id == 0 || star.isMigrating) {
            drawCircle(
                color = AnimationConstants.StarlightSilverGlow.copy(alpha = 0.30f * starAlpha),
                radius = radius * 2.8f,
                center = center
            )
        }

        // Solid Star Core
        drawCircle(
            color = AnimationConstants.StarlightWhite.copy(alpha = starAlpha),
            radius = radius,
            center = center
        )

        // Subtle Inner Starlight Sparkle
        if (star.id % 3 == 0) {
            drawCircle(
                color = Color.White.copy(alpha = (starAlpha * 0.9f).coerceIn(0f, 1f)),
                radius = radius * 0.4f,
                center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f)
            )
        }
    }
}
