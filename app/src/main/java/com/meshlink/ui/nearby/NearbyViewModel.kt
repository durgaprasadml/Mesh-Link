package com.meshlink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.TransportType
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.profile.AvatarAssets

enum class SortOption { RSSI, NAME, STATUS }

data class ActivePacketEvent(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val targetId: String,
    val packetType: PacketType
)

data class TrafficState(
    val packetCount: Int = 0,
    val latestPacketEvent: ActivePacketEvent? = null
)

data class NearbyUiState(
    val devices: List<BleDevice> = emptyList(),
    val sortOption: SortOption = SortOption.RSSI,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val packetCount: Int = 0,
    val latestPacketEvent: ActivePacketEvent? = null
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.RSSI)
    private val _isScanning = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _trafficState = MutableStateFlow(TrafficState())

    init {
        viewModelScope.launch {
            meshRepository.incomingMeshPayloads.collect { (senderId, meshPacket) ->
                _trafficState.value = TrafficState(
                    packetCount = _trafficState.value.packetCount + 1,
                    latestPacketEvent = ActivePacketEvent(
                        senderId = senderId,
                        targetId = meshPacket.targetId,
                        packetType = meshPacket.type
                    )
                )
            }
        }
    }

    private val devicesWithIdentity = combine(
        meshRepository.scannedDevices,
        userRepository.peerIdentities,
        userRepository.localIdentity
    ) { bleMap, peers, local ->
        bleMap.values.map { device ->
            val peerIdentity = peers[device.meshId] ?: peers[device.address] ?: (if (local?.userId == device.meshId) local else null)
            if (peerIdentity != null) {
                val avatarUriStr = when {
                    peerIdentity.galleryImageUri != null -> peerIdentity.galleryImageUri
                    peerIdentity.cameraImageUri != null -> peerIdentity.cameraImageUri
                    peerIdentity.selectedAvatarId != null -> AvatarAssets.buildAvatarUri(peerIdentity.selectedAvatarId)
                    else -> device.avatarUri
                }
                device.copy(
                    name = if (device.name.isBlank() || device.name == "Nearby Node") peerIdentity.displayName else device.name,
                    avatarUri = avatarUriStr
                )
            } else {
                device
            }
        }
    }

    val uiState: StateFlow<NearbyUiState> = combine(
        devicesWithIdentity,
        _sortOption,
        _isScanning,
        _errorMessage,
        _trafficState
    ) { devices, sortOption, isScanning, errorMessage, traffic ->
        val sortedList = when (sortOption) {
            SortOption.RSSI -> devices.sortedByDescending { it.rssi }
            SortOption.NAME -> devices.sortedBy { it.name.ifBlank { "~" } }
            SortOption.STATUS -> devices.sortedByDescending { it.isConnected }
        }
        
        NearbyUiState(
            devices = sortedList, 
            sortOption = sortOption,
            isScanning = isScanning,
            errorMessage = errorMessage,
            packetCount = traffic.packetCount,
            latestPacketEvent = traffic.latestPacketEvent
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
            if (device.transport == TransportType.BLE) {
                meshRepository.connectToPeer(device.address)
            }
            onConnected()
        }
    }
}
