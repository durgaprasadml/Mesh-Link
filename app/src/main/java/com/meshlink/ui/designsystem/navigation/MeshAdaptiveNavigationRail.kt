package com.meshlink.ui.designsystem.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.motion.MeshSignalPulse
import com.meshlink.ui.designsystem.motion.meshGlow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.haptics.rememberMeshHaptics

/**
 * Adaptive Navigation Rail for Mesh-Link 2026.
 * Designed for tablets, foldables, and wide landscape viewports.
 * Features vertical elastic spring indicator, state-aware icon morphing, and tactile feedback.
 */

@Composable
fun MeshAdaptiveNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<MeshNavItem> = defaultMeshNavItems,
    header: @Composable (() -> Unit)? = null
) {
    val haptics = rememberMeshHaptics()
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }

    Surface(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MeshTheme.colors.glassBorder,
                        MeshTheme.colors.border.copy(alpha = 0.2f)
                    )
                ),
                shape = RectangleShape
            ),
        color = MeshTheme.colors.glassSurface,
        tonalElevation = MeshTheme.elevation.navigation
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (header != null) {
                    header()
                    Spacer(modifier = Modifier.height(28.dp))
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEachIndexed { index, item ->
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
                            label = "RailIconScale_${item.route}"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = when {
                                !item.isEnabled -> MeshTheme.colors.textSecondary.copy(alpha = 0.4f)
                                isSelected && item.isEmergency -> MeshTheme.colors.emergency
                                isSelected -> MeshTheme.colors.primary
                                item.isEmergency -> MeshTheme.colors.emergency.copy(alpha = 0.8f)
                                else -> MeshTheme.colors.textSecondary
                            },
                            label = "RailContentColor_${item.route}"
                        )

                        val containerColor by animateColorAsState(
                            targetValue = when {
                                isSelected && item.isEmergency -> MeshTheme.colors.emergency.copy(alpha = 0.22f)
                                isSelected -> MeshTheme.colors.primary.copy(alpha = 0.18f)
                                else -> Color.Transparent
                            },
                            label = "RailContainerColor_${item.route}"
                        )

                        val itemContent = @Composable {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (item.isEnabled) 1.0f else 0.45f)
                                    .clip(MeshTheme.shapes.medium)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        enabled = item.isEnabled
                                    ) {
                                        if (item.isEmergency) haptics.sosTrigger() else haptics.selection()
                                        onNavigate(item.route)
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(containerColor)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.meshGlow(
                                                        color = if (item.isEmergency) MeshTheme.colors.emergency else MeshTheme.colors.primary,
                                                        radius = 10.dp,
                                                        alpha = 0.3f
                                                    )
                                                } else Modifier
                                            )
                                            .padding(10.dp),
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

                                    if (item.badgeCount > 0 || item.showBadgeDot) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 4.dp, y = (-2).dp)
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
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

                        if (index < items.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

