package com.meshlink.media.data

import android.media.MediaPlayer
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.di.DefaultDispatcher
import com.meshlink.di.IoDispatcher
import com.meshlink.di.MainDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton voice/audio message player.
 *
 * Key design decisions:
 * - All MediaPlayer lifecycle calls run on [mainDispatcher] (MediaPlayer is not thread-safe).
 * - File existence is checked on [ioDispatcher] before touching MediaPlayer.
 * - [prepareAsync] is used instead of the blocking [prepare] so the Main thread is never frozen
 *   during codec initialisation (which can take 200–800 ms for M4A/AAC files on low-end devices).
 * - [currentlyPreparing] lets the UI show a "Preparing…" indicator between the tap and playback.
 */
@Singleton
class VoicePlayer @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
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

    // File path currently being prepared by prepareAsync (null = not preparing)
    private val _currentlyPreparing = MutableStateFlow<String?>(null)
    val currentlyPreparing: StateFlow<String?> = _currentlyPreparing.asStateFlow()

    // Playback progress 0.0 to 1.0
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /**
     * Start playing [filePath], or toggle pause/resume if already loaded.
     *
     * Uses [prepareAsync] so this call returns immediately without blocking the caller's thread.
     * The player transitions: PREPARING → PLAYING once [OnPreparedListener] fires.
     */
    fun play(filePath: String) {
        applicationScope.launch(mainDispatcher) {
            playOnMain(filePath)
        }
    }

    private suspend fun playOnMain(filePath: String) {
        // File existence check on IO to avoid StrictMode / blocking Main
        val fileOk = withContext(ioDispatcher) {
            val f = java.io.File(filePath)
            f.exists() && f.length() > 0L
        }
        if (!fileOk) {
            MeshLogger.e(TAG, "Cannot play: file missing or empty: $filePath")
            stopOnMain()
            return
        }

        // If already loaded for this file, toggle pause / resume
        if (_currentlyPlaying.value == filePath || _currentlyPreparing.value == filePath) {
            val player = mediaPlayer
            if (player != null) {
                try {
                    if (player.isPlaying) {
                        player.pause()
                        progressJob?.cancel()
                        progressJob = null
                        return
                    } else if (_currentlyPreparing.value == null) {
                        // Prepared but paused — resume
                        player.start()
                        startProgressLoop()
                        return
                    } else {
                        // Still preparing — do nothing (tap is absorbed)
                        return
                    }
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "Toggle pause failed for $filePath: ${e.message}")
                }
            }
        }

        // Stop any existing playback / preparation before starting a new one
        stopOnMain()

        _currentlyPreparing.value = filePath

        try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setOnErrorListener { _, what, extra ->
                MeshLogger.e(TAG, "MediaPlayer error: what=$what extra=$extra for $filePath")
                applicationScope.launch(mainDispatcher) { stopOnMain() }
                true
            }

            player.setOnCompletionListener {
                applicationScope.launch(mainDispatcher) { stopOnMain() }
            }

            player.setOnPreparedListener { prepared ->
                // This callback is delivered on the thread that called prepareAsync —
                // for the internal MediaPlayer thread. We marshal back to Main immediately.
                applicationScope.launch(mainDispatcher) {
                    // Guard: another play() call may have already released this player
                    if (mediaPlayer !== prepared) return@launch
                    _currentlyPreparing.value = null
                    _currentlyPlaying.value = filePath
                    _progress.value = 0f
                    try {
                        prepared.start()
                        startProgressLoop()
                        MeshLogger.d(TAG, "Playing: $filePath")
                    } catch (e: Exception) {
                        MeshLogger.e(TAG, "start() failed for $filePath: ${e.message}")
                        stopOnMain()
                    }
                }
            }

            player.setDataSource(filePath)
            // Non-blocking — returns immediately; OnPreparedListener fires when ready
            player.prepareAsync()
            MeshLogger.d(TAG, "prepareAsync() called for $filePath")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Playback setup failed for $filePath: ${e.message}")
            stopOnMain()
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
        applicationScope.launch(mainDispatcher) {
            progressJob?.cancel()
            progressJob = null
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) it.pause()
                }
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Error pausing player: ${e.message}")
            }
        }
    }

    fun stop() {
        applicationScope.launch(mainDispatcher) {
            stopOnMain()
        }
    }

    private fun stopOnMain() {
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
            _currentlyPreparing.value = null
            _progress.value = 0f
        }
    }
}
