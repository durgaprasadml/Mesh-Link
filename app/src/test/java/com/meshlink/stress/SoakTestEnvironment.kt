package com.meshlink.stress

import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PacketPriority
import com.meshlink.simulator.core.SimulatedClock
import com.meshlink.simulator.core.SimulationEnvironment
import com.meshlink.simulator.metrics.NetworkRecorder
import com.meshlink.simulator.node.SimulatedNode
import com.meshlink.simulator.topology.TopologyBuilder
import com.meshlink.simulator.transport.SimulatedTransport
import java.io.File
import java.util.Random
import kotlin.time.Duration

class SoakTestEnvironment(
    private val scenario: StressScenario,
    private val outputDir: File = File("build/reports/stress")
) {

    fun runSoakTest(): SimulationEnvironment {
        val clock = SimulatedClock()
        val recorder = NetworkRecorder()
        val nodes = (1..scenario.nodeCount).map { i ->
            SimulatedNode("N$i", clock, recorder, SimulatedTransport("N$i", clock, com.meshlink.simulator.core.SimulationScheduler(clock), recorder, scenario.seed))
        }

        val links = TopologyBuilder.randomMesh(nodes.map { it.meshId }, density = 0.4f, seed = scenario.seed)
        val env = SimulationEnvironment.create(nodes, links, clock = clock, recorder = recorder)
        env.applyProfile(scenario.profile)

        val random = Random(scenario.seed)
        val nodeIds = env.nodeIds()
        
        val maxVirtualTimeMs = scenario.duration.inWholeMilliseconds
        val stepMs = 50L
        val monitorIntervalMs = 60_000L // every virtual minute
        var nextMonitorMs = monitorIntervalMs

        var totalActiveSessions = 0
        var totalActiveCoroutines = 0
        var maxObservedQueueSize = 0

        val maxQueueSize = 1000

        try {
            while (clock.currentTimeMs < maxVirtualTimeMs) {
                // Periodically inject traffic to keep network alive
                if (random.nextInt(100) < 5) {
                    val senderIndex = random.nextInt(nodeIds.size)
                    val targetIndex = (senderIndex + random.nextInt(nodeIds.size - 1) + 1) % nodeIds.size
                    env.node(nodeIds[senderIndex]).sendPacket(
                        nodeIds[targetIndex],
                        "Soak payload",
                        type = PacketType.TEXT,
                        priority = PacketPriority.NORMAL
                    )
                }
                
                env.step(stepMs)

                if (clock.currentTimeMs >= nextMonitorMs) {
                    nextMonitorMs += monitorIntervalMs
                    
                    // Resource monitoring
                    env.nodes.forEach { node ->
                        val qSize = node.queueOptimizer.size()
                        maxObservedQueueSize = maxOf(maxObservedQueueSize, qSize)

                        if (qSize > maxQueueSize) {
                            throw AssertionError("Queue size exceeded limits on node ${node.meshId}: $qSize")
                        }
                    }
                    // Additional bounded memory checks could be added here
                }
            }
            
            val collector = StressMetricsCollector(env.metrics)
            collector.evaluateAndReport(scenario)
            
        } catch (e: Exception) {
            persistFailureState(env, e)
            throw e
        } catch (e: AssertionError) {
            persistFailureState(env, e)
            throw e
        }
        
        return env
    }

    private fun persistFailureState(env: SimulationEnvironment, error: Throwable) {
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        val reportFile = File(outputDir, "soak_failure_${timestamp}.txt")
        val topologyFile = File(outputDir, "soak_topology_${timestamp}.json")
        
        reportFile.writeText("Error: ${error.message}\n\nScenario: $scenario\n\nMetrics:\n${env.metrics.generateReport()}")
        topologyFile.writeText(env.exportJson())
        
        println("Soak test failed. State persisted to ${outputDir.absolutePath}")
    }
}
