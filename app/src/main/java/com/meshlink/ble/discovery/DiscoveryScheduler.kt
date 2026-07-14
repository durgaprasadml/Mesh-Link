package com.meshlink.ble.discovery

/**
 * Calculates adaptive scan and idle windows based on power state and network stability.
 */
class DiscoveryScheduler(private val batteryAwareScanner: BatteryAwareScanner) {

    data class WindowConfig(
        val scanDurationMs: Long,
        val idleDurationMs: Long
    )

    /**
     * Determines the current scan window strategy.
     * @param hasActiveConnections True if the mesh is currently established and stable.
     */
    fun getNextWindowConfig(hasActiveConnections: Boolean): WindowConfig {
        val powerState = batteryAwareScanner.getCurrentPowerState()
        
        return when (powerState) {
            PowerState.OPTIMAL -> {
                if (hasActiveConnections) {
                    WindowConfig(scanDurationMs = 5000L, idleDurationMs = 5000L)
                } else {
                    // Aggressive scanning if plugged in and disconnected
                    WindowConfig(scanDurationMs = 10000L, idleDurationMs = 2000L)
                }
            }
            PowerState.BALANCED -> {
                if (hasActiveConnections) {
                    // Stable network, save battery
                    WindowConfig(scanDurationMs = 3000L, idleDurationMs = 12000L)
                } else {
                    // Need to find a network
                    WindowConfig(scanDurationMs = 5000L, idleDurationMs = 5000L)
                }
            }
            PowerState.RESTRICTED -> {
                if (hasActiveConnections) {
                    // Bare minimum to maintain awareness
                    WindowConfig(scanDurationMs = 2000L, idleDurationMs = 20000L)
                } else {
                    // Still need to find a network, but respect battery
                    WindowConfig(scanDurationMs = 3000L, idleDurationMs = 15000L)
                }
            }
        }
    }
}
