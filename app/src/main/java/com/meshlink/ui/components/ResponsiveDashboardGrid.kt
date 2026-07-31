package com.meshlink.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Responsive grid layout for dashboard cards.
 * Automatically computes columns based on screen breakpoint:
 * - COMPACT: 2-3 columns with equal width distribution or row wrap
 * - MEDIUM: 3 columns
 * - EXPANDED: 3-4 columns
 */
@Composable
fun ResponsiveDashboardGrid(
    items: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
    spacing: Dp = MeshTheme.spacing.medium
) {
    val breakpoint = rememberWindowBreakpoint()
    val columns = when (breakpoint) {
        WindowBreakpoint.COMPACT -> 2
        WindowBreakpoint.MEDIUM -> 3
        WindowBreakpoint.EXPANDED -> 4
    }

    Column(
        modifier = modifier.fillMaxWidth(),
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
                // Fill empty slots in row if items count is less than column count
                val emptySlots = columns - rowItems.size
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
