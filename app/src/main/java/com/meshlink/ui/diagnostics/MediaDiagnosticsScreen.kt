package com.meshlink.ui.diagnostics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.analytics.data.LogType
import com.meshlink.ui.analytics.DiagnosticEventUi
import com.meshlink.ui.analytics.LogEntryUi
import com.meshlink.ui.analytics.MeshDiagnosticsScreen

@Composable
fun MediaDiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: MediaDiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val diagnosticEvents = uiState.activeTransfers.map { transfer ->
        DiagnosticEventUi(
            id = transfer.transferId,
            timestamp = System.currentTimeMillis(),
            title = "Transfer ${transfer.transferId.take(8)}...",
            detail = "Status: ${transfer.status.name} | Transport: ${transfer.activeTransport.name} | Progress: ${(transfer.progress * 100).toInt()}%",
            category = LogType.RELAY
        )
    }

    val diagnosticLogs = mutableListOf<LogEntryUi>().apply {
        add(
            LogEntryUi(
                id = "media_subsystem_log",
                timestamp = System.currentTimeMillis(),
                level = "INFO",
                tag = "MediaEngine",
                message = "Mode: ${uiState.activeModeName} | Window: ${uiState.currentSlidingWindowSize} chunks | Speed: ${String.format("%.1f", uiState.averageSpeedBytesPerSec / 1024f)} KB/s",
                rawLogType = LogType.RELAY
            )
        )
        add(
            LogEntryUi(
                id = "media_totals_log",
                timestamp = System.currentTimeMillis(),
                level = "METRICS",
                tag = "TransferSession",
                message = "Completed: ${uiState.completedTransfersCount} | Failed: ${uiState.failedTransfersCount} | Total Bytes: ${uiState.totalBytesTransferred} B | Retries: ${uiState.totalRetries}",
                rawLogType = LogType.RELAY
            )
        )
        uiState.activeTransfers.forEach { transfer ->
            add(
                LogEntryUi(
                    id = "transfer_${transfer.transferId}",
                    timestamp = System.currentTimeMillis(),
                    level = transfer.status.name,
                    tag = "MediaTransfer",
                    message = "ID: ${transfer.transferId} via ${transfer.activeTransport} -> Speed: ${String.format("%.1f", transfer.currentSpeedBytesPerSec / 1024f)} KB/s",
                    rawLogType = LogType.RELAY
                )
            )
        }
    }

    MeshDiagnosticsScreen(
        title = "Media Transfer Diagnostics",
        events = diagnosticEvents,
        logs = diagnosticLogs,
        onBackClick = onBackClick
    )
}
