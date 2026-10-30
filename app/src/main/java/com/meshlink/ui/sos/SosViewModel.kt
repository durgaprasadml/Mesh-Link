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
    PREPARING,
    PENDING,
    SENDING,
    SENT,
    FAILED,
    CANCELLED;

    companion object {
        @Deprecated("Use SENT instead", ReplaceWith("SENT"))
        val DELIVERED = SENT
        @Deprecated("Use SENT instead", ReplaceWith("SENT"))
        val PARTIALLY_DELIVERED = SENT
        @Deprecated("Use PENDING instead", ReplaceWith("PENDING"))
        val QUEUED = PENDING
        @Deprecated("Use PREPARING instead", ReplaceWith("PREPARING"))
        val ENCRYPTING = PREPARING
    }
}

@Immutable
data class SosUiState(
    val status: SosStatus = SosStatus.SAFE,
    val currentSosId: String? = null,
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
    private var sosJob: kotlinx.coroutines.Job? = null
    private var retryJob: kotlinx.coroutines.Job? = null
    private var currentPendingSosId: String? = null

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
            currentStatus == SosStatus.PREPARING ||
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

        sosJob?.cancel()
        sosJob = viewModelScope.launch {
            val sosEventId = UUID.randomUUID().toString()
            currentPendingSosId = sosEventId
            try {
                // 1. Bounded Location Collection (3 seconds max timeout)
                val location = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                    locationProvider.getCurrentLocation()
                }
                val lat = location?.latitude
                val lng = location?.longitude
                val battery = location?.batteryPercent ?: locationProvider.getBatteryPercent()

                // 2. Bounded Dual Camera Capture (5 seconds max timeout)
                var frontFile: File? = null
                var rearFile: File? = null
                var captureStatus = "Camera unavailable"

                if (hasCamera) {
                    _uiState.update { it.copy(status = SosStatus.CAPTURING_FRONT, cameraCaptureStatus = "Capturing emergency photos...") }
                    val captureResult = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                        cameraController.captureDualSosImages(sosEventId)
                    }
                    if (captureResult != null) {
                        frontFile = captureResult.frontImage
                        rearFile = captureResult.rearImage
                        captureStatus = when {
                            captureResult.bothSucceeded -> "Front and rear photos captured"
                            frontFile != null -> "Front photo captured (rear unavailable: ${captureResult.rearError})"
                            rearFile != null -> "Rear photo captured (front unavailable: ${captureResult.frontError})"
                            else -> "Camera capture unavailable: ${captureResult.frontError ?: captureResult.rearError ?: "unknown"}"
                        }
                    } else {
                        captureStatus = "Camera capture timed out; photos marked unavailable"
                    }
                } else {
                    captureStatus = "Camera permission denied; alert prepared without photos."
                }

                _uiState.update {
                    it.copy(
                        status = SosStatus.PREPARING,
                        currentSosId = sosEventId,
                        latitude = lat,
                        longitude = lng,
                        batteryPercent = battery,
                        frontImagePath = frontFile?.absolutePath,
                        rearImagePath = rearFile?.absolutePath,
                        cameraCaptureStatus = captureStatus
                    )
                }

                // 3. Persist Prepared Package as PENDING
                _uiState.update { it.copy(status = SosStatus.PENDING) }

                // 4. Transmit over Wi-Fi Direct or Schedule Bounded Retries
                transmitOrScheduleRetry(sosEventId, frontFile, rearFile)

            } catch (e: Exception) {
                MeshLogger.e("SosViewModel", "Error preparing SOS package: ${e.message}", e)
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

    private suspend fun transmitOrScheduleRetry(
        sosEventId: String,
        frontFile: File?,
        rearFile: File?
    ) {
        _uiState.update { it.copy(status = SosStatus.SENDING) }
        val result = meshRepository.dispatchSos(sosEventId)
        if (frontFile != null || rearFile != null) {
            meshRepository.sendSosMedia(sosEventId, frontFile, rearFile)
        }

        val reachableRelays = meshRepository.scannedDevices.value.size
        if (result is com.meshlink.domain.model.MeshResult.Success && reachableRelays > 0) {
            _uiState.update {
                it.copy(
                    isSending = false,
                    sosSent = true,
                    status = SosStatus.SENT,
                    relaysReached = reachableRelays
                )
            }
        } else {
            // Retain visible PENDING status when Wi-Fi Direct is not currently available
            _uiState.update {
                it.copy(
                    isSending = false,
                    status = SosStatus.PENDING,
                    relaysReached = 0
                )
            }
            startBoundedRetryLoop(sosEventId, frontFile, rearFile)
        }
    }

    private fun startBoundedRetryLoop(
        sosEventId: String,
        frontFile: File?,
        rearFile: File?
    ) {
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 10
            while (attempts < maxAttempts && _uiState.value.status == SosStatus.PENDING) {
                kotlinx.coroutines.delay(15_000L) // Bounded frequency: retry every 15 seconds
                attempts++
                if (_uiState.value.status != SosStatus.PENDING) break
                MeshLogger.i("SosViewModel", "Retrying pending SOS $sosEventId (attempt $attempts/$maxAttempts)...")
                val res = meshRepository.dispatchSos(sosEventId)
                if (frontFile != null || rearFile != null) {
                    meshRepository.sendSosMedia(sosEventId, frontFile, rearFile)
                }
                val reachable = meshRepository.scannedDevices.value.size
                if (res is com.meshlink.domain.model.MeshResult.Success && reachable > 0) {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sosSent = true,
                            status = SosStatus.SENT,
                            relaysReached = reachable
                        )
                    }
                    break
                }
            }
        }
    }

    fun cancelSos() {
        val currentStatus = _uiState.value.status
        if (currentStatus == SosStatus.PENDING || currentStatus == SosStatus.ACTIVATING || currentStatus == SosStatus.PREPARING) {
            sosJob?.cancel()
            retryJob?.cancel()
            val sosId = currentPendingSosId
            if (sosId != null) {
                viewModelScope.launch {
                    meshRepository.cancelPendingSos(sosId)
                }
            }
            // Delete temporary captured files
            _uiState.value.frontImagePath?.let { File(it).delete() }
            _uiState.value.rearImagePath?.let { File(it).delete() }

            _uiState.update {
                it.copy(
                    status = SosStatus.CANCELLED,
                    isSending = false,
                    sosSent = false,
                    frontImagePath = null,
                    rearImagePath = null,
                    cameraCaptureStatus = "SOS cancelled"
                )
            }
        } else if (currentStatus == SosStatus.SENT) {
            MeshLogger.w("SosViewModel", "Cannot cancel SOS: package has already been sent. Sent records are preserved.")
        }
    }

    fun resetSos() {
        if (_uiState.value.status == SosStatus.PENDING) {
            cancelSos()
        }
        sosJob?.cancel()
        retryJob?.cancel()
        currentPendingSosId = null
        _uiState.update {
            it.copy(
                status = SosStatus.SAFE,
                isSending = false,
                sosSent = false,
                errorMessage = null,
                relaysReached = 0,
                frontImagePath = null,
                rearImagePath = null,
                cameraCaptureStatus = null
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
