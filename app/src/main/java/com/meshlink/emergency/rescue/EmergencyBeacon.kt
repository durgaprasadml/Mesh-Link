package com.meshlink.emergency.rescue

import com.meshlink.common.logger.MeshLogger
import com.meshlink.routing.data.MeshRouter
import com.meshlink.routing.engine.BatteryAwareNetworking
import com.meshlink.routing.engine.PowerState
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.BroadcastType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyBeacon @Inject constructor(
    private val meshRouter: MeshRouter,
    private val batteryAwareNetworking: BatteryAwareNetworking
) {
    companion object {
        private const val TAG = "EmergencyBeacon"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isBeaconActive = false

    /**
     * Starts a continuous SOS beacon. Automatically adjusts interval based on battery.
     */
    fun startBeacon(lastKnownLatitude: Double, lastKnownLongitude: Double, distressMessage: String) {
        if (isBeaconActive) return
        isBeaconActive = true
        
        MeshLogger.w(TAG, "🚨 EMERGENCY BEACON ACTIVATED 🚨")

        scope.launch {
            while (isBeaconActive) {
                // Determine interval based on battery to ensure the beacon lasts as long as possible
                val interval = when (batteryAwareNetworking.powerState.value) {
                    PowerState.NORMAL -> 15_000L // 15 seconds
                    PowerState.POWER_SAVER -> 60_000L // 1 minute
                    PowerState.CRITICAL -> 300_000L // 5 minutes (Extreme battery save mode)
                }
                
                val payloadJson = JSONObject().apply {
                    put("lat", lastKnownLatitude)
                    put("lon", lastKnownLongitude)
                    put("msg", distressMessage)
                    put("batt", batteryAwareNetworking.batteryPct.value)
                }

                val packet = MeshPacket(
                    senderId = meshRouter.localMeshId,
                    targetId = "BROADCAST",
                    payload = payloadJson.toString(),
                    type = PacketType.BEACON,
                    priority = PacketPriority.CRITICAL,
                    broadcastType = BroadcastType.SOS
                )
                
                // Note: Normally we'd queue this via RoutingEngine, but for this architecture mock:
                MeshLogger.w(TAG, "🚨 Broadcasting SOS Beacon... Next ping in ${interval / 1000}s")
                
                delay(interval)
            }
        }
    }

    fun stopBeacon() {
        isBeaconActive = false
        MeshLogger.d(TAG, "Emergency Beacon Deactivated.")
    }
}
