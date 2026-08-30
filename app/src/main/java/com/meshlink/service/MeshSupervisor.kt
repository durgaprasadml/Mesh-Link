package com.meshlink.service

import com.meshlink.ble.data.BleAdvertiserManager
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.routing.api.Router
import com.meshlink.routing.engine.RoutingTable
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.wifi.manager.WifiDirectManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class MeshSupervisor @Inject constructor(
    private val bleScannerManager: BleScannerManager,
    private val bleAdvertiserManager: BleAdvertiserManager,
    private val wifiDirectManager: WifiDirectManager,
    private val discoveryEngine: DiscoveryEngine,
    private val router: Router,
    private val routingTable: RoutingTable,
    private val meshRepository: MeshRepository,
    private val transportDiagnostics: TransportDiagnostics,
    @com.meshlink.di.ApplicationScope private val externalScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MeshSupervisor"
    }

    private val _subsystemStates = MutableStateFlow<Map<RadioSubsystem, RadioState>>(
        RadioSubsystem.values().associateWith { RadioState.STOPPED }
    )
    val subsystemStates: StateFlow<Map<RadioSubsystem, RadioState>> = _subsystemStates.asStateFlow()

    private var isSupervising = false

    init {
        externalScope.launch {
            wifiDirectManager.radioState.collect { state ->
                updateSubsystemState(RadioSubsystem.WIFI_DIRECT, state)
            }
        }
    }

    fun updateSubsystemState(subsystem: RadioSubsystem, state: RadioState) {
        val current = _subsystemStates.value.toMutableMap()
        current[subsystem] = state
        _subsystemStates.value = current
        MeshLogger.d(TAG, "Subsystem $subsystem updated to state $state")
    }

    fun startAllSubsystems() {
        if (isSupervising && isFullyOperational()) {
            MeshLogger.d(TAG, "[MeshStartup] All subsystems already supervising and operational. Skipping duplicate startup.")
            return
        }
        isSupervising = true
        MeshLogger.d(TAG, "[MeshStartup] Initializing and starting all mesh subsystems under supervisor supervision")

        externalScope.launch {
            try {
                updateSubsystemState(RadioSubsystem.BLE_SCANNER, RadioState.INITIALIZING)
                updateSubsystemState(RadioSubsystem.BLE_ADVERTISER, RadioState.INITIALIZING)
                updateSubsystemState(RadioSubsystem.GATT_SERVER, RadioState.INITIALIZING)
                updateSubsystemState(RadioSubsystem.WIFI_DIRECT, RadioState.INITIALIZING)
                updateSubsystemState(RadioSubsystem.DISCOVERY_ENGINE, RadioState.INITIALIZING)
                updateSubsystemState(RadioSubsystem.ROUTING_ENGINE, RadioState.INITIALIZING)
                updateSubsystemState(RadioSubsystem.PACKET_DISPATCHER, RadioState.INITIALIZING)

                // Start repository / core mesh layer
                meshRepository.autoStartMesh()

                // Start Wi-Fi Direct
                wifiDirectManager.startWifiDirect()

                updateSubsystemState(RadioSubsystem.BLE_SCANNER, RadioState.RUNNING)
                updateSubsystemState(RadioSubsystem.BLE_ADVERTISER, RadioState.RUNNING)
                updateSubsystemState(RadioSubsystem.GATT_SERVER, RadioState.RUNNING)
                updateSubsystemState(RadioSubsystem.WIFI_DIRECT, wifiDirectManager.radioState.value)
                updateSubsystemState(RadioSubsystem.DISCOVERY_ENGINE, RadioState.RUNNING)
                updateSubsystemState(RadioSubsystem.ROUTING_ENGINE, RadioState.RUNNING)
                updateSubsystemState(RadioSubsystem.PACKET_DISPATCHER, RadioState.RUNNING)

            } catch (e: Exception) {
                MeshLogger.e(TAG, "[MeshStartup] Error starting mesh subsystems: ${e.message}", e)
                isSupervising = false
                RadioSubsystem.values().forEach { sub ->
                    updateSubsystemState(sub, RadioState.STOPPED)
                }
            }
        }
    }

    fun forceRestartAllSubsystems() {
        MeshLogger.d(TAG, "[MeshStartup] Force restart requested for all mesh subsystems")
        isSupervising = false
        startAllSubsystems()
    }

    fun stopAllSubsystems() {
        isSupervising = false
        MeshLogger.d(TAG, "Stopping all supervised mesh subsystems")
        externalScope.launch {
            try {
                wifiDirectManager.stopWifiDirect()
                meshRepository.stopMesh()
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Error during subsystem shutdown: ${e.message}")
            } finally {
                RadioSubsystem.values().forEach { sub ->
                    updateSubsystemState(sub, RadioState.STOPPED)
                }
            }
        }
    }

    fun restartSubsystem(subsystem: RadioSubsystem) {
        MeshLogger.w(TAG, "Targeted restart requested for subsystem: $subsystem")
        updateSubsystemState(subsystem, RadioState.RECOVERING)
        externalScope.launch {
            try {
                when (subsystem) {
                    RadioSubsystem.BLE_SCANNER -> {
                        bleScannerManager.stopScanning()
                        bleScannerManager.startScanning()
                    }
                    RadioSubsystem.BLE_ADVERTISER -> {
                        bleAdvertiserManager.stopAdvertising()
                        meshRepository.refreshMesh()
                    }
                    RadioSubsystem.GATT_SERVER -> {
                        meshRepository.refreshMesh()
                    }
                    RadioSubsystem.WIFI_DIRECT -> {
                        wifiDirectManager.restartWifiDirect()
                    }
                    RadioSubsystem.DISCOVERY_ENGINE -> {
                        discoveryEngine.stop()
                        discoveryEngine.start()
                    }
                    RadioSubsystem.ROUTING_ENGINE -> {
                        routingTable.clear()
                        meshRepository.refreshMesh()
                    }
                    RadioSubsystem.PACKET_DISPATCHER -> {
                        meshRepository.refreshMesh()
                    }
                }
                val newState = if (subsystem == RadioSubsystem.WIFI_DIRECT) wifiDirectManager.radioState.value else RadioState.RUNNING
                updateSubsystemState(subsystem, newState)
                MeshLogger.d(TAG, "Subsystem $subsystem successfully recovered to $newState")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to recover subsystem $subsystem: ${e.message}")
                updateSubsystemState(subsystem, RadioState.FAILED)
            }
        }
    }

    fun isFullyOperational(): Boolean {
        return isSupervising && _subsystemStates.value.entries.all { (subsystem, state) ->
            if (subsystem == RadioSubsystem.WIFI_DIRECT) {
                state == RadioState.RUNNING || state == RadioState.STOPPED
            } else {
                state == RadioState.RUNNING
            }
        }
    }
}
