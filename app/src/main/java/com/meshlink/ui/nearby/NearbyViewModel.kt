package com.meshlink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.engine.MeshTopologyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import androidx.compose.runtime.Immutable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

enum class SortOption { RSSI, NAME, STATUS }

@Immutable
data class NearbyUiState(
    val devices: List<BleDevice> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.RSSI,
    val isScanning: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository,
    private val topologyManager: MeshTopologyManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RSSI)
    private val _isScanning = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<NearbyUiState> = combine(
        meshRepository.scannedDevices,
        topologyManager.reachableNodes,
        _searchQuery.debounce { if (it.isEmpty()) 0L else 250L }.distinctUntilChanged(),
        _sortOption,
        combine(_isScanning, _errorMessage) { scanning, err -> Pair(scanning, err) }
    ) { bleMap, reachableNodes, query, sortOption, scanStatus ->
        val isScanning = scanStatus.first
        val errorMessage = scanStatus.second
        withContext(Dispatchers.Default) {
            val mergedDevices = mutableMapOf<String, BleDevice>()

            // Direct physical devices
            bleMap.values.forEach { device ->
                mergedDevices[device.address] = device
            }

            // Indirect multi-hop mesh nodes
            reachableNodes.forEach { node ->
                if (!mergedDevices.containsKey(node.nodeId)) {
                    mergedDevices[node.nodeId] = BleDevice(
                        meshId = node.nodeId,
                        name = node.nodeId,
                        address = node.nodeId,
                        rssi = node.rssi,
                        hopCount = node.hopCount,
                        isMeshNode = true,
                        viaRelayId = node.viaRelayId
                    )
                }
            }

            var sortedList = when (sortOption) {
                SortOption.RSSI -> mergedDevices.values.toList().sortedByDescending { it.rssi }
                SortOption.NAME -> mergedDevices.values.toList().sortedBy { it.name.ifBlank { "~" } }
                SortOption.STATUS -> mergedDevices.values.toList().sortedBy { it.hopCount }
            }

            if (query.isNotBlank()) {
                sortedList = sortedList.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
                }
            }

            NearbyUiState(
                devices = sortedList,
                searchQuery = query,
                sortOption = sortOption,
                isScanning = isScanning,
                errorMessage = errorMessage
            )
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NearbyUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

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

            if (device.transport == TransportType.BLE) {
                meshRepository.connectToPeer(device.address)
            }
            onConnected()
        }
    }
}
