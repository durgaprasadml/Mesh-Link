package com.meshlink.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable Adaptive Navigation Container for Mesh-Link Phase 15.
 * Dynamically switches between Bottom Navigation Bar, Navigation Rail, and
 * Permanent Navigation Drawer based on Window Size Classes without altering navigation contracts.
 */

data class AdaptiveNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int? = null
)

@Composable
fun MeshAdaptiveNavigationContainer(
    items: List<AdaptiveNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    windowInfo: AdaptiveWindowInfo = rememberAdaptiveWindowInfo(),
    content: @Composable () -> Unit
) {
    when {
        windowInfo.isExpanded -> {
            // Permanent Navigation Drawer for Tablets & Desktop window sizes
            PermanentNavigationDrawer(
                drawerContent = {
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        color = MeshTheme.colors.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Mesh-Link",
                                style = MeshTheme.typography.headlineSmall,
                                color = MeshTheme.colors.primary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            items.forEach { item ->
                                NavigationRailItem(
                                    selected = item.id == selectedItemId,
                                    onClick = { onItemSelected(item.id) },
                                    icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                    label = { Text(text = item.label) },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                modifier = modifier,
                content = content
            )
        }
        windowInfo.isMedium || windowInfo.isLandscape -> {
            // Navigation Rail for Landscape and Large Phones
            Row(modifier = modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MeshTheme.colors.surfaceVariant,
                    header = {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                ) {
                    items.forEach { item ->
                        NavigationRailItem(
                            selected = item.id == selectedItemId,
                            onClick = { onItemSelected(item.id) },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(text = item.label) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
        else -> {
            // Bottom Navigation for Compact Portrait Phones
            Column(modifier = modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content()
                }

                NavigationBar(
                    containerColor = MeshTheme.colors.surfaceVariant
                ) {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = item.id == selectedItemId,
                            onClick = { onItemSelected(item.id) },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                            label = { Text(text = item.label) }
                        )
                    }
                }
            }
        }
    }
}
