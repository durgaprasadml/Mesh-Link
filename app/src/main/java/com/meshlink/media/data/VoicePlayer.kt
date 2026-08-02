package com.meshlink.media.data

import android.media.MediaPlayer
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.di.DefaultDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VoicePlayer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    companion object {
        private const val TAG = "VoicePlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    // Currently playing file path (null = nothing playing)
    private val _currentlyPlaying = MutableStateFlow<String?>(null)
    val currentlyPlaying: StateFlow<String?> = _currentlyPlaying.asStateFlow()

    // Playback progress 0.0 to 1.0
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    fun play(filePath: String) {
        val file = java.io.File(filePath)
        if (!file.exists() || file.length() == 0L) {
            MeshLogger.e(TAG, "Cannot play: file missing or empty: $filePath")
            stop()
            return
        }

        // If already playing this file, toggle pause / resume
        if (_currentlyPlaying.value == filePath) {
            try {
                val player = mediaPlayer
                if (player != null) {
                    if (player.isPlaying) {
                        pause()
                        return
                    } else {
                        player.start()
                        startProgressLoop()
                        return
                    }
                }
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Failed to toggle pause for $filePath: ${e.message}")
            }
        }

        // Stop any existing playback
        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnErrorListener { _, what, extra ->
                    MeshLogger.e(TAG, "MediaPlayer error: what=$what extra=$extra for $filePath")
                    stop()
                    true
                }
                setOnCompletionListener {
                    stop()
                }
                prepare()
                start()
            }

            _currentlyPlaying.value = filePath
            _progress.value = 0f

            startProgressLoop()
            MeshLogger.d(TAG, "Playing: $filePath")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Playback failed for $filePath: ${e.message}")
            stop()
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = applicationScope.launch(defaultDispatcher) {
            while (isActive) {
                try {
                    val player = mediaPlayer ?: break
                    if (!player.isPlaying) break
                    val current = player.currentPosition.toFloat()
                    val total = player.duration.toFloat()
                    if (total > 0) {
                        _progress.value = (current / total).coerceIn(0f, 1f)
                    }
                } catch (_: Exception) {
                    break
                }
                delay(100)
            }
        }
    }

    fun pause() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Error pausing player: ${e.message}")
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Error stopping player: ${e.message}")
        } finally {
            mediaPlayer = null
            _currentlyPlaying.value = null
            _progress.value = 0f
        }
    }
}
