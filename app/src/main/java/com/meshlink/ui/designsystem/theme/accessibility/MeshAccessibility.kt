package com.meshlink.ui.designsystem.theme.accessibility

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessibility Guidelines & Rule Enforcers for Mesh Link 2026.
 */
@Immutable
data class MeshAccessibilityRules(
    val minTouchTargetSize: Dp = 48.dp,
    val highContrastEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false,
    val talkBackEnabled: Boolean = false
)

val LocalMeshAccessibilityRules = staticCompositionLocalOf { MeshAccessibilityRules() }

/** Enforces Android standard 48dp minimum touch target for interactive UI elements. */
fun Modifier.meshMinTouchTarget(minSize: Dp = 48.dp): Modifier {
    return this.defaultMinSize(minWidth = minSize, minHeight = minSize)
}

/** Configures TalkBack friendly semantic description and role. */
fun Modifier.meshSemantics(
    description: String,
    role: Role? = null
): Modifier = this.semantics(mergeDescendants = true) {
    this.contentDescription = description
    role?.let { this.role = it }
}
