package com.meshlink.securitytest

import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.core.SimulationEnvironment
import com.meshlink.simulator.profile.NetworkProfile

/**
 * DSL for writing readable and deterministic security test scenarios.
 */
class SecurityScenarioRunner(private val seed: Long = SecurityTestFixtures.DEFAULT_SEED) {

    private lateinit var env: SimulationEnvironment
    private lateinit var recorder: SecurityEventRecorder
    private val simulator = AttackSimulator(seed)

    fun setup(
        nodes: List<String>,
        profile: NetworkProfile = NetworkProfile.PerfectNetwork,
        config: com.meshlink.simulator.node.SimulatedNode.NodeConfig = SecurityTestFixtures.strictSecurityConfig()
    ) {
        env = MeshSimulator.build {
            nodes(nodes)
            topology { ids -> com.meshlink.simulator.topology.TopologyBuilder.fullyConnected(ids) }
            profile(profile)
            nodeConfig(config)
            clockSeed(0L)
            randomSeed(seed)
        }
        recorder = SecurityEventRecorder(env.recorder)
    }

    fun env(): SimulationEnvironment = env
    fun eventRecorder(): SecurityEventRecorder = recorder
    fun attacker(): AttackSimulator = simulator

    fun step(timeMs: Long = 100L) {
        env.step(timeMs)
        recorder.pollEvents()
    }
    
    fun runUntilQuiet() {
        env.runUntilQuiet()
        recorder.pollEvents()
    }

    companion object {
        fun scenario(
            nodes: List<String> = listOf(SecurityTestFixtures.ALICE_ID, SecurityTestFixtures.BOB_ID),
            seed: Long = SecurityTestFixtures.DEFAULT_SEED,
            block: SecurityScenarioRunner.() -> Unit
        ) {
            val runner = SecurityScenarioRunner(seed)
            runner.setup(nodes)
            runner.block()
        }
    }
}
