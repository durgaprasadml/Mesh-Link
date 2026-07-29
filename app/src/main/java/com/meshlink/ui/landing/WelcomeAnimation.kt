package com.meshlink.ui.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.components.UserAvatarImage
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Glassmorphic Welcome Overlay for First-Time Users — v3.
 *
 * Changes from v2:
 *  • Card appears at progress > 0.40 (was 0.20) — avoids fighting the organic expansion scene
 *  • Card fades out at progress > 0.78 (was 0.85) — cleared before title formation
 *  • Avatar border ring has a slow breathing pulse animation synchronized to the first ripple wave
 *  • Welcome text uses slightly more letter-spacing for premium feel
 */
@Composable
fun WelcomeAnimation(
    displayName: String,
    avatarUri: String?,
    visible: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Scene window: appears after first organic connections, gone before title formation
    val cardVisible = visible && progress in 0.38f..0.75f

    val scaleFactor by animateFloatAsState(
        targetValue    = if (visible) 1.0f else 0.72f,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "welcomeScale"
    )

    // Slow pulsing ring that breathes with the first ripple wave
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringPulse"
    )

    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = cardVisible,
            enter   = fadeIn(tween(700)) + scaleIn(tween(700)),
            exit    = fadeOut(tween(500)) + scaleOut(tween(500))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scaleFactor)
                    .padding(horizontal = MeshTheme.spacing.large)
            ) {
                // ── Central Seed Star User Profile Avatar ────────────────────
                Box(
                    modifier         = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer starlight aurora (slow breathing)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(ringPulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AnimationConstants.StarlightWhiteGlow.copy(alpha = 0.45f),
                                        AnimationConstants.StarlightSilverGlow.copy(alpha = 0.18f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Starlight border ring (pulsing)
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .scale(ringPulse * 0.97f)
                            .clip(CircleShape)
                            .border(
                                width  = 2.dp,
                                brush  = Brush.linearGradient(
                                    listOf(
                                        AnimationConstants.StarlightWhite,
                                        AnimationConstants.StarlightSilver.copy(alpha = 0.7f)
                                    )
                                ),
                                shape  = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatarImage(
                            avatarUri   = avatarUri,
                            displayName = displayName,
                            size        = 84.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                // ── Glassmorphic Welcome Card ────────────────────────────────
                Surface(
                    shape          = RoundedCornerShape(22.dp),
                    color          = AnimationConstants.DimBackground,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.border(
                        width  = 1.dp,
                        brush  = Brush.linearGradient(
                            listOf(
                                AnimationConstants.StarlightSilver.copy(alpha = 0.28f),
                                AnimationConstants.StarlightWhite.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text          = "Welcome,",
                            style         = MaterialTheme.typography.titleMedium,
                            color         = AnimationConstants.StarlightSilver,
                            letterSpacing = 1.6.sp,
                            fontWeight    = FontWeight.Light
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text          = displayName.ifBlank { "Explorer" },
                            style         = MaterialTheme.typography.headlineMedium,
                            color         = AnimationConstants.StarlightWhite,
                            fontWeight    = FontWeight.Bold,
                            textAlign     = TextAlign.Center,
                            letterSpacing = 0.4.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text       = "You are now part of Mesh Link",
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = AnimationConstants.SoftWhiteTransparent,
                            textAlign  = TextAlign.Center,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}
