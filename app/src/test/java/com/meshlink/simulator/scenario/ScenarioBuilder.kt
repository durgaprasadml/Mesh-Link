package com.meshlink.simulator.scenario

import com.meshlink.domain.model.PacketType
import com.meshlink.simulator.core.SimulationEnvironment
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.topology.TopologyBuilder

/**
 * Reusable Scenario DSL for constructing and executing common network test scenarios.
 *
 * A [Scenario] defines a sequence of:
 * - Setup: nodes + topology + profile
 * - Actions: timed events scheduled via the virtual scheduler
 * - Expectations: delivery assertions evaluated after the run
 *
 * Usage:
 * ```kotlin
 * val result = ScenarioBuilder()
 *     .withNodes(5)
 *     .withTopology { ids -> TopologyBuilder.ring(ids) }
 *     .withProfile(NetworkProfile.HighLoss)
 *     .sendMessage(from = "node-0", to = "node-4", payload = "test")
 *     .atVirtualTime(1_000) { env -> env.goOffline("node-2") }
 *     .atVirtualTime(3_000) { env -> env.comeOnline("node-2") }
 *     .expectDelivery(from = "node-0", to = "node-4", withinMs = 5_000)
 *     .build()
 *     .run()
 * assertTrue(result.allPassed())
 * ```
 */
class ScenarioBuilder {

    private var nodeCount: Int = 2
    private var nodeIds: List<String>? = null
    private var topologyFactory: ((List<String>) -> List<com.meshlink.simulator.transport.Link>)? = null
    private var profile: NetworkProfile = NetworkProfile.PerfectNetwork
    private var maxVirtualMs: Long = 10_000L

    data class TimedAction(val atMs: Long, val action: (SimulationEnvironment) -> Unit)
    data class SendIntent(val fromNodeId: String, val toNodeId: String,
                         val payload: String, val type: PacketType = PacketType.TEXT,
                         val atMs: Long = 0L)
    data class DeliveryExpectation(val fromNodeId: String, val toNodeId: String,
                                   val packetPayload: String? = null, val withinMs: Long)

    private val timedActions = mutableListOf<TimedAction>()
    private val sendIntents = mutableListOf<SendIntent>()
    private val expectations = mutableListOf<DeliveryExpectation>()

    // ── DSL Methods ───────────────────────────────────────────────────────────────

    fun withNodes(n: Int): ScenarioBuilder { nodeCount = n; return this }
    fun withNodeIds(ids: List<String>): ScenarioBuilder { nodeIds = ids; return this }

    fun withTopology(factory: (List<String>) -> List<com.meshlink.simulator.transport.Link>): ScenarioBuilder {
        topologyFactory = factory; return this
    }

    fun withProfile(p: NetworkProfile): ScenarioBuilder { profile = p; return this }
    fun withMaxVirtualMs(ms: Long): ScenarioBuilder { maxVirtualMs = ms; return this }

    /**
     * Schedules a message send from [from] to [to] at virtual time [atMs].
     */
    fun sendMessage(
        from: String, to: String, payload: String,
        type: PacketType = PacketType.TEXT, atMs: Long = 0L
    ): ScenarioBuilder {
        sendIntents.add(SendIntent(from, to, payload, type, atMs))
        return this
    }

    /**
     * Schedules an action to execute at virtual time [ms].
     */
    fun atVirtualTime(ms: Long, action: (SimulationEnvironment) -> Unit): ScenarioBuilder {
        timedActions.add(TimedAction(ms, action))
        return this
    }

    /**
     * Asserts that a message with [payload] should be delivered from [from] to [to]
     * within [withinMs] virtual milliseconds.
     */
    fun expectDelivery(
        from: String, to: String, payload: String? = null, withinMs: Long = maxVirtualMs
    ): ScenarioBuilder {
        expectations.add(DeliveryExpectation(from, to, payload, withinMs))
        return this
    }

