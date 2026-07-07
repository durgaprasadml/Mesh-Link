package com.meshlink.ui.mesh

import androidx.lifecycle.ViewModel
import com.meshlink.data.ble.MeshPacket
import com.meshlink.data.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MeshDebugViewModel @Inject constructor(
    private val bleRepository: BleRepository
) : ViewModel() {

    val incomingMeshPayloads = bleRepository.incomingMeshPayloads.map { it.second }
    val scannedDevices = bleRepository.scannedDevices
    
    fun getKnownRoutes(): Map<String, String> {
        return bleRepository.meshRouter.routeTable.toMap()
    }
    
    fun getLocalIdentifier(): String {
        return bleRepository.meshRouter.localMeshId
    }
}
