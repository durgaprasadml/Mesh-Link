package com.meshlink.ui.landing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Minimal, Cinematic Mesh Link Landing Experience.
 *
 * Recreates the official 6-node Mesh Link logo procedurally:
 *   1. Starts with faint distant stars (~15% opacity) in deep space.
 *   2. Wireless discovery light beams travel to discover nodes step-by-step.
 *   3. Final 6-node logo pulses softly in unison (~900ms hold).
 *   4. First-time users see an elegant "Welcome to Mesh Link" text overlay.
 *   5. Smooth cinematic camera zoom enters Node 0 (center), transitioning cleanly into Home.
 */
@Composable
fun LandingScreen(
    onAnimationComplete: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current

    val isWelcome = uiState.isWelcomeMode
    val totalDurationMs = if (isWelcome) {
        AnimationConstants.WELCOME_ANIMATION_DURATION_MS
    } else {
        AnimationConstants.STARTUP_ANIMATION_DURATION_MS
    }

    val progressAnimatable = remember { Animatable(0f) }
    var timeMs by remember { mutableLongStateOf(0L) }

    // Haptic feedback milestone flags
    var hapticLogoComplete by remember { mutableStateOf(false) }
    var hapticZoomIgnition by remember { mutableStateOf(false) }

    // Master Animation Clock
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onAnimationComplete()
            return@LaunchedEffect
        }

        val startTime = withFrameNanos { it }

        val animJob = launch {
            progressAnimatable.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = totalDurationMs.toInt(),
                    easing = FastOutSlowInEasing
                )
            )
        }

        while (progressAnimatable.value < 1.0f && !uiState.isCompleted) {
            withFrameNanos { frameNanos ->
                val elapsedMs = (frameNanos - startTime) / 1_000_000L
                timeMs = elapsedMs
                val progress = progressAnimatable.value

                // Haptic feedback at key story moments
                if (progress >= AnimationConstants.PROGRESS_DISCOVERY_END && !hapticLogoComplete) {
                    hapticLogoComplete = true
                    try {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
                if (progress >= AnimationConstants.PROGRESS_HOLD_END && !hapticZoomIgnition) {
                    hapticZoomIgnition = true
                    try {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                }
            }
        }

        animJob.join()
        onAnimationComplete()
    }

    val progress = progressAnimatable.value

    // Welcome text is visible during logo hold phase if first-time user
    val showWelcomeText = isWelcome && progress in AnimationConstants.PROGRESS_DISCOVERY_END..AnimationConstants.PROGRESS_HOLD_END

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimationConstants.DeepCharcoalBg)
            .clickable(interactionSource = interactionSource, indication = null) {
                viewModel.onSkipClicked()
            }
    ) {
        // Procedural 6-Node Canvas
        MeshFormationCanvas(
            overallProgress = progress,
            timeMs = timeMs,
            isWelcomeMode = isWelcome,
            modifier = Modifier.fillMaxSize()
        )

        // First-time user welcome overlay
        WelcomeAnimation(
            displayName = uiState.userName,
            visible = showWelcomeText
        )
    }
}

