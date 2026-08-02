package com.meshlink.ui.designsystem.accessibility

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Comprehensive Accessibility Framework for Mesh-Link (Material 3 & Android A11y Guidelines).
 * Supports TalkBack, Screen Readers, Live Regions, Semantic Roles, Focus Traversal, Keyboard Navigation,
 * WCAG AA/AAA Contrast Validation, Reduced Motion Detection, and 48dp Minimum Touch Targets.
 */

@Immutable
data class MeshAccessibilityConfig(
    val highContrastEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false,
    val minTouchTargetDp: Dp = 48.dp,
    val dynamicFontScaleFactor: Float = 1.0f
)

val LocalMeshAccessibilityConfig = staticCompositionLocalOf { MeshAccessibilityConfig() }

object MeshAccessibility {
    val MIN_TOUCH_TARGET_SIZE: Dp = 48.dp

    /**
     * Minimum 48dp touch target modifier per WCAG 2.1 AA and Material 3 guidelines.
     */
    fun Modifier.meshTouchTarget(): Modifier = this.defaultMinSize(
        minWidth = MIN_TOUCH_TARGET_SIZE,
        minHeight = MIN_TOUCH_TARGET_SIZE
    )

    /**
     * Computes contrast ratio between foreground and background colors.
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val l1 = foreground.luminance() + 0.05f
        val l2 = background.luminance() + 0.05f
        return if (l1 > l2) l1 / l2 else l2 / l1
    }

    /**
     * Evaluates WCAG AA compliance (3.0 for large text, 4.5 for normal text).
     */
    fun isWcagAaCompliant(foreground: Color, background: Color, isLargeText: Boolean = false): Boolean {
        val ratio = calculateContrastRatio(foreground, background)
        return if (isLargeText) ratio >= 3.0f else ratio >= 4.5f
    }

    /**
     * Evaluates WCAG AAA compliance (4.5 for large text, 7.0 for normal text).
     */
    fun isWcagAaaCompliant(foreground: Color, background: Color, isLargeText: Boolean = false): Boolean {
        val ratio = calculateContrastRatio(foreground, background)
        return if (isLargeText) ratio >= 4.5f else ratio >= 7.0f
    }
}

// Global Extension Modifiers for Accessibility

fun Modifier.meshTouchTarget48dp(): Modifier = this.defaultMinSize(
    minWidth = MeshAccessibility.MIN_TOUCH_TARGET_SIZE,
    minHeight = MeshAccessibility.MIN_TOUCH_TARGET_SIZE
)

fun Modifier.meshMinTouchTarget(): Modifier = meshTouchTarget48dp()

/**
 * Applies semantic TalkBack description, optional role, state description, and heading status.
 */
fun Modifier.meshSemantics(
    description: String,
    role: Role? = null,
    stateDescription: String? = null,
    isHeading: Boolean = false,
    isSelected: Boolean? = null,
    isDisabled: Boolean = false
): Modifier = this.semantics {
    contentDescription = description
    role?.let { this.role = it }
    stateDescription?.let { this.stateDescription = it }
    if (isHeading) heading()
    isSelected?.let { this.selected = it }
    if (isDisabled) disabled()
}

/**
 * Configures screen reader focus traversal order.
 */
fun Modifier.meshTraversalGroup(index: Float = 0f): Modifier = this.semantics {
    isTraversalGroup = true
    traversalIndex = index
}

/**
 * Defines a Live Region for dynamic updates (e.g., status changes, notifications, errors).
 */
fun Modifier.meshLiveRegion(assertive: Boolean = false): Modifier = this.semantics {
    liveRegion = if (assertive) LiveRegionMode.Assertive else LiveRegionMode.Polite
}

/**
 * Clears child semantics and sets a single unified content description.
 */
fun Modifier.meshClearAndSetDescription(description: String): Modifier = this.clearAndSetSemantics {
    contentDescription = description
}

/**
 * Dynamic Font Scaling helper for TextStyle.
 */
@Composable
fun TextStyle.meshDynamicFont(fontScale: Float = LocalContext.current.resources.configuration.fontScale): TextStyle {
    val adjustedSize = (this.fontSize.value * fontScale.coerceIn(0.85f, 2.0f)).sp
    val adjustedLineHeight = if (this.lineHeight.isSp) {
        (this.lineHeight.value * fontScale.coerceIn(0.85f, 2.0f)).sp
    } else {
        this.lineHeight
    }
    return this.copy(fontSize = adjustedSize, lineHeight = adjustedLineHeight)
}

/**
 * System Reduced Motion detector hook.
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
 * Accessibility Announcer utility for TalkBack speech feedback.
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
