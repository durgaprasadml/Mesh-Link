package com.meshlink.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress

@Composable
fun DashboardCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(MeshSpacing.DashboardCardWidth),
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTintColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    badgeCount: Int = 0,
    /** When true, shows a subtle glow/active border — use when the feature has live data. */
    isActive: Boolean = false
) {
    // Animated glow border alpha — pulses to full when active, fades out when not
    val activeBorderAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.70f else 0.20f,
        animationSpec = tween(400),
        label = "active_border_alpha"
    )

    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .scaleOnPress(targetScale = 0.96f)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        iconContainerColor.copy(alpha = activeBorderAlpha),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = activeBorderAlpha * 0.5f)
                    )
                ),
                shape = RoundedCornerShape(MeshSpacing.CardCornerRadius)
            ),
        shape = RoundedCornerShape(MeshSpacing.CardCornerRadius),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isActive) 4.dp else 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshSpacing.CardInternalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // ── Icon Container: square-rounded with gradient background ──
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                Modifier.border(
                                    width = 0.dp,
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Gradient background for icon
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            color = iconContainerColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = iconTintColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // ── Badge ─────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = badgeCount > 0,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
