package com.meshlink.media.data

import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaveformGenerator @Inject constructor() {

    companion object {
        private const val TAG = "WaveformGenerator"
        private const val DEFAULT_SAMPLE_COUNT = 100
    }

    private val waveformCache = ConcurrentHashMap<String, FloatArray>()

    suspend fun generateWaveform(filePath: String, sampleCount: Int = DEFAULT_SAMPLE_COUNT): FloatArray = withContext(Dispatchers.IO) {
        val cached = waveformCache[filePath]
        if (cached != null && cached.size == sampleCount) {
            return@withContext cached
        }

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            val empty = FloatArray(sampleCount) { 0.1f }
            return@withContext empty
        }

        val amplitudes = try {
            extractAmplitudes(file, sampleCount)
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Failed to extract amplitudes from $filePath: ${e.message}")
            generateFallbackAmplitudes(filePath, sampleCount)
        }

        waveformCache[filePath] = amplitudes
        amplitudes
    }

    private fun extractAmplitudes(file: File, targetCount: Int): FloatArray {
        val fileLength = file.length()
        val result = FloatArray(targetCount)
        val step = (fileLength / targetCount.toLong()).coerceAtLeast(1L)

        FileInputStream(file).use { fis ->
            val buffer = ByteArray(1024)
            for (i in 0 until targetCount) {
                val read = fis.read(buffer)
                if (read <= 0) break
                var maxVal = 0
                for (b in 0 until read) {
                    val absVal = Math.abs(buffer[b].toInt())
                    if (absVal > maxVal) maxVal = absVal
                }
                result[i] = (maxVal.toFloat() / 128f).coerceIn(0.1f, 1.0f)
                if (step > buffer.size) {
                    fis.skip(step - buffer.size)
                }
            }
        }
        return result
    }

    private fun generateFallbackAmplitudes(seedPath: String, targetCount: Int): FloatArray {
        val result = FloatArray(targetCount)
        val hash = Math.abs(seedPath.hashCode())
        for (i in 0 until targetCount) {
            val sinVal = Math.sin((i + hash).toDouble() * 0.15).toFloat()
            result[i] = (0.2f + 0.6f * Math.abs(sinVal)).coerceIn(0.1f, 1.0f)
        }
        return result
    }

    fun clearCache() {
        waveformCache.clear()
    }
}
