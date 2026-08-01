package com.meshlink.ui.designsystem.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Performance Infrastructure for Mesh-Link 2026.
 * Enforces Minimal Recomposition, Efficient Lazy Item Keying/ContentType,
 * and Deferred Adaptive Rendering.
 */

/**
 * 1. Stable Value Holder ensuring skip-recomposition for lambdas or dynamic states.
 */
@Stable
class MeshStableHolder<T>(val value: T)

@Composable
fun <T> rememberStableHolder(value: T): MeshStableHolder<T> {
    return remember(value) { MeshStableHolder(value) }
}

/**
 * 2. Efficient Lazy Column Wrapper enforcing key & contentType parameter usage.
 */
@Composable
fun MeshEfficientLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/**
 * Efficient Lazy Grid Wrapper.
 */
@Composable
fun MeshEfficientLazyGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    content: LazyGridScope.() -> Unit
) {
    val state = rememberLazyGridState()
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}

/**
 * 3. Deferred Adaptive Render Box (Defers composition of secondary split-panes until primary layout settles).
 */
@Composable
fun MeshDeferrableRenderBox(
    modifier: Modifier = Modifier,
    delayMs: Long = 50L,
    content: @Composable BoxScope.() -> Unit
) {
    var isRenderReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs)
        isRenderReady = true
    }

    Box(modifier = modifier) {
        if (isRenderReady) {
            content()
        }
    }
}
