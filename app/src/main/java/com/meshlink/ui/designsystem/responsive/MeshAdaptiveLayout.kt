package com.meshlink.ui.designsystem.responsive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.responsive.MeshDeviceType
import com.meshlink.ui.designsystem.theme.responsive.MeshOrientation

/**
 * Adaptive Layout Infrastructure for Mesh-Link 2026.
 * Standardized form factor classifications, adaptive pane layout containers,
 * and navigation type resolution based on Window Size Classes and Device Profiles.
 */

enum class MeshFormFactor {
    PHONE,
    LARGE_PHONE,
    FOLDABLE,
    TABLET,
    DESKTOP,
    LANDSCAPE,
    PORTRAIT
}

enum class MeshAdaptiveNavigationType {
    BOTTOM_BAR,
    NAVIGATION_RAIL,
    PERMANENT_DRAWER
}

@Immutable
data class MeshDeviceProfile(
    val formFactor: MeshFormFactor = MeshFormFactor.PHONE,
    val deviceType: MeshDeviceType = MeshDeviceType.LARGE_PHONE,
    val orientation: MeshOrientation = MeshOrientation.PORTRAIT,
    val widthDp: Dp = 360.dp,
    val heightDp: Dp = 640.dp,
    val isLandscape: Boolean = false,
    val isFoldable: Boolean = false,
    val isTablet: Boolean = false,
    val isDesktop: Boolean = false,
    val recommendedColumns: Int = 1,
    val defaultPadding: Dp = 16.dp,
    val navigationType: MeshAdaptiveNavigationType = MeshAdaptiveNavigationType.BOTTOM_BAR
) {
    val isCompact: Boolean get() = formFactor == MeshFormFactor.PHONE || formFactor == MeshFormFactor.LARGE_PHONE
    val isExpanded: Boolean get() = formFactor == MeshFormFactor.TABLET || formFactor == MeshFormFactor.FOLDABLE || formFactor == MeshFormFactor.DESKTOP
}

val LocalMeshDeviceProfile = staticCompositionLocalOf { MeshDeviceProfile() }

@Composable
fun rememberMeshDeviceProfile(): MeshDeviceProfile {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val orientation = if (isLandscape) MeshOrientation.LANDSCAPE else MeshOrientation.PORTRAIT

    val deviceType = when {
        width < 360.dp -> MeshDeviceType.SMALL_PHONE
        width < 600.dp -> MeshDeviceType.LARGE_PHONE
        width < 840.dp -> MeshDeviceType.FOLDABLE
        width < 1200.dp -> MeshDeviceType.TABLET
        else -> MeshDeviceType.DESKTOP_PREVIEW
    }

    val formFactor = when {
        width >= 1200.dp -> MeshFormFactor.DESKTOP
        width >= 840.dp -> MeshFormFactor.TABLET
        width >= 600.dp -> MeshFormFactor.FOLDABLE
        isLandscape -> MeshFormFactor.LANDSCAPE
        width >= 360.dp -> MeshFormFactor.LARGE_PHONE
        else -> MeshFormFactor.PHONE
    }

    val recommendedColumns = when (formFactor) {
        MeshFormFactor.PHONE, MeshFormFactor.LARGE_PHONE, MeshFormFactor.PORTRAIT -> 1
        MeshFormFactor.FOLDABLE, MeshFormFactor.LANDSCAPE -> 2
        MeshFormFactor.TABLET, MeshFormFactor.DESKTOP -> 3
    }

    val defaultPadding = when (formFactor) {
        MeshFormFactor.PHONE -> 12.dp
        MeshFormFactor.LARGE_PHONE -> 16.dp
        MeshFormFactor.FOLDABLE -> 20.dp
        MeshFormFactor.LANDSCAPE -> 20.dp
        MeshFormFactor.TABLET -> 24.dp
        MeshFormFactor.DESKTOP -> 32.dp
        MeshFormFactor.PORTRAIT -> 16.dp
    }

    val navigationType = when {
        width >= 1200.dp -> MeshAdaptiveNavigationType.PERMANENT_DRAWER
        width >= 600.dp || isLandscape -> MeshAdaptiveNavigationType.NAVIGATION_RAIL
        else -> MeshAdaptiveNavigationType.BOTTOM_BAR
    }

    return remember(width, height, isLandscape) {
        MeshDeviceProfile(
            formFactor = formFactor,
            deviceType = deviceType,
            orientation = orientation,
            widthDp = width,
            heightDp = height,
            isLandscape = isLandscape,
            isFoldable = formFactor == MeshFormFactor.FOLDABLE,
            isTablet = formFactor == MeshFormFactor.TABLET,
            isDesktop = formFactor == MeshFormFactor.DESKTOP,
            recommendedColumns = recommendedColumns,
            defaultPadding = defaultPadding,
            navigationType = navigationType
        )
    }
}

@Composable
fun MeshDeviceProfileProvider(
    profile: MeshDeviceProfile = rememberMeshDeviceProfile(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalMeshDeviceProfile provides profile) {
        content()
    }
}

@Composable
fun MeshAdaptiveLayout(
    modifier: Modifier = Modifier,
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current,
    phone: (@Composable BoxScope.() -> Unit)? = null,
    largePhone: (@Composable BoxScope.() -> Unit)? = phone,
    foldable: (@Composable BoxScope.() -> Unit)? = largePhone,
    tablet: (@Composable BoxScope.() -> Unit)? = foldable,
    landscape: (@Composable BoxScope.() -> Unit)? = null,
    portrait: (@Composable BoxScope.() -> Unit)? = null,
    defaultLayout: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        when {
            profile.isLandscape && landscape != null -> landscape()
            !profile.isLandscape && portrait != null -> portrait()
            profile.formFactor == MeshFormFactor.DESKTOP && tablet != null -> tablet()
            profile.formFactor == MeshFormFactor.TABLET && tablet != null -> tablet()
            profile.formFactor == MeshFormFactor.FOLDABLE && foldable != null -> foldable()
            profile.formFactor == MeshFormFactor.LARGE_PHONE && largePhone != null -> largePhone()
            profile.formFactor == MeshFormFactor.PHONE && phone != null -> phone()
            else -> defaultLayout()
        }
    }
}

/**
 * Adaptive Triple-Pane Layout for Desktop, Tablet, and Large Foldables.
 */
@Composable
fun MeshTriplePaneLayout(
    navigationPane: @Composable () -> Unit,
    primaryListPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current
) {
    if (profile.isDesktop || (profile.isTablet && profile.isLandscape)) {
        Row(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(280.dp).fillMaxHeight()) { navigationPane() }
            Box(modifier = Modifier.width(360.dp).fillMaxHeight()) { primaryListPane() }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { detailPane() }
        }
    } else if (profile.isExpanded) {
        Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) { primaryListPane() }
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) { detailPane() }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) { primaryListPane() }
    }
}
