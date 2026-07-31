package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class TransferMetrics(
    val transferId: String,
    val progress: Float = 0f, // 0.0 to 1.0
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val currentChunk: Int = 0,
    val totalChunks: Int = 0,
    val currentSpeedBytesPerSec: Float = 0f,
    val averageSpeedBytesPerSec: Float = 0f,
    val etaMs: Long = 0L,
    val activeTransport: TransportType = TransportType.UNKNOWN,
    val retries: Int = 0,
    val crc32Errors: Int = 0,
    val packetLossRate: Float = 0f,
    val compressionRatio: Float = 1.0f,
    val resumeCount: Int = 0,
    val status: TransferState = TransferState.WAITING,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

@Singleton
class ProgressTracker @Inject constructor() {

    companion object {
        private const val TAG = "ProgressTracker"
        private const val SPEED_CALCULATION_WINDOW_MS = 2000L // 2 sec sliding window for instantaneous speed
    }

    private val metricsMap = ConcurrentHashMap<String, TransferMetrics>()
    private val speedTrackingMap = ConcurrentHashMap<String, Pair<Long, Long>>() // transferId -> (lastBytes, lastTime)

    private val _transferMetrics = MutableStateFlow<Map<String, TransferMetrics>>(emptyMap())
    val transferMetrics: StateFlow<Map<String, TransferMetrics>> = _transferMetrics.asStateFlow()

    fun initTransfer(
        transferId: String,
        totalBytes: Long,
        totalChunks: Int,
        transport: TransportType,
        status: TransferState = TransferState.PREPARING
    ) {
        val now = System.currentTimeMillis()
        speedTrackingMap[transferId] = 0L to now

        val initial = TransferMetrics(
            transferId = transferId,
            progress = 0f,
            bytesTransferred = 0L,
            totalBytes = totalBytes,
            currentChunk = 0,
            totalChunks = totalChunks,
            activeTransport = transport,
            status = status,
            lastUpdatedMs = now
        )
        metricsMap[transferId] = initial
        publish()
    }

    fun updateProgress(
        transferId: String,
        chunksTransferred: Int,
        bytesTransferred: Long,
        totalBytes: Long,
        totalChunks: Int,
        transport: TransportType,
        status: TransferState,
        startTimeMs: Long,
        retries: Int = 0,
        crc32Errors: Int = 0,
        packetLossRate: Float = 0f,
        compressionRatio: Float = 1.0f,
        resumeCount: Int = 0
    ) {
        val now = System.currentTimeMillis()
        val totalElapsedSec = maxOf(0.001f, (now - startTimeMs) / 1000f)
        val avgSpeed = bytesTransferred.toFloat() / totalElapsedSec

        // Instantaneous speed calculation
        val lastTracking = speedTrackingMap[transferId]
        var currentSpeed = avgSpeed

        if (lastTracking != null) {
            val (lastBytes, lastTime) = lastTracking
            val timeDiffSec = (now - lastTime) / 1000f
            if (timeDiffSec >= 0.5f) {
                val bytesDiff = maxOf(0L, bytesTransferred - lastBytes)
                currentSpeed = bytesDiff.toFloat() / timeDiffSec
                speedTrackingMap[transferId] = bytesTransferred to now
            }
        } else {
            speedTrackingMap[transferId] = bytesTransferred to now
        }

        // ETA calculation
        val remainingBytes = maxOf(0L, totalBytes - bytesTransferred)
        val speedForEta = if (currentSpeed > 100f) currentSpeed else avgSpeed
        val etaMs = if (speedForEta > 10f) ((remainingBytes / speedForEta) * 1000f).toLong() else 0L

        val progress = if (totalChunks > 0) (chunksTransferred.toFloat() / totalChunks.toFloat()).coerceIn(0f, 1f) else 0f

        val updated = TransferMetrics(
            transferId = transferId,
            progress = progress,
            bytesTransferred = bytesTransferred,
            totalBytes = totalBytes,
            currentChunk = chunksTransferred,
            totalChunks = totalChunks,
            currentSpeedBytesPerSec = currentSpeed,
            averageSpeedBytesPerSec = avgSpeed,
            etaMs = etaMs,
            activeTransport = transport,
            retries = retries,
            crc32Errors = crc32Errors,
            packetLossRate = packetLossRate,
            compressionRatio = compressionRatio,
            resumeCount = resumeCount,
            status = status,
            lastUpdatedMs = now
        )

        metricsMap[transferId] = updated
        publish()
    }

    fun updateStatus(transferId: String, status: TransferState) {
        metricsMap.computeIfPresent(transferId) { _, existing ->
            existing.copy(status = status, lastUpdatedMs = System.currentTimeMillis())
        }
        publish()
    }

    fun removeTransfer(transferId: String) {
        metricsMap.remove(transferId)
        speedTrackingMap.remove(transferId)
        publish()
    }

    fun getMetrics(transferId: String): TransferMetrics? = metricsMap[transferId]

    private fun publish() {
        _transferMetrics.update { metricsMap.toMap() }
    }
}
