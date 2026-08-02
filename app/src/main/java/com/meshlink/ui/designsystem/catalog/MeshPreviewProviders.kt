package com.meshlink.ui.designsystem.catalog

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotations for Mesh-Link Design System.
 * Enables quick multi-theme and multi-device previewing across Light, Dark, AMOLED, Tablet, Foldable, Desktop, Landscape, RTL, and Large Font modes.
 */

@Preview(name = "Light Mode", group = "Themes", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Dark Mode", group = "Themes", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class MeshThemePreviews

@Preview(name = "Phone Portrait", group = "Devices", device = "spec:width=411dp,height=891dp")
@Preview(name = "Phone Landscape", group = "Devices", device = "spec:width=891dp,height=411dp,orientation=landscape")
@Preview(name = "Tablet Landscape", group = "Devices", device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Preview(name = "Foldable Unfolded", group = "Devices", device = "spec:width=673dp,height=841dp")
@Preview(name = "Desktop Mode", group = "Devices", device = "spec:width=1920dp,height=1080dp")
annotation class MeshDevicePreviews

@Preview(name = "Standard Text (1.0x)", group = "FontScaling", fontScale = 1.0f)
@Preview(name = "Large Text (1.3x)", group = "FontScaling", fontScale = 1.3f)
@Preview(name = "Extra Large Text (1.5x)", group = "FontScaling", fontScale = 1.5f)
annotation class MeshFontScalePreviews

@Preview(name = "LTR Layout", group = "Locales", locale = "en")
@Preview(name = "RTL Layout", group = "Locales", locale = "ar")
annotation class MeshLocalePreviews

@MeshThemePreviews
@MeshDevicePreviews
@MeshFontScalePreviews
@MeshLocalePreviews
annotation class MeshFullComponentPreviews
