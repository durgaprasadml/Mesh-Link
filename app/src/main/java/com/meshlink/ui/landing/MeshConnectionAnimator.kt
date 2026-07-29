package com.meshlink.ui.landing

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Connection manager: weighted BFS discovery, heartbeat graph propagation,
 * Mesh Link signature packet reversal — v4.
 *
 * Key changes from v3:
 *  1. Weighted neighbor selection (4-factor score) for organic, non-symmetric topology.
 *  2. [HeartbeatWave] drives per-node [signatureWaveFlash] staggered acknowledgements.
 *  3. Mesh Link signature: at [PACKET_REVERSAL_START] all packets set [isReversing = true]
 *     and converge on the reversal target relay hub, which then emits the heartbeat wave.
 *  4. [buildConnections] randomizes relay discovery order and skips over-connected nodes.
 */
class MeshConnectionAnimator {

    val links            = mutableListOf<MeshConnectionLink>()
    val microConnections = mutableListOf<MicroConnection>()

    val signatureWave    = RadialSignatureWave()
    val heartbeatWave    = HeartbeatWave()
    val rippleWave       = ConstellationRippleWave()
    val echoWave         = ConstellationRippleWave()

    var isSignatureFlashActive  = false
    var signatureFlashIntensity = 0f

    // Launch-seed jitter for wave origin (set in buildConnections)
    private var waveJitterX = 0f
    private var waveJitterY = 0f

    // Id of the packet reversal convergence target
    private var reversalTargetId = -1

    // Tracks whether the heartbeat wave has been triggered this session
    private var heartbeatTriggered = false
    private var reversalStarted    = false

    // ── Connection graph construction ─────────────────────────────────────────

