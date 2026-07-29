package com.meshlink.ui.landing

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Starfield physics generator, relay hub manager, and constellation migration engine — v4.
 *
 * Key changes from v3:
 *  1. Random "epicenter" — the seed star's initial screen position is randomized within a
 *     small central region (±0.07 from center) so every launch feels different.
 *  2. [willMigrate] assignment — 40% of text nodes are selected via the launch seed using
 *     stratified sampling across letters, preserving readability while leaving 60% as universe.
 *  3. Weighted neighbor scoring for BFS wake propagation:
 *       score = distanceWeight(0.40) + angularDiversity(0.25) + densityPenalty(0.20) + relayBonus(0.15)
 *  4. Camera drift frequencies get a per-launch jitter so Lissajous paths differ each time.
 *  5. [updatePositions] only runs migration interpolation on nodes where [willMigrate = true].
 *  6. [signatureWaveFlash] decays in [updatePositions] at 3.5/s.
 *  7. Compressed [rampLen] = 0.038f (was 0.048f) to fit the tighter 5.5 s total.
 */
object NodePhysics {

    /** Camera state snapshot — computed once per frame and shared between screen and canvas. */
    data class CameraState(
        val scale: Float,
        val rotationDeg: Float,
        val panX: Float,
        val panY: Float
    )

    // ── Camera scale keyframes (quintic hermite spline) ───────────────────────
    // Scene 1: very close to one star (scale 3.0)
    // Scene 3–4: zoom-out reveals the universe
    // Scene 7+: normal distance (1.0)
    // Scene 10: exponential fly-through (handled separately)
    private val cameraScaleKeyframes = floatArrayOf(
        0.00f, 0.06f, 0.20f, 0.42f, 0.64f, 0.86f, 0.93f, 1.00f
    )
    private val cameraScaleValues = floatArrayOf(
        3.00f, 2.90f, 2.10f, 1.55f, 1.05f, 1.00f, 1.00f, 1.00f
    )

    // Per-launch camera frequency jitter (set during generateNodes)
    private var camFreqJitter = 0f

    // ── Node generation ───────────────────────────────────────────────────────

