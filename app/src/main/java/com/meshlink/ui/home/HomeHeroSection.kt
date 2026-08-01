package com.meshlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.UserAvatar
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.util.Calendar

/**
 * Returns a time-of-day greeting string based on local device time.
 */
private fun getTactileGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
fun HomeHeroSection(
    userIdentity: UserIdentity?,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = userIdentity?.displayName?.ifBlank { "Command Node" } ?: "Command Node"
    val meshId = userIdentity?.userId?.take(8)?.uppercase() ?: "OFFLINE"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // Top row: Greeting + Security Badge + User Avatar Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Time of day greeting + mesh identity tagline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getTactileGreeting().uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MeshTheme.colors.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MeshTheme.colors.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NODE #$meshId",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MeshTheme.colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Large Display Name
                Text(
                    text = displayName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = MeshTheme.colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Profile Avatar with Glowing Border
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MeshTheme.colors.primary,
                                MeshTheme.colors.primary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .tactileClick(onClick = onNavigateToSettings)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = "Open Profile Settings"
                    }
            ) {
                UserAvatar(
                    identity = userIdentity,
                    size = 54.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security & Telemetry Status Pill Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // E2E Encryption Badge
            Surface(
                shape = MeshTheme.shapes.pill,
                color = MeshTheme.colors.primary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MeshTheme.colors.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Signal E2E Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MeshTheme.colors.primary
                    )
                }
            }

            // Network Security Readiness
            Surface(
                shape = MeshTheme.shapes.pill,
                color = MeshTheme.colors.surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MeshTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MeshTheme.colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Zero-Cloud",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MeshTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Battery telemetry pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = "Battery optimal",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "98%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.textSecondary
                )
            }
        }
    }
}
