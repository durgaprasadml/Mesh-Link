package com.meshlink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository

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
    val sortOption: SortOption = SortOption.RSSI,
    val isScanning: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.RSSI)
    private val _isScanning = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NearbyUiState> = combine(
        meshRepository.scannedDevices,
        _sortOption,
        _isScanning,
        _errorMessage
    ) { bleMap, sortOption, isScanning, errorMessage ->
        
        val mergedDevices = mutableMapOf<String, BleDevice>()
        
        bleMap.values.forEach { device ->
            mergedDevices[device.address] = device
        }

        val sortedList = when (sortOption) {
            SortOption.RSSI -> mergedDevices.values.toList().sortedByDescending { it.rssi }
            SortOption.NAME -> mergedDevices.values.toList().sortedBy { it.name.ifBlank { "~" } }
            SortOption.STATUS -> mergedDevices.values.toList().sortedByDescending { it.isConnected }
        }
        
        NearbyUiState(
            devices = sortedList, 
            sortOption = sortOption,
            isScanning = isScanning,
            errorMessage = errorMessage
        )
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NearbyUiState())

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun startDiscovery() {
        _isScanning.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val user = userRepository.getLocalUser()
            if (user != null) {
                try {
                    meshRepository.autoStartMesh()
                } catch (e: Exception) {
                    _errorMessage.value = e.message ?: "Failed to start discovery"
                } finally {
                    _isScanning.value = false
                }
            } else {
                _errorMessage.value = "User not found. Please log in."
                _isScanning.value = false
            }
        }
    }
    
    fun connectToDevice(device: BleDevice, onConnected: () -> Unit) {
        viewModelScope.launch {

            if (device.transport == TransportType.BLE || device.transport == TransportType.HYBRID) {
                meshRepository.connectToPeer(device.address)
            }
            onConnected()
        }
    }
}
