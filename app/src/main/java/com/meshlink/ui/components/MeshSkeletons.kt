package com.meshlink.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.accessibility.rememberMeshReducedMotion
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable Animated Shimmer Skeleton screens for Mesh-Link 2026.
 * Provides skeletons for Home, Chat, Nearby, Broadcast, Media, Analytics, Settings, Profile, Security, Notifications, Sync.
 * Respects Android Reduced Motion settings.
 */

@Composable
fun modifierShimmer(
    baseColor: Color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.6f),
    highlightColor: Color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.2f)
): Modifier {
    val isReducedMotion = rememberMeshReducedMotion()
    if (isReducedMotion) {
        return Modifier.background(baseColor)
    }

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_float"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    return Modifier.background(brush)
}

@Composable
fun MeshSkeletonItem(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    widthFraction: Float = 1.0f,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .then(modifierShimmer())
    )
}

@Composable
fun MeshHomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(modifierShimmer())
                )
            }
        }
        repeat(6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(modifierShimmer())
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MeshSkeletonItem(height = 16.dp, widthFraction = 0.6f)
                    Spacer(modifier = Modifier.height(8.dp))
                    MeshSkeletonItem(height = 12.dp, widthFraction = 0.9f)
                }
            }
        }
    }
}

@Composable
fun MeshChatSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(5) { index ->
            val isRight = index % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isRight) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .width(if (isRight) 220.dp else 260.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(modifierShimmer())
                )
            }
        }
    }
}

@Composable
fun MeshNearbySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .then(modifierShimmer())
        )
        MeshSkeletonItem(height = 20.dp, widthFraction = 0.5f)
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(modifierShimmer())
            )
        }
    }
}

@Composable
fun MeshBroadcastSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(modifierShimmer())
            )
        }
    }
}

@Composable
fun MeshMediaSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(modifierShimmer())
            )
        }
    }
}

@Composable
fun MeshAnalyticsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .then(modifierShimmer())
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(modifierShimmer())
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(modifierShimmer())
            )
        }
    }
}

@Composable
fun MeshSettingsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(modifierShimmer())
            )
        }
    }
}

@Composable
fun MeshProfileSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .then(modifierShimmer())
        )
        MeshSkeletonItem(height = 24.dp, widthFraction = 0.4f)
        MeshSkeletonItem(height = 14.dp, widthFraction = 0.6f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(modifierShimmer())
        )
    }
}

@Composable
fun MeshSecuritySkeleton(modifier: Modifier = Modifier) {
    MeshAnalyticsSkeleton(modifier = modifier)
}

@Composable
fun MeshNotificationsSkeleton(modifier: Modifier = Modifier) {
    MeshSettingsSkeleton(modifier = modifier)
}

@Composable
fun MeshSyncSkeleton(modifier: Modifier = Modifier) {
    MeshBroadcastSkeleton(modifier = modifier)
}