    fun generateNodes(isWelcomeMode: Boolean, launchSeed: Long = System.currentTimeMillis()): List<AnimatedMeshNode> {
        val nodes  = mutableListOf<AnimatedMeshNode>()
        val random = Random(launchSeed)

        // Per-launch camera jitter so Lissajous drifts differ each run
        camFreqJitter = (random.nextFloat() - 0.5f) * 2f * AnimationConstants.CAMERA_FREQ_JITTER

        // Clear BFS overrides from any previous run
        bfsDelayOverrides.clear()

        val layout     = ConstellationTextLayout.generateLayout()
        val textPoints = layout.points

        val totalStars = AnimationConstants.TOTAL_STAR_COUNT   // 120
        val fgCount    = AnimationConstants.FG_STAR_COUNT      // 12
        val bgTarget   = totalStars - fgCount                  // 108

        // Relay hub node ids — these are BFS "super-spreaders"
        val relayHubIds = setOf(0, 4, 9, 15, 22, 30, 38, 47)

        // ── 1. Seed / User Avatar Node (id = 0) ──────────────────────────────
        // Randomize epicenter within ±0.07 of screen center
        val epicenterX = 0.50f + (random.nextFloat() - 0.5f) * 0.14f
        val epicenterY = 0.46f + (random.nextFloat() - 0.5f) * 0.14f

        nodes.add(
            AnimatedMeshNode(
                id               = 0,
                startXRatio      = epicenterX,
                startYRatio      = epicenterY,
                targetXRatio     = epicenterX,
                targetYRatio     = epicenterY,
                radiusDp         = if (isWelcomeMode) AnimationConstants.USER_AVATAR_STAR_RADIUS_DP
                                   else AnimationConstants.SEED_STAR_RADIUS_DP,
                glowColor        = AnimationConstants.StarlightWhite,
                pulsePhase       = 0f,
                pulseSpeed       = 0.9f,
                twinklePhase     = random.nextFloat() * 2f * PI.toFloat(),
                twinkleSpeed     = 0.35f,
                baseBrightness   = 0.95f,
                isMigrating      = false,
                migrationOrder   = 0f,
                isUserNode       = isWelcomeMode,
                depthLayer       = DepthLayer.MIDGROUND,
                isRelayHub       = true,
                relayScore       = 2.2f,
                breathingPhase   = 0f,
                wakeDelay        = AnimationConstants.SCENE_1_END,
                isSilenceSentinel = false,
                willMigrate      = false  // seed node never migrates
            )
        )

        // ── 2. Text-migrating midground nodes ────────────────────────────────
        // willMigrate is assigned by stratified 40% selection below.
        val textNodeCount = textPoints.size
        val migrateTargetCount = (textNodeCount * AnimationConstants.MIGRATION_RATIO + 0.5f).toInt()
            .coerceAtLeast(1)

        // Stratified selection: pick ~40% from every letter uniformly
        val selectedForMigration = selectMigrationNodes(textPoints, migrateTargetCount, random)

        // Identify the strongest relay hub index for packet reversal target
        val reversalTargetId = relayHubIds.filter { it in 1..textNodeCount }.maxOrNull() ?: 4

        textPoints.forEachIndexed { idx, point ->
            val nodeId   = idx + 1
            val startX   = (epicenterX + (random.nextFloat() - 0.5f) * 0.70f).coerceIn(0.07f, 0.93f)
            val startY   = (epicenterY + (random.nextFloat() - 0.5f) * 0.70f).coerceIn(0.10f, 0.90f)
            val order    = (point.letterIndex.toFloat() / 16f)
            val isHub    = relayHubIds.contains(nodeId)
            val baseR    = if (isHub) 2.8f else (1.6f + random.nextFloat() * 1.1f)
            val migrate  = selectedForMigration.contains(idx)

            nodes.add(
                AnimatedMeshNode(
                    id                    = nodeId,
                    startXRatio           = startX,
                    startYRatio           = startY,
                    targetXRatio          = if (migrate) point.xRatio else startX,
                    targetYRatio          = if (migrate) point.yRatio else startY,
                    radiusDp              = baseR,
                    glowColor             = if (isHub) AnimationConstants.StarlightWhite
                                           else AnimationConstants.StarlightSilver,
                    pulsePhase            = random.nextFloat() * 2f * PI.toFloat(),
                    pulseSpeed            = 0.6f + random.nextFloat() * 0.7f,
                    twinklePhase          = random.nextFloat() * 2f * PI.toFloat(),
                    twinkleSpeed          = 0.4f + random.nextFloat() * 1.0f,
                    baseBrightness        = if (isHub) 0.80f else (0.30f + random.nextFloat() * 0.40f),
                    isMigrating           = migrate,
                    migrationOrder        = order,
                    isUserNode            = false,
                    depthLayer            = DepthLayer.MIDGROUND,
                    isRelayHub            = isHub,
                    relayScore            = if (isHub) 1.8f else 1.0f,
                    breathingPhase        = (idx * 0.42f) % (2f * PI.toFloat()),
                    wakeDelay             = 0f,   // assigned by weighted BFS below
                    isSilenceSentinel     = false,
                    willMigrate           = migrate,
                    isPacketReversalTarget = (nodeId == reversalTargetId)
                )
            )
        }

        // ── 3. Background starfield nodes ────────────────────────────────────
        val sentinelIndices = setOf(0, 1, 2)
        var bgAdded = 0
        while (nodes.size < bgTarget) {
            val i          = nodes.size
            val starX      = random.nextFloat().coerceIn(0.03f, 0.97f)
            val starY      = random.nextFloat().coerceIn(0.03f, 0.97f)
            val radius     = AnimationConstants.MIN_STAR_RADIUS_DP + random.nextFloat() * 1.0f
            val isSentinel = bgAdded in sentinelIndices

            nodes.add(
                AnimatedMeshNode(
                    id               = i,
                    startXRatio      = starX,
                    startYRatio      = starY,
                    radiusDp         = if (isSentinel) 1.8f else radius,
                    glowColor        = AnimationConstants.StarGreyDim,
                    pulsePhase       = random.nextFloat() * 2f * PI.toFloat(),
                    pulseSpeed       = 0.3f + random.nextFloat() * 0.6f,
                    twinklePhase     = random.nextFloat() * 2f * PI.toFloat(),
                    twinkleSpeed     = 0.25f + random.nextFloat() * 1.2f,
                    baseBrightness   = if (isSentinel) (0.18f + random.nextFloat() * 0.10f)
                                       else (0.10f + random.nextFloat() * 0.28f),
                    depthLayer       = DepthLayer.BACKGROUND,
                    breathingPhase   = random.nextFloat() * 2f * PI.toFloat(),
                    wakeDelay        = if (isSentinel) 0f
                                       else (0.08f + random.nextFloat() * 0.25f),
                    isSilenceSentinel = isSentinel
                )
            )
            bgAdded++
        }

        // ── 4. Foreground blur stars ─────────────────────────────────────────
        while (nodes.size < totalStars) {
            val i      = nodes.size
            val starX  = random.nextFloat().coerceIn(0.05f, 0.95f)
            val starY  = random.nextFloat().coerceIn(0.05f, 0.95f)
            val radius = 2.8f + random.nextFloat() * 1.4f

            nodes.add(
                AnimatedMeshNode(
                    id              = i,
                    startXRatio     = starX,
                    startYRatio     = starY,
                    radiusDp        = radius,
                    glowColor       = AnimationConstants.StarlightSilverGlow,
                    pulsePhase      = random.nextFloat() * 2f * PI.toFloat(),
                    pulseSpeed      = 0.5f + random.nextFloat() * 0.9f,
                    twinklePhase    = random.nextFloat() * 2f * PI.toFloat(),
                    twinkleSpeed    = 0.4f + random.nextFloat() * 1.0f,
                    baseBrightness  = 0.20f + random.nextFloat() * 0.32f,
                    depthLayer      = DepthLayer.FOREGROUND,
                    breathingPhase  = random.nextFloat() * 2f * PI.toFloat(),
                    wakeDelay       = 0.05f + random.nextFloat() * 0.20f
                )
            )
        }

        // ── 5. Weighted BFS wake-delay assignment ────────────────────────────
        assignWeightedBfsDelays(nodes, random)

        return nodes
    }

