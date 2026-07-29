package com.meshlink.ui.landing

import kotlin.math.hypot

/**
 * Data structure representing a connection edge in the constellation graph.
 */
class MeshConnectionLink(
    val nodeA: AnimatedMeshNode,
    val nodeB: AnimatedMeshNode,
    val distance: Float,
    var type: ConnectionType = ConnectionType.CONNECTED,
    val appearanceDelay: Float = 0.15f,
    val isConstellationStroke: Boolean = false
) {
    var growthProgress = 0f // 0.0 -> 1.0 (progressive line growth)
    var alpha = 0f
    var pulseProgress = 0f
    var energyPulseBrightness = 0f // Temporary line brighten when packet passes
}

/**
 * Connection manager computing organic wave discovery propagation,
 * constellation letter stroke edges, global cinematic energy pulses, and white packet trails.
 */
class MeshConnectionAnimator {

    val links = mutableListOf<MeshConnectionLink>()
    val rippleWave = ConstellationRippleWave()

    fun buildConnections(nodes: List<AnimatedMeshNode>) {
        links.clear()

        if (nodes.isEmpty()) return

        val constellationLayout = ConstellationTextLayout.generateLayout()
        val textEdges = constellationLayout.edges

        // 1. Build procedural constellation letter stroke links between migrating stars (nodes 1..N)
        textEdges.forEach { (pA, pB) ->
            val n1 = nodes.getOrNull(pA + 1)
            val n2 = nodes.getOrNull(pB + 1)

            if (n1 != null && n2 != null) {
                val dx = n1.startXRatio - n2.startXRatio
                val dy = n1.startYRatio - n2.startYRatio
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                val appearanceDelay = AnimationConstants.PHASE_3_WAVE_PROPAGATION_END + (n1.migrationOrder.coerceAtLeast(n2.migrationOrder)) * 0.18f

                links.add(
                    MeshConnectionLink(
                        nodeA = n1,
                        nodeB = n2,
                        distance = dist,
                        type = ConnectionType.CONSTELLATION,
                        appearanceDelay = appearanceDelay,
                        isConstellationStroke = true
                    )
                )
            }
        }

        // 2. Build organic wave discovery links between starfield nodes (Node A -> B, C; B -> D...)
        val nodeCount = nodes.size
        val connectedMap = mutableMapOf<Int, MutableList<Int>>()

        // Breadth-First Search / Wave propagation ordering from Seed Node 0
        val visited = BooleanArray(nodeCount)
        val queue = ArrayDeque<Pair<Int, Float>>() // Pair(nodeId, discoveryDelay)

        visited[0] = true
        queue.add(Pair(0, AnimationConstants.PHASE_1_TWINKLE_END))

        val maxNeighborConnections = AnimationConstants.MAX_CONNECTIONS_PER_NODE

        while (queue.isNotEmpty()) {
            val (currentId, currentDelay) = queue.removeFirst()
            val currentNode = nodes[currentId]

            // Find nearest unvisited / nearby neighbors
            val candidateNeighbors = mutableListOf<Pair<Int, Float>>()
            for (j in 0 until nodeCount) {
                if (j == currentId) continue
                val targetNode = nodes[j]

                val dx = currentNode.startXRatio - targetNode.startXRatio
                val dy = currentNode.startYRatio - targetNode.startYRatio
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                if (dist < AnimationConstants.STAR_CONNECT_RADIUS_RATIO) {
                    candidateNeighbors.add(Pair(j, dist))
                }
            }

            // Sort neighbors by proximity
            candidateNeighbors.sortBy { it.second }

            var connectionCount = 0
            for ((neighborId, dist) in candidateNeighbors) {
                val existingCount = connectedMap[currentId]?.size ?: 0
                if (existingCount >= maxNeighborConnections) break

                val isAlreadyConnected = connectedMap[currentId]?.contains(neighborId) == true
                if (!isAlreadyConnected) {
                    connectedMap.getOrPut(currentId) { mutableListOf() }.add(neighborId)
                    connectedMap.getOrPut(neighborId) { mutableListOf() }.add(currentId)

                    val delay = currentDelay + 0.05f + dist * 0.3f

                    links.add(
                        MeshConnectionLink(
                            nodeA = currentNode,
                            nodeB = nodes[neighborId],
                            distance = dist,
                            type = ConnectionType.DISCOVERY,
                            appearanceDelay = delay,
                            isConstellationStroke = false
                        )
                    )

                    connectionCount++
                }

                if (!visited[neighborId]) {
                    visited[neighborId] = true
                    queue.add(Pair(neighborId, currentDelay + 0.06f))
                }
            }
        }
    }

