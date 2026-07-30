package com.meshlink.ui.landing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
 * Implements the continuous landing transition:
 *   1. Accelerating 6-node discovery with heavy connection line overlaps & travelling pulses.
 *   2. Logo completion breathing hold (400ms).
 *   3. 3D Depth Camera zoom through center node (780ms) with tiny glow bloom.
 *   4. Phase-based navigation trigger at 68% zoom progress, allowing Landing Screen to fade
 *      dissolve into Home Screen cleanly without pauses or black frames.
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

    // Haptic feedback & navigation phase flags
    var hapticLogoComplete by remember { mutableStateOf(false) }
    var hapticZoomIgnition by remember { mutableStateOf(false) }
    var navigationTriggered by remember { mutableStateOf(false) }

    val discoveryEndTimeMs = AnimationConstants.START_PAUSE_MS + AnimationConstants.DISCOVERY_TOTAL_MS // 1040ms
    val zoomStartTimeMs = discoveryEndTimeMs + AnimationConstants.LOGO_HOLD_MS + (if (isWelcome) AnimationConstants.WELCOME_TEXT_HOLD_MS else 0L)
    val navTriggerTimeMs = zoomStartTimeMs + (AnimationConstants.CENTER_ZOOM_DURATION_MS * 0.68f).toLong()

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
                    easing = LinearEasing
                )
            )
        }

        while (progressAnimatable.value < 1.0f && !uiState.isCompleted) {
            withFrameNanos { frameNanos ->
                val elapsedMs = (frameNanos - startTime) / 1_000_000L
                timeMs = elapsedMs

                // Haptic feedback at key story moments
                if (elapsedMs >= discoveryEndTimeMs && !hapticLogoComplete) {
                    hapticLogoComplete = true
                    try {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
                if (elapsedMs >= zoomStartTimeMs && !hapticZoomIgnition) {
                    hapticZoomIgnition = true
                    try {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                }

                // Trigger navigation phase during final 32% of camera zoom
                if (elapsedMs >= navTriggerTimeMs && !navigationTriggered) {
                    navigationTriggered = true
                    onAnimationComplete()
                }
            }
        }

        animJob.join()
        if (!navigationTriggered) {
            navigationTriggered = true
            onAnimationComplete()
        }
    }

    // Welcome text visible during welcome hold phase for first-time users
    val showWelcomeText = isWelcome && timeMs in discoveryEndTimeMs..(discoveryEndTimeMs + AnimationConstants.WELCOME_TEXT_HOLD_MS)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimationConstants.DeepCharcoalBg)
            .clickable(interactionSource = interactionSource, indication = null) {
                viewModel.onSkipClicked()
            }
    ) {
        // Procedural 6-Node Canvas (Beams, traveling light pulses, 6 identical star nodes)
        MeshFormationCanvas(
            timeMs = timeMs,
            isWelcomeMode = isWelcome,
            modifier = Modifier.fillMaxSize()
        )

        // First-time user welcome overlay text
        WelcomeAnimation(
            displayName = uiState.userName,
            visible = showWelcomeText
        )
    }
}