    /**
     * Builds and returns a [Scenario] ready to execute.
     */
    fun build(): Scenario {
        val ids = nodeIds ?: (0 until nodeCount).map { "node-$it" }
        val topoFactory = topologyFactory ?: { topIds -> TopologyBuilder.ring(topIds) }

        return Scenario(
            nodeIds = ids,
            topologyFactory = topoFactory,
            profile = profile,
            sendIntents = sendIntents.toList(),
            timedActions = timedActions.toList(),
            expectations = expectations.toList(),
            maxVirtualMs = maxVirtualMs
        )
    }
}

// ── Scenario Execution ────────────────────────────────────────────────────────

/**
 * An executable scenario produced by [ScenarioBuilder].
 * Call [run] to execute and obtain a [ScenarioResult].
 */
class Scenario(
    private val nodeIds: List<String>,
    private val topologyFactory: (List<String>) -> List<com.meshlink.simulator.transport.Link>,
    private val profile: NetworkProfile,
    private val sendIntents: List<ScenarioBuilder.SendIntent>,
    private val timedActions: List<ScenarioBuilder.TimedAction>,
    private val expectations: List<ScenarioBuilder.DeliveryExpectation>,
    private val maxVirtualMs: Long
) {
    /**
     * Executes the scenario and returns a [ScenarioResult] with all assertion outcomes.
     */
    fun run(): ScenarioResult {
        val env = MeshSimulator.build {
            nodes(nodeIds)
            topology(topologyFactory(nodeIds))
            profile(profile)
        }

        // Schedule timed actions
        timedActions.sortedBy { it.atMs }.forEach { action ->
            env.scheduler.scheduleAt(action.atMs) { action.action(env) }
        }

        // Schedule sends
        val traceIds = mutableMapOf<String, String>() // payload → traceId
        sendIntents.forEach { intent ->
            env.scheduler.scheduleAt(intent.atMs) {
                val traceId = env.node(intent.fromNodeId).sendPacket(
                    targetId = intent.toNodeId,
                    payload = intent.payload,
                    type = intent.type
                )
                traceIds[intent.payload] = traceId
            }
        }

        env.runUntilQuiet(maxStepMs = maxVirtualMs)

        // Evaluate expectations
        val results = expectations.map { exp ->
            val delivered = env.node(exp.toNodeId).receivedPackets().any { (_, pkt) ->
                (exp.packetPayload == null || pkt.payload == exp.packetPayload)
            }
            ScenarioResult.ExpectationResult(
                description = "Delivery from ${exp.fromNodeId} to ${exp.toNodeId}" +
                              if (exp.packetPayload != null) " (payload='${exp.packetPayload}')" else "",
                passed = delivered
            )
        }

        return ScenarioResult(
            results = results,
            env = env,
            traceIds = traceIds
        )
    }
}

/**
 * Result of a [Scenario] run.
 */
data class ScenarioResult(
    val results: List<ExpectationResult>,
    val env: SimulationEnvironment,
    val traceIds: Map<String, String>
) {
    data class ExpectationResult(val description: String, val passed: Boolean)

    fun allPassed(): Boolean = results.all { it.passed }
    fun failedExpectations(): List<ExpectationResult> = results.filter { !it.passed }

    fun assertAll() {
        val failed = failedExpectations()
        if (failed.isNotEmpty()) {
            val msg = failed.joinToString("\n") { "  FAILED: ${it.description}" }
            throw AssertionError("Scenario expectations failed:\n$msg\n\n${env.metrics.generateReport()}")
        }
    }
}

// ── Built-in Scenarios ────────────────────────────────────────────────────────

/**
 * Library of pre-built, reusable [Scenario] configurations.
 */
object Scenarios {

