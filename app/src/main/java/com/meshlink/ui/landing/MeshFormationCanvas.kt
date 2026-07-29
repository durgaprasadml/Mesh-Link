package com.meshlink.ui.landing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural GPU-optimized DrawScope Canvas rendering the 9-phase mesh animation storyboard.
 * Guarantees zero allocation in the draw loop for maximum 60/120 FPS performance.
 */
@Composable
fun MeshFormationCanvas(
    nodes: List<AnimatedMeshNode>,
    connectionAnimator: MeshConnectionAnimator,
    ambientDust: List<AmbientDustParticle>,
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

        // 1. Draw Ambient Background & Light Beams (Phase 1 Silence & Atmosphere)
        drawBackgroundAtmosphere(canvasWidth, canvasHeight, timeMs)

        // 2. Draw Ambient Dust Particle Swarm
        drawAmbientDust(ambientDust, canvasWidth, canvasHeight, timeMs)

        // 3. Draw Radar Scanning Waves (Phase 3 Scanning)
        if (overallProgress in AnimationConstants.PHASE_2_DISCOVERY_END..AnimationConstants.PHASE_5_SELF_HEALING_END) {
            drawRadarWaves(nodes, timeMs)
        }

        // 4. Draw Mesh Connection Links & Pulses (Phase 4 Connection & Phase 5 Self-Healing)
        drawMeshConnections(connectionAnimator.links, overallProgress)

        // 5. Draw Discovery Sparks & Data Packet Trails (Phase 6 Packet Routing)
        drawParticlesAndPackets(connectionAnimator.sparkPool, dataPackets, nodes)

        // 6. Draw Mesh Nodes & Halo Rings (Phase 2 Discovery & Phase 6 Stabilization)
        drawMeshNodes(nodes, overallProgress, isWelcomeMode)
    }
}

private fun DrawScope.drawBackgroundAtmosphere(width: Float, height: Float, timeMs: Long) {
    val timeSec = timeMs / 1000f

    // Deep radial gradient
    val centerOffset = Offset(
        width * (0.50f + sin(timeSec * 0.2f) * 0.05f),
        height * (0.45f + cos(timeSec * 0.15f) * 0.05f)
    )

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            AnimationConstants.DeepNavy,
            AnimationConstants.CharcoalBlack,
            Color(0xFF070912)
        ),
        center = centerOffset,
        radius = width.coerceAtLeast(height) * 0.85f
    )

    drawRect(brush = backgroundBrush)

    // Dynamic light beam overlay
    val beamOffset = sin(timeSec * 0.3f) * width * 0.2f
    val beamBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            AnimationConstants.ElectricBlue.copy(alpha = 0.06f),
            AnimationConstants.Cyan.copy(alpha = 0.04f),
            Color.Transparent
        ),
        start = Offset(beamOffset, 0f),
        end = Offset(width * 0.7f + beamOffset, height)
    )

    drawRect(brush = beamBrush)
}