    fun update(
        overallProgress: Float,
        packets: List<DataPacket>,
        deltaSec: Float
    ) {
        // 1. Update Connection Links
        links.forEach { link ->
            if (overallProgress >= link.appearanceDelay) {
                link.nodeA.isDiscovered = true
                link.nodeB.isDiscovered = true

                // Spring/progressive line growth
                if (link.growthProgress < 1.0f) {
                    val targetGrowth = ((overallProgress - link.appearanceDelay) / 0.12f).coerceIn(0f, 1f)
                    link.growthProgress += (targetGrowth - link.growthProgress) * 0.20f
                } else {
                    link.growthProgress = 1.0f
                }

                link.alpha = link.growthProgress

                // Fade temporary energy pulse brightness on line
                if (link.energyPulseBrightness > 0f) {
                    link.energyPulseBrightness = (link.energyPulseBrightness - deltaSec * 2.5f).coerceAtLeast(0f)
                }

                // Global Cinematic Energy Pulse (Phase 5 -> Phase 6)
                if (overallProgress in AnimationConstants.PHASE_5_LIVING_CONSTELLATION_END..AnimationConstants.PHASE_6_FINAL_PULSE_ZOOM_END) {
                    val pulsePhaseProgress = (overallProgress - AnimationConstants.PHASE_5_LIVING_CONSTELLATION_END) / 0.10f
                    val waveDist = (pulsePhaseProgress * 1.5f)
                    val linkCenterDist = hypot((link.nodeA.startXRatio - 0.5f).toDouble(), (link.nodeA.startYRatio - 0.44f).toDouble()).toFloat()

                    if (kotlin.math.abs(linkCenterDist - waveDist) < 0.15f) {
                        link.energyPulseBrightness = 1.0f
                    }
                }
            } else {
                link.growthProgress = 0f
                link.alpha = 0f
            }
        }

        // 2. Update Ripple Wave
        if (overallProgress > AnimationConstants.PHASE_4_CONSTELLATION_MIGRATION_END) {
            if (!rippleWave.isActive && overallProgress in AnimationConstants.PHASE_5_LIVING_CONSTELLATION_END..0.92f) {
                rippleWave.trigger(0.5f, 0.45f)
            }
            rippleWave.update(deltaSec)
        }

        // 3. Update White Energy Packet Trajectories
        packets.forEach { packet ->
            if (overallProgress >= packet.delayProgress && overallProgress < AnimationConstants.PHASE_6_FINAL_PULSE_ZOOM_END) {
                packet.isActive = true

                packet.progress += deltaSec * packet.speed * 0.7f
                if (packet.progress >= 1.0f) {
                    packet.progress = 0f

                    // Pick next connected edge
                    val matchingLinks = links.filter {
                        it.alpha > 0.4f && (it.nodeA.id == packet.toNodeId || it.nodeB.id == packet.toNodeId)
                    }

                    if (matchingLinks.isNotEmpty()) {
                        val chosen = matchingLinks[packet.id % matchingLinks.size]
                        chosen.energyPulseBrightness = 0.8f // Brighten edge temporarily
                        packet.fromNodeId = packet.toNodeId
                        packet.toNodeId = if (chosen.nodeA.id == packet.toNodeId) chosen.nodeB.id else chosen.nodeA.id
                    }
                }
            } else {
                packet.isActive = false
            }
        }
    }
}
