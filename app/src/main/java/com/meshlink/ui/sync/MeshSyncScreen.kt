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
 * MeshSyncScreen — Main Master Screen for Mesh-Link Phase 13: Offline Experience, Synchronization & Mesh Reliability.
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf(SyncFilterCategory.ALL) }

    val filteredPendingMessages = remember(state.pendingMessages, searchQuery, selectedFilterCategory) {
        state.pendingMessages.filter { msg ->
            val matchesSearch = searchQuery.isEmpty() ||
                    msg.id.contains(searchQuery, ignoreCase = true) ||
                    msg.recipientName.contains(searchQuery, ignoreCase = true) ||
                    msg.previewText.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedFilterCategory) {
                SyncFilterCategory.ALL -> true
                SyncFilterCategory.PENDING -> msg.status == "QUEUED" || msg.status == "PENDING"
                SyncFilterCategory.RETRY -> msg.status == "RETRYING"
                SyncFilterCategory.FAILED -> msg.status == "FAILED"
                SyncFilterCategory.DELIVERED -> msg.status == "DELIVERED"
                SyncFilterCategory.QUEUE -> true
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    val filteredTimelineEvents = remember(state.timelineEvents, searchQuery) {
        if (searchQuery.isEmpty()) state.timelineEvents
        else state.timelineEvents.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.detail.contains(searchQuery, ignoreCase = true) ||
                    it.eventType.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPeers = remember(state.peers, searchQuery, selectedFilterCategory) {
        state.peers.filter { peer ->
            val matchesSearch = searchQuery.isEmpty() ||
                    peer.name.contains(searchQuery, ignoreCase = true) ||
                    peer.peerId.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedFilterCategory) {
                SyncFilterCategory.PEERS -> true
                SyncFilterCategory.ALL -> true
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    MeshScreen(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
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
                .consumeWindowInsets(paddingValues)
        ) {
            // Persistent Offline / Sync State Banner
            OfflineBanner(
                syncUi = state.syncUi,
                recoveryUi = state.recoveryStatus,
                onRetryClick = onRetryClick,
                modifier = Modifier.padding(horizontal = MeshSpacing.ScreenPadding, vertical = 6.dp)
            )

            // Search Bar Component
            SyncSearch(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = MeshSpacing.ScreenPadding, vertical = 4.dp)
            )

            // Filter Chips Component
            SyncFilters(
                selectedCategory = selectedFilterCategory,
                onCategorySelected = { selectedFilterCategory = it },
                counts = mapOf(
                    SyncFilterCategory.ALL to state.pendingMessages.size + state.peers.size,
                    SyncFilterCategory.PENDING to state.queueUi.pendingCount,
                    SyncFilterCategory.DELIVERED to state.queueUi.completedCount,
                    SyncFilterCategory.FAILED to state.queueUi.failedCount,
                    SyncFilterCategory.RETRY to state.queueUi.retryingCount,
                    SyncFilterCategory.QUEUE to state.queueUi.totalQueueSize,
                    SyncFilterCategory.PEERS to state.peers.size
                ),
                modifier = Modifier.padding(horizontal = MeshSpacing.ScreenPadding, vertical = 2.dp)
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
                    // Responsive Tablet / Foldable Wide Screen Dual Column Layout
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
                                    item { MeshHealthDashboard(syncUi = state.syncUi, queueUi = state.queueUi, deliveryUi = state.deliveryUi, queueStatsUi = state.queueStatsUi, onForceSyncClick = onForceSyncClick) }
                                    item { SyncProgress(syncUi = state.syncUi) }
                                    item { QueueOverview(queueUi = state.queueUi) }
                                    if (filteredPendingMessages.isEmpty() && searchQuery.isNotEmpty()) {
                                        item { SyncEmptyState(onRefreshClick = onForceSyncClick) }
                                    } else {
                                        item { PendingMessages(pendingMessages = filteredPendingMessages, onCancelMessageClick = onCancelMessageClick, onForceRetryClick = onForceRetryMessageClick) }
                                    }
                                }
                                1 -> {
                                    item { MeshRecovery(recoveryUi = state.recoveryStatus) }
                                    item { PeerAvailability(peers = filteredPeers) }
                                }
                                2 -> {
                                    item { OfflineStorage(offlineUi = state.offlineUi) }
                                    item { DeliveryStatistics(deliveryUi = state.deliveryUi, queueStatsUi = state.queueStatsUi) }
                                    item { ConflictViewer(conflicts = state.conflicts) }
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
                                    item { RetryStatus(retryUi = state.retryUi) }
                                    item { DeliveryStatistics(deliveryUi = state.deliveryUi, queueStatsUi = state.queueStatsUi) }
                                    item { SyncTimeline(timelineEvents = filteredTimelineEvents) }
                                }
                                1 -> {
                                    item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                    item { SyncTimeline(timelineEvents = filteredTimelineEvents) }
                                }
                                2 -> {
                                    item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                    item { SyncTimeline(timelineEvents = filteredTimelineEvents) }
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
                                item { MeshHealthDashboard(syncUi = state.syncUi, queueUi = state.queueUi, deliveryUi = state.deliveryUi, queueStatsUi = state.queueStatsUi, onForceSyncClick = onForceSyncClick) }
                                item { SyncProgress(syncUi = state.syncUi) }
                                item { QueueOverview(queueUi = state.queueUi) }
                                if (filteredPendingMessages.isEmpty() && state.pendingMessages.isEmpty()) {
                                    item { SyncEmptyState(onRefreshClick = onForceSyncClick) }
                                } else {
                                    item { PendingMessages(pendingMessages = filteredPendingMessages, onCancelMessageClick = onCancelMessageClick, onForceRetryClick = onForceRetryMessageClick) }
                                }
                                item { RetryStatus(retryUi = state.retryUi) }
                                item { DeliveryStatistics(deliveryUi = state.deliveryUi, queueStatsUi = state.queueStatsUi) }
                                item { SyncTimeline(timelineEvents = filteredTimelineEvents) }
                            }
                            1 -> {
                                item { MeshRecovery(recoveryUi = state.recoveryStatus) }
                                item { PeerAvailability(peers = filteredPeers) }
                                item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                item { SyncTimeline(timelineEvents = filteredTimelineEvents) }
                            }
                            2 -> {
                                item { OfflineStorage(offlineUi = state.offlineUi) }
                                item { DeliveryStatistics(deliveryUi = state.deliveryUi, queueStatsUi = state.queueStatsUi) }
                                item { ConflictViewer(conflicts = state.conflicts) }
                                item { DeliveryHistoryCard(history = state.deliveryHistory) }
                                item { SyncTimeline(timelineEvents = filteredTimelineEvents) }
                            }
                        }
                    }
                }
            }
        }
    }
}
