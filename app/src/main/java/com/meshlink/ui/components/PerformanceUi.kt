package com.meshlink.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.meshlink.ui.designsystem.accessibility.rememberMeshReducedMotion
import com.meshlink.ui.designsystem.theme.motion.MeshMotion
import kotlinx.coroutines.delay

/**
 * Performance Perception UI Wrappers for Mesh-Link.
 * Optimizes perceived app responsiveness through progressive loading, lazy placeholder replacements,
 * deferred animation triggers, and smooth crossfade transitions.
 */

@Composable
fun ProgressiveLoadingContainer(
    isLoading: Boolean,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minSkeletonDurationMs: Long = 300L
) {
    var showContent by remember { mutableStateOf(!isLoading) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(minSkeletonDurationMs)
            showContent = true
        } else {
            showContent = false
        }
    }

    PerformanceCrossfade(
        targetState = showContent,
        modifier = modifier
    ) { isReady ->
        if (isReady) {
            content()
        } else {
            skeleton()
        }
    }
}

@Composable
fun PerformanceCrossfade(
    targetState: Boolean,
    modifier: Modifier = Modifier,
    durationMs: Int = MeshMotion.DURATION_SHORT_2,
    content: @Composable (Boolean) -> Unit
) {
    val isReducedMotion = rememberMeshReducedMotion()
    Crossfade(
        targetState = targetState,
        animationSpec = tween(if (isReducedMotion) 0 else durationMs),
        modifier = modifier,
        label = "performance_crossfade"
    ) { state ->
        content(state)
    }
}

@Composable
fun IntelligentFadeIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isReducedMotion = rememberMeshReducedMotion()
    PerformanceCrossfade(
        targetState = visible,
        modifier = modifier,
        durationMs = if (isReducedMotion) 0 else MeshMotion.DURATION_MEDIUM_1
    ) { isVis ->
        if (isVis) {
            content()
        }
    }
}

@Composable
fun DeferredAnimationLauncher(
    delayMs: Long = 100L,
    content: @Composable (Boolean) -> Unit
) {
    var animateNow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMs)
        animateNow = true
    }

    content(animateNow)
}

@Composable
fun PerformanceAnimatedContent(
    targetState: Any?,
    modifier: Modifier = Modifier,
    content: @Composable (Any?) -> Unit
) {
    val isReducedMotion = rememberMeshReducedMotion()
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            MeshMotion.fadeThroughEnter(isReducedMotion) togetherWith MeshMotion.fadeThroughExit(isReducedMotion)
        },
        label = "performance_animated_content"
    ) { state ->
        content(state)
    }
}
