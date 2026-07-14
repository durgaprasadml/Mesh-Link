package com.meshlink.video.streaming

import android.util.Size
import androidx.lifecycle.LifecycleOwner
import com.meshlink.common.logger.MeshLogger
import com.meshlink.video.camera.CameraController
import com.meshlink.video.codec.VideoCodecManager
import com.meshlink.video.transport.VideoTransport
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoStreamManager @Inject constructor(
    private val cameraController: CameraController,
    private val codecManager: VideoCodecManager,
    private val transport: VideoTransport
) {
    companion object {
        private const val TAG = "VideoStreamManager"
    }

    private var isStreaming = false
    private var currentCallId: String? = null
    private var currentTargetId: String? = null
    private var currentSenderId: String? = null
    
    private var outboundSeqNum = 0L
    private var lifecycleOwner: LifecycleOwner? = null

    init {
        // Encoder -> Transport
        codecManager.onEncodedFrame = { nalUnit, ptsUs ->
            if (isStreaming) {
                currentCallId?.let { _ ->
                    currentTargetId?.let { targetId ->
                        currentSenderId?.let { senderId ->
                            transport.sendVideoFrame(senderId, targetId, outboundSeqNum++, ptsUs, nalUnit)
                        }
                    }
                }
            }
        }

        // Transport -> Decoder
        transport.onIncomingFrame = { nalUnit, senderId, seqNum, ptsUs ->
            if (isStreaming && senderId == currentTargetId) {
                // Here we might need a JitterBuffer for video if frames get out of order, 
                // but for now we pipe directly to decoder.
                // Out-of-order video frames (especially P-frames) will cause corruption, 
                // so we should monitor sequence numbers and request KeyFrames on drops.
                codecManager.decodeFrame(nalUnit, ptsUs)
            }
        }
    }

    fun bindLifecycle(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
    }

    fun startStreaming(senderId: String, targetId: String, callId: String) {
        if (isStreaming) return
        val owner = lifecycleOwner
        if (owner == null) {
            MeshLogger.e(TAG, "Cannot start streaming: LifecycleOwner not bound")
            return
        }

        isStreaming = true
        currentCallId = callId
        currentTargetId = targetId
        currentSenderId = senderId
        outboundSeqNum = 0L

        // Default to 720p 30fps for Wi-Fi Direct. BLE would use 240p 10fps.
        val width = 720
        val height = 1280
        val fps = 30
        val bitrate = 1_500_000 // 1.5 Mbps
        
        codecManager.startEncoder(width, height, fps, bitrate)
        
        // Start camera and feed it the encoder surface
        codecManager.inputSurface?.let { surface ->
            cameraController.startCamera(owner, surface, Size(width, height))
        }

        MeshLogger.d(TAG, "Started full-duplex video streaming for call $callId")
    }

    fun stopStreaming() {
        if (!isStreaming) return
        isStreaming = false
        
        cameraController.stopCamera()
        codecManager.stopEncoder()
        codecManager.stopDecoder() // Ensure UI unbinds Surface before this
        
        currentCallId = null
        currentTargetId = null
        currentSenderId = null
        MeshLogger.d(TAG, "Stopped video streaming")
    }

    fun requestKeyFrame() {
        if (isStreaming) {
            codecManager.requestKeyFrame()
        }
    }

    fun switchCamera() {
        val owner = lifecycleOwner ?: return
        cameraController.switchCamera(owner)
    }

    fun toggleScreenShare() {
        // Switch input surface from Camera to MediaProjection
        MeshLogger.d(TAG, "Screen share toggled (TODO: Implement MediaProjection binding)")
    }
}
