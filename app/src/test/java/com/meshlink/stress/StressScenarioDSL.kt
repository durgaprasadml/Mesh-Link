package com.meshlink.stress

import com.meshlink.simulator.profile.NetworkProfile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Defines a complete stress test scenario configuration.
 */
data class StressScenario(
    val nodeCount: Int,
    val duration: Duration,
    val profile: NetworkProfile,
    val seed: Long,
    val messageCount: Int,
    val concurrentSenders: Int
)

/**
 * DSL Builder for defining a [StressScenario].
 */
class StressScenarioBuilder {
    private var nodeCount: Int = 10
    private var duration: Duration = 1.minutes
    private var profile: NetworkProfile = NetworkFailureProfiles.PerfectNetwork
    private var seed: Long = 42L
    private var messageCount: Int = 1000
    private var concurrentSenders: Int = 5

    fun nodes(count: Int) { this.nodeCount = count }
    fun duration(duration: Duration) { this.duration = duration }
    fun profile(profile: NetworkProfile) { this.profile = profile }
    fun seed(seed: Long) { this.seed = seed }
    fun messages(count: Int) { this.messageCount = count }
    fun concurrentSenders(count: Int) { this.concurrentSenders = count }

    fun build(): StressScenario = StressScenario(
        nodeCount = nodeCount,
        duration = duration,
        profile = profile,
        seed = seed,
        messageCount = messageCount,
        concurrentSenders = concurrentSenders
    )
}

/**
 * Entry point for defining a declarative stress test scenario.
 */
fun stressTest(block: StressScenarioBuilder.() -> Unit): StressScenario {
    val builder = StressScenarioBuilder()
    builder.block()
    return builder.build()
}
