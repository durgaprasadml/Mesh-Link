package com.meshlink.ui.home

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable presentation-only Material 3 Pull to Refresh container.
 * Invokes [onRefresh] callback when user triggers pull to refresh gesture.
 * Strictly UI presentation layer only with zero repository/ViewModel modifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshPullRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        state = state,
        content = content
    )
}
