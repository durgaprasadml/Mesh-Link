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
        val mediaDir = File(context.filesDir, "mesh_media")
        if (!mediaDir.exists()) mediaDir.mkdirs()

        // 1. Primary: AMR-WB (16kHz mono voice compression with minimal container overhead)
        var started = startRecordingWithFormat(
            mediaDir = mediaDir,
            fileName = "voice_${System.currentTimeMillis()}.amr",
            outputFormat = MediaRecorder.OutputFormat.AMR_WB,
            audioEncoder = MediaRecorder.AudioEncoder.AMR_WB,
            samplingRate = 16_000,
            bitRate = 16_000
        )

        // 2. Fallback: AAC in MPEG-4 container (standard Android fallback)
        if (!started) {
            MeshLogger.w(TAG, "AMR-WB recording failed to start, falling back to AAC-M4A")
            cleanupInternal()
            started = startRecordingWithFormat(
                mediaDir = mediaDir,
                fileName = "voice_${System.currentTimeMillis()}.m4a",
                outputFormat = MediaRecorder.OutputFormat.MPEG_4,
                audioEncoder = MediaRecorder.AudioEncoder.AAC,
                samplingRate = 16_000,
                bitRate = 16_000
            )
        }

        return started
    }

    private fun startRecordingWithFormat(
        mediaDir: File,
        fileName: String,
        outputFormat: Int,
        audioEncoder: Int,
        samplingRate: Int,
        bitRate: Int
    ): Boolean {
        return try {
            outputFile = File(mediaDir, fileName)

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(outputFormat)
                setAudioEncoder(audioEncoder)
                setAudioChannels(1) // Mono
                setAudioEncodingBitRate(bitRate)
                setAudioSamplingRate(samplingRate)
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

            MeshLogger.d(TAG, "Recording started ($fileName): ${outputFile?.absolutePath}")
            true
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start recording ($fileName): ${e.message}")
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            outputFile?.delete()
            outputFile = null
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
        val recordedFile = path?.let { File(it) }
        val isValid = stopSuccessful && duration >= 300L && recordedFile != null && recordedFile.exists() && recordedFile.canRead() && recordedFile.length() > 0L

        MeshLogger.i(TAG, "[AUDIO_FILE_CHECK] path=$path exists=${recordedFile?.exists()} canRead=${recordedFile?.canRead()} length=${recordedFile?.length()}B duration=${duration}ms stopSuccessful=$stopSuccessful valid=$isValid")

        return if (isValid) {
            MeshLogger.i(TAG, "[AUDIO_FILE_READY] Recording finalized successfully: $path (${duration}ms, ${recordedFile!!.length()}B)")
            path to duration
        } else {
            val discardReason = "duration=${duration}ms, exists=${recordedFile?.exists()}, length=${recordedFile?.length()}"
            MeshLogger.w(TAG, "[AUDIO_TRANSFER_FAILURE] Recording discarded ($discardReason)")
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
