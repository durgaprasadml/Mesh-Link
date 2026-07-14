package com.meshlink.video.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.meshlink.common.logger.MeshLogger
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCodecManager @Inject constructor() {
    companion object {
        private const val TAG = "VideoCodecManager"
        private const val TIMEOUT_US = 10000L
    }

    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    
    // Surface we provide to CameraX
    var inputSurface: Surface? = null
        private set

    // Surface we get from Compose (VideoRenderer) to draw decoded frames
    private var outputSurface: Surface? = null

    var onEncodedFrame: ((ByteArray, Long) -> Unit)? = null // (NAL Unit, Presentation Time)

    private var isEncoding = false
    private var isDecoding = false
    
    private var encoderThread: Thread? = null
    private var decoderThread: Thread? = null

    fun startEncoder(width: Int, height: Int, fps: Int, bitrate: Int, mimeType: String = MediaFormat.MIMETYPE_VIDEO_HEVC) {
        try {
            val format = MediaFormat.createVideoFormat(mimeType, width, height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 I-Frame per second

            encoder = MediaCodec.createEncoderByType(mimeType)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            
            // Create the input surface for CameraX
            inputSurface = encoder?.createInputSurface()
            encoder?.start()
            isEncoding = true
            
            startEncoderLoop()
            MeshLogger.d(TAG, "Encoder started ($mimeType, ${width}x${height}, ${bitrate/1000}kbps)")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start encoder: ${e.message}")
        }
    }

    fun stopEncoder() {
        isEncoding = false
        encoderThread?.interrupt()
        try {
            encoder?.stop()
            encoder?.release()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping encoder: ${e.message}")
        } finally {
            encoder = null
            inputSurface?.release()
            inputSurface = null
        }
    }

    private fun startEncoderLoop() {
        encoderThread = Thread {
            val bufferInfo = MediaCodec.BufferInfo()
            while (isEncoding) {
                try {
                    val codec = encoder ?: break
                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    while (outputBufferIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            // Extract NAL unit
                            val encodedData = ByteArray(bufferInfo.size)
                            outputBuffer.get(encodedData)
                            outputBuffer.clear()
                            
                            // Callback to StreamManager
                            onEncodedFrame?.invoke(encodedData, bufferInfo.presentationTimeUs)
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                    }
                } catch (e: Exception) {
                    if (isEncoding) {
                        MeshLogger.e(TAG, "Encoder loop error: ${e.message}")
                    }
                }
            }
        }
        encoderThread?.start()
    }

    fun startDecoder(surface: Surface, width: Int, height: Int, mimeType: String = MediaFormat.MIMETYPE_VIDEO_HEVC) {
        this.outputSurface = surface
        try {
            val format = MediaFormat.createVideoFormat(mimeType, width, height)
            
            decoder = MediaCodec.createDecoderByType(mimeType)
            // Configure to decode directly to the provided Surface (zero-copy display)
            decoder?.configure(format, surface, null, 0)
            decoder?.start()
            isDecoding = true
            MeshLogger.d(TAG, "Decoder started ($mimeType)")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start decoder: ${e.message}")
        }
    }

    fun stopDecoder() {
        isDecoding = false
        try {
            decoder?.stop()
            decoder?.release()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping decoder: ${e.message}")
        } finally {
            decoder = null
        }
    }

    fun decodeFrame(nalUnit: ByteArray, ptsUs: Long) {
        val codec = decoder ?: return
        try {
            val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(nalUnit)
                codec.queueInputBuffer(inputBufferIndex, 0, nalUnit.size, ptsUs, 0)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            while (outputBufferIndex >= 0) {
                // Release with render=true to automatically draw to the Surface
                codec.releaseOutputBuffer(outputBufferIndex, true)
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Decode error: ${e.message}")
        }
    }

    fun requestKeyFrame() {
        try {
            val bundle = android.os.Bundle()
            bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            encoder?.setParameters(bundle)
            MeshLogger.d(TAG, "Requested KeyFrame (I-Frame)")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to request KeyFrame: ${e.message}")
        }
    }
}
