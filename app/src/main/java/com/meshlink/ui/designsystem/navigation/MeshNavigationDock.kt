package com.meshlink.ui.designsystem.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.motion.MeshSignalPulse
import com.meshlink.ui.designsystem.motion.meshGlow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.haptics.rememberMeshHaptics

/**
 * Floating Navigation Dock for Mesh-Link 2026.
 * Completely redesigned glass navigation dock featuring an elastic spring active indicator,
 * icon morphing, dynamic label transitions, context-aware colors, emergency SOS highlight,
 * pressed/disabled states, and notification badge overlays.
 */

data class MeshNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isEmergency: Boolean = false,
    val badgeCount: Int = 0,
    val showBadgeDot: Boolean = false,
    val isEnabled: Boolean = true
)

val defaultMeshNavItems = listOf(
    MeshNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    MeshNavItem("nearby", "Nearby", Icons.Filled.Wifi, Icons.Outlined.Wifi),
    MeshNavItem("sos", "SOS", Icons.Filled.Warning, Icons.Outlined.Warning, isEmergency = true),
    MeshNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun MeshNavigationDock(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<MeshNavItem> = defaultMeshNavItems
) {
    val haptics = rememberMeshHaptics()
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(MeshTheme.shapes.jumbo)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MeshTheme.colors.glassBorder,
                            MeshTheme.colors.border.copy(alpha = 0.25f)
                        )
                    ),
                    shape = MeshTheme.shapes.jumbo
                ),
            color = MeshTheme.colors.glassSurface,
            tonalElevation = MeshTheme.elevation.navigation,
            shadowElevation = 12.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val containerWidth = maxWidth
                val itemCount = items.size.coerceAtLeast(1)
                val itemWidth = containerWidth / itemCount

                // Sliding Elastic Active Indicator Pill
                if (selectedIndex >= 0) {
                    val activeItem = items[selectedIndex]
                    val targetOffsetX = itemWidth * selectedIndex + (itemWidth - 64.dp) / 2

                    val animatedIndicatorX by animateDpAsState(
                        targetValue = targetOffsetX,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "ElasticIndicatorX"
                    )

                    val indicatorColor by animateColorAsState(
                        targetValue = if (activeItem.isEmergency) {
                            MeshTheme.colors.emergency.copy(alpha = 0.22f)
                        } else {
                            MeshTheme.colors.primary.copy(alpha = 0.18f)
                        },
                        label = "ElasticIndicatorColor"
                    )

                    val indicatorGlowColor by animateColorAsState(
                        targetValue = if (activeItem.isEmergency) {
                            MeshTheme.colors.emergency
                        } else {
                            MeshTheme.colors.primary
                        },
                        label = "ElasticIndicatorGlow"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = animatedIndicatorX, y = 2.dp)
                            .width(64.dp)
                            .height(56.dp)
                            .clip(MeshTheme.shapes.pill)
                            .meshGlow(color = indicatorGlowColor, radius = 12.dp, alpha = 0.25f)
                            .background(indicatorColor)
                            .border(
                                width = 1.dp,
                                color = indicatorGlowColor.copy(alpha = 0.35f),
                                shape = MeshTheme.shapes.pill
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val iconScale by animateFloatAsState(
                            targetValue = when {
                                !item.isEnabled -> 0.9f
                                isPressed -> 0.90f
                                isSelected -> 1.15f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "IconScale_${item.route}"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = when {
                                !item.isEnabled -> MeshTheme.colors.textSecondary.copy(alpha = 0.4f)
                                isSelected && item.isEmergency -> MeshTheme.colors.emergency
                                isSelected -> MeshTheme.colors.primary
                                item.isEmergency -> MeshTheme.colors.emergency.copy(alpha = 0.8f)
                                else -> MeshTheme.colors.textSecondary
                            },
                            label = "ContentColor_${item.route}"
                        )

                        val itemContent = @Composable {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .alpha(if (item.isEnabled) 1.0f else 0.45f)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        enabled = item.isEnabled
                                    ) {
                                        if (item.isEmergency) haptics.sosTrigger() else haptics.selection()
                                        onNavigate(item.route)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                                tint = contentColor,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .scale(iconScale)
                                            )
                                        }

                                        // Notification Badge overlay
                                        if (item.badgeCount > 0 || item.showBadgeDot) {
                                            val badgeScale by animateFloatAsState(
                                                targetValue = 1.0f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "BadgeScale_${item.route}"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = 6.dp, y = (-2).dp)
                                                    .scale(badgeScale)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (item.isEmergency) MeshTheme.colors.emergency else MeshTheme.colors.primary
                                                    )
                                                    .padding(
                                                        horizontal = if (item.badgeCount > 0) 5.dp else 4.dp,
                                                        vertical = if (item.badgeCount > 0) 2.dp else 4.dp
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (item.badgeCount > 0) {
                                                    Text(
                                                        text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        lineHeight = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val labelScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.05f else 1.0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "LabelScale_${item.route}"
                                    )

                                    Text(
                                        text = item.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor,
                                        letterSpacing = 0.3.sp,
                                        modifier = Modifier.scale(labelScale)
                                    )
                                }
                            }
                        }

                        if (item.isEmergency && isSelected) {
                            MeshSignalPulse(
                                active = true,
                                color = MeshTheme.colors.emergency
                            ) {
                                itemContent()
                            }
                        } else {
                            itemContent()
                        }
                    }
                }
            }
        }
    }
}

