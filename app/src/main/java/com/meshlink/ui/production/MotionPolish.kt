package com.meshlink.ui.production

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.designsystem.motion.MeshAnimationEngine

/**
 * Standardized Motion Polish for Mesh-Link Phase 15.
 * Provides unified screen transitions, card micro-interactions, FAB motion,
 * and bottom sheet entrance/exit curves reusing MeshAnimationEngine.
 */

object MeshMotionPolish {

    // Standardized durations across Phase 1-15 UI
    const val DURATION_FAST = 150
    const val DURATION_MEDIUM = 300
    const val DURATION_SLOW = 500

    // Standardized Enter Transitions
    val ScreenEnter: EnterTransition = fadeIn(
        animationSpec = tween(durationMillis = DURATION_MEDIUM)
    ) + slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 10 },
        animationSpec = tween(durationMillis = DURATION_MEDIUM)
    )

    val ScreenExit: ExitTransition = fadeOut(
        animationSpec = tween(durationMillis = DURATION_FAST)
    )

    val CardEnter: EnterTransition = fadeIn(
        animationSpec = tween(durationMillis = DURATION_FAST)
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val CardExit: ExitTransition = fadeOut(
        animationSpec = tween(durationMillis = DURATION_FAST)
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(durationMillis = DURATION_FAST)
    )

    val BottomSheetEnter: EnterTransition = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn(animationSpec = tween(DURATION_MEDIUM))

    val BottomSheetExit: ExitTransition = slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(DURATION_FAST)
    ) + fadeOut(animationSpec = tween(DURATION_FAST))
}

@Composable
fun MeshAnimatedContentContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = MeshMotionPolish.CardEnter,
    exit: ExitTransition = MeshMotionPolish.CardExit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier
    ) {
        content()
    }
}
