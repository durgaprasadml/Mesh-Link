package com.meshlink.ui.broadcast

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    var filterState by remember { mutableStateOf(BroadcastFilterState()) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }
    var selectedBroadcastForDetail by remember { mutableStateOf<BroadcastMessageUiState?>(null) }

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
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BroadcastTopBar(
                stats = statistics,
                isFilterActive = filterState.isActive,
                onBack = onBack,
                onToggleFilters = { isFilterSheetVisible = true }
            )
        },
        bottomBar = {
            BroadcastComposer(
                onSendBroadcast = onSendBroadcast
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top overview strip: Dashboard statistics & Recipient Summary
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                
                RecipientSummary(
                    peerIdentities = peerIdentities,
                    modifier = Modifier.padding(horizontal = MeshTheme.spacing.medium)
                )

                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

                BroadcastStatistics(
                    stats = statistics,
                    onFilterClick = { newFilter ->
                        filterState = newFilter
                    }
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))

            // Main Timeline
            BroadcastTimeline(
                messages = messages,
                peerIdentities = peerIdentities,
                filterState = filterState,
                onSelectBroadcast = { item ->
                    selectedBroadcastForDetail = item
                },
                onCopyText = copyToClipboard,
                modifier = Modifier.weight(1f)
            )
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

    // Detail Bottom Sheet
    selectedBroadcastForDetail?.let { detail ->
        BroadcastDetailSheet(
            uiState = detail,
            onCopyText = copyToClipboard,
            onDismiss = { selectedBroadcastForDetail = null }
        )
    }
}