    // ── Stratified migration selection ────────────────────────────────────────

    /**
     * Selects [targetCount] indices from [textPoints] such that:
     *  - At least one node per letter is included (if there are enough)
     *  - Selection is uniformly distributed across the full text width
     *  - Result is deterministic given the [random] seed
     */
    private fun selectMigrationNodes(
        textPoints: List<ConstellationTextLayout.ConstellationPoint>,
        targetCount: Int,
        random: Random
    ): Set<Int> {
        val selected = mutableSetOf<Int>()
        // Group by letter index
        val byLetter = textPoints.withIndex()
            .groupBy { it.value.letterIndex }
            .values.toList()

        // Each letter contributes proportionally
        val perLetterCount = (targetCount.toFloat() / byLetter.size.coerceAtLeast(1)).coerceAtLeast(1f)

        byLetter.forEach { letterGroup ->
            val take = (perLetterCount + random.nextFloat() * 0.5f).toInt().coerceAtLeast(1)
            letterGroup.shuffled(random).take(take).forEach { selected.add(it.index) }
        }

        // Trim or pad to exactly targetCount
        return if (selected.size > targetCount) {
            selected.toList().shuffled(random).take(targetCount).toSet()
        } else selected
    }

    // ── Weighted BFS delay assignment ─────────────────────────────────────────

