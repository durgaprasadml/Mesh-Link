package com.meshlink.stress

import com.meshlink.simulator.core.SimulationEnvironment
import java.util.Random

class FaultInjector(
    private val env: SimulationEnvironment,
    private val seed: Long = 42L
) {
    private val random = Random(seed)

    /**
     * Randomly crashes nodes based on probability.
     */
    fun injectRandomCrashes(probability: Double = 0.05) {
        env.nodes.forEach { node ->
            if (random.nextDouble() < probability) {
                env.crash(node.meshId)
            }
        }
    }

    /**
     * Simulates route flapping by disabling random links.
     */
    fun injectLinkFlapping(probability: Double = 0.1) {
        env.allLinks().forEach { link ->
            if (random.nextDouble() < probability) {
                if (link.isEnabled) {
                    env.disableLink(link.fromNodeId, link.toNodeId)
                } else {
                    env.enableLink(link.fromNodeId, link.toNodeId)
                }
            }
        }
    }

    /**
     * Simulates a network partition by splitting nodes into two groups
     * and severing all links between them.
     */
    fun createNetworkPartition(groupA: List<String>, groupB: List<String>) {
        env.partition(groupA, groupB)
    }

    /**
     * Restores a previously created network partition.
     */
    fun healNetworkPartition(groupA: List<String>, groupB: List<String>) {
        env.heal(groupA, groupB)
    }
}
