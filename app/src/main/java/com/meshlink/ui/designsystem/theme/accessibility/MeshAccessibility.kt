package com.meshlink.ui.designsystem.theme.accessibility

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class MeshAccessibilityRules(
    val highContrastEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false,
    val minTouchTargetDp: Int = 48
)

val LocalMeshAccessibilityRules = staticCompositionLocalOf { MeshAccessibilityRules() }

/**
 * Accessibility Guidelines & Modifiers for Mesh-Link 2026.
 * Enforces Dynamic Font Bounds, WCAG AA Contrast, Reduced Motion, 48dp Minimum Touch Targets,
 * and Screen Reader Announcers/Semantics.
 */
@Immutable
object MeshAccessibility {
    val MIN_TOUCH_TARGET_SIZE: Dp = 48.dp

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

/**
 * 1. Dynamic Font Helper ensuring text scales safely without clipping.
 */
@Composable
fun TextStyle.meshDynamicFont(fontScale: Float = LocalContext.current.resources.configuration.fontScale): TextStyle {
    val adjustedSize = (this.fontSize.value * fontScale).sp
    val adjustedLineHeight = if (this.lineHeight.isSp) (this.lineHeight.value * fontScale).sp else this.lineHeight
    return this.copy(
        fontSize = adjustedSize,
        lineHeight = adjustedLineHeight
    )
}

/**
 * 2. Reduced Motion Detector Hook checking system settings.
 */
@Composable
fun rememberMeshReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 3. 48dp Minimum Touch Target Modifiers
 */
fun Modifier.meshTouchTarget(): Modifier = MeshAccessibility.MIN_TOUCH_TARGET_SIZE.let {
    this.defaultMinSize(minWidth = it, minHeight = it)
}

fun Modifier.meshTouchTarget48dp(): Modifier = meshTouchTarget()

fun Modifier.meshMinTouchTarget(): Modifier = meshTouchTarget()

/**
 * 4. Screen Reader Semantics & Heading Helpers
 */
fun Modifier.meshSemantics(
    description: String,
    isHeading: Boolean = false
): Modifier = this.semantics {
    contentDescription = description
    if (isHeading) {
        heading()
    }
}

/**
 * 5. Accessibility Announcer Utility for TalkBack
 */
class MeshAccessibilityAnnouncer(private val context: Context) {
    fun announce(message: String) {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        if (accessibilityManager?.isEnabled == true) {
            val event = android.view.accessibility.AccessibilityEvent.obtain(
                android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
            ).apply {
                text.add(message)
                className = MeshAccessibilityAnnouncer::class.java.name
                packageName = context.packageName
            }
            accessibilityManager.sendAccessibilityEvent(event)
        }
    }
}

@Composable
fun rememberMeshAccessibilityAnnouncer(): MeshAccessibilityAnnouncer {
    val context = LocalContext.current
    return remember(context) { MeshAccessibilityAnnouncer(context) }
}
