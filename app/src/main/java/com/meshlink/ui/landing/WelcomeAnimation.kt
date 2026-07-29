package com.meshlink.ui.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
 * Glassmorphic Welcome Overlay for First-Time Users (First-Time Seed Node Flow).
 * Displays user's selected profile picture as the initial glowing seed star,
 * surrounding network waves spreading outward, and the welcome message card.
 */
@Composable
fun WelcomeAnimation(
    displayName: String,
    avatarUri: String?,
    visible: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val scaleFactor by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "welcomeScale"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible && progress in 0.20f..0.85f,
            enter = fadeIn(tween(500)) + scaleIn(tween(500)),
            exit = fadeOut(tween(400)) + scaleOut(tween(400))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scaleFactor)
                    .padding(horizontal = MeshTheme.spacing.large)
            ) {
                // Central Seed Star User Profile Avatar
                Box(
                    modifier = Modifier.size(105.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer starlight halo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AnimationConstants.StarlightWhiteGlow.copy(alpha = 0.5f),
                                        AnimationConstants.StarlightSilverGlow.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Starlight border ring
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(AnimationConstants.StarlightWhite, AnimationConstants.StarlightSilver)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatarImage(
                            avatarUri = avatarUri,
                            displayName = displayName,
                            size = 84.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

                // Glassmorphic Welcome Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AnimationConstants.DimBackground,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    AnimationConstants.StarlightSilver.copy(alpha = 0.3f),
                                    AnimationConstants.StarlightWhite.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "Welcome,",
                            style = MaterialTheme.typography.titleMedium,
                            color = AnimationConstants.StarlightSilver,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = displayName.ifBlank { "Explorer" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = AnimationConstants.StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "You are now part of Mesh Link",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AnimationConstants.SoftWhiteTransparent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
