package com.meshlink.voice.streaming

import com.meshlink.common.logger.MeshLogger
import com.meshlink.voice.audio.AudioEngine
import com.meshlink.voice.codec.JitterBuffer
import com.meshlink.voice.codec.VoiceCodecManager
import com.meshlink.voice.models.AudioFrame
import com.meshlink.voice.transport.VoiceTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class AudioStreamer @Inject constructor(
    private val audioEngine: AudioEngine,
    private val codecManager: VoiceCodecManager,
    private val jitterBuffer: JitterBuffer,
    private val transport: VoiceTransport
) {
    companion object {
        private const val TAG = "AudioStreamer"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var isStreaming = false
    private var currentCallId: String? = null
    private var currentTargetId: String? = null
    private var currentSenderId: String? = null
    
    private var outboundSeqNum = 0L

    init {
        // Microphone -> Encoder -> Transport
        audioEngine.onAudioDataReady = { pcmData ->
            if (isStreaming) {
                codecManager.encode(pcmData)
            }
        }
        
        codecManager.onEncodedData = { encodedData ->
            if (isStreaming) {
                currentCallId?.let { callId ->
                    currentTargetId?.let { targetId ->
                        currentSenderId?.let { senderId ->
                            transport.sendVoiceFrame(senderId, targetId, callId, outboundSeqNum++, encodedData)
                        }
                    }
                }
            }
        }

        // Transport -> JitterBuffer -> Decoder -> Speaker
        transport.onIncomingFrame = { encodedData, callId, seqNum ->
            if (isStreaming && callId == currentCallId) {
                val frame = AudioFrame(callId, seqNum, encodedData)
                jitterBuffer.addFrame(frame)
            }
        }

        jitterBuffer.onFrameReadyForDecode = { frame ->
            codecManager.decode(frame.payload)
        }

        codecManager.onDecodedData = { pcmData ->
            audioEngine.playAudioData(pcmData)
        }
    }

    fun startStreaming(senderId: String, targetId: String, callId: String, bitrate: Int) {
        if (isStreaming) return
        isStreaming = true
        currentCallId = callId
        currentTargetId = targetId
        currentSenderId = senderId
        outboundSeqNum = 0L

        codecManager.startEncoder(bitrate)
        codecManager.startDecoder()
        jitterBuffer.start()
        audioEngine.startRecording()
        audioEngine.startPlayback()
        MeshLogger.d(TAG, "Started full-duplex streaming for call $callId")
    }

    fun stopStreaming() {
        if (!isStreaming) return
        isStreaming = false
        audioEngine.stopRecording()
        audioEngine.stopPlayback()
        jitterBuffer.stop()
        codecManager.stopEncoder()
        codecManager.stopDecoder()
        currentCallId = null
        currentTargetId = null
        currentSenderId = null
        MeshLogger.d(TAG, "Stopped streaming")
    }
}
