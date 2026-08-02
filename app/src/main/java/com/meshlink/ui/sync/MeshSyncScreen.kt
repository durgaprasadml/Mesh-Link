package com.meshlink.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.MeshTopAppBar
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * MeshSyncScreen — Main Master Screen for Mesh-Link Phase 14: Offline Experience, Synchronization & Mesh Reliability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshSyncScreen(
    state: MeshSyncUiState,
    onBackClick: () -> Unit,
    onForceSyncClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
    onCancelMessageClick: ((PendingMessageUi) -> Unit)? = null,
    onForceRetryMessageClick: ((PendingMessageUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Overview & Queue, 1 = Recovery & Peers, 2 = Storage & History

    MeshScreen(
        modifier = modifier,
        topBar = {
            MeshTopAppBar(
                title = "Offline Sync & Mesh Reliability",
                onBackClick = onBackClick,
                actions = {
                    if (onForceSyncClick != null) {
                        IconButton(onClick = onForceSyncClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh & Sync",
                                modifier = Modifier.syncSpinnerAnimation(state.syncUi.isSyncing)
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Persistent Offline / Sync State Banner
            OfflineBanner(
                syncUi = state.syncUi,
                recoveryUi = state.recoveryStatus,
                onRetryClick = onRetryClick,
                modifier = Modifier.padding(horizontal = MeshSpacing.ScreenPadding, vertical = 6.dp)
            )

            // Mission Control Primary Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Dashboard & Queue") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recovery & Peers (${state.peers.count { it.status == PeerAvailabilityStatus.ONLINE }})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Storage & History") }
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 840.dp

                if (isWideScreen) {
                    // Responsive Tablet / Wide Screen Dual Column Layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MeshSpacing.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                top = MeshSpacing.CardSpacing,
                                bottom = MeshSpacing.ListBottomSpacing
                            ),
                            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                        ) {
                            when (selectedTab) {
                                0 -> {
                                    item { SyncDashboard(syncUi = state.syncUi, queueUi = state.queueUi, deliveryUi = state.deliveryUi, onForceSyncClick = onForceSyncClick) }
                                    item { SyncProgressCard(syncUi = state.syncUi) }
                                    item { QueueStatusCard(queueUi = state.queueUi) }
                                    item { PendingMessagesList(pendingMessages = state.pendingMessages, onCancelMessageClick = onCancelMessageClick, onForceRetryClick = onForceRetryMessageClick) }
                                }
                                1 -> {
                                    item { MeshRecoveryCard(recoveryUi = state.recoveryStatus) }
                                    item { PeerAvailabilityCard(peers = state.peers) }
                                }
                                2 -> {
                                    item { OfflineStorageCard(offlineUi = state.offlineUi) }
                                    item { QueueStatisticsCard(stats = state.queueStatsUi) }
                                    item { ConflictViewerCard(conflicts = state.conflicts) }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                top = MeshSpacing.CardSpacing,
                                bottom = MeshSpacing.ListBottomSpacing
                            ),
                            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                        ) {
                            when (selectedTab) {
                                0 -> {
                                    item { RetryStatusCard(retryUi = state.retryUi) }
                                    item { QueueStatisticsCard(stats = state.queueStatsUi) }
                                    item { SyncTimelineCard(timelineEvents = state.timelineEvents) }
                                }
                                1 -> {
                                    item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                    item { SyncTimelineCard(timelineEvents = state.timelineEvents) }
                                }
                                2 -> {
                                    item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                    item { SyncTimelineCard(timelineEvents = state.timelineEvents) }
                                }
                            }
                        }
                    }
                } else {
                    // Mobile / Compact Single Column Scroll View
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MeshSpacing.ScreenPadding),
                        contentPadding = PaddingValues(
                            top = MeshSpacing.CardSpacing,
                            bottom = MeshSpacing.ListBottomSpacing
                        ),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                item { SyncDashboard(syncUi = state.syncUi, queueUi = state.queueUi, deliveryUi = state.deliveryUi, onForceSyncClick = onForceSyncClick) }
                                item { SyncProgressCard(syncUi = state.syncUi) }
                                item { QueueStatusCard(queueUi = state.queueUi) }
                                item { PendingMessagesList(pendingMessages = state.pendingMessages, onCancelMessageClick = onCancelMessageClick, onForceRetryClick = onForceRetryMessageClick) }
                                item { RetryStatusCard(retryUi = state.retryUi) }
                                item { QueueStatisticsCard(stats = state.queueStatsUi) }
                                item { SyncTimelineCard(timelineEvents = state.timelineEvents) }
                            }
                            1 -> {
                                item { MeshRecoveryCard(recoveryUi = state.recoveryStatus) }
                                item { PeerAvailabilityCard(peers = state.peers) }
                                item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                item { SyncTimelineCard(timelineEvents = state.timelineEvents) }
                            }
                            2 -> {
                                item { OfflineStorageCard(offlineUi = state.offlineUi) }
                                item { QueueStatisticsCard(stats = state.queueStatsUi) }
                                item { ConflictViewerCard(conflicts = state.conflicts) }
                                item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                item { SyncTimelineCard(timelineEvents = state.timelineEvents) }
                            }
                        }
                    }
                }
            }
        }
    }
}
