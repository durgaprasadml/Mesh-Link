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
    SAFE,
    ACTIVATING,
    CAPTURING_FRONT,
    CAPTURING_REAR,
    ENCRYPTING,
    SENDING,
    DELIVERED,
    PARTIALLY_DELIVERED,
    QUEUED,
    FAILED
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
    
    // Readiness states
    val isCameraReady: Boolean = true,
    val isLocationReady: Boolean = true,
    val isBleReady: Boolean = true,
    val isWifiDirectReady: Boolean = true,
    val isSosSetupComplete: Boolean = true,

    // Expanded UI fields
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
        checkReadiness()
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

    fun checkReadiness() {
        val hasCamera = cameraController.hasCameraPermission()
        val hasLocation = com.meshlink.ui.components.isLocationPermissionGranted(context)
        val isBle = com.meshlink.ui.components.isBluetoothPermissionGranted(context) && com.meshlink.ui.components.isBluetoothEnabled(context)
        val isWifi = com.meshlink.ui.components.isWifiEnabled(context)
        val isReady = hasCamera && hasLocation && isBle && isWifi

        _uiState.update {
            it.copy(
                isCameraReady = hasCamera,
                isLocationReady = hasLocation,
                isBleReady = isBle,
                isWifiDirectReady = isWifi,
                isSosSetupComplete = isReady
            )
        }
    }

    fun refreshLocation() {
        checkReadiness()
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

    fun sendSos() {
        val currentStatus = _uiState.value.status
        if (_uiState.value.isSending ||
            currentStatus == SosStatus.ACTIVATING ||
            currentStatus == SosStatus.CAPTURING_FRONT ||
            currentStatus == SosStatus.CAPTURING_REAR ||
            currentStatus == SosStatus.ENCRYPTING ||
            currentStatus == SosStatus.SENDING) {
            MeshLogger.w("SosViewModel", "SOS dispatch already in progress, ignoring duplicate trigger")
            return
        }

        val hasCamera = cameraController.hasCameraPermission()

        _uiState.update { 
            it.copy(
                isSending = true, 
                status = SosStatus.ACTIVATING,
                errorMessage = null,
                frontImagePath = null,
                rearImagePath = null,
                cameraCaptureStatus = if (hasCamera) "Capturing emergency photos..." else "Camera permission not granted"
            ) 
        }

        viewModelScope.launch {
            val sosEventId = UUID.randomUUID().toString()
            try {
                // 1. Immediately dispatch the SOS alert packet to the mesh
                _uiState.update { it.copy(status = SosStatus.SENDING) }
                val result = meshRepository.dispatchSos(sosEventId)
                val reachableRelays = meshRepository.scannedDevices.value.size

                // 2. Immediately start camera capture (if permitted) - NEVER request permission here
                var frontFile: File? = null
                var rearFile: File? = null
                var captureStatus = "Camera unavailable"

                if (hasCamera) {
                    _uiState.update { it.copy(status = SosStatus.CAPTURING_FRONT, cameraCaptureStatus = "Capturing front camera...") }
                    val captureResult = cameraController.captureDualSosImages(sosEventId)
                    frontFile = captureResult.frontImage
                    rearFile = captureResult.rearImage

                    captureStatus = when {
                        captureResult.bothSucceeded -> "Front and rear cameras captured"
                        frontFile != null -> "Front camera captured (rear failed: ${captureResult.rearError})"
                        rearFile != null -> "Rear camera captured (front failed: ${captureResult.frontError})"
                        else -> "Camera capture failed: ${captureResult.frontError ?: captureResult.rearError ?: "unknown"}"
                    }

                    // 3. Encrypt and dispatch captured photos through existing TransferManager pipeline
                    if (captureResult.hasAtLeastOneImage) {
                        _uiState.update { it.copy(status = SosStatus.ENCRYPTING, cameraCaptureStatus = "Encrypting and dispatching photos...") }
                        meshRepository.sendSosMedia(sosEventId, frontFile, rearFile)
                    }
                } else {
                    captureStatus = "Camera permission denied; alert broadcasted without photos."
                }

                if (result is com.meshlink.domain.model.MeshResult.Success) {
                    val finalStatus = when {
                        reachableRelays == 0 -> SosStatus.QUEUED
                        hasCamera && (frontFile == null || rearFile == null) && (frontFile != null || rearFile != null) -> SosStatus.PARTIALLY_DELIVERED
                        else -> SosStatus.DELIVERED
                    }

                    _uiState.update { 
                        it.copy(
                            isSending = false, 
                            sosSent = true,
                            status = finalStatus,
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
