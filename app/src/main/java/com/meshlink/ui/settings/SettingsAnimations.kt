package com.meshlink.ui.settings

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

object SettingsAnimations {

    val DurationMedium = 300
    val DurationLong = 400

    fun sharedAxisXForward(): ContentTransform {
        return (slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 4 },
            animationSpec = tween(DurationMedium, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(DurationMedium)
        )).togetherWith(
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(DurationMedium, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(DurationMedium)
            )
        )
    }

    fun sharedAxisXBackward(): ContentTransform {
        return (slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(DurationMedium, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(DurationMedium)
        )).togetherWith(
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(DurationMedium, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(DurationMedium)
            )
        )
    }

    fun fadeThrough(): ContentTransform {
        return (fadeIn(
            animationSpec = tween(DurationMedium, delayMillis = 50)
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(DurationMedium)
        )).togetherWith(
            fadeOut(
                animationSpec = tween(50)
            )
        )
    }

    fun searchExpand(): EnterTransition {
        return fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { -20 },
            animationSpec = tween(200)
        )
    }

    fun searchCollapse(): ExitTransition {
        return fadeOut(animationSpec = tween(150)) + slideOutVertically(
            targetOffsetY = { -20 },
            animationSpec = tween(150)
        )
    }
}
