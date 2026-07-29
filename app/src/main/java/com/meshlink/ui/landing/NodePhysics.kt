package com.meshlink.ui.landing

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Starfield physics generator and constellation migration manager.
 * Generates 140 independent star nodes, handles organic floating noise and night-sky twinkling,
 * and interpolates star migration into procedural constellation text points ("WELCOME TO MESH LINK").
 */
object NodePhysics {

    fun generateNodes(isWelcomeMode: Boolean): List<AnimatedMeshNode> {
        val nodes = mutableListOf<AnimatedMeshNode>()

        // 1. Generate procedural constellation layout points (35-40 stroke anchor points)
        val constellationLayout = ConstellationTextLayout.generateLayout()
        val textPoints = constellationLayout.points

        val seed = 42L
        val random = Random(seed)

        val totalStars = AnimationConstants.TOTAL_STAR_COUNT // 140 stars

        // Node 0: Seed Star / User Avatar Node
        nodes.add(
            AnimatedMeshNode(
                id = 0,
                startXRatio = 0.50f,
                startYRatio = 0.44f,
                targetXRatio = 0.50f,
                targetYRatio = 0.44f,
                radiusDp = if (isWelcomeMode) AnimationConstants.USER_AVATAR_STAR_RADIUS_DP else AnimationConstants.SEED_STAR_RADIUS_DP,
                glowColor = AnimationConstants.StarlightWhite,
                pulsePhase = 0f,
                pulseSpeed = 1.2f,
                twinklePhase = 0f,
                twinkleSpeed = 0.5f,
                baseBrightness = 0.95f,
                isMigrating = false,
                migrationOrder = 0f,
                isUserNode = isWelcomeMode
            )
        )

        // 2. Map procedural text points to 20-30% of star nodes (migrating stars)
        textPoints.forEachIndexed { idx, point ->
            val nodeId = idx + 1
            // Scatter starting floating position across night sky
            val startX = (0.10f + random.nextFloat() * 0.80f).coerceIn(0.08f, 0.92f)
            val startY = (0.12f + random.nextFloat() * 0.76f).coerceIn(0.10f, 0.90f)

            // Letter emergence order (W -> WE -> WEL -> WELCOME -> WELCOME TO -> MESH LINK)
            val order = (point.letterIndex.toFloat() / 16f)

            nodes.add(
                AnimatedMeshNode(
                    id = nodeId,
                    startXRatio = startX,
                    startYRatio = startY,
                    targetXRatio = point.xRatio,
                    targetYRatio = point.yRatio,
                    radiusDp = 2.2f + random.nextFloat() * 1.5f,
                    glowColor = AnimationConstants.StarlightSilver,
                    pulsePhase = random.nextFloat() * 2f * Math.PI.toFloat(),
                    pulseSpeed = 0.8f + random.nextFloat() * 0.8f,
                    twinklePhase = random.nextFloat() * 2f * Math.PI.toFloat(),
                    twinkleSpeed = 0.6f + random.nextFloat() * 1.4f,
                    baseBrightness = 0.35f + random.nextFloat() * 0.45f,
                    isMigrating = true,
                    migrationOrder = order,
                    isUserNode = false
                )
            )
        }

        // 3. Generate remaining 70-80% background starfield nodes (stationary / floating stars)
        val currentMigratingCount = nodes.size
        for (i in currentMigratingCount until totalStars) {
            val starX = (random.nextFloat()).coerceIn(0.02f, 0.98f)
            val starY = (random.nextFloat()).coerceIn(0.02f, 0.98f)
            val radius = AnimationConstants.MIN_STAR_RADIUS_DP + random.nextFloat() * (AnimationConstants.MAX_STAR_RADIUS_DP - AnimationConstants.MIN_STAR_RADIUS_DP)

            nodes.add(
                AnimatedMeshNode(
                    id = i,
                    startXRatio = starX,
                    startYRatio = starY,
                    targetXRatio = starX,
                    targetYRatio = starY,
                    radiusDp = radius,
                    glowColor = AnimationConstants.StarlightSilver,
                    pulsePhase = random.nextFloat() * 2f * Math.PI.toFloat(),
                    pulseSpeed = 0.5f + random.nextFloat() * 1.0f,
                    twinklePhase = random.nextFloat() * 2f * Math.PI.toFloat(),
                    twinkleSpeed = 0.4f + random.nextFloat() * 1.8f,
                    baseBrightness = 0.15f + random.nextFloat() * 0.55f,
                    isMigrating = false,
                    migrationOrder = 1.0f,
                    isUserNode = false
                )
            )
        }

        return nodes
    }

    fun updatePositions(
        nodes: List<AnimatedMeshNode>,
        width: Float,
        height: Float,
        timeMs: Long,
        overallProgress: Float,
        reduceMotion: Boolean
    ) {
        val timeSec = timeMs / 1000f

        // Constellation Migration progress (Phase 4: 0.50f -> 0.72f)
        val globalMigrationProgress = if (overallProgress >= AnimationConstants.PHASE_3_WAVE_PROPAGATION_END) {
            ((overallProgress - AnimationConstants.PHASE_3_WAVE_PROPAGATION_END) / (AnimationConstants.PHASE_4_CONSTELLATION_MIGRATION_END - AnimationConstants.PHASE_3_WAVE_PROPAGATION_END)).coerceIn(0f, 1f)
        } else {
            0f
        }

        nodes.forEach { star ->
            // Independent star twinkling
            val twinkleOffset = sin(timeSec * star.twinkleSpeed + star.twinklePhase).toFloat() * 0.22f
            star.currentBrightness = (star.baseBrightness + twinkleOffset).coerceIn(0.10f, 1.00f)

            val baseX = width * star.startXRatio
            val baseY = height * star.startYRatio
            val targetX = width * star.targetXRatio
            val targetY = height * star.targetYRatio

            if (reduceMotion) {
                if (star.isMigrating) {
                    star.currentX = lerp(baseX, targetX, globalMigrationProgress)
                    star.currentY = lerp(baseY, targetY, globalMigrationProgress)
                } else {
                    star.currentX = baseX
                    star.currentY = baseY
                }
            } else {
                // Organic floating noise drift for starfield stars
                val driftX = sin(timeSec * star.pulseSpeed + star.twinklePhase).toFloat() * 8f
                val driftY = cos(timeSec * 0.7f * star.pulseSpeed + star.twinklePhase * 1.5f).toFloat() * 8f

                val floatedStartX = baseX + driftX
                val floatedStartY = baseY + driftY

                if (star.isMigrating) {
                    // Staggered migration per letter (W -> WE -> WEL ...)
                    val starMigrationProgress = if (globalMigrationProgress > star.migrationOrder * 0.5f) {
                        ((globalMigrationProgress - star.migrationOrder * 0.5f) / 0.50f).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    val easedProgress = smoothStep(starMigrationProgress)
                    star.currentX = lerp(floatedStartX, targetX + driftX * 0.2f, easedProgress)
                    star.currentY = lerp(floatedStartY, targetY + driftY * 0.2f, easedProgress)
                } else {
                    star.currentX = floatedStartX
                    star.currentY = floatedStartY
                }
            }

            // Pulse factor
            star.pulseIntensity = (sin(timeSec * 3f + star.pulsePhase).toFloat() * 0.15f).coerceIn(-0.2f, 0.2f)
        }
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }

    private fun smoothStep(t: Float): Float {
        return t * t * (3 - 2 * t)
    }
}
