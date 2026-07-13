package com.meshlink.media.data

import android.media.MediaPlayer
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.DefaultDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VoicePlayer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "VoicePlayer"
    }

    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    // Currently playing file path (null = nothing playing)
    private val _currentlyPlaying = MutableStateFlow<String?>(null)
    val currentlyPlaying: StateFlow<String?> = _currentlyPlaying.asStateFlow()

    // Playback progress 0.0 to 1.0
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    fun play(filePath: String) {
        // Validate file exists before attempting playback
        val file = java.io.File(filePath)
        if (!file.exists() || file.length() == 0L) {
            MeshLogger.e(TAG, "Cannot play: file missing or empty: $filePath")
            return
        }

        // If already playing this file, toggle pause
        if (_currentlyPlaying.value == filePath) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    pause()
                    return
                }
            } catch (_: Exception) { /* IllegalStateException */ }
        }

        // Stop any existing playback
        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }

            _currentlyPlaying.value = filePath
            _progress.value = 0f

            mediaPlayer?.setOnCompletionListener {
                stop()
            }

            // Progress tracking loop
            progressJob = scope.launch {
                while (isActive) {
                    try {
                        val player = mediaPlayer ?: break
                        if (!player.isPlaying) break
                        val current = player.currentPosition.toFloat()
                        val total = player.duration.toFloat()
                        if (total > 0) {
                            _progress.value = current / total
                        }
                    } catch (_: Exception) { break }
                    delay(100)
                }
            }

            MeshLogger.d(TAG, "Playing: $filePath")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Playback failed: ${e.message}")
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        progressJob?.cancel()
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _currentlyPlaying.value = null
        _progress.value = 0f
    }
}
