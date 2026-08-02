package com.meshlink.ui.designsystem.theme.haptics

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Indication
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Standard Interaction Feedback and Ripple Tokens for Mesh-Link Design System.
 */
@Immutable
object InteractionTokens {
    val PressAlpha: Float = 0.12f
    val FocusAlpha: Float = 0.20f
    val HoverAlpha: Float = 0.08f
    val DragAlpha: Float = 0.16f

    val FocusBorderWidth: Dp = 2.dp
    val SelectionBorderWidth: Dp = 1.5.dp

    @Composable
    fun rememberMeshRipple(
        bounded: Boolean = true,
        radius: Dp = Dp.Unspecified,
        color: Color = MeshTheme.colors.primary
    ): Indication {
        return ripple(bounded = bounded, radius = radius, color = color)
    }

    /**
     * Perform standard haptic feedback for interactions.
     */
    fun performHaptic(view: View, type: MeshHapticType) {
        when (type) {
            MeshHapticType.PRESS -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            MeshHapticType.LONG_PRESS -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            MeshHapticType.SELECTION -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            MeshHapticType.SUCCESS -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            MeshHapticType.ERROR -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }
}

enum class MeshHapticType {
    PRESS,
    LONG_PRESS,
    SELECTION,
    SUCCESS,
    ERROR
}

@Composable
fun rememberMeshHaptic(): (MeshHapticType) -> Unit {
    val view = LocalView.current
    return { type ->
        InteractionTokens.performHaptic(view, type)
    }
}
