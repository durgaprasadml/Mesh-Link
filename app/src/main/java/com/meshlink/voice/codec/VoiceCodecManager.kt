package com.meshlink.voice.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.meshlink.common.logger.MeshLogger
import com.meshlink.voice.audio.AudioEngine
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encoding and decoding of raw PCM data to/from AAC.
 * Defaults to AAC-LC for broad compatibility and reasonable low-bitrate performance.
 * For true ultra-low bitrate (e.g. 12kbps for BLE), Opus is better, but requires NDK.
 */
@Singleton
class VoiceCodecManager @Inject constructor() {
    companion object {
        private const val TAG = "VoiceCodecManager"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val TIMEOUT_US = 10000L
        
        // Adaptive Bitrates
        const val BITRATE_WIFI = 64000
        const val BITRATE_BLE = 16000
    }

    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    
    var onEncodedData: ((ByteArray) -> Unit)? = null
    var onDecodedData: ((ByteArray) -> Unit)? = null

    fun startEncoder(bitrate: Int = BITRATE_BLE) {
        try {
            val format = MediaFormat.createAudioFormat(MIME_TYPE, AudioEngine.SAMPLE_RATE, 1)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()
            MeshLogger.d(TAG, "Encoder started at ${bitrate/1000}kbps")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start encoder: ${e.message}")
        }
    }

    fun stopEncoder() {
        try {
            encoder?.stop()
            encoder?.release()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping encoder: ${e.message}")
        } finally {
            encoder = null
        }
    }

    fun startDecoder() {
        try {
            val format = MediaFormat.createAudioFormat(MIME_TYPE, AudioEngine.SAMPLE_RATE, 1)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            
            // Note: AAC sometimes requires CSD (Codec Specific Data) buffer 0 to initialize the decoder.
            // A basic ADTS header or the 2-byte AudioSpecificConfig might be needed depending on the payload.
            // For raw AAC-LC at 16000Hz, Mono, the ASC is 0x14 0x08.
            val csd0 = byteArrayOf(0x14, 0x08)
            format.setByteBuffer("csd-0", ByteBuffer.wrap(csd0))

            decoder = MediaCodec.createDecoderByType(MIME_TYPE)
            decoder?.configure(format, null, null, 0)
            decoder?.start()
            MeshLogger.d(TAG, "Decoder started")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start decoder: ${e.message}")
        }
    }

    fun stopDecoder() {
        try {
            decoder?.stop()
            decoder?.release()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping decoder: ${e.message}")
        } finally {
            decoder = null
        }
    }

    fun encode(pcmData: ByteArray) {
        val codec = encoder ?: return
        try {
            val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(pcmData)
                codec.queueInputBuffer(inputBufferIndex, 0, pcmData.size, System.nanoTime() / 1000, 0)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            while (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val encodedData = ByteArray(bufferInfo.size)
                    outputBuffer.get(encodedData)
                    outputBuffer.clear()
                    onEncodedData?.invoke(encodedData)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Encode error: ${e.message}")
        }
    }

    fun decode(encodedData: ByteArray) {
        val codec = decoder ?: return
        try {
            val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(encodedData)
                codec.queueInputBuffer(inputBufferIndex, 0, encodedData.size, System.nanoTime() / 1000, 0)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            while (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val pcmData = ByteArray(bufferInfo.size)
                    outputBuffer.get(pcmData)
                    outputBuffer.clear()
                    onDecodedData?.invoke(pcmData)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Decode error: ${e.message}")
        }
    }
}
