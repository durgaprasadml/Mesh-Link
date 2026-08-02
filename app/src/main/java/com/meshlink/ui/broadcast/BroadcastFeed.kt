package com.meshlink.ui.broadcast

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BroadcastFeed(
    messages: List<Message>,
    peerIdentities: Map<String, UserIdentity>,
    filterState: BroadcastFilterState,
    onSelectBroadcast: (BroadcastMessageUiState) -> Unit,
    onCopyText: (String) -> Unit,
    onCreateBroadcastClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val uiMessages = remember(messages, peerIdentities) {
        messages.map { msg ->
            BroadcastMessageUiState(
                message = msg,
                senderIdentity = peerIdentities[msg.senderId]
            )
        }
    }

    val filteredMessages = remember(uiMessages, filterState) {
        if (!filterState.isActive) uiMessages
        else uiMessages.filter { filterState.matches(it) }
    }

    val groupedMessages = remember(filteredMessages) {
        filteredMessages.groupBy { msg ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(msg.timestamp))
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isAtBottom = visibleItems.isEmpty() ||
                    (visibleItems.last().index >= listState.layoutInfo.totalItemsCount - 2)
            if (isAtBottom) {
                listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
            }
        }
    }

    AnimatedContent(
        targetState = filteredMessages.isEmpty(),
        label = "feed_transition"
    ) { isEmpty ->
        if (isEmpty) {
            NoBroadcasts(
                onCreateBroadcast = onCreateBroadcastClick,
                title = if (filterState.isActive) "No matching broadcasts" else "No broadcasts yet",
                subtitle = if (filterState.isActive) "Try adjusting or clearing your search filters." else "Community announcements sent across the mesh will appear here.",
                modifier = modifier
            )
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
                    stickyHeader(key = "date_header_$dateKey") {
                        val headerLabel = remember(dateKey) {
                            try {
                                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                if (dateKey == todayStr) "Today"
                                else SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(date ?: Date())
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
