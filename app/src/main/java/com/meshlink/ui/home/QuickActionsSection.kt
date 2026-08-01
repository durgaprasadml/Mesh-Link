package com.meshlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

private data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val containerColor: Color,
    val onClick: () -> Unit,
    val badgeCount: Int = 0
)

@Composable
fun QuickActionsSection(
    onNavigateToNearby: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToDiagnostics: (() -> Unit)?,
    onStartConversation: () -> Unit,
    modifier: Modifier = Modifier,
    nearbyCount: Int = 0
) {
    val items = listOf(
        QuickActionItem(
            title = "Scan Mesh",
            subtitle = if (nearbyCount > 0) "$nearbyCount discovered" else "Discover peers",
            icon = Icons.Default.Wifi,
            accentColor = MeshTheme.colors.primary,
            containerColor = MeshTheme.colors.primary.copy(alpha = 0.12f),
            onClick = onNavigateToNearby,
            badgeCount = nearbyCount
        ),
        QuickActionItem(
            title = "Broadcast",
            subtitle = "Mesh-wide post",
            icon = Icons.Default.Campaign,
            accentColor = Color(0xFF00E676),
            containerColor = Color(0xFF00E676).copy(alpha = 0.12f),
            onClick = onNavigateToBroadcast
        ),
        QuickActionItem(
            title = "Emergency",
            subtitle = "SOS Beacon",
            icon = Icons.Default.Warning,
            accentColor = Color(0xFFFF5252),
            containerColor = Color(0xFFFF5252).copy(alpha = 0.12f),
            onClick = onNavigateToSos
        ),
        QuickActionItem(
            title = "Diagnostics",
            subtitle = "Route Telemetry",
            icon = Icons.Default.NetworkCheck,
            accentColor = Color(0xFF00B0FF),
            containerColor = Color(0xFF00B0FF).copy(alpha = 0.12f),
            onClick = { onNavigateToDiagnostics?.invoke() ?: onNavigateToNearby() }
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(vertical = 8.dp)
    ) {
        // Section title header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MeshTheme.colors.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "TACTICAL QUICK ACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MeshTheme.colors.textPrimary,
                letterSpacing = 1.sp
            )
        }

        // 2x2 Tactical Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(item = items[0])
                QuickActionCard(item = items[2])
            }

            // Right Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(item = items[1])
                QuickActionCard(item = items[3])
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    item: QuickActionItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = item.accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .tactileClick(onClick = item.onClick, pressScale = 0.95f),
        color = MeshTheme.colors.surface,
        tonalElevation = MeshTheme.elevation.flat,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(item.containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MeshTheme.colors.textPrimary
                    )

                    if (item.badgeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(item.accentColor)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${item.badgeCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = MeshTheme.colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
