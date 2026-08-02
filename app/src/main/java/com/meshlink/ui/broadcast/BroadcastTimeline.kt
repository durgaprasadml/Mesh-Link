package com.meshlink.ui.broadcast

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.UserIdentity

/**
 * Public wrapper around [BroadcastFeed] for backwards compatibility.
 */
@Composable
fun BroadcastTimeline(
    messages: List<Message>,
    peerIdentities: Map<String, UserIdentity>,
    filterState: BroadcastFilterState,
    onSelectBroadcast: (BroadcastMessageUiState) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onCreateBroadcastClick: () -> Unit = {}
) {
    BroadcastFeed(
        messages = messages,
        peerIdentities = peerIdentities,
        filterState = filterState,
        onSelectBroadcast = onSelectBroadcast,
        onCopyText = onCopyText,
        onCreateBroadcastClick = onCreateBroadcastClick,
        modifier = modifier,
        listState = listState
    )
}
