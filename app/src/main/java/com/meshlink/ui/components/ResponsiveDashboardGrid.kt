package com.meshlink.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.LayoutConstants
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Standardized Responsive Dashboard Layout for Mesh Link.
 * Rules:
 * - COMPACT (<600dp): Single horizontal row (LazyRow) so dashboard cards fit on 1 line without vertical clipping.
 * - MEDIUM (600-840dp): 3-column equal-width grid.
 * - EXPANDED (840dp+): 3-4 column grid.
 */
@Composable
fun ResponsiveDashboardGrid(
    items: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
    spacing: Dp = LayoutConstants.CardSpacing
) {
    val breakpoint = rememberWindowBreakpoint()

    when (breakpoint) {
        WindowBreakpoint.COMPACT -> {
            // Horizontal LazyRow for Compact screens to preserve 1-row vertical viewport height
            LazyRow(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = LayoutConstants.ScreenHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                itemsIndexed(items) { index, itemComposable ->
                    itemComposable(Modifier.width(180.dp))
                }
            }
        }
        WindowBreakpoint.MEDIUM, WindowBreakpoint.EXPANDED -> {
            val columns = if (breakpoint == WindowBreakpoint.MEDIUM) 3 else 4
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = LayoutConstants.ScreenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                val chunkedItems = items.chunked(columns)
                chunkedItems.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        rowItems.forEach { itemComposable ->
                            itemComposable(Modifier.weight(1f))
                        }
                        val emptySlots = columns - rowItems.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
