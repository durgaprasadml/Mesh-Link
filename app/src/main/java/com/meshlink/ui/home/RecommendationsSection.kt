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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun RecommendationsSection(
    nearbyDevices: List<BleDevice>,
    unreadCount: Int,
    onNavigateToNearby: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recommendations = remember(nearbyDevices, unreadCount) {
        val list = mutableListOf<MeshRecommendation>()

        if (nearbyDevices.isNotEmpty()) {
            val peerName = nearbyDevices.first().name
            list.add(
                MeshRecommendation(
                    id = "rec_nearby",
                    title = "Connect with $peerName",
                    description = "A new mesh peer is within range. Tap to start secure offline conversation.",
                    actionText = "Connect",
                    icon = Icons.Default.Wifi,
                    priority = MeshRecommendation.RecommendationPriority.HIGH,
                    onClick = onNavigateToNearby
                )
            )
        } else {
            list.add(
                MeshRecommendation(
                    id = "rec_scan",
                    title = "Scan for Mesh Peers",
                    description = "Enable BLE & Wi-Fi Direct scanning to discover nearby Mesh-Link nodes.",
                    actionText = "Scan Mesh",
                    icon = Icons.Default.Wifi,
                    priority = MeshRecommendation.RecommendationPriority.NORMAL,
                    onClick = onNavigateToNearby
                )
            )
        }

        if (unreadCount > 0) {
            list.add(
                MeshRecommendation(
                    id = "rec_unread",
                    title = "$unreadCount Unread Message${if (unreadCount > 1) "s" else ""}",
                    description = "Pending offline communications waiting in your encrypted inbox.",
                    actionText = "View Inbox",
                    icon = Icons.Default.ChatBubble,
                    priority = MeshRecommendation.RecommendationPriority.URGENT,
                    onClick = {}
                )
            )
        } else {
            list.add(
                MeshRecommendation(
                    id = "rec_broadcast",
                    title = "Mesh Broadcast Channel",
                    description = "Post an offline announcement to all connected mesh nodes in range.",
                    actionText = "Post Broadcast",
                    icon = Icons.Default.Campaign,
                    priority = MeshRecommendation.RecommendationPriority.SUGGESTION,
                    onClick = onNavigateToBroadcast
                )
            )
        }

        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFFFB300))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "SMART NETWORK SUGGESTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = MeshTheme.colors.textPrimary,
                letterSpacing = 1.sp
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            recommendations.forEach { recommendation ->
                RecommendationCard(recommendation = recommendation)
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: MeshRecommendation,
    modifier: Modifier = Modifier
) {
    val accentColor = when (recommendation.priority) {
        MeshRecommendation.RecommendationPriority.URGENT -> Color(0xFFFF5252)
        MeshRecommendation.RecommendationPriority.HIGH -> MeshTheme.colors.primary
        MeshRecommendation.RecommendationPriority.NORMAL -> Color(0xFF00B0FF)
        MeshRecommendation.RecommendationPriority.SUGGESTION -> Color(0xFFFFB300)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(0.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .tactileClick(onClick = recommendation.onClick, pressScale = 0.97f),
        color = MeshTheme.colors.surface,
        tonalElevation = MeshTheme.elevation.flat
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = recommendation.icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = recommendation.description,
                    fontSize = 12.sp,
                    color = MeshTheme.colors.textSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = recommendation.actionText,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