    /** Two-node direct message exchange. */
    fun twoNodeChat(): Scenario = ScenarioBuilder()
        .withNodeIds(listOf("alice", "bob"))
        .withTopology { ids -> TopologyBuilder.line(ids) }
        .withProfile(NetworkProfile.PerfectNetwork)
        .sendMessage("alice", "bob", "hello from alice")
        .sendMessage("bob", "alice", "hello from bob")
        .expectDelivery("alice", "bob", withinMs = 500)
        .expectDelivery("bob", "alice", withinMs = 500)
        .build()

    /** N-hop linear delivery end-to-end. */
    fun meshRelay(hops: Int): Scenario {
        val ids = (0..hops).map { "node-$it" }
        return ScenarioBuilder()
            .withNodeIds(ids)
            .withTopology { TopologyBuilder.line(it) }
            .withProfile(NetworkProfile.PerfectNetwork)
            .sendMessage(ids.first(), ids.last(), "relay-test")
            .expectDelivery(ids.first(), ids.last(), withinMs = hops * 200L)
            .build()
    }

    /** Node goes offline, packet is stored, node returns, delivery succeeds. */
    fun storeAndForwardRecovery(): Scenario = ScenarioBuilder()
        .withNodeIds(listOf("sender", "relay", "receiver"))
        .withTopology { ids -> TopologyBuilder.line(ids) }
        .withProfile(NetworkProfile.PerfectNetwork)
        .atVirtualTime(0) { env -> env.goOffline("relay") }
        .sendMessage("sender", "receiver", "store-and-forward-test", atMs = 100)
        .atVirtualTime(2_000) { env -> env.comeOnline("relay") }
        .expectDelivery("sender", "receiver", withinMs = 5_000)
        .withMaxVirtualMs(6_000)
        .build()

    /** KEY_EXCHANGE handshake between two peers. */
    fun keyExchangeFlow(peerA: String = "peer-a", peerB: String = "peer-b"): Scenario = ScenarioBuilder()
        .withNodeIds(listOf(peerA, peerB))
        .withTopology { ids -> TopologyBuilder.line(ids) }
        .withProfile(NetworkProfile.PerfectNetwork)
        .sendMessage(peerA, peerB, "key-init", type = PacketType.KEY_EXCHANGE)
        .expectDelivery(peerA, peerB, withinMs = 200)
        .build()

    /** Broadcast storm scenario — triggers storm, verifies delivery to all in ring. */
    fun broadcastStorm(nodeCount: Int = 6): Scenario {
        val ids = (0 until nodeCount).map { "node-$it" }
        return ScenarioBuilder()
            .withNodeIds(ids)
            .withTopology { TopologyBuilder.ring(it) }
            .withProfile(NetworkProfile.CongestedNetwork)
            .sendMessage(ids[0], "BROADCAST", "storm-test")
            .expectDelivery(ids[0], ids[nodeCount / 2], withinMs = 5_000)
            .withMaxVirtualMs(8_000)
            .build()
    }

    /** Network is partitioned then healed — verifies eventual delivery. */
    fun networkPartition(groupSize: Int = 3): Scenario {
        val groupA = (0 until groupSize).map { "a-$it" }
        val groupB = (0 until groupSize).map { "b-$it" }
        val allIds = groupA + groupB
        return ScenarioBuilder()
            .withNodeIds(allIds)
            .withTopology { ids ->
                TopologyBuilder.ring(groupA) + TopologyBuilder.ring(groupB) +
                listOf(
                    com.meshlink.simulator.transport.Link(groupA.last(), groupB.first()),
                    com.meshlink.simulator.transport.Link(groupB.first(), groupA.last())
                )
            }
            .withProfile(NetworkProfile.PerfectNetwork)
            .sendMessage(groupA.first(), groupB.last(), "cross-partition")
            .atVirtualTime(100) { env -> env.partition(groupA, groupB) }
            .atVirtualTime(3_000) { env -> env.heal(groupA, groupB) }
            .expectDelivery(groupA.first(), groupB.last(), withinMs = 8_000)
            .withMaxVirtualMs(9_000)
            .build()
    }
}
