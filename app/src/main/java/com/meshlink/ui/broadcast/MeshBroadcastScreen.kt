package com.meshlink.ui.broadcast

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshBroadcastScreen(
    messages: List<Message>,
    peerIdentities: Map<String, UserIdentity>,
    onBack: () -> Unit,
    onSendBroadcast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var filterState by remember { mutableStateOf(BroadcastFilterState()) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    var selectedBroadcastForDetail by remember { mutableStateOf<BroadcastMessageUiState?>(null) }

    val isExpandedLayout = configuration.screenWidthDp >= 840

    val statistics = remember(messages, peerIdentities) {
        BroadcastStatisticsUi.calculate(messages, peerIdentities.size)
    }

    val copyToClipboard = remember(context) {
        { text: String ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Broadcast Message", text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Broadcast text copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    MeshScreen(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BroadcastTopBar(
                stats = statistics,
                isFilterActive = filterState.isActive,
                isSearchVisible = isSearchVisible,
                onBack = onBack,
                onToggleSearch = { isSearchVisible = !isSearchVisible },
                onToggleFilters = { isFilterSheetVisible = true }
            )
        },
        bottomBar = {
            if (!isExpandedLayout) {
                BroadcastComposer(
                    onSendBroadcast = onSendBroadcast
                )
            }
        }
    ) { paddingValues ->
        if (isExpandedLayout) {
            // Dual Pane Adaptive Layout for Tablets & Foldables
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                ) {
                    // Composer anchored at top for expanded
                    BroadcastComposer(onSendBroadcast = onSendBroadcast)

                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        BroadcastSearch(
                            query = filterState.searchQuery,
                            onQueryChange = { query ->
                                filterState = filterState.copy(searchQuery = query)
                            },
                            modifier = Modifier.padding(horizontal = MeshTheme.spacing.medium, vertical = 4.dp)
                        )
                    }

                    BroadcastFilterChipsRow(
                        filterState = filterState,
                        onUpdateFilter = { filterState = it }
                    )

                    BroadcastFeed(
                        messages = messages,
                        peerIdentities = peerIdentities,
                        filterState = filterState,
                        onSelectBroadcast = { item -> selectedBroadcastForDetail = item },
                        onCopyText = copyToClipboard,
                        onCreateBroadcastClick = { /* Focus input */ },
                        modifier = Modifier.weight(1f)
                    )
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Detail Side Panel
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                ) {
                    selectedBroadcastForDetail?.let { detail ->
                        BroadcastDetailSheetContent(
                            uiState = detail,
                            onCopyText = copyToClipboard,
                            onDismiss = { selectedBroadcastForDetail = null },
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select a broadcast to inspect details",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Single Column Phone Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search Bar toggle
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    BroadcastSearch(
                        query = filterState.searchQuery,
                        onQueryChange = { query ->
                            filterState = filterState.copy(searchQuery = query)
                        },
                        modifier = Modifier.padding(horizontal = MeshTheme.spacing.medium, vertical = 4.dp)
                    )
                }

                // Filter Chips Row
                BroadcastFilterChipsRow(
                    filterState = filterState,
                    onUpdateFilter = { filterState = it }
                )

                // Dominant Feed (~65-70% of screen)
                BroadcastFeed(
                    messages = messages,
                    peerIdentities = peerIdentities,
                    filterState = filterState,
                    onSelectBroadcast = { item -> selectedBroadcastForDetail = item },
                    onCopyText = copyToClipboard,
                    onCreateBroadcastClick = { /* Focus composer */ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Filter Bottom Sheet
    if (isFilterSheetVisible) {
        BroadcastFiltersSheet(
            filterState = filterState,
            onUpdateFilter = { updated -> filterState = updated },
            onDismiss = { isFilterSheetVisible = false }
        )
    }

    // Detail Bottom Sheet for compact screens
    if (!isExpandedLayout && selectedBroadcastForDetail != null) {
        BroadcastDetailSheet(
            uiState = selectedBroadcastForDetail!!,
            onCopyText = copyToClipboard,
            onDismiss = { selectedBroadcastForDetail = null }
        )
    }
}
