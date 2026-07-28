package com.meshlink.stress

import com.meshlink.simulator.profile.NetworkProfile
import com.meshlink.simulator.transport.TransportConfig
import com.meshlink.simulator.transport.TransportType

/**
 * Extended failure profiles for stress testing.
 * Uses [NetworkProfile.Custom] since [NetworkProfile] is a sealed class.
 */
object NetworkFailureProfiles {

    /**
     * Highly unstable Bluetooth link with frequent small drops,
     * high variance in latency, and moderate packet loss.
     */
    val FlakyBluetooth: NetworkProfile = NetworkProfile.Custom(
        TransportConfig(
            latencyRangeMs = 50..500,
            packetLossRate = 0.15f,
            corruptionRate = 0.05f,
            bandwidthBytesPerSec = 20_000,
            type = TransportType.BLE
        )
    )

    /**
     * Profile simulating rapid route changes and flapping.
     * High latency variance combined with intermittent high loss.
     */
    val RouteFlapping: NetworkProfile = NetworkProfile.Custom(
        TransportConfig(
            latencyRangeMs = 100..1000,
            packetLossRate = 0.25f,
            corruptionRate = 0.01f,
            bandwidthBytesPerSec = 50_000,
            type = TransportType.BLE
        )
    )

    // Standard references for convenience in DSL
    val PerfectNetwork = NetworkProfile.PerfectNetwork
    val TypicalBLE = NetworkProfile.TypicalBLE
    val HighLatency = NetworkProfile.HighLatency
    val HighLoss = NetworkProfile.HighLoss
    val Partitioned = NetworkProfile.PartitionedNetwork
    val Congested = NetworkProfile.CongestedNetwork
    val NightlyStress = NetworkProfile.NightlyStress
}
