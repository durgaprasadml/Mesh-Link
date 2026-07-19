package com.meshlink.ui.mesh

import androidx.lifecycle.ViewModel
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.repository.MeshRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.BleDevice

data class MeshDebugUiState(
    val scannedDevices: Map<String, BleDevice> = emptyMap(),
    val routeTable: Map<String, String> = emptyMap(),
    val localIdentifier: String = ""
)

@HiltViewModel
class MeshDebugViewModel @Inject constructor(
    private val meshRepository: MeshRepository
) : ViewModel() {

    val incomingMeshPayloads = meshRepository.incomingMeshPayloads.map { it.second }

    private val _routeTable = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        viewModelScope.launch {
            while (true) {
                _routeTable.value = meshRepository.getRouteTable()
                delay(1000)
            }
        }
    }

    val uiState = combine(
        meshRepository.scannedDevices,
        _routeTable
    ) { devices, routes ->
        MeshDebugUiState(
            scannedDevices = devices,
            routeTable = routes,
            localIdentifier = meshRepository.getLocalMeshId()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeshDebugUiState())
}
