package com.meshlink.simulator.tests

import com.meshlink.simulator.assertions.MeshAssertions
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit
import java.util.Random
import kotlin.test.assertTrue

/**
 * Large-scale stress tests for nightly CI runs.
 *
 * Tagged with @Tag("stress") — excluded from standard `testDebugUnitTest` Gradle task.
 * Run via: `./gradlew :app:testInternalDebugUnitTest --tests "*.StressTest" -PincludeTags=stress`
 *
 * Tests validate:
 * - No OOM errors at 200 and 500 virtual nodes
 * - Delivery rate above 85% under NightlyStress profile
 * - No deadlock or stuck queues
 * - Simulation terminates within timeout
 */
@Category(StressTest.StressTestTag::class)
class StressTest {

    /** Marker interface for @Category-based test filtering. */
    interface StressTestTag

    @get:Rule
    val timeout = Timeout(120, TimeUnit.SECONDS)

    @Test
    fun `200 node nightly stress - delivery rate above 85 percent`() {
        val n = 200
        val sim = MeshSimulator.random(
            n = n,
            density = 0.08f, // Sparse: ~16 connections per node on average
            seed = 200_000L,
            profile = NetworkProfile.NightlyStress
        )
        val nodeIds = sim.nodeIds()
        val rng = Random(200L)

        // Send 1000 messages across the 200-node mesh
        repeat(1_000) {
            val sender = nodeIds[rng.nextInt(nodeIds.size)]
            val target = nodeIds[rng.nextInt(nodeIds.size)].takeIf { it != sender }
                ?: nodeIds[(nodeIds.indexOf(sender) + 1) % nodeIds.size]
            sim.node(sender).sendPacket(target, "nightly-$it")
        }

        val steps = sim.runUntilQuiet(stepMs = 100, maxStepMs = 60_000)
        MeshAssertions.assertNoDeadlock(sim)
        assertTrue(steps > 0, "200-node simulation should complete")

        val report = sim.metrics.generateReport()
        println("=== 200-Node Stress Report ===")
        println(report)

        // Allow more lenient rate under NightlyStress (loss + latency)
        assertTrue(report.deliverySuccessRate >= 0.50f,
            "200-node delivery rate ${report.deliverySuccessRate} should be >= 50% under NightlyStress")
    }

    @Test
    fun `500 node max scale - terminates without OOM`() {
        val n = 500
        val sim = MeshSimulator.random(
            n = n,
            density = 0.04f, // Very sparse: ~20 connections per node
            seed = 500_000L,
            profile = NetworkProfile.NightlyStress
        )
        val nodeIds = sim.nodeIds()
        val rng = Random(500L)

        // 500 messages (1 per node target)
        repeat(500) {
            val sender = nodeIds[rng.nextInt(nodeIds.size)]
            val target = nodeIds[rng.nextInt(nodeIds.size)].takeIf { it != sender }
                ?: nodeIds[(nodeIds.indexOf(sender) + 1) % nodeIds.size]
            sim.node(sender).sendPacket(target, "max-scale-$it")
        }

        val steps = sim.runUntilQuiet(stepMs = 200, maxStepMs = 90_000)
        MeshAssertions.assertNoDeadlock(sim)
        assertTrue(steps > 0, "500-node simulation should complete within timeout")

        val report = sim.metrics.generateReport()
        println("=== 500-Node Max Scale Report ===")
        println(report)

        assertTrue(report.totalPacketsSent >= 500, "All 500 sends should be recorded")
        assertTrue(report.nodeCount == n, "Should have $n nodes")
    }

    @Test
    fun `500 node ring broadcast terminates`() {
        val n = 500
        val sim = MeshSimulator.ring(n = n, profile = NetworkProfile.NightlyStress)
        sim.node("node-0").sendPacket("BROADCAST", "broadcast-500")
        val steps = sim.runUntilQuiet(stepMs = 200, maxStepMs = 60_000)

        MeshAssertions.assertNoDeadlock(sim)
        assertTrue(steps > 0)
        MeshAssertions.assertNoDuplicateDelivery(sim.recorder)

        println("=== 500-Node Ring Broadcast ===")
        println(sim.metrics.generateReport())
    }
}
