package com.meshlink.ui.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable Adaptive Layout Framework for Mesh-Link 2026.
 * Supports Phone (Compact), Tablet/Foldable (Medium/Expanded), and Two-Pane responsive views.
 */

@Composable
fun AdaptiveSinglePane(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()
    }
}

/**
 * Responsive Two-Pane Layout for Master-Detail views on Medium and Expanded devices.
 */
@Composable
fun AdaptiveTwoPane(
    firstPane: @Composable () -> Unit,
    secondPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstPaneRatio: Float = 0.40f,
    paneSpacing: Dp = 1.dp
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(firstPaneRatio)
                .fillMaxHeight()
        ) {
            firstPane()
        }

        if (paneSpacing > 0.dp) {
            Spacer(modifier = Modifier.width(paneSpacing))
        }

        Box(
            modifier = Modifier
                .weight(1f - firstPaneRatio)
                .fillMaxHeight()
        ) {
            secondPane()
        }
    }
}

/**
 * Adaptive Content Layout switcher that renders single pane on compact devices,
 * and two-pane master-detail on medium / expanded viewports.
 */
@Composable
fun AdaptiveContentLayout(
    windowSizeClass: WindowSizeClass,
    singlePaneContent: @Composable () -> Unit,
    twoPaneFirst: @Composable () -> Unit,
    twoPaneSecond: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    if (isCompact) {
        AdaptiveSinglePane(
            modifier = modifier,
            content = singlePaneContent
        )
    } else {
        AdaptiveTwoPane(
            firstPane = twoPaneFirst,
            secondPane = twoPaneSecond,
            modifier = modifier
        )
    }
}