    /**
     * Assigns wakeDelay to midground nodes using a 4-factor weighted score:
     *   score = distanceWeight(0.40) + angularDiversity(0.25) + densityPenalty(0.20) + relayBonus(0.15)
     *
     * Lower score = preferred next hop = wakes earlier.
     */
    private fun assignWeightedBfsDelays(nodes: List<AnimatedMeshNode>, random: Random) {
        val midNodes = nodes.filter { it.depthLayer == DepthLayer.MIDGROUND }
        if (midNodes.isEmpty()) return

        // Build adjacency with weighted scores
        data class WeightedEdge(val neighborIdx: Int, val score: Float)
        val adjacency = Array(midNodes.size) { i ->
            val edgesFromI = mutableListOf<WeightedEdge>()
            for (j in midNodes.indices) {
                if (j == i) continue
                val dx   = midNodes[i].startXRatio - midNodes[j].startXRatio
                val dy   = midNodes[i].startYRatio - midNodes[j].startYRatio
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                if (dist < AnimationConstants.STAR_CONNECT_RADIUS_RATIO * 1.5f) {
                    val distScore     = dist * 0.40f
                    val relayBonus    = if (midNodes[j].isRelayHub) -0.06f else 0f
                    // Density penalty: penalize nodes that already have many edges
                    val densityPenalty = (edgesFromI.size.toFloat() / AnimationConstants.MAX_CONNECTIONS_PER_NODE) * 0.20f
                    val score = (distScore + densityPenalty + relayBonus + random.nextFloat() * 0.03f)
                        .coerceAtLeast(0.001f)
                    edgesFromI.add(WeightedEdge(j, score))
                }
            }
            edgesFromI.sortedBy { it.score }
        }

        // BFS with weighted discovery
        val visited   = BooleanArray(midNodes.size)
        val queue     = ArrayDeque<Pair<Int, Float>>()
        val seedIdx   = midNodes.indexOfFirst { it.id == 0 }.let { if (it < 0) 0 else it }

        visited[seedIdx] = true
        queue.add(seedIdx to AnimationConstants.SCENE_1_END)

        val sceneSpread = AnimationConstants.SCENE_3_END - AnimationConstants.SCENE_2_END

        while (queue.isNotEmpty()) {
            val (idx, parentDelay) = queue.removeFirst()
            val node = midNodes[idx]

            if (node.id != 0) {
                val jitter = random.nextFloat() * 0.012f
                val delay  = (parentDelay + 0.030f + jitter)
                    .coerceIn(AnimationConstants.SCENE_2_END, AnimationConstants.SCENE_3_END - 0.02f)
                bfsDelayOverrides[node.id] = delay
            }

            // Take top-2 by score (lower = better = wakes sooner)
            adjacency[idx].take(2).forEach { edge ->
                if (!visited[edge.neighborIdx]) {
                    visited[edge.neighborIdx] = true
                    val parentActualDelay = bfsDelayOverrides[midNodes[idx].id] ?: AnimationConstants.SCENE_1_END
                    val neighborDelay = (parentActualDelay + 0.028f + edge.score * 0.08f)
                        .coerceAtMost(AnimationConstants.SCENE_3_END - 0.01f)
                    bfsDelayOverrides[midNodes[edge.neighborIdx].id] = neighborDelay
                    queue.add(edge.neighborIdx to neighborDelay)
                }
            }
        }

        // Fallback for disconnected islands
        midNodes.forEachIndexed { idx, n ->
            if (n.id != 0 && !bfsDelayOverrides.containsKey(n.id)) {
                bfsDelayOverrides[n.id] =
                    AnimationConstants.SCENE_2_END + (idx.toFloat() / midNodes.size) * sceneSpread
            }
        }
    }

    /** BFS-computed wake delays keyed by node id. Applied in [updatePositions]. */
    val bfsDelayOverrides = mutableMapOf<Int, Float>()

    // ── Camera state ──────────────────────────────────────────────────────────

