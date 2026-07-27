package com.meshlink.simulator.profile

import com.meshlink.simulator.transport.TransportConfig
import com.meshlink.simulator.transport.TransportType

/**
 * Predefined named network profiles for the mesh simulation framework.
 *
 * Profiles encapsulate a [TransportConfig] that can be applied globally to all links
 * in a simulation, or per-link for heterogeneous network conditions.
 *
 * Usage:
 * ```kotlin
 * // Apply globally
 * sim.applyProfile(NetworkProfile.HighLoss)
 *
 * // Apply to a specific link
 * link.applyProfile(NetworkProfile.HighLatency)
 * ```
 */
sealed class NetworkProfile(val config: TransportConfig) {

    /**
     * Zero-loss, zero-latency ideal network.
     * Use for testing pure routing logic without transport artifacts.
     */
    object PerfectNetwork : NetworkProfile(
        TransportConfig(
            latencyRangeMs = 0..0,
            packetLossRate = 0f,
            corruptionRate = 0f,
            bandwidthBytesPerSec = Int.MAX_VALUE
        )
    )

    /**
     * Representative real-world BLE conditions:
     * 10–50ms latency, 1% packet loss.
     */
    object TypicalBLE : NetworkProfile(
        TransportConfig(
            latencyRangeMs = 10..50,
            packetLossRate = 0.01f,
            type = TransportType.BLE
        )
    )

    /**
     * High-latency long-range mesh: 200–500ms latency, 2% loss.
     * Simulates stretched mesh with weak radio links.
     */
    object HighLatency : NetworkProfile(
        TransportConfig(
            latencyRangeMs = 200..500,
            packetLossRate = 0.02f,
            bandwidthBytesPerSec = 50_000
        )
    )

    /**
     * High packet loss (30%) with moderate latency.
     * Simulates poor RF conditions, crowded spectrum, or interference.
     */
    object HighLoss : NetworkProfile(
        TransportConfig(
            latencyRangeMs = 20..80,
            packetLossRate = 0.30f
        )
    )

    /**
     * Complete network partition — all packets dropped (100% loss).
     * Use to simulate a severed link or isolated subnet.
     */
    object PartitionedNetwork : NetworkProfile(
        TransportConfig(
            latencyRangeMs = 0..0,
            packetLossRate = 1.0f
        )
    )

    /**
     * Congested network with high latency (100–300ms), 10% loss,
     * and severely limited bandwidth (10 KB/s).
     */
    object CongestedNetwork : NetworkProfile(
        TransportConfig(
            latencyRangeMs = 100..300,
            packetLossRate = 0.10f,
            bandwidthBytesPerSec = 10_000
        )
    )

    /**
     * Nightly stress test profile: moderate latency, 5% loss.
     * Balanced for 200–500 node scale without being too aggressive.
     */
    object NightlyStress : NetworkProfile(
        TransportConfig(
            latencyRangeMs = 50..150,
            packetLossRate = 0.05f,
            bandwidthBytesPerSec = 50_000
        )
    )

    companion object {
        /** All built-in profiles indexed by name. */
        val all: Map<String, NetworkProfile> = mapOf(
            "PerfectNetwork" to PerfectNetwork,
            "TypicalBLE" to TypicalBLE,
            "HighLatency" to HighLatency,
            "HighLoss" to HighLoss,
            "PartitionedNetwork" to PartitionedNetwork,
            "CongestedNetwork" to CongestedNetwork,
            "NightlyStress" to NightlyStress
        )

        /** Looks up a profile by name. Throws [IllegalArgumentException] if not found. */
        fun byName(name: String): NetworkProfile =
            all[name] ?: throw IllegalArgumentException("Unknown profile: $name. Available: ${all.keys}")
    }
}
