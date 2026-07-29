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
import androidx.compose.ui.draw.alpha
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
 * Glassmorphic Welcome Overlay for First-Time Users (Phase 7 User Joins).
 * Displays user's selected profile picture, "Welcome, <DisplayName>", and "Welcome to Mesh Link".
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
            visible = visible && progress in 0.55f..0.96f,
            enter = fadeIn(tween(400)) + scaleIn(tween(500)),
            exit = fadeOut(tween(300)) + scaleOut(tween(300))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scaleFactor)
                    .padding(horizontal = MeshTheme.spacing.large)
            ) {
                // Central User Profile Avatar with Glowing Ring Container
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glowing halo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AnimationConstants.ElectricBlue.copy(alpha = 0.5f),
                                        AnimationConstants.Cyan.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Avatar border ring
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(
                                    listOf(AnimationConstants.Cyan, AnimationConstants.ElectricBlue)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatarImage(
                            avatarUri = avatarUri,
                            displayName = displayName,
                            size = 90.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MeshTheme.spacing.large))

                // Glassmorphic Welcome Text Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = AnimationConstants.DimBackground,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    AnimationConstants.SoftWhite.copy(alpha = 0.25f),
                                    AnimationConstants.ElectricBlue.copy(alpha = 0.15f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "Welcome,",
                            style = MaterialTheme.typography.titleMedium,
                            color = AnimationConstants.Cyan,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = displayName.ifBlank { "Explorer" },
                            style = MaterialTheme.typography.headlineLarge,
                            color = AnimationConstants.SoftWhite,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Welcome to Mesh Link",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AnimationConstants.SoftWhiteTransparent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
