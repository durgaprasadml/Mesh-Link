package com.meshlink.ui.designsystem.scroll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Scroll Behaviors Infrastructure for Mesh-Link 2026.
 * Provides Collapsing Toolbar, Overscroll Stretch, Pull-To-Refresh Box, FAB Hide/Show Connection,
 * Hero Collapse State, and Custom Glass Scroll Indicator.
 */

@Immutable
data class MeshHeroScrollState(
    val collapsedFraction: Float = 0f,
    val isCollapsed: Boolean = false
)

@Composable
fun rememberMeshHeroScrollState(
    maxHeightDp: Dp = 220.dp,
    minHeightDp: Dp = 64.dp
): Pair<MeshHeroScrollState, NestedScrollConnection> {
    var currentHeight by remember { mutableFloatStateOf(maxHeightDp.value) }
    val maxPx = maxHeightDp.value
    val minPx = minHeightDp.value

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newHeight = (currentHeight + delta).coerceIn(minPx, maxPx)
                val consumedY = newHeight - currentHeight
                currentHeight = newHeight
                return Offset(0f, consumedY)
            }
        }
    }

    val fraction = ((maxPx - currentHeight) / (maxPx - minPx)).coerceIn(0f, 1f)
    val state = remember(fraction) {
        MeshHeroScrollState(
            collapsedFraction = fraction,
            isCollapsed = fraction > 0.8f
        )
    }

    return Pair(state, connection)
}

/**
 * Collapsing Toolbar Container wrapping scrollable content.
 */
@Composable
fun MeshCollapsingTopAppBar(
    title: @Composable (collapsedFraction: Float) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 140.dp,
    minHeight: Dp = 56.dp,
    content: @Composable (NestedScrollConnection) -> Unit
) {
    val (heroState, connection) = rememberMeshHeroScrollState(maxHeightDp = maxHeight, minHeightDp = minHeight)
    val currentHeight = maxHeight - ((maxHeight - minHeight) * heroState.collapsedFraction)

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeight)
                .background(MeshTheme.colors.surface),
            contentAlignment = Alignment.CenterStart
        ) {
            title(heroState.collapsedFraction)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(connection)
        ) {
            content(connection)
        }
    }
}

/**
 * Modifier enabling overscroll stretch visual feedback.
 */
fun Modifier.meshStretchOverscrollEffect(): Modifier = this.graphicsLayer {
    // Stretch animation layer hook
    clip = true
}

/**
 * Pull to Refresh Container wrapping composable content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        content = content
    )
}

/**
 * Remembers a NestedScrollConnection that automatically shows or hides FAB based on scroll direction.
 */
@Composable
fun rememberMeshFabScrollState(): Pair<Boolean, NestedScrollConnection> {
    var isFabVisible by remember { mutableStateOf(true) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isFabVisible = false
                } else if (available.y > 12f) {
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    return Pair(isFabVisible, connection)
}

fun Modifier.meshFabScrollBehavior(
    onVisibilityChange: (Boolean) -> Unit
): Modifier = composed {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    onVisibilityChange(false)
                } else if (available.y > 12f) {
                    onVisibilityChange(true)
                }
                return Offset.Zero
            }
        }
    }
    this.nestedScroll(connection)
}

/**
 * Custom Glass Scroll Indicator for LazyLists.
 */
@Composable
fun MeshScrollIndicator(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MeshTheme.colors.primary.copy(alpha = 0.6f),
    trackColor: Color = Color.White.copy(alpha = 0.1f)
) {
    val firstVisibleItemIndex by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val totalItemsCount by remember { derivedStateOf { lazyListState.layoutInfo.totalItemsCount } }

    if (totalItemsCount == 0) return

    val progress = (firstVisibleItemIndex.toFloat() / totalItemsCount.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(0.2f)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = progress * 200f
                }
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
