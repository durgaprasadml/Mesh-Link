package com.meshlink.ui.sync

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable Compose animations for Mesh Sync, Queue, and Reliability UI components.
 */

@Composable
fun Modifier.syncSpinnerAnimation(isSyncing: Boolean): Modifier {
    if (MeshTheme.reduceMotion || !isSyncing) return this
    val infiniteTransition = rememberInfiniteTransition(label = "SyncSpinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )
    return this.rotate(angle)
}

@Composable
fun Modifier.retryPulseAnimation(isRetrying: Boolean): Modifier {
    if (MeshTheme.reduceMotion || !isRetrying) return this
    val infiniteTransition = rememberInfiniteTransition(label = "RetryPulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )
    return this
        .scale(scalePulse)
        .alpha(alphaPulse)
}

@Composable
fun Modifier.reconnectRippleAnimation(isReconnecting: Boolean): Modifier {
    if (MeshTheme.reduceMotion || !isReconnecting) return this
    val infiniteTransition = rememberInfiniteTransition(label = "ReconnectRipple")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RippleAlpha"
    )
    return this.alpha(alpha)
}

@Composable
fun Modifier.offlineFadeAnimation(isOffline: Boolean): Modifier {
    val targetAlpha = if (isOffline) 0.85f else 1.0f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = if (MeshTheme.reduceMotion) 0 else 300),
        label = "OfflineFade"
    )
    return this.graphicsLayer { alpha = animatedAlpha }
}

@Composable
fun AnimatedQueueProgress(
    targetProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable (animatedProgress: Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = if (MeshTheme.reduceMotion) 0 else 500,
            easing = FastOutSlowInEasing
        ),
        label = "QueueProgress"
    )
    Box(modifier = modifier) {
        content(animatedProgress)
    }
}

@Composable
fun PeerReconnectAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(if (MeshTheme.reduceMotion) 0 else 250)) + expandVertically(),
        exit = fadeOut(tween(if (MeshTheme.reduceMotion) 0 else 200)) + shrinkVertically()
    ) {
        content()
    }
}

@Composable
fun RecoveryTransitionLayout(
    isRestoring: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = MeshTheme.reduceMotion
    AnimatedContent(
        targetState = isRestoring,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            } else {
                (fadeIn(tween(300)) + scaleIn(initialScale = 0.98f)) togetherWith
                        (fadeOut(tween(200)) + scaleOut(targetScale = 0.98f))
            }
        },
        label = "RecoveryTransition"
    ) { _ ->
        content()
    }
}
