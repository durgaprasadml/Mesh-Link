package com.meshlink.alarm

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.meshlink.R
import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EmergencyAlarmManager {
    private const val TAG = "EmergencyAlarmManager"
    private const val WAKE_LOCK_TAG = "MeshLink:EmergencyAlarmWakeLock"

    private val _isAlarmPlaying = MutableStateFlow(false)
    val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousSpeakerState: Boolean? = null

    @Synchronized
    fun toggleAlarm(context: Context) {
        if (_isAlarmPlaying.value) {
            stopAlarm(context)
        } else {
            startAlarm(context)
        }
    }

    @Synchronized
    fun startAlarm(context: Context) {
        if (_isAlarmPlaying.value) {
            MeshLogger.d(TAG, "Alarm is already playing")
            return
        }

        MeshLogger.d(TAG, "Starting Emergency Loud Alarm")
        val appContext = context.applicationContext

        try {
            // 1. Configure Maximum Volume on STREAM_ALARM
            setMaxAlarmVolume(appContext)

            // 2. Request Audio Focus & Speaker Routing
            setupAudioOutput(appContext)

            // 3. Acquire Screen WakeLock
            acquireWakeLock(appContext)

            // 4. Start Continuous Vibration
            startVibration(appContext)

            // 5. Start Foreground Service (which manages high-priority notification & launch)
            EmergencyAlarmService.startService(appContext)

            // 6. Launch Full-Screen Emergency Alarm Activity
            launchEmergencyActivity(appContext)

            // 7. Start Media Player Playback (with 1 retry attempt)
            startMediaPlayer(appContext, isRetry = false)

            _isAlarmPlaying.value = true
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Fatal error during alarm start", e)
            stopAlarm(appContext)
        }
    }

    @Synchronized
    fun stopAlarm(context: Context) {
        MeshLogger.d(TAG, "Stopping Emergency Loud Alarm")
        val appContext = context.applicationContext

        // Stop Media Player
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        // Abandon Audio Focus & Reset Audio Routing
        cleanupAudioOutput(appContext)

        // Stop Vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error cancelling vibration", e)
        } finally {
            vibrator = null
        }

        // Release WakeLock
        releaseWakeLock()

        // Stop Foreground Service
        EmergencyAlarmService.stopService(appContext)

        _isAlarmPlaying.value = false
    }

    private fun setMaxAlarmVolume(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to set max alarm volume", e)
        }
    }

    private fun setupAudioOutput(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(audioAttributes)
                    .build()
                audioManager.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }

            // Force audio output to speaker
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val speakerDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speakerDevice != null) {
                    audioManager.setCommunicationDevice(speakerDevice)
                }
            } else {
                @Suppress("DEPRECATION")
                previousSpeakerState = audioManager.isSpeakerphoneOn
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed setting up audio focus / speaker output", e)
        }
    }

    private fun cleanupAudioOutput(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    audioManager.abandonAudioFocusRequest(request)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                previousSpeakerState?.let { oldState ->
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = oldState
                    previousSpeakerState = null
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed cleaning up audio output", e)
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock(context: Context) {
        try {
            if (wakeLock?.isHeld == true) return
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                WAKE_LOCK_TAG
            ).apply {
                acquire()
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to release wake lock", e)
        } finally {
            wakeLock = null
        }
    }

    private fun startVibration(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start continuous vibration", e)
        }
    }

    private fun startMediaPlayer(context: Context, isRetry: Boolean) {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer = MediaPlayer.create(context, R.raw.emergency_alarm, audioAttributes, 0)?.apply {
                isLooping = true
                start()
            } ?: run {
                // Manual fallback creation if create returns null
                MediaPlayer().apply {
                    val afd = context.resources.openRawResourceFd(R.raw.emergency_alarm)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    prepare()
                    start()
                }
            }
            MeshLogger.d(TAG, "MediaPlayer playing emergency_alarm.mp3 successfully")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "MediaPlayer error (isRetry=$isRetry)", e)
            if (!isRetry) {
                // Retry once
                try {
                    Thread.sleep(200)
                } catch (_: InterruptedException) {}
                startMediaPlayer(context, isRetry = true)
            } else {
                MeshLogger.e(TAG, "MediaPlayer retry failed. Stopping alarm gracefully.")
                stopAlarm(context)
            }
        }
    }

    private fun launchEmergencyActivity(context: Context) {
        try {
            val activityIntent = Intent(context, EmergencyAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to launch EmergencyAlarmActivity", e)
        }
    }
}