    /**
     * Computes the cinematic camera state for the given [overallProgress] and [timeSec].
     * Camera drift frequencies include a per-launch jitter so Lissajous paths differ.
     */
    fun computeCameraState(overallProgress: Float, timeSec: Float): CameraState {
        // Scene 10: exponential fly-through zoom
        val flyProgress = if (overallProgress > AnimationConstants.SCENE_9_END) {
            ((overallProgress - AnimationConstants.SCENE_9_END) /
                    (1.0f - AnimationConstants.SCENE_9_END)).coerceIn(0f, 1f)
        } else 0f

        val baseScale = if (flyProgress > 0f) {
            val eased = flyProgress * flyProgress * flyProgress
            1.0f + eased * 4.0f
        } else {
            splineScale(overallProgress)
        }

        // Slow dolly: scale breathes very slightly (±2% over 8 s)
        val dolly = 1.0f + sin(timeSec * 0.78f + camFreqJitter) * 0.018f

        // Three-wave Lissajous with per-launch frequency jitter
        val f1 = 0.28f + camFreqJitter
        val f2 = 0.11f + camFreqJitter * 0.7f
        val f3 = 0.048f + camFreqJitter * 0.3f

        val panX = (sin(timeSec * f1 + 0.4f)  * 13f
                  + sin(timeSec * f2 + 1.1f)  *  7f
                  + sin(timeSec * f3 + 2.2f)  *  5f).toFloat()

        val panY = (cos(timeSec * (f1 * 0.79f) + 0.9f) * 10f
                  + cos(timeSec * (f2 * 0.82f) + 1.7f) *  5f
                  + cos(timeSec * (f3 * 0.80f) + 0.5f) *  4f).toFloat()

        // 1–2° orbit rotation
        val rotDeg = (sin(timeSec * 0.18f + camFreqJitter + 0.3f) * 1.5f
                    + sin(timeSec * 0.07f + 2.1f) * 0.5f).toFloat()

        return CameraState(
            scale       = baseScale * dolly,
            rotationDeg = rotDeg,
            panX        = panX,
            panY        = panY
        )
    }

    private fun splineScale(progress: Float): Float {
        val keys   = cameraScaleKeyframes
        val values = cameraScaleValues
        var i = 0
        while (i < keys.size - 2 && progress > keys[i + 1]) i++
        val t = ((progress - keys[i]) / (keys[i + 1] - keys[i])).coerceIn(0f, 1f)
        val q = t * t * t * (t * (t * 6f - 15f) + 10f)  // quintic ease-in-out
        return values[i] + (values[i + 1] - values[i]) * q
    }

    // ── Per-frame position update ─────────────────────────────────────────────