private fun DrawScope.drawAmbientDust(
    dustParticles: List<AmbientDustParticle>,
    width: Float,
    height: Float,
    timeMs: Long
) {
    val timeSec = timeMs / 1000f

    dustParticles.forEach { particle ->
        val x = (width * particle.xRatio + sin(timeSec * 0.5f + particle.xRatio * 10f) * 15f) % width
        val y = (height * particle.yRatio + cos(timeSec * 0.4f + particle.yRatio * 10f) * 15f) % height
        val alpha = (particle.baseAlpha * (0.6f + sin(timeSec * 1.5f + particle.xRatio * 20f) * 0.4f)).coerceIn(0f, 1f)

        drawCircle(
            color = AnimationConstants.SoftWhite.copy(alpha = alpha),
            radius = particle.sizeDp.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawRadarWaves(
    nodes: List<AnimatedMeshNode>,
    timeMs: Long
) {
    val timeSec = timeMs / 1000f
    val radarNodes = nodes.filter { it.id % 4 == 0 && it.alpha > 0.5f }

    radarNodes.forEach { node ->
        val pulseTime = (timeSec * 1.2f + node.id) % 2.0f
        if (pulseTime < 1.0f) {
            val radius = node.radiusDp.dp.toPx() + pulseTime * 140f
            val alpha = ((1.0f - pulseTime) * 0.35f * node.alpha).coerceIn(0f, 1f)

            drawCircle(
                color = AnimationConstants.Cyan.copy(alpha = alpha),
                radius = radius,
                center = Offset(node.currentX, node.currentY),
                style = Stroke(width = 1.8f)
            )
        }
    }
}

private fun DrawScope.drawMeshConnections(
    links: List<MeshConnectionLink>,
    overallProgress: Float
) {
    val logoFade = if (overallProgress > AnimationConstants.PHASE_8_LOGO_EMERGENCE_END) {
        (1.0f - ((overallProgress - AnimationConstants.PHASE_8_LOGO_EMERGENCE_END) / 0.04f)).coerceIn(0.2f, 1f)
    } else {
        1.0f
    }

    links.forEach { link ->
        if (link.alpha > 0.02f && link.growthProgress > 0.02f) {
            val n1 = link.nodeA
            val n2 = link.nodeB

            val start = Offset(n1.currentX, n1.currentY)
            val fullEnd = Offset(n2.currentX, n2.currentY)

            // Calculate growing line tip
            val currentEnd = Offset(
                start.x + (fullEnd.x - start.x) * link.growthProgress,
                start.y + (fullEnd.y - start.y) * link.growthProgress
            )

            val baseColor = when (link.type) {
                ConnectionType.DISCOVERY -> AnimationConstants.ConnectionDiscovery
                ConnectionType.CONNECTED -> AnimationConstants.ConnectionConnected
                ConnectionType.RELAY -> AnimationConstants.ConnectionRelay
                ConnectionType.BROADCAST -> AnimationConstants.ConnectionBroadcast
                ConnectionType.INACTIVE -> AnimationConstants.ConnectionInactive
            }

            val alpha = (link.alpha * logoFade).coerceIn(0f, 1f)
            val strokeWidth = if (n1.isUserNode || n2.isUserNode) 2.5f.dp.toPx() else 1.5f.dp.toPx()

            // Outer soft glow line
            drawLine(
                color = baseColor.copy(alpha = alpha * 0.35f),
                start = start,
                end = currentEnd,
                strokeWidth = strokeWidth * 2.8f,
                cap = StrokeCap.Round
            )

            // Inner core connection line
            drawLine(
                color = baseColor.copy(alpha = alpha),
                start = start,
                end = currentEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Traveling link pulse dot
            if (link.pulseProgress > 0f && link.growthProgress >= 0.95f) {
                val pulseX = start.x + (fullEnd.x - start.x) * link.pulseProgress
                val pulseY = start.y + (fullEnd.y - start.y) * link.pulseProgress

                drawCircle(
                    color = AnimationConstants.Cyan.copy(alpha = alpha * 0.8f),
                    radius = 2.5f.dp.toPx(),
                    center = Offset(pulseX, pulseY)
                )
            }
        }
    }
}

private fun DrawScope.drawParticlesAndPackets(
    sparks: List<DiscoverySpark>,
    packets: List<DataPacket>,
    nodes: List<AnimatedMeshNode>
) {
    // 1. Discovery Sparks
    sparks.forEach { spark ->
        if (spark.active && spark.life > 0f) {
            drawCircle(
                color = spark.color.copy(alpha = spark.life),
                radius = spark.size,
                center = Offset(spark.x, spark.y)
            )
        }
    }

    // 2. Data Packets (Phase 6 Packet Routing)
    packets.forEach { packet ->
        if (packet.isActive) {
            val fromNode = nodes.getOrNull(packet.fromNodeId)
            val toNode = nodes.getOrNull(packet.toNodeId)

            if (fromNode != null && toNode != null && fromNode.alpha > 0.3f && toNode.alpha > 0.3f) {
                val px = fromNode.currentX + (toNode.currentX - fromNode.currentX) * packet.progress
                val py = fromNode.currentY + (toNode.currentY - fromNode.currentY) * packet.progress

                packet.currentX = px
                packet.currentY = py

                // Packet trail
                val trailX = fromNode.currentX + (toNode.currentX - fromNode.currentX) * (packet.progress - 0.06f).coerceAtLeast(0f)
                val trailY = fromNode.currentY + (toNode.currentY - fromNode.currentY) * (packet.progress - 0.06f).coerceAtLeast(0f)

                drawLine(
                    color = packet.color.copy(alpha = 0.3f),
                    start = Offset(trailX, trailY),
                    end = Offset(px, py),
                    strokeWidth = packet.sizeDp.dp.toPx() * 0.8f,
                    cap = StrokeCap.Round
                )

                // Packet core
                drawCircle(
                    color = packet.color,
                    radius = packet.sizeDp.dp.toPx(),
                    center = Offset(px, py)
                )

                // Glowing aura around packet
                drawCircle(
                    color = packet.color.copy(alpha = 0.4f),
                    radius = packet.sizeDp.dp.toPx() * 2.2f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

private fun DrawScope.drawMeshNodes(
    nodes: List<AnimatedMeshNode>,
    overallProgress: Float,
    isWelcomeMode: Boolean
) {
    nodes.forEach { node ->
        // Appearance alpha fade based on node delay threshold
        val nodeAlpha = if (overallProgress >= node.appearDelay) {
            ((overallProgress - node.appearDelay) / 0.12f).coerceIn(0f, 1f)
        } else {
            0f
        }
        node.alpha = nodeAlpha

        if (nodeAlpha > 0.01f) {
            val center = Offset(node.currentX, node.currentY)
            val baseRadius = node.radiusDp.dp.toPx() * (1f + node.breathingOffset)

            // Skip rendering central node inner graphics if Welcome mode avatar container is handling it
            if (node.isUserNode && isWelcomeMode) {
                // Outer user aura rings
                for (ring in 1..3) {
                    val ringRadius = baseRadius + ring * 18f
                    val ringAlpha = (0.35f / ring) * nodeAlpha
                    drawCircle(
                        color = AnimationConstants.ElectricBlue.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
                return@forEach
            }

            // Outer Radial Glow Halo
            drawCircle(
                color = node.glowColor.copy(alpha = 0.25f * nodeAlpha),
                radius = baseRadius * 2.6f,
                center = center
            )

            // Halo Rings
            for (r in 1..node.haloRingCount) {
                val ringRadius = baseRadius * (1.3f + r * 0.45f)
                val ringAlpha = (0.28f / r) * nodeAlpha
                drawCircle(
                    color = node.glowColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
            }

            // Solid Core Node
            drawCircle(
                color = node.glowColor.copy(alpha = nodeAlpha),
                radius = baseRadius,
                center = center
            )

            // Inner Highlight Spot
            drawCircle(
                color = AnimationConstants.SoftWhite.copy(alpha = 0.85f * nodeAlpha),
                radius = baseRadius * 0.35f,
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f)
            )
        }
    }
}
