package com.meshlink.ui.designsystem.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.haptics.rememberMeshHaptics

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

val expandedMeshNavItems = listOf(
    MeshNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    MeshNavItem("nearby", "Nearby", Icons.Filled.Wifi, Icons.Outlined.Wifi),
    MeshNavItem("broadcast", "Broadcast", Icons.Filled.Wifi, Icons.Outlined.Wifi),
    MeshNavItem("diagnostics", "Analytics", Icons.Filled.Home, Icons.Outlined.Home),
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp)
        ) {
            val containerWidth = maxWidth
            val itemCount = items.size.coerceAtLeast(1)
            val itemWidth = containerWidth / itemCount

            // Sliding Indicator Pill
            if (selectedIndex >= 0) {
                val activeItem = items[selectedIndex]
                val targetOffsetX = itemWidth * selectedIndex + (itemWidth - 64.dp) / 2

                val animatedIndicatorX by animateDpAsState(
                    targetValue = targetOffsetX,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "IndicatorX"
                )

                val indicatorColor by animateColorAsState(
                    targetValue = if (activeItem.isEmergency) {
                        androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                    } else {
                        androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                    },
                    label = "IndicatorColor"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedIndicatorX, y = 6.dp)
                        .width(64.dp)
                        .height(32.dp)
                        .clip(MeshTheme.shapes.pill)
                        .background(indicatorColor)
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

                    val contentColor by animateColorAsState(
                        targetValue = when {
                            !item.isEnabled -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            isSelected && item.isEmergency -> androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                            isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                            item.isEmergency -> androidx.compose.material3.MaterialTheme.colorScheme.error
                            else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "ContentColor_${item.route}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = item.isEnabled
                            ) {
                                if (item.isEmergency) haptics.sosTrigger() else haptics.selection()
                                onNavigate(item.route)
                            }
                            .semantics {
                                role = Role.Tab
                                contentDescription = "${item.label} tab"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )

                                if (item.badgeCount > 0 || item.showBadgeDot) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 6.dp, y = (-2).dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (item.isEmergency) Color(0xFFD32F2F) else Color(0xFF004D40)
                                            )
                                            .padding(
                                                horizontal = if (item.badgeCount > 0) 4.dp else 3.dp,
                                                vertical = if (item.badgeCount > 0) 1.dp else 3.dp
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
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Permanent Navigation Drawer for Expanded Viewports (Tablets / Foldables / Desktops >840dp).
 */
@Composable
fun MeshPermanentNavigationDrawer(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<MeshNavItem> = expandedMeshNavItems
) {
    val haptics = rememberMeshHaptics()

    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp, horizontal = 12.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Mesh-Link",
                    style = MeshTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Decentralized Mesh Network",
                    style = MeshTheme.typography.bodySmall,
                    color = MeshTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Items
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val interactionSource = remember { MutableInteractionSource() }

                    val containerColor by animateColorAsState(
                        targetValue = when {
                            isSelected && item.isEmergency -> Color(0xFFFFDADA)
                            isSelected -> MeshTheme.colors.primary.copy(alpha = 0.15f)
                            else -> Color.Transparent
                        },
                        label = "DrawerContainer_${item.route}"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = when {
                            !item.isEnabled -> MeshTheme.colors.textSecondary.copy(alpha = 0.4f)
                            isSelected && item.isEmergency -> Color(0xFFD32F2F)
                            isSelected -> MeshTheme.colors.primary
                            item.isEmergency -> Color(0xFFD32F2F)
                            else -> MeshTheme.colors.textSecondary
                        },
                        label = "DrawerContent_${item.route}"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(MeshTheme.shapes.pill)
                            .background(containerColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = item.isEnabled
                            ) {
                                if (item.isEmergency) haptics.sosTrigger() else haptics.selection()
                                onNavigate(item.route)
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = item.label,
                            style = MeshTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            modifier = Modifier.weight(1f)
                        )

                        if (item.badgeCount > 0 || item.showBadgeDot) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isEmergency) Color(0xFFD32F2F) else MeshTheme.colors.primary
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}




