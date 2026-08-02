package com.meshlink.ui.production

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-Preview Annotations for Mesh-Link Phase 15.
 * Provides complete coverage across themes, viewports, font scaling, and orientations.
 */

@Preview(name = "Light Mode - Phone", group = "Themes", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode - Phone", group = "Themes", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class MeshThemePreviews

@Preview(name = "Tablet Portrait", group = "Devices", device = Devices.TABLET, showBackground = true)
@Preview(name = "Foldable Unfolded", group = "Devices", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Landscape Phone", group = "Devices", device = "spec:parent=pixel_5,orientation=landscape", showBackground = true)
annotation class MeshAdaptiveDevicePreviews

@Preview(name = "Large Font Scale (1.5x)", group = "Accessibility", fontScale = 1.5f, showBackground = true)
@Preview(name = "High Contrast Mode", group = "Accessibility", fontScale = 1.3f, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class MeshAccessibilityPreviews
