package com.meshlink.ui.broadcast

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BroadcastTimeline(
    messages: List<Message>,
    peerIdentities: Map<String, UserIdentity>,
    filterState: BroadcastFilterState,
    onSelectBroadcast: (BroadcastMessageUiState) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    // Map domain messages to UI presentation models
    val uiMessages = remember(messages, peerIdentities) {
        messages.map { msg ->
            BroadcastMessageUiState(
                message = msg,
                senderIdentity = peerIdentities[msg.senderId]
            )
        }
    }

    // Apply active UI filters
    val filteredMessages = remember(uiMessages, filterState) {
        if (!filterState.isActive) uiMessages
        else uiMessages.filter { filterState.matches(it) }
    }

    // Group messages by date for sticky headers
    val groupedMessages = remember(filteredMessages) {
        filteredMessages.groupBy { msg ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(msg.timestamp))
        }
    }

    // Auto-scroll to bottom when user sends or receives new broadcast
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isAtBottom = visibleItems.isEmpty() ||
                    (visibleItems.last().index >= listState.layoutInfo.totalItemsCount - 2)
            if (isAtBottom) {
                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
            }
        }
    }

    AnimatedContent(
        targetState = filteredMessages.isEmpty(),
        label = "timeline_content_transition"
    ) { isEmpty ->
        if (isEmpty) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = MeshSpacing.ScreenPadding, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Campaign,
                    title = if (filterState.isActive) "No matching broadcasts" else "No broadcasts yet",
                    description = if (filterState.isActive) "Try adjusting or clearing your filters" else "Broadcast messages sent to all nearby devices will appear here"
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = MeshTheme.spacing.medium,
                    vertical = MeshTheme.spacing.mediumSmall
                ),
                verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.mediumSmall)
            ) {
                groupedMessages.forEach { (dateKey, itemsInDate) ->
                    stickyHeader(key = "header_$dateKey") {
                        val headerLabel = remember(dateKey) {
                            try {
                                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)
                                SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(date ?: Date())
                            } catch (e: Exception) {
                                dateKey
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = MeshTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = MeshTheme.elevation.level1
                            ) {
                                Text(
                                    text = headerLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(
                        items = itemsInDate,
                        key = { it.messageId },
                        contentType = { "broadcast_card" }
                    ) { item ->
                        Box(modifier = Modifier.animateItem()) {
                            BroadcastCard(
                                uiState = item,
                                onSelectBroadcast = onSelectBroadcast,
                                onCopyText = onCopyText
                            )
                        }
                    }
                }
            }
        }
    }
}
