package com.meshlink.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.analytics.data.LogType
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshDiagnosticsScreen(
    title: String = "Developer & Mesh Diagnostics",
    events: List<DiagnosticEventUi>,
    logs: List<LogEntryUi>,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<LogType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isExportDialogOpen by remember { mutableStateOf(false) }

    MeshScreen(
        modifier = modifier,
        topBar = {
            AnalyticsTopBar(
                title = title,
                meshStatus = "DIAGNOSTICS",
                activeConnectionsCount = events.size,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onExportClick = { isExportDialogOpen = true },
                onSearchQueryChange = { searchQuery = it }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWideScreen = maxWidth >= 840.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MeshSpacing.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = MeshSpacing.TopSafeArea, bottom = MeshSpacing.ListBottomSpacing),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        item { EventTimeline(events = events) }
                        item { DiagnosticsActions(onExportClick = { isExportDialogOpen = true }, onRefreshClick = onRefreshClick) }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(top = MeshSpacing.TopSafeArea, bottom = MeshSpacing.ListBottomSpacing),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        item {
                            LogViewer(
                                logs = logs,
                                selectedFilter = selectedFilter,
                                onFilterSelect = { selectedFilter = it },
                                searchQuery = searchQuery
                            )
                        }
                    }
                }
            } else {
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
                    item { EventTimeline(events = events) }
                    item {
                        LogViewer(
                            logs = logs,
                            selectedFilter = selectedFilter,
                            onFilterSelect = { selectedFilter = it },
                            searchQuery = searchQuery
                        )
                    }
                    item { DiagnosticsActions(onExportClick = { isExportDialogOpen = true }, onRefreshClick = onRefreshClick) }
                    item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
                }
            }
        }

        ExportDialog(
            isOpen = isExportDialogOpen,
            isExporting = false,
            onConfirmExport = {
                onExportClick()
                isExportDialogOpen = false
            },
            onDismiss = { isExportDialogOpen = false }
        )
    }
}
