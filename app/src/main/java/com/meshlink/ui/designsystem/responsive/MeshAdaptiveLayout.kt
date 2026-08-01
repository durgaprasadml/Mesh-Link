package com.meshlink.ui.designsystem.responsive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
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
 * Standardized form factor classifications and layout helpers inherited by all screens.
 */

enum class MeshFormFactor {
    PHONE,
    LARGE_PHONE,
    FOLDABLE,
    TABLET,
    LANDSCAPE,
    PORTRAIT
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
    val isHalfOpenedPosture: Boolean = false,
    val recommendedColumns: Int = 1,
    val defaultPadding: Dp = 16.dp
) {
    val isCompact: Boolean get() = formFactor == MeshFormFactor.PHONE || formFactor == MeshFormFactor.LARGE_PHONE
    val isExpanded: Boolean get() = formFactor == MeshFormFactor.TABLET || formFactor == MeshFormFactor.FOLDABLE
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
        isLandscape -> MeshFormFactor.LANDSCAPE
        width < 360.dp -> MeshFormFactor.PHONE
        width < 600.dp -> MeshFormFactor.LARGE_PHONE
        width < 840.dp -> MeshFormFactor.FOLDABLE
        else -> MeshFormFactor.TABLET
    }

    val recommendedColumns = when (formFactor) {
        MeshFormFactor.PHONE -> 1
        MeshFormFactor.LARGE_PHONE -> 1
        MeshFormFactor.FOLDABLE -> 2
        MeshFormFactor.TABLET -> 3
        MeshFormFactor.LANDSCAPE -> 2
        MeshFormFactor.PORTRAIT -> 1
    }

    val defaultPadding = when (formFactor) {
        MeshFormFactor.PHONE -> 12.dp
        MeshFormFactor.LARGE_PHONE -> 16.dp
        MeshFormFactor.FOLDABLE -> 24.dp
        MeshFormFactor.TABLET -> 32.dp
        MeshFormFactor.LANDSCAPE -> 20.dp
        MeshFormFactor.PORTRAIT -> 16.dp
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
            recommendedColumns = recommendedColumns,
            defaultPadding = defaultPadding
        )
    }
}

/**
 * Adaptive layout provider that injects [MeshDeviceProfile] into composition.
 */
@Composable
fun MeshDeviceProfileProvider(
    profile: MeshDeviceProfile = rememberMeshDeviceProfile(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalMeshDeviceProfile provides profile) {
        content()
    }
}

/**
 * Adaptive layout container that conditionally renders layout slots based on form factor.
 */
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
            profile.formFactor == MeshFormFactor.TABLET && tablet != null -> tablet()
            profile.formFactor == MeshFormFactor.FOLDABLE && foldable != null -> foldable()
            profile.formFactor == MeshFormFactor.LARGE_PHONE && largePhone != null -> largePhone()
            profile.formFactor == MeshFormFactor.PHONE && phone != null -> phone()
            else -> defaultLayout()
        }
    }
}
