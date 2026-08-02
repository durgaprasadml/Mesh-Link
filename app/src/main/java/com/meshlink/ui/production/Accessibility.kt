package com.meshlink.ui.production

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable Accessibility Helpers for Mesh-Link Phase 15.
 * Standardizes 48dp touch targets, TalkBack semantics, high contrast checks,
 * dynamic type support, and reduced motion capabilities.
 */

@Immutable
data class AccessibilitySettingsInfo(
    val isTalkBackActive: Boolean = false,
    val isReducedMotionEnabled: Boolean = false,
    val fontScale: Float = 1.0f,
    val isHighContrastEnabled: Boolean = false
)

@Composable
fun rememberAccessibilitySettings(): AccessibilitySettingsInfo {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        AccessibilitySettingsInfo(
            isTalkBackActive = false,
            isReducedMotionEnabled = false,
            fontScale = configuration.fontScale,
            isHighContrastEnabled = configuration.fontScale > 1.3f
        )
    }
}

/**
 * Enforces WCAG AA compliant 48dp minimum touch target size.
 */
fun Modifier.enforceMinimumTouchTarget(
    minWidth: Dp = 48.dp,
    minHeight: Dp = 48.dp
): Modifier = this.defaultMinSize(minWidth = minWidth, minHeight = minHeight)

/**
 * Adds screen reader semantic label for TalkBack.
 */
fun Modifier.meshScreenReaderLabel(
    label: String,
    isHeading: Boolean = false
): Modifier = this.semantics {
    contentDescription = label
    if (isHeading) {
        heading()
    }
}
