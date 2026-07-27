package com.meshlink.simulator.transport

/**
 * Transport medium type simulated in the mesh network.
 */
enum class TransportType { BLE, WIFI_DIRECT }

/**
 * Configuration for a simulated virtual transport link.
 *
 * All values are applied per-packet when the [SimulatedTransport] dispatches packets.
 * Use the companion factory methods to create commonly-used configurations, or build
 * custom configs inline.
 *
 * @param latencyRangeMs Range of virtual delivery latency in milliseconds (inclusive).
 *   A random value is sampled from this range for each packet.
 * @param packetLossRate Probability (0.0..1.0) that a packet is silently dropped.
 * @param corruptionRate Probability (0.0..1.0) that a packet payload is corrupted (bitflipped).
 * @param bandwidthBytesPerSec Simulated bandwidth cap. Currently tracked in metrics only
 *   (actual throttling is not applied to maintain test speed).
 * @param type The simulated transport medium.
 */
data class TransportConfig(
    val latencyRangeMs: IntRange = 5..20,
    val packetLossRate: Float = 0f,
    val corruptionRate: Float = 0f,
    val bandwidthBytesPerSec: Int = 100_000,
    val type: TransportType = TransportType.BLE
) {
    init {
        require(packetLossRate in 0f..1f) { "packetLossRate must be in [0,1]" }
        require(corruptionRate in 0f..1f) { "corruptionRate must be in [0,1]" }
        require(latencyRangeMs.first >= 0) { "latency must be non-negative" }
    }

    /**
     * Samples a random latency value from [latencyRangeMs].
     * Uses the provided [random] for deterministic seeding in tests.
     */
    fun sampleLatency(random: java.util.Random): Long {
        if (latencyRangeMs.first == latencyRangeMs.last) return latencyRangeMs.first.toLong()
        return (latencyRangeMs.first + random.nextInt(
            latencyRangeMs.last - latencyRangeMs.first + 1
        )).toLong()
    }

    /**
     * Returns true if the packet should be dropped based on [packetLossRate].
     */
    fun shouldDrop(random: java.util.Random): Boolean =
        packetLossRate > 0f && random.nextFloat() < packetLossRate

    /**
     * Returns true if the packet payload should be corrupted.
     */
    fun shouldCorrupt(random: java.util.Random): Boolean =
        corruptionRate > 0f && random.nextFloat() < corruptionRate

    companion object {
        /** Zero-loss, zero-latency ideal link. */
        val Perfect = TransportConfig(latencyRangeMs = 0..0, packetLossRate = 0f)

        /** Typical BLE radio conditions. */
        val TypicalBle = TransportConfig(
            latencyRangeMs = 10..50,
            packetLossRate = 0.01f,
            type = TransportType.BLE
        )

        /** Typical Wi-Fi Direct conditions. */
        val TypicalWifi = TransportConfig(
            latencyRangeMs = 5..20,
            packetLossRate = 0.005f,
            bandwidthBytesPerSec = 10_000_000,
            type = TransportType.WIFI_DIRECT
        )

        /** High-latency long-range mesh link. */
        val HighLatency = TransportConfig(
            latencyRangeMs = 200..500,
            packetLossRate = 0.02f
        )

        /** High packet loss (poor RF conditions). */
        val HighLoss = TransportConfig(
            latencyRangeMs = 20..80,
            packetLossRate = 0.30f
        )

        /** Complete black-hole — all packets dropped. */
        val Partitioned = TransportConfig(
            latencyRangeMs = 0..0,
            packetLossRate = 1.0f
        )

        /** Congested network with high latency and moderate loss. */
        val Congested = TransportConfig(
            latencyRangeMs = 100..300,
            packetLossRate = 0.10f,
            bandwidthBytesPerSec = 10_000
        )

        /** Nightly stress test profile. */
        val NightlyStress = TransportConfig(
            latencyRangeMs = 50..150,
            packetLossRate = 0.05f,
            bandwidthBytesPerSec = 50_000
        )
    }
}
