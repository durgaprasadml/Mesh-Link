package com.meshlink.media.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.di.DefaultDispatcher
import com.meshlink.di.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val MAX_DURATION_MS = 60_000L
        private const val TAG = "VoiceRecorder"
    }

    private val recorderLock = Any()

    @Volatile private var isRecorderStarted = false
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var timerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    fun startRecording(): Boolean = synchronized(recorderLock) {
        cleanupInternal()
        return try {
            val mediaDir = File(context.filesDir, "mesh_media")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            outputFile = File(mediaDir, "voice_${System.currentTimeMillis()}.m4a")

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1) // Mono
                setAudioEncodingBitRate(16_000) // 16 kbps
                setAudioSamplingRate(16_000) // 16 kHz
                setMaxDuration(MAX_DURATION_MS.toInt())
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            isRecorderStarted = true
            _isRecording.value = true
            _elapsedMs.value = 0L

            // Timer with auto-stop at MAX_DURATION_MS
            timerJob = applicationScope.launch(defaultDispatcher) {
                val startTime = System.currentTimeMillis()
                while (isActive && _isRecording.value) {
                    val elapsed = System.currentTimeMillis() - startTime
                    _elapsedMs.value = elapsed
                    if (elapsed >= MAX_DURATION_MS) {
                        withContext(mainDispatcher) {
                            stopRecording()
                        }
                        break
                    }
                    delay(100)
                }
            }

            MeshLogger.d(TAG, "Recording started: ${outputFile?.absolutePath}")
            true
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start recording: ${e.message}")
            cleanupInternal()
            false
        }
    }

    /**
     * Stop recording and return the file path and duration.
     * Returns null if recording failed or duration was too short (<300ms).
     */
    fun stopRecording(): Pair<String, Long>? = synchronized(recorderLock) {
        val duration = _elapsedMs.value
        timerJob?.cancel()
        timerJob = null

        val wasStarted = isRecorderStarted
        isRecorderStarted = false

        val rec = recorder
        recorder = null
        _isRecording.value = false

        var stopSuccessful = false
        if (wasStarted && rec != null) {
            try {
                rec.stop()
                stopSuccessful = true
            } catch (e: Exception) {
                MeshLogger.w(TAG, "MediaRecorder stop failed (e.g. recording too short): ${e.message}")
            } finally {
                try {
                    rec.release()
                } catch (_: Exception) {}
            }
        }

        val path = outputFile?.absolutePath
        return if (stopSuccessful && duration >= 300L && path != null && File(path).exists()) {
            MeshLogger.d(TAG, "Recording stopped successfully: $path (${duration}ms)")
            path to duration
        } else {
            MeshLogger.w(TAG, "Recording discarded (duration=${duration}ms, path=$path)")
            outputFile?.delete()
            outputFile = null
            null
        }
    }

    fun cancelRecording() = synchronized(recorderLock) {
        cleanupInternal()
    }

    private fun cleanupInternal() {
        timerJob?.cancel()
        timerJob = null

        val wasStarted = isRecorderStarted
        isRecorderStarted = false

        val rec = recorder
        recorder = null
        _isRecording.value = false
        _elapsedMs.value = 0L

        if (rec != null) {
            if (wasStarted) {
                try {
                    rec.stop()
                } catch (_: Exception) {}
            }
            try {
                rec.release()
            } catch (_: Exception) {}
        }

        outputFile?.delete()
        outputFile = null
    }
}
