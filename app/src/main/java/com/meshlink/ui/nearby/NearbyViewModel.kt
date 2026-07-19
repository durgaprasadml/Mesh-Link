package com.meshlink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.wifi.data.WifiDirectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption { RSSI, NAME, STATUS }

data class NearbyUiState(
    val devices: List<BleDevice> = emptyList(),
    val sortOption: SortOption = SortOption.RSSI
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository,
    private val wifiDirectManager: WifiDirectManager
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.RSSI)

    val uiState: StateFlow<NearbyUiState> = combine(
        meshRepository.scannedDevices,
        _sortOption
    ) { devicesMap, sortOption ->
        val sortedList = when (sortOption) {
            SortOption.RSSI -> devicesMap.values.toList().sortedByDescending { it.rssi }
            SortOption.NAME -> devicesMap.values.toList().sortedBy { it.name.ifBlank { "~" } }
            SortOption.STATUS -> devicesMap.values.toList().sortedByDescending { it.rssi > -70 }
        }
        NearbyUiState(devices = sortedList, sortOption = sortOption)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NearbyUiState())

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

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
