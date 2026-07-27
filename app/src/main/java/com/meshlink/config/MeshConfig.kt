package com.meshlink.config

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global configuration for the Mesh Link application.
 *
 * Responsibility: Provide immutable configuration parameters for the mesh network.
 * Lifecycle: Singleton, application scoped.
 * Thread Safety: Immutable, thread-safe.
 */
@Singleton
data class MeshConfig(
    val maxRelayPackets: Int,
    val defaultTtl: Int,
    val maxHops: Int,
    val routingRetryCount: Int,
    val routingRetryIntervalMs: Long
) {
    @Inject constructor() : this(
        maxRelayPackets = 500,
        defaultTtl = 3,
        maxHops = 10,
        routingRetryCount = 3,
        routingRetryIntervalMs = 2000L
    )
}
