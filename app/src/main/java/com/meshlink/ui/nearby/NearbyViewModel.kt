package com.meshlink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.data.repository.BleRepository
import com.meshlink.data.wifi.WifiDirectManager
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val bleRepository: BleRepository,
    private val userRepository: UserRepository,
    private val wifiDirectManager: WifiDirectManager
) : ViewModel() {

    val nearbyDevices: StateFlow<List<BleDevice>> = bleRepository.scannedDevices
        .map { it.values.toList().sortedByDescending { device -> device.rssi } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startDiscovery() {
        viewModelScope.launch {
            val user = userRepository.getLocalUser()
            if (user != null) {
                bleRepository.autoStartMesh()
                
                // Ignite Wi-Fi subsystem concurrently with Bluetooth BLE
                wifiDirectManager.start()
                wifiDirectManager.igniteBackgroundDiscovery()
            }
        }
    }
}
