package com.meshlink.ui.designsystem.theme.accessibility

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Immutable
data class MeshAccessibilityRules(
    val highContrastEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false,
    val minTouchTargetDp: Int = 48
)

val LocalMeshAccessibilityRules = staticCompositionLocalOf { MeshAccessibilityRules() }

/**
 * Accessibility Guidelines & Modifiers for Mesh-Link 2026 Original Design System.
 */
@Immutable
object MeshAccessibility {
    val MIN_TOUCH_TARGET_SIZE = 48.dp

    /**
     * Ensures minimum touch target size of 48dp x 48dp per WCAG / M3 standards.
     */
    fun Modifier.meshTouchTarget(): Modifier = this.defaultMinSize(
        minWidth = MIN_TOUCH_TARGET_SIZE,
        minHeight = MIN_TOUCH_TARGET_SIZE
    )

    fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val l1 = foreground.luminance() + 0.05f
        val l2 = background.luminance() + 0.05f
        return if (l1 > l2) l1 / l2 else l2 / l1
    }

    fun isWcagAaCompliant(foreground: Color, background: Color, isLargeText: Boolean = false): Boolean {
        val ratio = calculateContrastRatio(foreground, background)
        return if (isLargeText) ratio >= 3.0f else ratio >= 4.5f
    }
}

fun Modifier.meshTouchTarget(): Modifier = MeshAccessibility.MIN_TOUCH_TARGET_SIZE.let {
    this.defaultMinSize(minWidth = it, minHeight = it)
}

fun Modifier.meshMinTouchTarget(): Modifier = meshTouchTarget()
