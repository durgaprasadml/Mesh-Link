package com.meshlink.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.data.location.LocationProvider
import com.meshlink.domain.repository.MeshRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SosStatus {
    SAFE, BROADCASTING, DELIVERED, FAILED
}

data class SosUiState(
    val status: SosStatus = SosStatus.SAFE,
    val isFetchingLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPercent: Int = 0,
    val sosSent: Boolean = false,
    val isSending: Boolean = false,
    
    // New fields for the expanded UI
    val address: String? = null,
    val isBleEnabled: Boolean = true,
    val isWifiDirectEnabled: Boolean = true,
    val meshHealth: String = "Excellent",
    val connectedNodesCount: Int = 3,
    val relaysReached: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    init {
        refreshLocation()
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true) }
            val location = locationProvider.getCurrentLocation()
            _uiState.update {
                it.copy(
                    isFetchingLocation = false,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    batteryPercent = location?.batteryPercent ?: locationProvider.getBatteryPercent()
                )
            }
        }
    }

    fun sendSos() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isSending = true, 
                    status = SosStatus.BROADCASTING,
                    errorMessage = null
                ) 
            }
            try {
                meshRepository.sendSos()
                _uiState.update { 
                    it.copy(
                        isSending = false, 
                        sosSent = true,
                        status = SosStatus.DELIVERED,
                        relaysReached = 5 // Mock value for visual completion
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSending = false,
                        status = SosStatus.FAILED,
                        errorMessage = e.message ?: "Failed to broadcast SOS"
                    ) 
                }
            }
        }
    }
    
    fun resetSos() {
        _uiState.update {
            it.copy(
                status = SosStatus.SAFE,
                isSending = false,
                sosSent = false,
                errorMessage = null,
                relaysReached = 0
            )
        }
    }
}
