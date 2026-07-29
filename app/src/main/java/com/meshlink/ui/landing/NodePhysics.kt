package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Organic graph generator and physics simulation manager.
 * Generates procedural node positions, handles harmonic floating noise,
 * and interpolates node positions towards logo targets in Phase 8.
 */
object NodePhysics {

    fun generateNodes(isWelcomeMode: Boolean): List<AnimatedMeshNode> {
        val nodes = mutableListOf<AnimatedMeshNode>()

        // Logo target anchors (normalized x,y 0.0..1.0)
        val logoAnchors = listOf(
            Pair(0.50f, 0.42f), // 0: Center / User Node
            Pair(0.50f, 0.28f), // 1: Top
            Pair(0.64f, 0.35f), // 2: Top Right
            Pair(0.64f, 0.51f), // 3: Bottom Right
            Pair(0.50f, 0.58f), // 4: Bottom
            Pair(0.36f, 0.51f), // 5: Bottom Left
            Pair(0.36f, 0.35f), // 6: Top Left
            Pair(0.74f, 0.26f), // 7: Satellite Top Right
            Pair(0.76f, 0.60f), // 8: Satellite Bottom Right
            Pair(0.24f, 0.60f), // 9: Satellite Bottom Left
            Pair(0.26f, 0.26f), // 10: Satellite Top Left
            Pair(0.50f, 0.16f), // 11: High North
            Pair(0.50f, 0.70f)  // 12: Low South
        )

        // Seeded random for unique graph layout each launch
        val seed = System.currentTimeMillis()
        val random = Random(seed)

        // Node 0: Central User Node (or central hub)
        nodes.add(
            AnimatedMeshNode(
                id = 0,
                xRatio = 0.50f,
                yRatio = 0.44f,
                targetXRatio = logoAnchors[0].first,
                targetYRatio = logoAnchors[0].second,
                radiusDp = if (isWelcomeMode) AnimationConstants.USER_NODE_RADIUS_DP else 14f,
                glowColor = AnimationConstants.ElectricBlue,
                pulsePhase = 0f,
                pulseSpeed = 1.2f,
                floatNoiseOffsetX = 0f,
                floatNoiseOffsetY = 0f,
                appearDelay = 0.05f,
                haloRingCount = 3,
                isUserNode = isWelcomeMode
            )
        )

        // Generate surrounding regular nodes
        val colors = listOf(
            AnimationConstants.Cyan,
            AnimationConstants.ElectricBlue,
            AnimationConstants.Teal,
            AnimationConstants.PurpleAccent
        )

        val totalRegularNodes = AnimationConstants.REGULAR_NODE_COUNT
        for (i in 1 until totalRegularNodes) {
            val anchor = logoAnchors.getOrElse(i) {
                val angle = (i.toFloat() / totalRegularNodes) * 2f * Math.PI.toFloat()
                val rad = 0.25f + random.nextFloat() * 0.20f
                Pair(
                    (0.50f + cos(angle.toDouble()).toFloat() * rad).coerceIn(0.12f, 0.88f),
                    (0.44f + sin(angle.toDouble()).toFloat() * rad).coerceIn(0.15f, 0.78f)
                )
            }

            // Procedural offset for floating start position
            val startX = (anchor.first + (random.nextFloat() - 0.5f) * 0.18f).coerceIn(0.10f, 0.90f)
            val startY = (anchor.second + (random.nextFloat() - 0.5f) * 0.18f).coerceIn(0.12f, 0.82f)

            val delay = 0.08f + (i.toFloat() / totalRegularNodes) * 0.32f
            val radius = AnimationConstants.MIN_NODE_RADIUS_DP + random.nextFloat() * (AnimationConstants.MAX_NODE_RADIUS_DP - AnimationConstants.MIN_NODE_RADIUS_DP)

            nodes.add(
                AnimatedMeshNode(
                    id = i,
                    xRatio = startX,
                    yRatio = startY,
                    targetXRatio = anchor.first,
                    targetYRatio = anchor.second,
                    radiusDp = radius,
                    glowColor = colors[i % colors.size],
                    pulsePhase = random.nextFloat() * 2f * Math.PI.toFloat(),
                    pulseSpeed = 0.8f + random.nextFloat() * 0.8f,
                    floatNoiseOffsetX = random.nextFloat() * 100f,
                    floatNoiseOffsetY = random.nextFloat() * 100f,
                    appearDelay = delay,
                    haloRingCount = if (i % 3 == 0) 2 else 1,
                    isUserNode = false
                )
            )
        }

        return nodes
    }

