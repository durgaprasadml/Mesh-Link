package com.meshlink.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.data.repository.BleRepository
import com.meshlink.data.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SosState(
    val isFetchingLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPercent: Int = 0,
    val sosSent: Boolean = false,
    val isSending: Boolean = false
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val bleRepository: BleRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _sosState = MutableStateFlow(SosState())
    val sosState: StateFlow<SosState> = _sosState.asStateFlow()

    init {
        refreshLocation()
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _sosState.update { it.copy(isFetchingLocation = true) }
            val location = locationProvider.getCurrentLocation()
            _sosState.update {
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
            _sosState.update { it.copy(isSending = true) }
            try {
                bleRepository.sendSos()
                _sosState.update { it.copy(isSending = false, sosSent = true) }
            } catch (e: Exception) {
                _sosState.update { it.copy(isSending = false) }
            }
        }
    }
}
