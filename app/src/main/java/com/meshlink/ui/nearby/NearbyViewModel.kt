package com.meshlink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.wifi.data.WifiDirectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NearbyUiState(
    val devices: List<BleDevice> = emptyList()
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository,
    private val wifiDirectManager: WifiDirectManager
) : ViewModel() {

    val uiState: StateFlow<NearbyUiState> = meshRepository.scannedDevices
        .map { devicesMap ->
            NearbyUiState(devices = devicesMap.values.toList().sortedByDescending { it.rssi })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NearbyUiState())

    fun startDiscovery() {
        viewModelScope.launch {
            val user = userRepository.getLocalUser()
            if (user != null) {
                meshRepository.autoStartMesh()
                
                // Ignite Wi-Fi subsystem concurrently with Bluetooth BLE
                wifiDirectManager.startDiscovery()
            }
        }
    }
}
