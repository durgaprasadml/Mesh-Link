package com.meshlink.ui.production

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.responsive.LocalMeshDeviceProfile
import com.meshlink.ui.designsystem.responsive.MeshDeviceProfile
import com.meshlink.ui.designsystem.responsive.MeshFormFactor
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Adaptive Layout Utilities for Mesh-Link Phase 15.
 * Extends MeshAdaptiveLayout to provide responsive screen scaffolds, two-pane layouts,
 * foldable split panes, and landscape adaptive containers.
 */

@Immutable
data class AdaptiveWindowInfo(
    val profile: MeshDeviceProfile,
    val isCompact: Boolean = profile.isCompact,
    val isMedium: Boolean = profile.formFactor == MeshFormFactor.LARGE_PHONE || profile.formFactor == MeshFormFactor.LANDSCAPE,
    val isExpanded: Boolean = profile.isExpanded,
    val isLandscape: Boolean = profile.isLandscape,
    val isFoldable: Boolean = profile.isFoldable,
    val isTablet: Boolean = profile.isTablet
)

@Composable
fun rememberAdaptiveWindowInfo(): AdaptiveWindowInfo {
    val profile = LocalMeshDeviceProfile.current
    return remember(profile) {
        AdaptiveWindowInfo(profile = profile)
    }
}

/**
 * Responsive Screen Scaffold adapting layout structure based on window size class.
 */
@Composable
fun MeshAdaptiveScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo(),
    content: @Composable (AdaptiveWindowInfo) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content(windowInfo)
            }

            if (windowInfo.isCompact) {
                bottomBar()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            floatingActionButton()
        }
    }
}

/**
 * Two-pane adaptive layout for tablets, foldables, and landscape screens.
 */
@Composable
fun MeshAdaptiveTwoPaneLayout(
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo(),
    splitRatio: Float = 0.4f,
    showDetailInCompact: Boolean = false
) {
    if (windowInfo.isExpanded || windowInfo.isLandscape) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(splitRatio)
            ) {
                listPane()
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f - splitRatio)
            ) {
                detailPane()
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            if (showDetailInCompact) {
                detailPane()
            } else {
                listPane()
            }
        }
    }
}

/**
 * Foldable posture aware split pane layout.
 */
@Composable
fun MeshFoldableSplitPane(
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo()
) {
    if (windowInfo.isFoldable || windowInfo.isTablet) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) { primaryContent() }
            Box(modifier = Modifier.weight(1f)) { secondaryContent() }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) { primaryContent() }
            Box(modifier = Modifier.weight(1f)) { secondaryContent() }
        }
    }
}