    fun buildConnections(nodes: List<AnimatedMeshNode>, launchSeed: Long = System.currentTimeMillis()) {
        links.clear()
        microConnections.clear()
        heartbeatTriggered = false
        reversalStarted    = false

        if (nodes.isEmpty()) return
        val random = Random(launchSeed)

        // Per-launch wave origin jitter
        waveJitterX = (random.nextFloat() - 0.5f) * 2f * AnimationConstants.SIGNATURE_ORIGIN_JITTER
        waveJitterY = (random.nextFloat() - 0.5f) * 2f * AnimationConstants.SIGNATURE_ORIGIN_JITTER

        reversalTargetId = nodes.firstOrNull { it.isPacketReversalTarget }?.id ?: 4

        val layout    = ConstellationTextLayout.generateLayout()
        val textEdges = layout.edges

        // ── 1. Constellation skeleton strokes (appear in letter-order during Scene 7) ──
        textEdges.forEachIndexed { idx, (pA, pB) ->
            val n1 = nodes.getOrNull(pA + 1) ?: return@forEachIndexed
            val n2 = nodes.getOrNull(pB + 1) ?: return@forEachIndexed

            // Only draw if both nodes will actually migrate
            if (!n1.willMigrate || !n2.willMigrate) return@forEachIndexed

            val dx   = n1.startXRatio - n2.startXRatio
            val dy   = n1.startYRatio - n2.startYRatio
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            val migOrder    = n1.migrationOrder.coerceAtLeast(n2.migrationOrder)
            val appearDelay = AnimationConstants.SCENE_6_END + migOrder * 0.14f

            links.add(
                MeshConnectionLink(
                    nodeA                = n1,
                    nodeB                = n2,
                    distance             = dist,
                    type                 = ConnectionType.CONSTELLATION,
                    appearanceDelay      = appearDelay,
                    isConstellationStroke = true,
                    breathingPhase       = (idx * 0.38f) % (2f * PI.toFloat()),
                    letterIndex          = n1.migrationOrder.toInt().coerceIn(0, 16)
                )
            )
        }

        // ── 2. Weighted BFS organic discovery links (Scenes 2–3) ─────────────
        // 4-factor weighted scoring: distance(0.40) + angular_diversity(0.25)
        //                          + density_penalty(0.20) + relay_bonus(0.15)
        val nodeCount      = nodes.size
        val connectedMap   = mutableMapOf<Int, MutableList<Int>>()
        val linkAngles     = mutableMapOf<Int, MutableList<Float>>()   // existing link angles per node
        val visited        = BooleanArray(nodeCount)
        val queue          = ArrayDeque<Pair<Int, Float>>()

        // Randomize initial BFS seed: not always node 0 but always within relay hubs
        val relayNodes = nodes.filter { it.isRelayHub && it.depthLayer == DepthLayer.MIDGROUND }
        val initNode   = relayNodes.randomOrNull() ?: nodes[0]
        val initIdx    = nodes.indexOf(initNode).coerceAtLeast(0)

        visited[initIdx] = true
        queue.add(initIdx to AnimationConstants.SCENE_1_END)

        val maxConn = AnimationConstants.MAX_CONNECTIONS_PER_NODE

        while (queue.isNotEmpty()) {
            val (currentId, parentDelay) = queue.removeFirst()
            val currentNode = nodes[currentId]

            // Compute weighted scores for all nearby candidates
            val candidates = mutableListOf<Triple<Int, Float, Float>>()  // (idx, dist, score)

            for (j in 0 until nodeCount) {
                if (j == currentId) continue
                val target = nodes[j]
                if (target.depthLayer != DepthLayer.MIDGROUND) continue

                val dx   = currentNode.startXRatio - target.startXRatio
                val dy   = currentNode.startYRatio - target.startYRatio
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                if (dist > AnimationConstants.STAR_CONNECT_RADIUS_RATIO) continue

                // 1. Distance weight
                val distScore = dist * 0.40f

                // 2. Angular diversity — prefer directions not yet covered
                val angle          = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                val existingAngles = linkAngles[currentId]
                val angularScore   = if (existingAngles.isNullOrEmpty()) 0f else {
                    val minAngDiff = existingAngles.minOf { a ->
                        val diff = kotlin.math.abs(a - angle)
                        minOf(diff, (2f * PI - diff).toFloat())
                    }
                    // Reward large angular gaps (higher diversity = lower penalty)
                    -(minAngDiff / PI.toFloat()) * 0.25f   // negative = reward
                }

                // 3. Density penalty — avoid over-connected nodes
                val density       = (connectedMap[j]?.size ?: 0).toFloat() / maxConn
                val densityPenalty = density * 0.20f

                // 4. Relay hub bonus
                val relayBonus = if (target.isRelayHub) -0.08f else 0f

                val score = (distScore + angularScore + densityPenalty + relayBonus
                        + random.nextFloat() * 0.025f).coerceAtLeast(0.001f)
                candidates.add(Triple(j, dist, score))
            }

            candidates.sortBy { it.third }

            val maxStep = if (currentNode.isRelayHub) 3 else 2
            var taken   = 0

            for ((neighborIdx, distRaw, score) in candidates) {
                val existing = connectedMap[currentId]?.size ?: 0
                if (existing >= maxConn || taken >= maxStep) break
                if (connectedMap[currentId]?.contains(neighborIdx) == true) continue

                connectedMap.getOrPut(currentId) { mutableListOf() }.add(neighborIdx)
                connectedMap.getOrPut(neighborIdx) { mutableListOf() }.add(currentId)

                // Track link angles for future angular diversity scoring
                val dx = (currentNode.startXRatio - nodes[neighborIdx].startXRatio).toDouble()
                val dy = (currentNode.startYRatio - nodes[neighborIdx].startYRatio).toDouble()
                linkAngles.getOrPut(currentId) { mutableListOf() }.add(atan2(dy, dx).toFloat())
                linkAngles.getOrPut(neighborIdx) { mutableListOf() }.add(atan2(-dy, -dx).toFloat())

                val linkDelay = (parentDelay + 0.035f + distRaw * 0.22f + random.nextFloat() * 0.015f)
                    .coerceIn(AnimationConstants.SCENE_2_END, AnimationConstants.SCENE_3_END)

                links.add(
                    MeshConnectionLink(
                        nodeA           = currentNode,
                        nodeB           = nodes[neighborIdx],
                        distance        = score,
                        type            = ConnectionType.DISCOVERY,
                        appearanceDelay = linkDelay,
                        breathingPhase  = random.nextFloat() * 2f * PI.toFloat()
                    )
                )
                taken++

                if (!visited[neighborIdx]) {
                    visited[neighborIdx] = true
                    val nextDelay = (parentDelay + 0.032f + distRaw * 0.18f)
                        .coerceAtMost(AnimationConstants.SCENE_3_END)
                    queue.add(neighborIdx to nextDelay)
                }
            }
        }

        // ── 3. Long-distance relay links ──────────────────────────────────────
        val relayHubs = nodes.filter { it.isRelayHub }
        for (i in relayHubs.indices) {
            val rA = relayHubs[i]
            val rB = relayHubs[(i + 2) % relayHubs.size]
            val dx = rA.startXRatio - rB.startXRatio
            val dy = rA.startYRatio - rB.startYRatio
            val d  = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            links.add(
                MeshConnectionLink(
                    nodeA           = rA,
                    nodeB           = rB,
                    distance        = d,
                    type            = ConnectionType.MULTI_HOP_RELAY,
                    appearanceDelay = AnimationConstants.SCENE_3_END + 0.05f * i,
                    isMultiHopRelay = true,
                    breathingPhase  = i * 0.65f
                )
            )
        }

        // ── 4. Background micro-connections ───────────────────────────────────
        val bgNodes = nodes.filter { it.depthLayer == DepthLayer.BACKGROUND }
        for (i in 0..10) {
            val nA = bgNodes.getOrNull(i * 4)?.id ?: 0
            val nB = bgNodes.getOrNull(i * 4 + 2)?.id ?: 1
            microConnections.add(MicroConnection(id = i, nodeAId = nA, nodeBId = nB))
        }
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    fun update(
        overallProgress: Float,
        packets: List<DataPacket>,
        nodes: List<AnimatedMeshNode>,
        deltaSec: Float,
        timeMs: Long
    ) {
        val timeSec = timeMs / 1000f

        // ── 1. Plasma connection growth and breathing ─────────────────────────
        links.forEach { link ->
            if (overallProgress >= link.appearanceDelay) {
                link.nodeA.isDiscovered = true
                link.nodeB.isDiscovered = true

                if (link.growthProgress < 1.0f) {
                    val target = ((overallProgress - link.appearanceDelay) / 0.080f).coerceIn(0f, 1f)
                    link.growthProgress += (target - link.growthProgress) * 0.25f
                    link.plasmaStage = when {
                        link.growthProgress < 0.18f -> PlasmaGrowthStage.SPARK
                        link.growthProgress < 0.65f -> PlasmaGrowthStage.STREAK
                        link.growthProgress < 0.92f -> PlasmaGrowthStage.FULL_CONNECTION
                        else                        -> PlasmaGrowthStage.STABILIZING
                    }
                } else {
                    link.growthProgress = 1.0f
                    link.plasmaStage    = PlasmaGrowthStage.STABLE_GLOW
                }

                val breath   = sin(timeSec * 1.85f + link.breathingPhase) * AnimationConstants.BREATHING_AMPLITUDE
                val baseAlpha = when {
                    link.isConstellationStroke -> 0.85f
                    link.isMultiHopRelay       -> 0.28f
                    else                       -> 0.38f
                }
                link.alpha = (link.growthProgress * baseAlpha + breath.toFloat()).coerceIn(0f, 1f)

                // Heartbeat wave flash
                val midX = (link.nodeA.startXRatio + link.nodeB.startXRatio) * 0.5f
                val midY = (link.nodeA.startYRatio + link.nodeB.startYRatio) * 0.5f
                val overlap = signatureWave.nodeOverlap(midX, midY)
                if (overlap > 0.04f) {
                    link.energyPulseBrightness = link.energyPulseBrightness.coerceAtLeast(overlap * 0.85f)
                }

                // Combined node flash boost
                val nodeFlash = ((link.nodeA.signatureWaveFlash + link.nodeB.signatureWaveFlash) * 0.5f)
                    .coerceIn(0f, 1f)
                if (nodeFlash > 0f) {
                    link.energyPulseBrightness = link.energyPulseBrightness.coerceAtLeast(nodeFlash * 0.7f)
                }

                if (link.energyPulseBrightness > 0f) {
                    link.energyPulseBrightness = (link.energyPulseBrightness - deltaSec * 2.2f).coerceAtLeast(0f)
                }
            } else {
                link.growthProgress = 0f
                link.alpha          = 0f
            }
        }

        // ── 2. Mesh Link Signature: packet reversal ───────────────────────────
        if (!reversalStarted && overallProgress >= AnimationConstants.PACKET_REVERSAL_START) {
            reversalStarted = true
            packets.forEach { packet ->
                if (packet.isActive && packet.state == PacketState.TRAVELING) {
                    packet.isReversing = true
                    packet.toNodeId    = reversalTargetId
                    packet.state       = PacketState.CONVERGING
                }
            }
        }

        // ── 3. Signature flash intensity ──────────────────────────────────────
        if (overallProgress in AnimationConstants.SIGNATURE_CONVERGENCE_START..AnimationConstants.SIGNATURE_FLASH_FADE) {
            isSignatureFlashActive = true
            signatureFlashIntensity = if (overallProgress <= AnimationConstants.SIGNATURE_FLASH_PEAK) {
                ((overallProgress - AnimationConstants.SIGNATURE_CONVERGENCE_START) /
                        (AnimationConstants.SIGNATURE_FLASH_PEAK - AnimationConstants.SIGNATURE_CONVERGENCE_START))
                    .coerceIn(0f, 1f)
            } else {
                1f - ((overallProgress - AnimationConstants.SIGNATURE_FLASH_PEAK) /
                        (AnimationConstants.SIGNATURE_FLASH_FADE - AnimationConstants.SIGNATURE_FLASH_PEAK))
                    .coerceIn(0f, 1f)
            }
        } else {
            isSignatureFlashActive  = false
            signatureFlashIntensity = 0f
        }

        // ── 4. Radial wave (visual ring) trigger/update ───────────────────────
        if (overallProgress >= AnimationConstants.SIGNATURE_CONVERGENCE_START
            && !signatureWave.isActive
            && overallProgress < AnimationConstants.SIGNATURE_FLASH_FADE) {
            signatureWave.trigger(jitterX = waveJitterX, jitterY = waveJitterY)
        }
        signatureWave.update(deltaSec)

        // ── 5. HeartbeatWave (graph-propagated per-node flash) ───────────────
        if (overallProgress >= AnimationConstants.SIGNATURE_FLASH_PEAK - 0.005f
            && !heartbeatTriggered && nodes.isNotEmpty()) {
            heartbeatTriggered = true
            val origin = nodes.firstOrNull { it.isPacketReversalTarget }
                ?: nodes.firstOrNull { it.isRelayHub && it.isDiscovered }
                ?: nodes[0]
            heartbeatWave.trigger(origin.id, nodes, links)
        }
        heartbeatWave.update(deltaSec, nodes)

        // ── 6. Legacy ripple / echo waves ─────────────────────────────────────
        if (overallProgress > AnimationConstants.SCENE_8_END) {
            if (!rippleWave.isActive && overallProgress < 0.89f) {
                rippleWave.trigger(0.5f, 0.46f, isEchoWave = false)
            }
            if (!echoWave.isActive && overallProgress in 0.91f..0.94f) {
                echoWave.trigger(0.5f, 0.46f, isEchoWave = true)
            }
            rippleWave.update(deltaSec)
            echoWave.update(deltaSec)
        }

        // ── 7. Background micro-connections ──────────────────────────────────
        if (overallProgress in AnimationConstants.SCENE_2_END..AnimationConstants.PHASE_6_FINAL_PULSE_ZOOM_END) {
            microConnections.forEachIndexed { idx, micro ->
                if (!micro.isActive && (timeSec.toInt() + idx) % 4 == 0) {
                    val bgNodes = nodes.filter { it.depthLayer == DepthLayer.BACKGROUND }
                    val nA = bgNodes.getOrNull((idx * 5) % bgNodes.size.coerceAtLeast(1))?.id ?: 0
                    val nB = bgNodes.getOrNull((idx * 5 + 3) % bgNodes.size.coerceAtLeast(1))?.id ?: 1
                    micro.reset(nA, nB, lifespan = 2.0f + (idx % 3) * 0.40f, alpha = 0.16f)
                }
                micro.update(deltaSec)
            }
        }

        // ── 8. Data packet routing ────────────────────────────────────────────
        packets.forEach { packet ->
            if (overallProgress >= packet.delayProgress
                && overallProgress < AnimationConstants.PHASE_6_FINAL_PULSE_ZOOM_END) {

                packet.isActive = true

                when {
                    packet.isReversing -> {
                        // Travel toward reversalTargetId
                        val targetNode = nodes.getOrNull(reversalTargetId)
                        if (targetNode != null) {
                            packet.progress += deltaSec * packet.baseSpeed * packet.reversalSpeedMult * 0.80f
                            if (packet.progress >= 1.0f) {
                                packet.progress = 0f
                                packet.fromNodeId = packet.toNodeId
                                // Once arrived at target, fade out
                                if (packet.toNodeId == reversalTargetId) {
                                    packet.state  = PacketState.FADING_OUT
                                    packet.fadeAlpha = 1.0f
                                }
                            }
                        }
                    }
                    packet.state == PacketState.FADING_OUT -> {
                        packet.fadeAlpha = (packet.fadeAlpha - deltaSec * 1.2f).coerceAtLeast(0f)
                        if (packet.fadeAlpha <= 0f) packet.isActive = false
                    }
                    packet.state == PacketState.PAUSED_AT_RELAY -> {
                        packet.pauseTimerSec += deltaSec
                        if (packet.pauseTimerSec >= 0.20f) {
                            packet.state = if (packet.willDisappear && packet.pauseTimerSec > 0.32f) {
                                PacketState.FADING_OUT
                            } else {
                                PacketState.TRAVELING
                            }
                            packet.pauseTimerSec = 0f
                        }
                    }
                    else -> {
                        packet.progress += deltaSec * packet.currentSpeed * 0.75f
                        if (packet.progress >= 1.0f) {
                            packet.progress = 0f
                            val targetNode = nodes.getOrNull(packet.toNodeId)
                            if (targetNode != null && targetNode.isRelayHub) {
                                packet.state = PacketState.PAUSED_AT_RELAY
                                packet.pauseTimerSec = 0f
                            }
                            // Pick next hop from active links
                            val matchingLinks = links.filter {
                                it.alpha > 0.22f &&
                                (it.nodeA.id == packet.toNodeId || it.nodeB.id == packet.toNodeId)
                            }
                            if (matchingLinks.isNotEmpty()) {
                                val chosen = matchingLinks[(packet.id + timeSec.toInt()) % matchingLinks.size]
                                chosen.energyPulseBrightness = 0.80f
                                packet.fromNodeId = packet.toNodeId
                                packet.toNodeId   = if (chosen.nodeA.id == packet.toNodeId)
                                    chosen.nodeB.id else chosen.nodeA.id
                            }
                        }
                    }
                }
            } else if (overallProgress < packet.delayProgress) {
                packet.isActive = false
            }
        }
    }
}

// ── Connection link data class ────────────────────────────────────────────────

class MeshConnectionLink(
    val nodeA: AnimatedMeshNode,
    val nodeB: AnimatedMeshNode,
    val distance: Float,
    var type: ConnectionType              = ConnectionType.CONNECTED,
    val appearanceDelay: Float            = 0.15f,
    val isConstellationStroke: Boolean    = false,
    val isMultiHopRelay: Boolean          = false,
    val breathingPhase: Float             = 0f,
    val letterIndex: Int                  = 0   // v4: for progressive title rendering
) {
    var growthProgress        = 0f
    var alpha                 = 0f
    var pulseProgress         = 0f
    var energyPulseBrightness = 0f
    var plasmaStage           = PlasmaGrowthStage.SPARK
}
