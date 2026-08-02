package com.meshlink.ui.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.motion.MeshTransitionSystem

object NotificationAnimations {
    val SlideInExpand: EnterTransition = expandVertically(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(animationSpec = tween(durationMillis = 250))

    val SlideOutShrink: ExitTransition = shrinkVertically(
        animationSpec = tween(durationMillis = 200)
    ) + fadeOut(animationSpec = tween(durationMillis = 200))

    val AlertPulseSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    @Composable
    fun PulseContainer(
        enabled: Boolean = true,
        content: @Composable (Float) -> Unit
    ) {
        if (!enabled) {
            content(1f)
            return
        }

        val infiniteTransition = rememberInfiniteTransition(label = "AlertPulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = AlertPulseSpec,
            label = "PulseAlpha"
        )
        content(alpha)
    }

    @Composable
    fun AccessibleNotificationRow(
        contentDescription: String,
        modifier: Modifier = Modifier,
        minTouchTargetSize: Dp = 48.dp,
        onClick: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minTouchTargetSize)
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    if (onClick != null) {
                        this.role = Role.Button
                    }
                },
            onClick = onClick ?: {},
            enabled = onClick != null
        ) {
            content()
        }
    }
}
