package com.meshlink.ui.production

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable Loading States for Mesh-Link Phase 15.
 * Provides Shimmer effect modifier, Skeleton Cards, Skeleton Lists, Progress Overlays, and Inline Loaders.
 */

fun Modifier.meshShimmer(
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAnim"
    )

    val shimmerColors = listOf(
        MeshTheme.colors.surfaceVariant.copy(alpha = 0.6f),
        MeshTheme.colors.surfaceVariant.copy(alpha = 0.2f),
        MeshTheme.colors.surfaceVariant.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200f, translateAnim.value - 200f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    this
        .clip(shape)
        .background(brush)
}

@Composable
fun MeshSkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .meshShimmer(RoundedCornerShape(16.dp)),
        color = Color.Transparent
    ) {}
}

@Composable
fun MeshSkeletonListItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .meshShimmer(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .meshShimmer()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .meshShimmer()
            )
        }
    }
}

@Composable
fun MeshSkeletonList(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount) {
            MeshSkeletonListItem()
        }
    }
}

@Composable
fun MeshLoadingOverlay(
    modifier: Modifier = Modifier,
    message: String? = "Loading..."
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MeshTheme.colors.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )

            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MeshTheme.typography.bodyMedium,
                    color = MeshTheme.colors.onBackground
                )
            }
        }
    }
}

@Composable
fun MeshInlineLoader(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Row(
        modifier = modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MeshTheme.colors.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp)
        )

        if (message != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MeshTheme.typography.bodySmall,
                color = MeshTheme.colors.onSurfaceVariant
            )
        }
    }
}
