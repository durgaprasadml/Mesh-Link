package com.meshlink.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.meshlink.ui.designsystem.theme.MeshTheme

object ProfileAnimations {

    val SpringSpec: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 350f
    )

    val HeroEnterTransition: EnterTransition = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
            slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = spring(stiffness = 400f))

    val CardEnterTransition: EnterTransition = fadeIn(tween(350)) +
            scaleIn(initialScale = 0.95f, animationSpec = SpringSpec)

    val QrZoomInTransition: EnterTransition = fadeIn(tween(300)) +
            scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.7f))

    val QrZoomOutTransition: ExitTransition = fadeOut(tween(250)) +
            scaleOut(targetScale = 0.85f)

    val ListStaggerEnter: EnterTransition = fadeIn(tween(300)) +
            slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300))

    val FadeThroughEnter: EnterTransition = fadeIn(tween(250, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 0.96f)

    val FadeThroughExit: ExitTransition = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 0.96f)

    val ExpandCardTransition: EnterTransition = expandVertically(animationSpec = spring()) + fadeIn(tween(200))
    val ShrinkCardTransition: ExitTransition = shrinkVertically(animationSpec = spring()) + fadeOut(tween(200))

    @Composable
    fun PulseAvatarEffect(
        isPulsing: Boolean = true,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        if (MeshTheme.reduceMotion || !isPulsing) {
            Box(modifier = modifier) { content() }
            return
        }

        val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "AvatarPulseScale"
        )

        Box(modifier = modifier.scale(scale)) {
            content()
        }
    }
}
