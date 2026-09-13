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
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.meshlink.domain.model.BleDevice
import com.meshlink.alarm.EmergencyAlarmManager

import androidx.compose.runtime.Immutable

import com.meshlink.common.logger.MeshLogger
import com.meshlink.video.camera.CameraController
import java.io.File
import java.util.UUID

enum class SosStatus {
    SAFE, BROADCASTING, DELIVERED, FAILED
}

@Immutable
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
    val nearbyResponders: List<BleDevice> = emptyList(),
    val relaysReached: Int = 0,
    val errorMessage: String? = null,
    val isFlashlightOn: Boolean = false,
    val isAlarmPlaying: Boolean = false,
    val frontImagePath: String? = null,
    val rearImagePath: String? = null,
    val cameraCaptureStatus: String? = null
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val locationProvider: LocationProvider,
    @ApplicationContext private val context: Context,
    private val cameraController: CameraController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var cameraId: String? = null

    init {
        refreshLocation()
        
        viewModelScope.launch {
            meshRepository.scannedDevices.collect { devices ->
                _uiState.update { it.copy(nearbyResponders = devices.values.toList()) }
            }
        }

        viewModelScope.launch {
            EmergencyAlarmManager.isAlarmPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isAlarmPlaying = isPlaying) }
            }
        }
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

    fun hasCameraPermission(): Boolean {
        return cameraController.hasCameraPermission()
    }

    fun sendSos(hasPermission: Boolean? = null) {
        if (_uiState.value.isSending || _uiState.value.status == SosStatus.BROADCASTING) {
            MeshLogger.w("SosViewModel", "SOS dispatch already in progress, ignoring duplicate trigger")
            return
        }

        val permissionGranted = hasPermission ?: cameraController.hasCameraPermission()

        _uiState.update { 
            it.copy(
                isSending = true, 
                status = SosStatus.BROADCASTING,
                errorMessage = null,
                frontImagePath = null,
                rearImagePath = null,
                cameraCaptureStatus = if (permissionGranted) "Capturing front and rear cameras..." else "Camera permission not granted"
            ) 
        }

        viewModelScope.launch {
            val sosEventId = UUID.randomUUID().toString()
            try {
                // 1. Immediately dispatch the SOS alert packet to the mesh
                val result = meshRepository.dispatchSos(sosEventId)
                val reachableRelays = meshRepository.scannedDevices.value.size

                // 2. Immediately start camera capture (if permitted)
                var frontFile: File? = null
                var rearFile: File? = null
                var captureStatus = "Camera unavailable"

                if (permissionGranted) {
                    val captureResult = cameraController.captureDualSosImages(sosEventId)
                    frontFile = captureResult.frontImage
                    rearFile = captureResult.rearImage

                    captureStatus = when {
                        captureResult.bothSucceeded -> "Front and rear cameras captured"
                        frontFile != null -> "Front camera captured (rear failed: ${captureResult.rearError})"
                        rearFile != null -> "Rear camera captured (front failed: ${captureResult.frontError})"
                        else -> "Camera capture failed: ${captureResult.frontError ?: captureResult.rearError ?: "unknown"}"
                    }

                    // 3. Immediately send captured photos through existing TransferManager pipeline
                    if (captureResult.hasAtLeastOneImage) {
                        meshRepository.sendSosMedia(sosEventId, frontFile, rearFile)
                    }
                } else {
                    captureStatus = "Camera permission denied; alert broadcasted without photos."
                }

                if (result is com.meshlink.domain.model.MeshResult.Success) {
                    _uiState.update { 
                        it.copy(
                            isSending = false, 
                            sosSent = true,
                            status = SosStatus.DELIVERED,
                            relaysReached = reachableRelays,
                            frontImagePath = frontFile?.absolutePath,
                            rearImagePath = rearFile?.absolutePath,
                            cameraCaptureStatus = captureStatus
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isSending = false, 
                            status = SosStatus.FAILED,
                            errorMessage = "Failed to broadcast SOS",
                            frontImagePath = frontFile?.absolutePath,
                            rearImagePath = rearFile?.absolutePath,
                            cameraCaptureStatus = captureStatus
                        ) 
                    }
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

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }



    fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            if (cameraId == null) {
                cameraId = cameraManager.cameraIdList.firstOrNull()
            }
            val newFlashlightState = !uiState.value.isFlashlightOn
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, newFlashlightState)
                _uiState.update { it.copy(isFlashlightOn = newFlashlightState) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Flashlight unavailable: ${e.message}") }
        }
    }

    fun toggleAlarm() {
        try {
            EmergencyAlarmManager.toggleAlarm(context)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Alarm unavailable: ${e.message}") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            if (uiState.value.isFlashlightOn) {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraId?.let { id -> cameraManager.setTorchMode(id, false) }
            }
        } catch (e: Exception) {
            // Ignore during cleanup
        }
    }
}
