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

data class SosUiState(
    val isFetchingLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPercent: Int = 0,
    val sosSent: Boolean = false,
    val isSending: Boolean = false
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
            _uiState.update { it.copy(isSending = true) }
            try {
                meshRepository.sendSos()
                _uiState.update { it.copy(isSending = false, sosSent = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }
}
