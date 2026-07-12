package com.meshlink.data.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRecorder @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val MAX_DURATION_MS = 10_000L
        private const val TAG = "VoiceRecorder"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var timerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    fun startRecording(): Boolean {
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
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setMaxDuration(MAX_DURATION_MS.toInt())
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            _elapsedMs.value = 0L

            // Timer with auto-stop at 10 seconds
            timerJob = scope.launch {
                val startTime = System.currentTimeMillis()
                while (isActive && _isRecording.value) {
                    val elapsed = System.currentTimeMillis() - startTime
                    _elapsedMs.value = elapsed
                    if (elapsed >= MAX_DURATION_MS) {
                        withContext(Dispatchers.Main) {
                            stopRecording()
                        }
                        break
                    }
                    delay(100)
                }
            }

            Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            cleanup()
            false
        }
    }

    /**
     * Stop recording and return the file path and duration.
     * Returns null if recording failed.
     */
    fun stopRecording(): Pair<String, Long>? {
        return try {
            val duration = _elapsedMs.value
            timerJob?.cancel()
            timerJob = null

            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            _isRecording.value = false

            val path = outputFile?.absolutePath
            if (path != null && File(path).exists()) {
                Log.d(TAG, "Recording stopped: $path (${duration}ms)")
                path to duration
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording: ${e.message}")
            cleanup()
            null
        }
    }

    fun cancelRecording() {
        cleanup()
    }

    private fun cleanup() {
        timerJob?.cancel()
        timerJob = null
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        recorder = null
        _isRecording.value = false
        _elapsedMs.value = 0L
        outputFile?.delete()
    }
}
