package com.meshlink.simulator.node

import com.meshlink.routing.engine.PowerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-free stub for [com.meshlink.routing.engine.BatteryAwareNetworking].
 *
 * Production [BatteryAwareNetworking] requires an Android [Context] to read
 * battery status via [IntentFilter]. This stub exposes the same logical API
 * with configurable state, enabling pure-JVM unit tests.
 *
 * [RoutingEngine] uses [BatteryAwareNetworking] for:
 * - TTL shrinking under [PowerState.CRITICAL]
 * - Broadcast relay gating via [canRelayBackgroundTraffic]
 * - Probabilistic relay under [PowerState.POWER_SAVER]
 *
 * The stub defaults to [PowerState.NORMAL] and full relay capability.
 *
 * @param initialPowerState Starting power state (default: NORMAL).
 * @param initialBatteryPct Starting battery percentage (default: 100).
 */
class SimulatedBatteryNetworking(
    initialPowerState: PowerState = PowerState.NORMAL,
    initialBatteryPct: Int = 100
) {
    private val _powerState = MutableStateFlow(initialPowerState)

    /** Exposes power state as a StateFlow — matches the production API contract. */
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()

    private var _batteryPct: Int = initialBatteryPct

    /** Current battery percentage. */
    val batteryPct: Int get() = _batteryPct

    /** Sets the power state, triggering any StateFlow observers. */
    fun setPowerState(state: PowerState) { _powerState.value = state }

    /** Sets the battery percentage (0–100). */
    fun setBatteryPct(pct: Int) {
        require(pct in 0..100) { "batteryPct must be in [0, 100]" }
        _batteryPct = pct
        // Auto-derive power state from battery percentage
        _powerState.value = when {
            pct <= 15 -> PowerState.CRITICAL
            pct <= 30 -> PowerState.POWER_SAVER
            else -> PowerState.NORMAL
        }
    }

    /** @return true if the node should participate in generic mesh relays. */
    fun canRelayBackgroundTraffic(): Boolean = _powerState.value != PowerState.CRITICAL

    /**
     * @return Broadcast relay probability (1.0 = always, 0.1 = rarely).
     * Mirrors the production BatteryAwareNetworking.getBroadcastProbability().
     */
    fun getBroadcastProbability(): Float = when (_powerState.value) {
        PowerState.NORMAL -> 1.0f
        PowerState.POWER_SAVER -> 0.5f
        PowerState.CRITICAL -> 0.1f
    }

    override fun toString(): String =
        "SimulatedBatteryNetworking(state=${_powerState.value}, battery=${_batteryPct}%)"
}
