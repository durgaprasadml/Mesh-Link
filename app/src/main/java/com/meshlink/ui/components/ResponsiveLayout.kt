package com.meshlink.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowBreakpoint {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberWindowBreakpoint(): WindowBreakpoint {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return remember(screenWidthDp) {
        when {
            screenWidthDp < 600 -> WindowBreakpoint.COMPACT
            screenWidthDp < 840 -> WindowBreakpoint.MEDIUM
            else -> WindowBreakpoint.EXPANDED
        }
    }
}

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

/**
 * Responsive TwoPaneLayout composable.
 * Renders side-by-side on MEDIUM/EXPANDED width or in LANDSCAPE orientation,
 * and stacked top-to-bottom on COMPACT portrait screens.
 */
@Composable
fun TwoPaneLayout(
    firstPane: @Composable ColumnScope.() -> Unit,
    secondPane: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    firstPaneWeight: Float = 0.5f,
    spacing: Dp = 16.dp
) {
    val breakpoint = rememberWindowBreakpoint()
    val landscape = isLandscape()
    val isTwoPane = breakpoint != WindowBreakpoint.COMPACT || landscape

    if (isTwoPane) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Column(
                modifier = Modifier
                    .weight(firstPaneWeight)
                    .fillMaxHeight(),
                content = firstPane
            )
            Column(
                modifier = Modifier
                    .weight(1f - firstPaneWeight)
                    .fillMaxHeight(),
                content = secondPane
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = firstPane
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = secondPane
            )
        }
    }
}
