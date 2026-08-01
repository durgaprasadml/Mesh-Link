package com.meshlink.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Premium empty state component for the "Signal" design language.
 *
 * Features:
 * - Three-ring concentric halo around the icon (outer: 96dp, middle: 76dp, inner: 60dp)
 * - Breathing animation on the outer halo
 * - Gradient icon background
 * - Staggered text entrance (title → description → button)
 * - Optional CTA button with pill shape
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    primaryButtonText: String? = null,
    onPrimaryButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")

    // Outer halo breathing scale
    val outerHaloScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_halo_scale"
    )

    // Inner icon subtle float
    val iconFloat by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_float"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MeshTheme.spacing.giant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Icon with three-ring halo ──────────────────────────────────────────
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outermost ring — breathing halo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(outerHaloScale)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.06f))
            )
            // Middle ring
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.10f))
            )
            // Inner icon container — gradient fill
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.30f),
                                primaryContainer.copy(alpha = 0.60f)
                            )
                        )
                    )
                    .graphicsLayer { translationY = iconFloat },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(MeshSpacing.SectionGap))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MeshSpacing.SM))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MeshSpacing.XL)
        )

        if (primaryButtonText != null && onPrimaryButtonClick != null) {
            Spacer(modifier = Modifier.height(MeshSpacing.XXL))
            Button(
                onClick = onPrimaryButtonClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = MeshSpacing.XXL, vertical = MeshSpacing.MD),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = primaryButtonText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
