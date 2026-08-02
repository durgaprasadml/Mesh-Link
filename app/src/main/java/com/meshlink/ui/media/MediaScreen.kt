package com.meshlink.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.diagnostics.MediaDiagnosticsViewModel
import com.meshlink.ui.media.models.MediaUi
import com.meshlink.ui.media.models.TransferDirectionUi
import com.meshlink.ui.media.models.TransferStatisticsUi
import com.meshlink.ui.media.models.TransferStatus
import com.meshlink.ui.media.models.TransferUi

/**
 * Public entry-point bridge screen for Media, Attachments & File Transfer.
 * Strictly collects ViewModel state and delegates rendering to [MeshMediaScreen].
 */
@Composable
fun MediaScreen(
    onBack: () -> Unit,
    viewModel: MediaDiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val activeTransfersUi = uiState.activeTransfers.map { metric ->
        TransferUi(
            transferId = metric.transferId,
            fileName = "File_${metric.transferId.take(8)}",
            direction = TransferDirectionUi.OUTGOING,
            progress = metric.progress,
            speedBytesPerSec = metric.averageSpeedBytesPerSec,
            totalSizeBytes = metric.totalBytes,
            transferredSizeBytes = metric.bytesTransferred,
            status = TransferStatus.TRANSFERRING,
            transportType = metric.activeTransport.name,
            retryCount = metric.retries,
            crcErrors = metric.crc32Errors
        )
    }

    val statisticsUi = TransferStatisticsUi(
        filesSentCount = uiState.completedTransfersCount,
        filesReceivedCount = 0,
        totalTransferredBytes = uiState.totalBytesTransferred,
        failedTransfersCount = uiState.failedTransfersCount,
        averageSpeedBytesPerSec = uiState.averageSpeedBytesPerSec,
        bleTransfersCount = uiState.bleTransfersCount,
        wifiTransfersCount = uiState.wifiTransfersCount,
        activeSlidingWindowSize = uiState.currentSlidingWindowSize,
        transportModeName = uiState.activeModeName
    )

    MeshMediaScreen(
        mediaList = emptyList<MediaUi>(),
        activeTransfers = activeTransfersUi,
        statistics = statisticsUi,
        onBack = onBack,
        onPauseTransfer = { id -> viewModel.pauseTransfer(id) },
        onResumeTransfer = { id -> viewModel.resumeTransfer(id) },
        onCancelTransfer = { id -> viewModel.cancelTransfer(id) }
    )
}
