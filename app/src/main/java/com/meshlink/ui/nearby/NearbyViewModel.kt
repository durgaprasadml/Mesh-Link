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
import com.meshlink.common.logger.MeshLogger
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
            val localUser = userRepository.getLocalUser()
            val localCanonicalId = localUser?.let { com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId) }
            val localShortId = localUser?.let {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId).toByteArray(Charsets.UTF_8))
                hashBytes.copyOf(8).joinToString("") { b -> "%02x".format(b) }
            }

            // Direct physical devices
            bleMap.values.forEach { device ->
                val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(device.meshId.ifBlank { device.address })
                // Do not display own device in discovery
                if (localCanonicalId != null && (canonicalId == localCanonicalId || (localShortId != null && canonicalId == localShortId))) {
                    return@forEach
                }

                val profile = userRepository.getUserProfile(canonicalId) ?: userRepository.getUserProfile(device.meshId)
                val rawDisplayName = device.displayName?.trim()
                val cachedProfileName = userRepository.getUserDisplayName(canonicalId).takeIf { !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId) }
                    ?: userRepository.getUserDisplayName(device.meshId).takeIf { !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId) }
                    ?: userRepository.getUserDisplayName(device.address).takeIf { !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId) }

                val finalDisplayName = if (!rawDisplayName.isNullOrBlank() && !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(rawDisplayName, canonicalId)) {
                    rawDisplayName
                } else if (cachedProfileName != null) {
                    cachedProfileName
                } else {
                    null
                }

                // Strictly use Mesh-Link profile display name; never fall back to smartphone hardware/device model!
                val finalName = finalDisplayName ?: "Mesh Peer"

                mergedDevices[canonicalId] = device.copy(
                    name = finalName,
                    displayName = finalDisplayName,
                    bluetoothDeviceName = device.bluetoothDeviceName,
                    profilePhotoPath = profile?.profilePhotoPath,
                    profilePhotoHash = profile?.profilePhotoHash,
                    hopCount = 0,
                    isMeshNode = false
                )
            }

            // Indirect multi-hop mesh nodes
            reachableNodes.forEach { node ->
                val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(node.nodeId)
                // Do not display own device
                if (localCanonicalId != null && (canonicalId == localCanonicalId || (localShortId != null && canonicalId == localShortId))) {
                    return@forEach
                }

                if (!mergedDevices.containsKey(canonicalId)) {
                    val profile = userRepository.getUserProfile(canonicalId) ?: userRepository.getUserProfile(node.nodeId)
                    val resolvedName = userRepository.getUserDisplayName(canonicalId).takeIf { !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId) }
                        ?: userRepository.getUserDisplayName(node.nodeId).takeIf { !com.meshlink.core.data.UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId) }
                    val finalName = resolvedName ?: "Mesh Peer"
                    mergedDevices[canonicalId] = BleDevice(
                        meshId = node.nodeId,
                        name = finalName,
                        displayName = resolvedName,
                        address = node.nodeId,
                        rssi = node.rssi,
                        profilePhotoPath = profile?.profilePhotoPath,
                        profilePhotoHash = profile?.profilePhotoHash,
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
                    it.address.contains(query, ignoreCase = true) ||
                    it.meshId.contains(query, ignoreCase = true)
                }
            }

            com.meshlink.common.logger.MeshLogger.d("NearbyViewModel", "[NearbyDiscovery] UI State updated: ${sortedList.size} devices visible")

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
                    _isScanning.value = meshRepository.getMeshStatus().isBleScanning
                } catch (e: Exception) {
                    _errorMessage.value = e.message ?: "Failed to start discovery"
                    _isScanning.value = false
                }
            } else {
                _errorMessage.value = "User not found. Please log in."
                _isScanning.value = false
            }
        }
    }
    
    fun connectToDevice(
        device: BleDevice,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (device.isConnected) {
                onSuccess()
                return@launch
            }
            try {
                val result = when (device.transport) {
                    TransportType.BLE -> {
                        meshRepository.connectDevice(device.address)
                    }
                    TransportType.WIFI_DIRECT -> {
                        meshRepository.connectPeer(device.address)
                    }
                }
                when (result) {
                    is com.meshlink.domain.model.MeshResult.Success -> {
                        MeshLogger.i("NearbyViewModel", "Successfully initiated connection to ${device.address}")
                        onSuccess()
                    }
                    is com.meshlink.domain.model.MeshResult.Error -> {
                        val msg = result.error.message
                        MeshLogger.w("NearbyViewModel", "Failed to connect to ${device.address}: $msg")
                        _errorMessage.value = "Failed to connect to ${device.name.ifBlank { device.address }}: $msg"
                        onError(msg)
                    }
                }
            } catch (e: Exception) {
                MeshLogger.e("NearbyViewModel", "Exception connecting to ${device.address}: ${e.message}", e)
                _errorMessage.value = "Connection error: ${e.message}"
                onError(e.message ?: "Unknown connection error")
            }
        }
    }
}
