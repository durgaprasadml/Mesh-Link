package com.meshlink.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.analytics.data.LogType

@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val tacticalState = TacticalAnalyticsUiState(
        health = MeshHealthUi(
            networkHealth = if (uiState.activeNodes.isNotEmpty()) "HEALTHY" else "STANDBY",
            healthScore = if (uiState.stats.deliveryRate > 0) uiState.stats.deliveryRate.toInt() else 100,
            activeNodesCount = uiState.activeNodes.size,
            connectedPeersCount = uiState.activeNodes.size,
            relayNodesCount = uiState.stats.packetsRelayed.coerceAtLeast(0),
            activeRoutesCount = uiState.routeTableSize
        ),
        connectionQuality = ConnectionQualityUi(
            isBleActive = true,
            isWifiDirectActive = true,
            signalQualityPercent = if (uiState.activeNodes.isNotEmpty()) 90 else 0,
            meshStateText = if (uiState.activeNodes.isNotEmpty()) "OPERATIONAL" else "STANDBY"
        ),
        packetStats = PacketStatisticsUi(
            sent = uiState.stats.packetsSent,
            delivered = uiState.stats.packetsDelivered,
            relayed = uiState.stats.packetsRelayed,
            failed = uiState.stats.packetsFailed,
            deliveryRatePercent = uiState.stats.deliveryRate,
            avgHops = uiState.stats.avgHopCount.toDouble()
        ),
        hopDistribution = uiState.hopDistribution,
        activeNodes = uiState.activeNodes,
        events = uiState.recentLog.mapIndexed { idx, log ->
            DiagnosticEventUi(
                id = "${log.timestamp}_$idx",
                timestamp = log.timestamp,
                title = log.event,
                detail = log.detail,
                category = log.type
            )
        },
        logEntries = uiState.recentLog.mapIndexed { idx, log ->
            LogEntryUi(
                id = "${log.timestamp}_$idx",
                timestamp = log.timestamp,
                level = log.type.name,
                tag = "MeshRelay",
                message = "${log.event}: ${log.detail}",
                rawLogType = log.type
            )
        }
    )

    MeshAnalyticsScreen(
        state = tacticalState,
        onBackClick = onBack
    )
}