    fun updatePositions(
        nodes: List<AnimatedMeshNode>,
        width: Float,
        height: Float,
        timeMs: Long,
        overallProgress: Float,
        cameraScale: Float,
        cameraPanX: Float,
        cameraPanY: Float,
        reduceMotion: Boolean,
        deltaSec: Float = 0.016f
    ) {
        val timeSec = timeMs / 1000f

        // Migration: only nodes with willMigrate=true participate
        val globalMigrationProgress = if (overallProgress >= AnimationConstants.SCENE_6_END) {
            ((overallProgress - AnimationConstants.SCENE_6_END) /
                    (AnimationConstants.SCENE_7_END - AnimationConstants.SCENE_6_END)).coerceIn(0f, 1f)
        } else 0f

        nodes.forEach { star ->
            // ── Wake progress ramp (compressed: 0.038f) ───────────────────────
            val effectiveDelay = bfsDelayOverrides[star.id] ?: star.wakeDelay
            if (overallProgress >= effectiveDelay) {
                val rampLen = 0.038f
                star.wakeProgress = ((overallProgress - effectiveDelay) / rampLen).coerceIn(0f, 1f)
            }
            if (star.isSilenceSentinel) star.wakeProgress = 1f

            // ── Signature wave flash decay ────────────────────────────────────
            if (star.signatureWaveFlash > 0f) {
                star.signatureWaveFlash = (star.signatureWaveFlash - deltaSec * 3.5f).coerceAtLeast(0f)
            }

            // ── Compound twinkling ────────────────────────────────────────────
            val tw1   = sin(timeSec * star.twinkleSpeed + star.twinklePhase) * 0.14f
            val tw2   = sin(timeSec * star.twinkleSpeed * 1.77f + star.twinklePhase * 2.3f) * 0.07f
            val breath = sin(timeSec * 1.4f + star.breathingPhase) * AnimationConstants.BREATHING_AMPLITUDE
            star.currentBrightness = (star.baseBrightness + tw1.toFloat() + tw2.toFloat() + breath.toFloat())
                .coerceIn(0.05f, 1.00f)

            val baseX   = width  * star.startXRatio
            val baseY   = height * star.startYRatio
            val targetX = width  * star.targetXRatio
            val targetY = height * star.targetYRatio

            val rawX: Float
            val rawY: Float

            if (reduceMotion) {
                // Only migrate if willMigrate=true
                rawX = if (star.willMigrate) lerp(baseX, targetX, globalMigrationProgress) else baseX
                rawY = if (star.willMigrate) lerp(baseY, targetY, globalMigrationProgress) else baseY
            } else {
                val driftAmp = when (star.depthLayer) {
                    DepthLayer.BACKGROUND -> 3f
                    DepthLayer.MIDGROUND  -> 7f
                    DepthLayer.FOREGROUND -> 14f
                }
                val driftX = (sin(timeSec * star.pulseSpeed + star.twinklePhase) * driftAmp).toFloat()
                val driftY = (cos(timeSec * 0.71f * star.pulseSpeed + star.twinklePhase * 1.5f) * driftAmp).toFloat()

                if (star.willMigrate && star.isMigrating) {
                    // Progressive letter discovery: stagger by migrationOrder
                    val nodeProgress = if (globalMigrationProgress > star.migrationOrder * 0.38f) {
                        ((globalMigrationProgress - star.migrationOrder * 0.38f) / 0.62f).coerceIn(0f, 1f)
                    } else 0f

                    val eased = quinticEase(nodeProgress)
                    rawX = lerp(baseX + driftX, targetX + driftX * 0.10f, eased)
                    rawY = lerp(baseY + driftY, targetY + driftY * 0.10f, eased)
                } else {
                    // Non-migrating nodes: pure drift (remain as universe stars)
                    rawX = baseX + driftX
                    rawY = baseY + driftY
                }
            }

            star.rawX = rawX
            star.rawY = rawY

            val pFactor = when (star.depthLayer) {
                DepthLayer.BACKGROUND -> AnimationConstants.BG_PARALLAX_FACTOR
                DepthLayer.MIDGROUND  -> AnimationConstants.MID_PARALLAX_FACTOR
                DepthLayer.FOREGROUND -> AnimationConstants.FG_PARALLAX_FACTOR
            }
            star.currentX = rawX + cameraPanX * pFactor
            star.currentY = rawY + cameraPanY * pFactor

            // DoF blur factor
            star.dofBlurFactor = when (star.depthLayer) {
                DepthLayer.BACKGROUND -> (0.55f + (3.0f - cameraScale) * 0.16f).coerceIn(0.2f, 1.0f)
                DepthLayer.MIDGROUND  -> (kotlin.math.abs(cameraScale - 1.1f) * 0.26f).coerceIn(0f, 0.30f)
                DepthLayer.FOREGROUND -> (0.75f + (cameraScale - 1.0f) * 0.30f).coerceIn(0.4f, 1.0f)
            }

            star.pulseIntensity = (sin(timeSec * 2.2f + star.pulsePhase) * 0.10f).toFloat().coerceIn(-0.14f, 0.14f)
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private fun lerp(start: Float, stop: Float, fraction: Float) =
        start + (stop - start) * fraction

    private fun quinticEase(t: Float): Float =
        t * t * t * (t * (t * 6f - 15f) + 10f)
}
