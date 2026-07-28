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

class StressScenarioRunner(
    private val scenario: StressScenario,
    private val outputDir: File = File("build/reports/stress")
) {

    fun run(): SimulationEnvironment {
        val clock = SimulatedClock()
        val recorder = NetworkRecorder()
        val scheduler = com.meshlink.simulator.core.SimulationScheduler(clock)
        val nodes = (1..scenario.nodeCount).map { i ->
            SimulatedNode("N$i", clock, recorder, SimulatedTransport("N$i", clock, scheduler, recorder, scenario.seed))
        }

        // We use a robust topology by default for stress
        val links = TopologyBuilder.randomMesh(nodes.map { it.meshId }, density = 0.4f, seed = scenario.seed)
        val env = SimulationEnvironment.create(nodes, links, clock = clock, scheduler = scheduler, recorder = recorder)
        env.applyProfile(scenario.profile)

        val random = Random(scenario.seed)
        val nodeIds = env.nodeIds()
        
        try {
            // Generate background traffic
            for (i in 1..scenario.messageCount) {
                val senderIndex = random.nextInt(nodeIds.size)
                var targetIndex = random.nextInt(nodeIds.size)
                while (targetIndex == senderIndex) {
                    targetIndex = random.nextInt(nodeIds.size)
                }
                
                val sender = env.node(nodeIds[senderIndex])
                val targetId = nodeIds[targetIndex]
                
                // Mix packet types
                val type = when (random.nextInt(100)) {
                    in 0..60 -> PacketType.TEXT
                    in 61..80 -> PacketType.MEDIA_CHUNK
                    in 81..95 -> PacketType.VOICE_FRAME
                    else -> PacketType.SOS
                }
                
                val priority = if (type == PacketType.SOS) PacketPriority.HIGH else PacketPriority.NORMAL
                sender.sendPacket(targetId, "Stress Payload $i", type = type, priority = priority)
                
                // Step a bit to interleave traffic
                if (i % scenario.concurrentSenders == 0) {
                    env.step(virtualMs = 50)
                }
            }

            // Run until completion or max timeout (1 min virtual time)
            env.runUntilQuiet(maxStepMs = scenario.duration.inWholeMilliseconds)
            
            val collector = StressMetricsCollector(env.metrics)
            collector.evaluateAndReport(scenario)
            
        } catch (e: AssertionError) {
            persistFailureState(env, e)
            throw e
        } catch (e: Exception) {
            persistFailureState(env, e)
            throw e
        }

        return env
    }

    private fun persistFailureState(env: SimulationEnvironment, error: Throwable) {
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        val reportFile = File(outputDir, "stress_failure_${timestamp}.txt")
        val timelineFile = File(outputDir, "stress_timeline_${timestamp}.json")
        val topologyFile = File(outputDir, "stress_topology_${timestamp}.json")
        
        reportFile.writeText("Error: ${error.message}\n\nScenario: $scenario\n\nMetrics:\n${env.metrics.generateReport()}")
        
        // Output topology
        topologyFile.writeText(env.exportJson())
        
        // Output timeline
        timelineFile.writeText("[\n" + env.recorder.allEvents().joinToString(",\n") { it.toString() } + "\n]")
        
        println("Stress test failed. State persisted to ${outputDir.absolutePath}")
    }
}
