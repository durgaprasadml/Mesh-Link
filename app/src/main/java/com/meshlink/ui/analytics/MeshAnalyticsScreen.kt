package com.meshlink.ui.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshAnalyticsScreen(
    state: TacticalAnalyticsUiState,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onSearchQueryChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExportDialogOpen by remember { mutableStateOf(false) }

    MeshScreen(
        modifier = modifier,
        topBar = {
            AnalyticsTopBar(
                title = "📊 Mesh Command Center",
                meshStatus = state.health.networkHealth,
                activeConnectionsCount = state.health.connectedPeersCount,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onExportClick = { isExportDialogOpen = true },
                onSearchQueryChange = onSearchQueryChange
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWideScreen = maxWidth >= 840.dp
            val isMediumScreen = maxWidth >= 600.dp && maxWidth < 840.dp

            if (isWideScreen) {
                // Two or Three Column Adaptive Dashboard for Tablets & Desktop
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MeshSpacing.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    // Left Column
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = MeshSpacing.TopSafeArea, bottom = MeshSpacing.ListBottomSpacing),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        item { NetworkOverview(health = state.health) }
                        item { ConnectionQualityCard(quality = state.connectionQuality) }
                        item { PacketStatistics(stats = state.packetStats) }
                        item { BatteryImpact(batteryUi = state.batteryImpact) }
                    }

                    // Right Column
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = MeshSpacing.TopSafeArea, bottom = MeshSpacing.ListBottomSpacing),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        item { RoutingTopology(routes = state.routes, activeNodes = state.activeNodes) }
                        item { ThroughputGraph(hopDistribution = state.hopDistribution) }
                        item { StorageStatistics(storage = state.storageStats) }
                    }
                }
            } else {
                // Single Column Dashboard for Mobile Phones
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MeshSpacing.ScreenPadding,
                        end = MeshSpacing.ScreenPadding,
                        top = MeshSpacing.TopSafeArea,
                        bottom = MeshSpacing.ListBottomSpacing
                    ),
                    verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    item { NetworkOverview(health = state.health) }
                    item { ConnectionQualityCard(quality = state.connectionQuality) }
                    item { PacketStatistics(stats = state.packetStats) }
                    item { RoutingTopology(routes = state.routes, activeNodes = state.activeNodes) }
                    item { ThroughputGraph(hopDistribution = state.hopDistribution) }
                    item { BatteryImpact(batteryUi = state.batteryImpact) }
                    item { StorageStatistics(storage = state.storageStats) }
                    item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
                }
            }
        }

        ExportDialog(
            isOpen = isExportDialogOpen,
            isExporting = state.isExporting,
            onConfirmExport = {
                onExportClick()
                isExportDialogOpen = false
            },
            onDismiss = { isExportDialogOpen = false }
        )
    }
}
