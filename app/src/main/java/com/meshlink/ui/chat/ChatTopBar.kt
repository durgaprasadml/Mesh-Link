package com.meshlink.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.domain.model.UserIdentity
import com.meshlink.messaging.presentation.ConnectionState

/**
 * Top bar wrapper delegating to modern Material 3 [ConversationTopBar].
 */
@Composable
fun ChatTopBar(
    peerIdentity: UserIdentity,
    peerAddress: String,
    fallbackName: String,
    connectionState: ConnectionState,
    selectionState: SelectionState,
    onBackClick: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onDeleteChat: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConversationTopBar(
        peerIdentity = peerIdentity,
        peerAddress = peerAddress,
        fallbackName = fallbackName,
        connectionState = connectionState,
        selectionState = selectionState,
        onBackClick = onBackClick,
        onClearSelection = onClearSelection,
        onDeleteSelected = onDeleteSelected,
        onCopySelected = onCopySelected,
        onDeleteChat = onDeleteChat,
        onSearchClick = onSearchClick,
        modifier = modifier
    )
}
