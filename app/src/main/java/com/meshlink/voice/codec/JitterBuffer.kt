package com.meshlink.voice.codec

import com.meshlink.voice.models.AudioFrame
import java.util.concurrent.PriorityBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class JitterBuffer @Inject constructor(
    @com.meshlink.di.DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    companion object {
        // Buffers approx 150ms of audio (assuming 20ms frames)
        private const val TARGET_BUFFER_SIZE = 8 
        private const val MAX_BUFFER_SIZE = 30
        private const val POP_DELAY_MS = 20L
    }

    private val scope = CoroutineScope(defaultDispatcher)
    private var playoutJob: Job? = null

    // Min-heap based on sequenceNumber
    private val buffer = PriorityBlockingQueue<AudioFrame>(MAX_BUFFER_SIZE, compareBy { it.sequenceNumber })
    
    var onFrameReadyForDecode: ((AudioFrame) -> Unit)? = null
    
    private var isBuffering = true
    private var lastPlayedSeqNum: Long = -1

    fun start() {
        isBuffering = true
        lastPlayedSeqNum = -1
        buffer.clear()
        
        playoutJob = scope.launch {
            while (isActive) {
                if (isBuffering) {
                    if (buffer.size >= TARGET_BUFFER_SIZE) {
                        isBuffering = false
                    } else {
                        delay(10)
                        continue
                    }
                }

                val nextFrame = buffer.peek()
                if (nextFrame != null) {
                    // Check if it's the right time to play it or if we should skip missing frames
                    if (lastPlayedSeqNum == -1L || nextFrame.sequenceNumber > lastPlayedSeqNum) {
                        val frame = buffer.poll()
                        if (frame != null) {
                            lastPlayedSeqNum = frame.sequenceNumber
                            onFrameReadyForDecode?.invoke(frame)
                        }
                    } else {
                        // Old frame arrived late, discard
                        buffer.poll()
                        continue
                    }
                } else {
                    // Underflow
                    isBuffering = true
                    // Packet Loss Concealment (PLC): invoke with empty/repeated frame if needed
                    // For now, we just wait and glitch. Real PLC would reuse the last decoded PCM block.
                }

                delay(POP_DELAY_MS)
            }
        }
    }

    fun stop() {
        playoutJob?.cancel()
        buffer.clear()
    }

    fun addFrame(frame: AudioFrame) {
        if (buffer.size < MAX_BUFFER_SIZE) {
            buffer.add(frame)
        }
    }
}
