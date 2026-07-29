package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color
import kotlin.math.hypot

/**
 * Data structure representing a connection edge in the mesh graph.
 */
class MeshConnectionLink(
    val nodeA: AnimatedMeshNode,
    val nodeB: AnimatedMeshNode,
    val distance: Float,
    var type: ConnectionType = ConnectionType.CONNECTED,
    val appearanceDelay: Float = 0.25f
) {
    var growthProgress = 0f // 0.0 -> 1.0 (spring growth)
    var alpha = 0f
    var isSelfHealing = false
    var pulseProgress = 0f
}

/**
 * Connection manager computing progressive link growth, pulse propagation,
 * self-healing re-routing, and packet trajectories.
 */
class MeshConnectionAnimator {

    val links = mutableListOf<MeshConnectionLink>()
    val sparkPool = List(20) { DiscoverySpark() }

    fun buildConnections(nodes: List<AnimatedMeshNode>) {
        links.clear()
        val nodeCount = nodes.size

        for (i in 0 until nodeCount) {
            val n1 = nodes[i]
            var connectionsCount = 0

            for (j in i + 1 until nodeCount) {
                val n2 = nodes[j]
                val dx = n1.xRatio - n2.xRatio
                val dy = n1.yRatio - n2.yRatio
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                // Connect nodes if within distance threshold or if connecting to Central/Logo node
                if (dist < AnimationConstants.NODE_CONNECT_RADIUS_RATIO || n1.isUserNode || n2.isUserNode || (i <= 6 && j <= 6)) {
                    val delay = (n1.appearDelay.coerceAtLeast(n2.appearDelay)) + 0.08f
                    val type = when {
                        n1.isUserNode || n2.isUserNode -> ConnectionType.CONNECTED
                        (i + j) % 5 == 0 -> ConnectionType.RELAY
                        (i + j) % 7 == 0 -> ConnectionType.BROADCAST
                        (i + j) % 3 == 0 -> ConnectionType.DISCOVERY
                        else -> ConnectionType.CONNECTED
                    }

                    val link = MeshConnectionLink(
                        nodeA = n1,
                        nodeB = n2,
                        distance = dist,
                        type = type,
                        appearanceDelay = delay
                    )

                    // Mark a subset of links to participate in Phase 5 self-healing (re-routing)
                    if ((i * 3 + j) % 4 == 0) {
                        link.isSelfHealing = true
                    }

                    links.add(link)
                    connectionsCount++
                    if (connectionsCount >= AnimationConstants.MAX_CONNECTIONS_PER_NODE) break
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
            val n1Visible = link.nodeA.alpha > 0.1f
            val n2Visible = link.nodeB.alpha > 0.1f

            if (n1Visible && n2Visible && overallProgress >= link.appearanceDelay) {
                // Spring growth interpolation
                if (link.growthProgress < 1.0f) {
                    val targetGrowth = ((overallProgress - link.appearanceDelay) / 0.15f).coerceIn(0f, 1f)
                    link.growthProgress += (targetGrowth - link.growthProgress) * 0.18f
                } else {
                    link.growthProgress = 1.0f
                }

                // Phase 5 Self-Healing re-organization
                if (link.isSelfHealing && overallProgress in AnimationConstants.PHASE_4_CONNECTION_END..AnimationConstants.PHASE_6_PACKET_ROUTING_END) {
                    val healingPhase = (overallProgress - AnimationConstants.PHASE_4_CONNECTION_END) / (AnimationConstants.PHASE_6_PACKET_ROUTING_END - AnimationConstants.PHASE_4_CONNECTION_END)
                    val sinFade = kotlin.math.sin(healingPhase * Math.PI.toFloat()).toFloat()
                    link.alpha = 0.3f + 0.7f * (1f - sinFade * 0.7f)
                } else {
                    link.alpha = link.growthProgress
                }

                // Periodic line pulse
                link.pulseProgress = (link.pulseProgress + deltaSec * 0.8f) % 1.0f

                // Spawn discovery spark when link first reaches 90% growth
                if (link.growthProgress in 0.88f..0.92f && link.alpha > 0.8f) {
                    val spark = sparkPool.firstOrNull { !it.active }
                    spark?.spawn(
                        (link.nodeA.currentX + link.nodeB.currentX) / 2f,
                        (link.nodeA.currentY + link.nodeB.currentY) / 2f,
                        if (link.type == ConnectionType.RELAY) AnimationConstants.PurpleAccent else AnimationConstants.Cyan
                    )
                }
            } else {
                link.growthProgress = 0f
                link.alpha = 0f
            }
        }

        // 2. Update Discovery Sparks
        sparkPool.forEach { it.update(deltaSec) }

        // 3. Update Packet Trajectories along active links
        packets.forEach { packet ->
            if (overallProgress >= packet.delayProgress && overallProgress < AnimationConstants.PHASE_8_LOGO_EMERGENCE_END) {
                packet.isActive = true

                if (packet.isRelaying) {
                    packet.relayPauseTimer -= deltaSec
                    if (packet.relayPauseTimer <= 0f) {
                        packet.isRelaying = false
                    }
                } else {
                    packet.progress += deltaSec * packet.speed * 0.8f
                    if (packet.progress >= 1.0f) {
                        packet.progress = 0f

                        // 30% chance to pause at relay node (Phase 6 Packet Routing)
                        if (packet.id % 3 == 0) {
                            packet.isRelaying = true
                            packet.relayPauseTimer = 0.25f
                        }

                        // Pick next edge
                        val matchingLinks = links.filter {
                            it.alpha > 0.5f && (it.nodeA.id == packet.toNodeId || it.nodeB.id == packet.toNodeId)
                        }

                        if (matchingLinks.isNotEmpty()) {
                            val chosen = matchingLinks[packet.id % matchingLinks.size]
                            packet.fromNodeId = packet.toNodeId
                            packet.toNodeId = if (chosen.nodeA.id == packet.toNodeId) chosen.nodeB.id else chosen.nodeA.id
                        }
                    }
                }
            } else {
                packet.isActive = false
            }
        }
    }
}
