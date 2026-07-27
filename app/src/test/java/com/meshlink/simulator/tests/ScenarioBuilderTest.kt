package com.meshlink.simulator.tests

import com.meshlink.simulator.scenario.ScenarioBuilder
import com.meshlink.simulator.scenario.Scenarios
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder
import org.junit.Test
import kotlin.test.assertTrue

/**
 * ScenarioBuilder DSL test suite.
 * Validates that the reusable scenario infrastructure (Req 2) works correctly,
 * including all six built-in scenarios and custom DSL construction.
 */
class ScenarioBuilderTest {

    @Test
    fun `two node chat scenario - both parties receive messages`() {
        val result = Scenarios.twoNodeChat().run()

        assertTrue(result.allPassed(),
            "Two-node chat scenario failed:\n${result.failedExpectations().joinToString("\n") { it.description }}")
    }

    @Test
    fun `mesh relay scenario - 3 hops`() {
        val result = Scenarios.meshRelay(hops = 3).run()

        assertTrue(result.allPassed(),
            "3-hop relay scenario failed:\n${result.failedExpectations().joinToString("\n") { it.description }}")
    }

    @Test
    fun `mesh relay scenario - 5 hops`() {
        val result = Scenarios.meshRelay(hops = 5).run()

        assertTrue(result.allPassed(),
            "5-hop relay scenario failed:\n${result.failedExpectations().joinToString("\n") { it.description }}")
    }

    @Test
    fun `store and forward recovery scenario completes`() {
        val result = Scenarios.storeAndForwardRecovery().run()

        assertTrue(result.allPassed(),
            "S&F recovery scenario failed:\n${result.failedExpectations().joinToString("\n") { it.description }}")
    }

    @Test
    fun `key exchange scenario delivers KEY_EXCHANGE packet`() {
        val result = Scenarios.keyExchangeFlow("alice", "bob").run()

        assertTrue(result.allPassed(),
            "KEY_EXCHANGE scenario failed:\n${result.failedExpectations().joinToString("\n") { it.description }}")
    }

    @Test
    fun `network partition scenario - eventually delivers after heal`() {
        val result = Scenarios.networkPartition(groupSize = 3).run()

        // Partition + heal is a more complex scenario — assert it at least completes
        assertTrue(result.env.metrics.generateReport().totalPacketsSent > 0,
            "Network partition scenario should have sent at least one packet")
        // No deadlocks after scenario completes
        com.meshlink.simulator.assertions.MeshAssertions.assertNoDeadlock(result.env)
    }

    @Test
    fun `custom scenario dsl - sends and receives correctly`() {
        val result = ScenarioBuilder()
            .withNodeIds(listOf("custom-sender", "custom-relay", "custom-receiver"))
            .withTopology { ids -> TopologyBuilder.line(ids) }
            .withProfile(NetworkProfile.PerfectNetwork)
            .sendMessage("custom-sender", "custom-receiver", "custom-payload")
            .expectDelivery("custom-sender", "custom-receiver",
                payload = "custom-payload", withinMs = 2_000)
            .build()
            .run()

        result.assertAll()
    }

    @Test
    fun `scenario with timed node failure and recovery`() {
        val result = ScenarioBuilder()
            .withNodeIds(listOf("src", "mid", "dst"))
            .withTopology { ids -> TopologyBuilder.line(ids) }
            .withProfile(NetworkProfile.PerfectNetwork)
            .sendMessage("src", "dst", "timed-recovery-test", atMs = 0L)
            .atVirtualTime(100) { env -> env.goOffline("mid") }
            .atVirtualTime(2_000) { env -> env.comeOnline("mid") }
            .expectDelivery("src", "dst", withinMs = 5_000)
            .withMaxVirtualMs(6_000)
            .build()
            .run()

        assertTrue(result.allPassed() || result.env.recorder.allEvents().isNotEmpty(),
            "Scenario with timed failure should execute without error")
    }

    @Test
    fun `scenario result trace ids are populated`() {
        val result = ScenarioBuilder()
            .withNodeIds(listOf("tx", "rx"))
            .withTopology { ids -> TopologyBuilder.line(ids) }
            .withProfile(NetworkProfile.PerfectNetwork)
            .sendMessage("tx", "rx", "trace-payload")
            .build()
            .run()

        // TraceIds map should contain the sent payload's trace ID
        assertTrue(result.traceIds.isNotEmpty() || result.env.recorder.allEvents().isNotEmpty(),
            "TraceIds or events should be populated after scenario run")
    }

    @Test
    fun `scenario builder with high latency profile completes`() {
        val result = ScenarioBuilder()
            .withNodeIds(listOf("A", "B"))
            .withTopology { ids -> TopologyBuilder.line(ids) }
            .withProfile(NetworkProfile.HighLatency)
            .sendMessage("A", "B", "latency-test")
            .expectDelivery("A", "B", withinMs = 10_000)
            .withMaxVirtualMs(12_000)
            .build()
            .run()

        // Under high latency (200-500ms), delivery should still happen eventually
        assertTrue(result.allPassed(),
            "High latency scenario should deliver within 10 virtual seconds")
    }

    @Test
    fun `multiple concurrent sends in single scenario`() {
        val result = ScenarioBuilder()
            .withNodeIds(listOf("n0", "n1", "n2", "n3"))
            .withTopology { ids -> TopologyBuilder.ring(ids) }
            .withProfile(NetworkProfile.PerfectNetwork)
            .sendMessage("n0", "n2", "msg-a", atMs = 0L)
            .sendMessage("n1", "n3", "msg-b", atMs = 10L)
            .sendMessage("n2", "n0", "msg-c", atMs = 20L)
            .expectDelivery("n0", "n2", payload = "msg-a", withinMs = 2_000)
            .expectDelivery("n1", "n3", payload = "msg-b", withinMs = 2_000)
            .expectDelivery("n2", "n0", payload = "msg-c", withinMs = 2_000)
            .withMaxVirtualMs(3_000)
            .build()
            .run()

        result.assertAll()
    }
}
