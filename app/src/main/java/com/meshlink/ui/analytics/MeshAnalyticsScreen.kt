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
    var isExportSheetOpen by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(state.searchQuery) }
    var selectedFilter by remember { mutableStateOf(state.activeFilter) }

    MeshScreen(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            AnalyticsTopBar(
                title = "Mesh Analytics",
                subtitle = "Network Insights",
                meshStatus = state.health.networkHealth,
                activeConnectionsCount = state.health.connectedPeersCount,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onExportClick = { isExportSheetOpen = true },
                onSearchToggle = { isSearchBarVisible = !isSearchBarVisible },
                onSearchQueryChange = { query ->
                    searchQuery = query
                    onSearchQueryChange?.invoke(query)
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            val isWideScreen = maxWidth >= 840.dp
            val isMediumScreen = maxWidth >= 600.dp && maxWidth < 840.dp

            if (isWideScreen) {
                // Tablet / Desktop Multi-Column Dashboard
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MeshSpacing.ScreenPadding)
                ) {
                    // Search & Filters Bar
                    Column(
                        modifier = Modifier.padding(vertical = MeshTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)
                    ) {
                        AnimatedVisibility(visible = isSearchBarVisible) {
                            AnalyticsSearch(
                                query = searchQuery,
                                onQueryChange = {
                                    searchQuery = it
                                    onSearchQueryChange?.invoke(it)
                                }
                            )
                        }
                        AnalyticsFilters(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        // Left Column (Health, Stats, Charts, Performance)
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(top = 8.dp, bottom = MeshSpacing.ListBottomSpacing),
                            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                        ) {
                            item { HealthSummary(health = state.health) }
                            item {
                                NetworkStatistics(
                                    packetStats = state.packetStats,
                                    transferStats = state.transferStats,
                                    activeSessionsCount = state.health.activeSessionsCount
                                )
                            }
                            item {
                                AnalyticsCharts(
                                    hopDistribution = state.hopDistribution,
                                    throughputSeries = state.throughputSeries
                                )
                            }
                            item { ConnectionQuality(quality = state.connectionQuality) }
                            item { PerformanceOverview(batteryImpact = state.batteryImpact) }
                        }

                        // Right Column (Topology, Routing, Transfers, Timeline)
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(top = 8.dp, bottom = MeshSpacing.ListBottomSpacing),
                            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                        ) {
                            item { MeshTopology(routes = state.routes, activeNodes = state.activeNodes) }
                            item { RoutingInsights(routes = state.routes, packetStats = state.packetStats) }
                            item { TransferAnalytics(transferStats = state.transferStats) }
                            item { AnalyticsTimeline(timelineEvents = state.timelineEvents) }
                        }
                    }
                }
            } else {
                // Mobile Phone Single Column Dashboard
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
                    item {
                        AnimatedVisibility(visible = isSearchBarVisible) {
                            AnalyticsSearch(
                                query = searchQuery,
                                onQueryChange = {
                                    searchQuery = it
                                    onSearchQueryChange?.invoke(it)
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    item {
                        AnalyticsFilters(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it }
                        )
                    }

                    item { HealthSummary(health = state.health) }
                    item {
                        NetworkStatistics(
                            packetStats = state.packetStats,
                            transferStats = state.transferStats,
                            activeSessionsCount = state.health.activeSessionsCount
                        )
                    }
                    item {
                        AnalyticsCharts(
                            hopDistribution = state.hopDistribution,
                            throughputSeries = state.throughputSeries
                        )
                    }
                    item { MeshTopology(routes = state.routes, activeNodes = state.activeNodes) }
                    item { RoutingInsights(routes = state.routes, packetStats = state.packetStats) }
                    item { TransferAnalytics(transferStats = state.transferStats) }
                    item { ConnectionQuality(quality = state.connectionQuality) }
                    item { PerformanceOverview(batteryImpact = state.batteryImpact) }
                    item { AnalyticsTimeline(timelineEvents = state.timelineEvents) }
                    item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
                }
            }
        }

        AnalyticsExportSheet(
            isOpen = isExportSheetOpen,
            onDismiss = { isExportSheetOpen = false },
            onExportSummary = { onExportClick() },
            onExportDiagnostics = { onExportClick() },
            onShareReport = { onExportClick() }
        )
    }
}
