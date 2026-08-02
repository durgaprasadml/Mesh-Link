package com.meshlink.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable Responsive Layout Wrappers for Mesh-Link Phase 15.
 * Provides adaptive Cards, Grids, Dialogs, Sheets, and Toolbars.
 */

@Composable
fun MeshResponsiveCard(
    modifier: Modifier = Modifier,
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = when {
        windowInfo.isExpanded -> 24.dp
        windowInfo.isMedium -> 20.dp
        else -> 16.dp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
        color = MeshTheme.colors.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun MeshResponsiveGrid(
    modifier: Modifier = Modifier,
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo(),
    content: LazyGridScope.() -> Unit
) {
    val columns = when {
        windowInfo.isExpanded -> GridCells.Fixed(3)
        windowInfo.isMedium || windowInfo.isLandscape -> GridCells.Fixed(2)
        else -> GridCells.Fixed(1)
    }

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
fun MeshResponsiveDialogContainer(
    modifier: Modifier = Modifier,
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo(),
    content: @Composable ColumnScope.() -> Unit
) {
    val maxDialogWidth = when {
        windowInfo.isExpanded -> 560.dp
        windowInfo.isMedium -> 480.dp
        else -> 360.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = modifier
                .widthIn(max = maxDialogWidth)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            color = MeshTheme.colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    }
}

@Composable
fun MeshResponsiveToolbar(
    modifier: Modifier = Modifier,
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo(),
    title: @Composable () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val paddingHorizontal = if (windowInfo.isExpanded) 24.dp else 16.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(MeshTheme.colors.surface),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingHorizontal, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            }

            Box(modifier = Modifier.weight(1f)) {
                title()
            }

            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
    }
}