    fun generateAmbientDust(): List<AmbientDustParticle> {
        val random = Random(42)
        val particles = mutableListOf<AmbientDustParticle>()
        for (i in 0 until AnimationConstants.AMBIENT_DUST_PARTICLE_COUNT) {
            particles.add(
                AmbientDustParticle(
                    xRatio = random.nextFloat(),
                    yRatio = random.nextFloat(),
                    sizeDp = 1.2f + random.nextFloat() * 2.8f,
                    speedX = (random.nextFloat() - 0.5f) * 0.03f,
                    speedY = (random.nextFloat() - 0.5f) * 0.03f,
                    baseAlpha = 0.15f + random.nextFloat() * 0.45f
                )
            )
        }
        return particles
    }

    fun generateDataPackets(nodes: List<AnimatedMeshNode>): List<DataPacket> {
        val packets = mutableListOf<DataPacket>()
        val random = Random(123)
        val colors = listOf(
            AnimationConstants.Cyan,
            AnimationConstants.ElectricBlue,
            AnimationConstants.Teal,
            AnimationConstants.PurpleAccent
        )

        for (i in 0 until AnimationConstants.PACKET_COUNT) {
            val fromIndex = random.nextInt(nodes.size)
            var toIndex = random.nextInt(nodes.size)
            if (toIndex == fromIndex) {
                toIndex = (fromIndex + 1) % nodes.size
            }

            packets.add(
                DataPacket(
                    id = i,
                    fromNodeId = fromIndex,
                    toNodeId = toIndex,
                    speed = 0.4f + random.nextFloat() * 0.6f,
                    color = colors[i % colors.size],
                    sizeDp = 2.5f + random.nextFloat() * 2.5f,
                    delayProgress = 0.35f + (i.toFloat() / AnimationConstants.PACKET_COUNT) * 0.30f
                )
            )
        }
        return packets
    }

    fun updatePositions(
        nodes: List<AnimatedMeshNode>,
        width: Float,
        height: Float,
        timeMs: Long,
        overallProgress: Float,
        reduceMotion: Boolean
    ) {
        val logoProgress = if (overallProgress > AnimationConstants.PHASE_8_LOGO_EMERGENCE_END - 0.15f) {
            ((overallProgress - (AnimationConstants.PHASE_8_LOGO_EMERGENCE_END - 0.15f)) / 0.15f).coerceIn(0f, 1f)
        } else {
            0f
        }

        val timeSec = timeMs / 1000f

        nodes.forEach { node ->
            val baseX = width * node.xRatio
            val baseY = height * node.yRatio
            val targetX = width * node.targetXRatio
            val targetY = height * node.targetYRatio

            if (reduceMotion) {
                node.currentX = lerp(baseX, targetX, logoProgress)
                node.currentY = lerp(baseY, targetY, logoProgress)
            } else {
                // Procedural floating noise
                val driftX = sin(timeSec * node.pulseSpeed + node.floatNoiseOffsetX).toFloat() * 12f
                val driftY = cos(timeSec * 0.8f * node.pulseSpeed + node.floatNoiseOffsetY).toFloat() * 12f

                val floatedX = baseX + driftX
                val floatedY = baseY + driftY

                node.currentX = lerp(floatedX, targetX, logoProgress)
                node.currentY = lerp(floatedY, targetY, logoProgress)
            }

            // Breathing scale & pulse
            node.breathingOffset = sin(timeSec * 2.5f + node.pulsePhase).toFloat() * 0.15f
        }
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }
}
