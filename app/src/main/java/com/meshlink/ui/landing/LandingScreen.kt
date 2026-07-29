package com.meshlink.ui.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Master Landing Experience Screen — v4 (Flagship Refinement Edition).
 *
 * Durations: 5.5 s (existing users) / 7.5 s (first-time users).
 *
 * Scene 1  Silence              0.00–0.06  Deep space, 3 twinkling sentinels
 * Scene 2  First Discovery      0.06–0.16  Seed star wakes, first connection
 * Scene 3  Organic Expansion    0.16–0.42  Weighted BFS one-at-a-time growth
 * Scene 4  Living Network       0.42–0.54  Packets route, connections breathe
 * Scene 5  Camera Journey       0.48–0.65  Cinematic dolly + zoom-out
 * Scene 6  Hidden Identity      0.54–0.64  Natural mesh, no text hint
 * Scene 7  Title Formation      0.64–0.80  40% of nodes migrate progressively
 * Scene 8  Living Logo          0.80–0.86  Constellation breathes
 * Scene 9  Synchronization Wave 0.86–0.93  Heartbeat graph propagation
 * Scene 10 Entering the Mesh   0.93–1.00  Fly-through → home transition
 *
 * Business logic, routing, onboarding, BLE, ViewModel — UNCHANGED.
 */
@Composable
fun LandingScreen(
    onAnimationComplete: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel()
) {
    val uiState          by viewModel.uiState.collectAsStateWithLifecycle()
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback   = LocalHapticFeedback.current

    val isWelcome       = uiState.isWelcomeMode
    val totalDurationMs = if (isWelcome) {
        AnimationConstants.WELCOME_ANIMATION_DURATION_MS
    } else {
        AnimationConstants.STARTUP_ANIMATION_DURATION_MS
    }

    val progressAnimatable = remember { Animatable(0f) }
    var timeMs by remember { mutableLongStateOf(0L) }
    val launchSeed = remember { System.currentTimeMillis() }

    // Pre-allocate all animation objects (no allocs during draw)
    val nodes = remember(isWelcome, launchSeed) {
        NodePhysics.generateNodes(isWelcome, launchSeed)
    }
    val connectionAnimator = remember(launchSeed) { MeshConnectionAnimator() }
    val dataPackets = remember(nodes) {
        // 22 packets; stagger 0.28→0.68; 15% fade out mid-route (willDisappear)
        List(22) { id ->
            DataPacket(
                id            = id,
                fromNodeId    = (id * 7) % nodes.size,
                toNodeId      = (id * 7 + 4) % nodes.size,
                baseSpeed     = 0.52f + (id % 5) * 0.12f,
                delayProgress = 0.28f + (id / 22f) * 0.40f,
                willDisappear = (id % 7 == 3)
            )
        }
    }

    LaunchedEffect(nodes) {
        connectionAnimator.buildConnections(nodes, launchSeed)
    }

    // Haptic milestone flags
    var hapticIgnition   by remember { androidx.compose.runtime.mutableStateOf(false) }
    var hapticSignature  by remember { androidx.compose.runtime.mutableStateOf(false) }
    var hapticTransition by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Master animation clock loop
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) { onAnimationComplete(); return@LaunchedEffect }

        val startTime = withFrameNanos { it }

        val animJob = launch {
            progressAnimatable.animateTo(
                targetValue  = 1.0f,
                animationSpec = tween(
                    durationMillis = totalDurationMs.toInt(),
                    easing         = LinearEasing
                )
            )
        }

        var prevFrameNanos = startTime

        while (progressAnimatable.value < 1.0f && !uiState.isCompleted) {
            withFrameNanos { frameNanos ->
                val elapsedMs = (frameNanos - startTime) / 1_000_000L
                val deltaSec  = ((frameNanos - prevFrameNanos) / 1_000_000_000.0).toFloat()
                    .coerceIn(0.001f, 0.050f)   // guard against frame spikes
                prevFrameNanos = frameNanos
                timeMs = elapsedMs

                val progress = progressAnimatable.value
                val timeSec  = elapsedMs / 1000f

                // Compute cinematic camera state
                val camera = NodePhysics.computeCameraState(progress, timeSec)

                // Haptic feedback at key story moments
                if (progress >= AnimationConstants.SCENE_2_END && !hapticIgnition) {
                    hapticIgnition = true
                    try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                }
                if (progress >= AnimationConstants.SIGNATURE_FLASH_PEAK - 0.005f && !hapticSignature) {
                    hapticSignature = true
                    try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                }
                if (progress >= 0.94f && !hapticTransition) {
                    hapticTransition = true
                    try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                }

                // Update node physics (positions, wakeProgress, flash decay)
                NodePhysics.updatePositions(
                    nodes           = nodes,
                    width           = 1080f,
                    height          = 2200f,
                    timeMs          = elapsedMs,
                    overallProgress = progress,
                    cameraScale     = camera.scale,
                    cameraPanX      = camera.panX,
                    cameraPanY      = camera.panY,
                    reduceMotion    = false,
                    deltaSec        = deltaSec
                )

                // Update connections, packets, heartbeat wave, signature
                connectionAnimator.update(
                    overallProgress = progress,
                    packets         = dataPackets,
                    nodes           = nodes,
                    deltaSec        = deltaSec,
                    timeMs          = elapsedMs
                )
            }
        }

        animJob.join()
        onAnimationComplete()
    }

    val progress = progressAnimatable.value
    val timeSec  = timeMs / 1000f

    // ── Camera transform ─────────────────────────────────────────────────────
    val camera = NodePhysics.computeCameraState(progress, timeSec)

    // Scene 10 canvas alpha: radial dissolve (edges fade, center stays sharp longer)
    val canvasAlpha = if (progress > AnimationConstants.SCENE_9_END) {
        val t = ((progress - AnimationConstants.SCENE_9_END) /
                (1.0f - AnimationConstants.SCENE_9_END)).coerceIn(0f, 1f)
        1.0f - (t * t)   // quadratic fade — keeps center sharp longer
    } else 1.0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimationConstants.DeepSpaceNavy)
            .clickable(interactionSource = interactionSource, indication = null) {
                viewModel.onSkipClicked()
            }
    ) {
        // Procedural canvas (all 11 render passes)
        MeshFormationCanvas(
            nodes              = nodes,
            connectionAnimator = connectionAnimator,
            dataPackets        = dataPackets,
            overallProgress    = progress,
            timeMs             = timeMs,
            isWelcomeMode      = isWelcome,
            modifier           = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX    = camera.scale
                    scaleY    = camera.scale
                    rotationZ = camera.rotationDeg
                    alpha     = canvasAlpha
                }
        )

        // First-time user welcome card overlay
        if (isWelcome) {
            WelcomeAnimation(
                displayName = uiState.userName,
                avatarUri   = uiState.avatarUri,
                visible     = true,
                progress    = progress
            )
        }

        // Tap-to-skip hint — visible from first discovery through end of living logo
        AnimatedVisibility(
            visible = progress in AnimationConstants.SCENE_2_END..0.84f,
            enter   = fadeIn(tween(600)),
            exit    = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text         = "Tap anywhere to skip",
                style        = MaterialTheme.typography.labelMedium,
                color        = AnimationConstants.SoftWhiteTransparent.copy(alpha = 0.45f),
                letterSpacing = 0.9.sp
            )
        }
    }
}
