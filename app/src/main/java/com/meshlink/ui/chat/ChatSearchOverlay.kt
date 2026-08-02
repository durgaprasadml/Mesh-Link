package com.meshlink.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Search overlay wrapper delegating to modern Material 3 [ChatSearch].
 */
@Composable
fun ChatSearchOverlay(
    isVisible: Boolean,
    searchQuery: String,
    matchCount: Int,
    currentMatchIndex: Int,
    onQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChatSearch(
        isVisible = isVisible,
        searchQuery = searchQuery,
        matchCount = matchCount,
        currentMatchIndex = currentMatchIndex,
        onQueryChange = onQueryChange,
        onNextMatch = onNextMatch,
        onPreviousMatch = onPreviousMatch,
        onCloseSearch = onCloseSearch,
        modifier = modifier
    )
}
