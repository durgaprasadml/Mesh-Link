package com.meshlink.voice.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class AudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "AudioEngine"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    var onAudioDataReady: ((ByteArray) -> Unit)? = null
    var isRecording = false
        private set

    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            MeshLogger.e(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                MeshLogger.e(TAG, "AudioRecord initialization failed")
                return
            }

            // Enable Hardware Audio Effects if available
            val audioSessionId = audioRecord!!.audioSessionId
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioSessionId)
                aec?.enabled = true
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(audioSessionId)
                ns?.enabled = true
            }
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(audioSessionId)
                agc?.enabled = true
            }

            audioRecord?.startRecording()
            isRecording = true

            scope.launch {
                val buffer = ByteArray(minBufferSize)
                while (isActive && isRecording) {
                    val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readResult > 0) {
                        onAudioDataReady?.invoke(buffer.copyOf(readResult))
                    }
                }
            }
            MeshLogger.d(TAG, "Started recording")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start recording: ${e.message}")
            stopRecording()
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            aec?.release()
            ns?.release()
            agc?.release()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping recording: ${e.message}")
        } finally {
            audioRecord = null
            aec = null
            ns = null
            agc = null
        }
    }

    fun startPlayback() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, 
            AudioFormat.CHANNEL_OUT_MONO, 
            AUDIO_FORMAT
        )

        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT,
                minBufferSize * 2,
                AudioTrack.MODE_STREAM
            )

            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.play()
                MeshLogger.d(TAG, "Started playback")
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start playback: ${e.message}")
        }
    }

    fun playAudioData(pcmData: ByteArray) {
        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
            try {
                audioTrack?.write(pcmData, 0, pcmData.size)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Error writing to AudioTrack: ${e.message}")
            }
        }
    }

    fun stopPlayback() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping playback: ${e.message}")
        } finally {
            audioTrack = null
        }
    }
}
