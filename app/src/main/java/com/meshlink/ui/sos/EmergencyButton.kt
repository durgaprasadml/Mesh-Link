package com.meshlink.ui.sos

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme
import kotlinx.coroutines.delay

@Composable
fun EmergencyButton(
    state: SosUiState,
    onActivate: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var countdownValue by remember { mutableIntStateOf(3) }

    val infiniteTransition = rememberInfiniteTransition(label = "sos_button_pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_breathing_scale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sos_button_scale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed && state.status == SosStatus.SAFE) {
            countdownValue = 3
            progress.snapTo(0f)

            for (i in 3 downTo 1) {
                countdownValue = i
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                progress.animateTo(
                    targetValue = (4 - i) / 3f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            }

            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onActivate()
            isPressed = false
        } else {
            progress.animateTo(0f, animationSpec = tween(300))
            countdownValue = 3
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = when (state.status) {
                    SosStatus.SAFE -> if (isPressed) "Holding to activate. $countdownValue seconds remaining." else "Hold for 3 seconds to send emergency SOS."
                    SosStatus.BROADCASTING -> "Broadcasting SOS distress message"
                    SosStatus.DELIVERED -> "SOS delivered successfully"
                    SosStatus.FAILED -> "SOS broadcast failed. Tap to retry."
                }
                role = Role.Button
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp)
        ) {
            // Background pulsing aura / Beacon rings
            if (state.status == SosStatus.SAFE && !isPressed) {
                EmergencyBeaconPulse(size = 180.dp, enabled = true)
            } else if (state.status == SosStatus.BROADCASTING) {
                EmergencyBeaconPulse(size = 180.dp, color = MeshTheme.colors.warning, enabled = true)
            } else if (state.status == SosStatus.DELIVERED) {
                EmergencyBeaconPulse(size = 180.dp, color = MeshTheme.colors.success, enabled = false)
            }

            // Outer Hold Countdown Progress Ring
            if (isPressed && state.status == SosStatus.SAFE) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(210.dp),
                    color = MeshTheme.colors.danger,
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }

            // Main Interactive SOS Circle
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .scale(if (state.status == SosStatus.SAFE && !isPressed) breathingScale * buttonScale else buttonScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = when (state.status) {
                                SosStatus.SAFE -> listOf(
                                    MeshTheme.colors.danger.copy(alpha = 0.85f),
                                    MeshTheme.colors.danger
                                )
                                SosStatus.BROADCASTING -> listOf(
                                    MeshTheme.colors.warning.copy(alpha = 0.85f),
                                    MeshTheme.colors.warning
                                )
                                SosStatus.DELIVERED -> listOf(
                                    MeshTheme.colors.success.copy(alpha = 0.85f),
                                    MeshTheme.colors.success
                                )
                                SosStatus.FAILED -> listOf(
                                    MeshTheme.colors.danger,
                                    Color(0xFF8B0000)
                                )
                            }
                        )
                    )
                    .border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .pointerInput(state.status) {
                        if (state.status == SosStatus.SAFE || state.status == SosStatus.FAILED) {
                            detectTapGestures(
                                onPress = {
                                    if (state.status == SosStatus.FAILED) {
                                        onActivate()
                                    } else {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isPressed && state.status == SosStatus.SAFE -> {
                        Text(
                            text = "$countdownValue",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 64.sp
                            ),
                            color = Color.White
                        )
                    }
                    state.status == SosStatus.BROADCASTING -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SENDING",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                    }
                    state.status == SosStatus.DELIVERED -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "SOS Delivered",
                                modifier = Modifier.size(56.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "DELIVERED",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                    state.status == SosStatus.FAILED -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry SOS",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TAP RETRY",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency SOS Icon",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SOS",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                isPressed -> "HOLDING... KEEP PRESSING"
                state.status == SosStatus.SAFE -> "HOLD FOR 3 SECONDS TO DISTRESS"
                state.status == SosStatus.BROADCASTING -> "BROADCAST ACTIVE - MESH TRANSMITTING"
                state.status == SosStatus.DELIVERED -> "ALERT DELIVERED TO RESPONDERS"
                state.status == SosStatus.FAILED -> "BROADCAST FAILED - TAP BUTTON TO RETRY"
                else -> ""
            },
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = if (isPressed || state.status == SosStatus.FAILED) MeshTheme.colors.danger else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
