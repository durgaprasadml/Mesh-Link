package com.meshlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Adaptive Layout Framework for Mesh-Link Home Screen.
 * Phone (Compact < 600dp): Single Pane messaging list.
 * Tablet / Foldable / Landscape (>= 600dp): Responsive Two-Pane Master-Detail (Chats + Chat Preview).
 * Strictly presentation layer only.
 */

@Composable
fun HomeAdaptiveLayout(
    singlePaneContent: @Composable () -> Unit,
    masterPaneContent: @Composable () -> Unit,
    detailPaneContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    splitRatio: Float = 0.38f
) {
    val configuration = LocalConfiguration.current
    val isExpandedOrLandscape = configuration.screenWidthDp >= 600

    if (isExpandedOrLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Master Pane: Recent Chats & Actions (38% width)
            Box(
                modifier = Modifier
                    .weight(splitRatio)
                    .fillMaxHeight()
            ) {
                masterPaneContent()
            }

            VerticalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Detail Pane: Conversation Preview / Active Chat (62% width)
            Box(
                modifier = Modifier
                    .weight(1f - splitRatio)
                    .fillMaxHeight()
            ) {
                detailPaneContent()
            }
        }
    } else {
        // Single Pane for compact phone viewports
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            singlePaneContent()
        }
    }
}
