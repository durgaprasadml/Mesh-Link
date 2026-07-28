package com.meshlink.simulator.transport

import java.util.concurrent.atomic.AtomicBoolean

/**
 * A directed virtual link between two simulated nodes.
 *
 * Links are directional — packet delivery flows from [fromNodeId] to [toNodeId].
 * For a bidirectional link, two [Link] instances are created (one for each direction).
 *
 * Links can be enabled/disabled at runtime to simulate disconnections, partitions,
 * and dynamic topology changes during a simulation run.
 *
 * The [config] governs latency, packet loss, and corruption for all packets
 * traversing this link. Config can be swapped at runtime via [applyConfig].
 *
 * @param fromNodeId Source node ID.
 * @param toNodeId   Destination node ID.
 * @param config     Transport characteristics for this link.
 */
data class Link(
    val fromNodeId: String,
    val toNodeId: String,
    var config: TransportConfig = TransportConfig.TypicalBle
) {
    private val _enabled = AtomicBoolean(true)

    /** Whether this link is currently active. */
    val isEnabled: Boolean get() = _enabled.get()

    /** Disables this link — all packets traversing it are silently dropped. */
    fun disable() { _enabled.set(false) }

    /** Re-enables this link after a [disable] call. */
    fun enable() { _enabled.set(true) }

    /** Replaces the transport configuration for this link at runtime. */
    fun applyConfig(newConfig: TransportConfig) { config = newConfig }

    /** Applies a named [NetworkProfile] to this link. */
    fun applyProfile(profile: com.meshlink.simulator.profile.NetworkProfile) {
        config = profile.config
    }

    override fun toString(): String =
        "Link($fromNodeId→$toNodeId, ${config.type}, " +
        "latency=${config.latencyRangeMs}ms, loss=${config.packetLossRate * 100}%, " +
        "enabled=$isEnabled)"
}
