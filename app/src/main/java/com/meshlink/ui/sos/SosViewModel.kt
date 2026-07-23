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
    val nearbyResponders: List<BleDevice> = emptyList(),
    val relaysReached: Int = 0,
    val errorMessage: String? = null,
    val isFlashlightOn: Boolean = false,
    val isAlarmPlaying: Boolean = false
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val locationProvider: LocationProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var cameraId: String? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    init {
        refreshLocation()
        
        viewModelScope.launch {
            meshRepository.scannedDevices.collect { devices ->
                _uiState.update { it.copy(nearbyResponders = devices.values.toList()) }
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
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                
                audioFocusRequest?.let { request ->
                    audioManager.abandonAudioFocusRequest(request)
                    audioFocusRequest = null
                }
                
                _uiState.update { it.copy(isAlarmPlaying = false) }
            } else {
                val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                    
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(audioAttributes)
                    .build()
                    
                audioManager.requestAudioFocus(audioFocusRequest!!)
                
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alarmUri)
                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    prepare()
                    start()
                }
                _uiState.update { it.copy(isAlarmPlaying = true) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Alarm unavailable: ${e.message}") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
            
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                audioFocusRequest = null
            }
            
            if (uiState.value.isFlashlightOn) {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraId?.let { id -> cameraManager.setTorchMode(id, false) }
            }
        } catch (e: Exception) {
            // Ignore during cleanup
        }
    }
}
