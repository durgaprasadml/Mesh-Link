package com.meshlink.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.transfer.MediaTransferSessionManager
import com.meshlink.transfer.TransferMetrics
import com.meshlink.transfer.TransferState
import com.meshlink.transfer.TransportType
import com.meshlink.transport.HybridTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class MediaDiagnosticsUiState(
    val activeTransfers: List<TransferMetrics> = emptyList(),
    val completedTransfersCount: Int = 0,
    val failedTransfersCount: Int = 0,
    val totalTransfersCount: Int = 0,
    val averageSpeedBytesPerSec: Float = 0f,
    val totalBytesTransferred: Long = 0L,
    val totalRetries: Int = 0,
    val totalCrc32Errors: Int = 0,
    val averageCompressionRatio: Float = 1.0f,
    val bleTransfersCount: Int = 0,
    val wifiTransfersCount: Int = 0,
    val currentSlidingWindowSize: Int = 4,
    val activeModeName: String = "HYBRID_ACTIVE"
)

@HiltViewModel
class MediaDiagnosticsViewModel @Inject constructor(
    private val sessionManager: MediaTransferSessionManager,
    private val hybridTransport: HybridTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaDiagnosticsUiState())
    val uiState: StateFlow<MediaDiagnosticsUiState> = _uiState.asStateFlow()

    init {
        combine(
            sessionManager.activeMetrics,
            hybridTransport.metrics
        ) { metricsMap, hybridMetrics ->
            val list = metricsMap.values.toList()
            val active = list.filter {
                it.status == TransferState.SENDING ||
                it.status == TransferState.RECEIVING ||
                it.status == TransferState.PREPARING ||
                it.status == TransferState.VERIFYING ||
                it.status == TransferState.RESUMING
            }

            val completedCount = list.count { it.status == TransferState.COMPLETED }
            val failedCount = list.count { it.status == TransferState.FAILED }
            val totalCount = list.size

            val totalBytes = list.sumOf { it.bytesTransferred }
            val avgSpeed = if (active.isNotEmpty()) active.map { it.averageSpeedBytesPerSec }.average().toFloat() else 0f
            val totalRetries = list.sumOf { it.retries }
            val totalCrc = list.sumOf { it.crc32Errors }
            val avgComp = if (list.isNotEmpty()) list.map { it.compressionRatio }.average().toFloat() else 1.0f

            val bleCount = list.count { it.activeTransport == TransportType.BLE }
            val wifiCount = list.count { it.activeTransport == TransportType.WIFI_DIRECT }
            val windowSize = if (active.isNotEmpty()) active.first().totalChunks.coerceIn(2, 32) else 4

            MediaDiagnosticsUiState(
                activeTransfers = active,
                completedTransfersCount = completedCount,
                failedTransfersCount = failedCount,
                totalTransfersCount = totalCount,
                averageSpeedBytesPerSec = avgSpeed,
                totalBytesTransferred = totalBytes,
                totalRetries = totalRetries,
                totalCrc32Errors = totalCrc,
                averageCompressionRatio = avgComp,
                bleTransfersCount = bleCount,
                wifiTransfersCount = wifiCount,
                currentSlidingWindowSize = windowSize,
                activeModeName = hybridMetrics.activeMode.name
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun pauseTransfer(transferId: String) {
        sessionManager.pauseTransfer(transferId)
    }

    fun resumeTransfer(transferId: String) {
        sessionManager.resumeTransfer(transferId)
    }

    fun cancelTransfer(transferId: String) {
        sessionManager.cancelTransfer(transferId)
    }
}
