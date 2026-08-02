package com.meshlink.ui.designsystem.responsive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.shell.LocalMeshWindowInsets
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Adaptive UI Helpers for Mesh-Link 2026.
 * Standardizes dynamic spacing, responsive cards, adaptive typography scaling,
 * responsive grid calculation, adaptive navigation container, and FAB placement.
 */

@Immutable
data class MeshAdaptiveSpacing(
    val tiny: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val contentPadding: PaddingValues = PaddingValues(16.dp)
)

@Composable
fun rememberMeshAdaptiveSpacing(
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current
): MeshAdaptiveSpacing {
    val scaleFactor = when (profile.formFactor) {
        MeshFormFactor.PHONE -> 0.85f
        MeshFormFactor.LARGE_PHONE -> 1.0f
        MeshFormFactor.FOLDABLE -> 1.25f
        MeshFormFactor.TABLET -> 1.5f
        MeshFormFactor.DESKTOP -> 1.6f
        MeshFormFactor.LANDSCAPE -> 1.1f
        MeshFormFactor.PORTRAIT -> 1.0f
    }

    return remember(profile.formFactor) {
        MeshAdaptiveSpacing(
            tiny = (4 * scaleFactor).dp,
            small = (8 * scaleFactor).dp,
            medium = (16 * scaleFactor).dp,
            large = (24 * scaleFactor).dp,
            extraLarge = (32 * scaleFactor).dp,
            contentPadding = PaddingValues(
                horizontal = profile.defaultPadding,
                vertical = (16 * scaleFactor).dp
            )
        )
    }
}

/**
 * Responsive Card Container adapting corner radius, padding, elevation, and layout.
 */
@Composable
fun MeshAdaptiveCard(
    modifier: Modifier = Modifier,
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current,
    backgroundColor: Color = MeshTheme.colors.surfaceVariant,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = when (profile.formFactor) {
        MeshFormFactor.PHONE -> 16.dp
        MeshFormFactor.LARGE_PHONE -> 20.dp
        MeshFormFactor.FOLDABLE -> 24.dp
        MeshFormFactor.TABLET -> 28.dp
        MeshFormFactor.DESKTOP -> 32.dp
        else -> 20.dp
    }

    val padding = contentPadding ?: when (profile.formFactor) {
        MeshFormFactor.PHONE -> PaddingValues(12.dp)
        MeshFormFactor.LARGE_PHONE -> PaddingValues(16.dp)
        MeshFormFactor.FOLDABLE -> PaddingValues(20.dp)
        MeshFormFactor.TABLET -> PaddingValues(24.dp)
        MeshFormFactor.DESKTOP -> PaddingValues(28.dp)
        else -> PaddingValues(16.dp)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        color = backgroundColor,
        tonalElevation = MeshTheme.elevation.card
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}

/**
 * Adaptive Lazy Grid calculating columns based on screen width & min item width.
 */
@Composable
fun MeshAdaptiveLazyGrid(
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 160.dp,
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current,
    contentPadding: PaddingValues = PaddingValues(profile.defaultPadding),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minItemWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}

/**
 * Adaptive Typography helper scaling font styles responsively.
 */
@Composable
fun TextStyle.meshAdaptive(
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current
): TextStyle {
    val multiplier = when (profile.formFactor) {
        MeshFormFactor.PHONE -> 0.92f
        MeshFormFactor.LARGE_PHONE -> 1.0f
        MeshFormFactor.FOLDABLE -> 1.1f
        MeshFormFactor.TABLET -> 1.18f
        MeshFormFactor.DESKTOP -> 1.25f
        else -> 1.0f
    }
    return this.copy(
        fontSize = (this.fontSize.value * multiplier).sp,
        lineHeight = if (this.lineHeight.isSp) (this.lineHeight.value * multiplier).sp else this.lineHeight
    )
}

/**
 * Adaptive Navigation Container automatically switching between Bottom Dock and Navigation Rail/Drawer.
 */
@Composable
fun MeshAdaptiveNavigationLayout(
    modifier: Modifier = Modifier,
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current,
    bottomNavigation: (@Composable () -> Unit)? = null,
    navigationRail: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val insets = LocalMeshWindowInsets.current

    if (profile.isExpanded || profile.isLandscape) {
        Row(modifier = modifier.fillMaxSize()) {
            if (navigationRail != null) {
                Box(modifier = Modifier.fillMaxHeight()) {
                    navigationRail()
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content(insets.safeContentPadding)
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content(insets.safeContentPadding)
                }
                if (bottomNavigation != null) {
                    bottomNavigation()
                }
            }
        }
    }
}

/**
 * Adaptive FAB Host positioning FAB responsively depending on device form factor.
 */
@Composable
fun MeshAdaptiveFabHost(
    modifier: Modifier = Modifier,
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current,
    fab: @Composable () -> Unit
) {
    val insets = LocalMeshWindowInsets.current

    val alignment = if (profile.isExpanded || profile.isLandscape) {
        Alignment.BottomStart
    } else {
        Alignment.BottomEnd
    }

    val paddingValues = if (profile.isExpanded || profile.isLandscape) {
        PaddingValues(start = 88.dp, bottom = 24.dp)
    } else {
        PaddingValues(end = 20.dp, bottom = insets.navigationBarHeight + 80.dp)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = alignment
    ) {
        fab()
    }
}
