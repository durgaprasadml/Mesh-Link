package com.meshlink.ui.components.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Redesigned Material 3 SOS Emergency Alert Card component.
 * Displays high-priority emergency alerts with visual hierarchy,
 * animated breathing icons, location details, battery level, connection state,
 * dark mode support, and TalkBack accessibility semantics.
 */
@Composable
fun SosEmergencyCard(
    message: Message,
    onLocationClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Color palette carefully crafted for high contrast, emergency theme, & Material 3 compliance
    val headerGradient = Brush.horizontalGradient(
        colors = if (isDark) {
            listOf(Color(0xFFD32F2F), Color(0xFF8E0000))
        } else {
            listOf(Color(0xFFD32F2F), Color(0xFFB71C1C))
        }
    )

    val cardBgColor = if (isDark) Color(0xFF2C1616) else Color(0xFFFFF5F5)
    val bodyBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val cardBorderColor = if (isDark) Color(0xFFEF5350).copy(alpha = 0.35f) else Color(0xFFD32F2F).copy(alpha = 0.25f)

    val primaryTextColor = if (isDark) Color(0xFFE6E1E5) else MaterialTheme.colorScheme.onSurface
    val labelTextColor = if (isDark) Color(0xFFCAC4D0) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTintColor = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
    val dividerColor = if (isDark) Color(0xFF362727) else Color(0xFFF0E0E0)
    val footerBgColor = if (isDark) Color(0xFF8E0000) else Color(0xFFB71C1C)

    // Breathing pulse animation for emergency icon
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulseAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    val hasLocation = message.latitude != null && message.longitude != null
    val latStr = message.latitude?.let { String.format(Locale.US, "%.6f", it) } ?: "Unavailable"
    val lngStr = message.longitude?.let { String.format(Locale.US, "%.6f", it) } ?: "Unavailable"

    val batteryText = if (message.batteryPercent != null && message.batteryPercent >= 0) {
        "${message.batteryPercent}%"
    } else {
        "Unknown"
    }

    val connectionText = when (message.status) {
        DeliveryStatus.RELAYED -> "Mesh Relayed"
        else -> "Direct Mesh"
    }

    val talkBackDescription = remember(message, formattedTime, latStr, lngStr, batteryText, connectionText) {
        buildString {
            append("Emergency SOS Alert. ")
            append("Priority: High Priority Emergency. ")
            if (hasLocation) {
                append("Current Location: Latitude $latStr, Longitude $lngStr. ")
            } else {
                append("Current Location: Unavailable. ")
            }
            append("Battery level: $batteryText. ")
            append("Connection type: $connectionText. ")
            append("Sent at $formattedTime. ")
            append("Immediate Assistance Required.")
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = talkBackDescription
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // HEADER SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Emergency Warning Icon",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "SOS EMERGENCY",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Emergency Alert",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // EMERGENCY BADGE BELOW TITLE
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5252))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HIGH PRIORITY",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // BODY SECTION
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = bodyBgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. LOCATION BLOCK
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (hasLocation) {
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(
                                            role = Role.Button,
                                            onClickLabel = "Open location in maps"
                                        ) {
                                            onLocationClick(message.latitude!!, message.longitude!!)
                                        }
                                } else Modifier
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = iconTintColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current Location",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 26.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Latitude",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelTextColor
                                )
                                Text(
                                    text = latStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryTextColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Longitude",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelTextColor
                                )
                                Text(
                                    text = lngStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryTextColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    // 2. METADATA ROW (BATTERY, CONNECTION, SENT TIME)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Battery
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.BatteryFull,
                                    contentDescription = null,
                                    tint = iconTintColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Battery",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = batteryText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor,
                                modifier = Modifier.padding(start = 20.dp)
                            )
                        }

                        // Connection
                        Column(modifier = Modifier.weight(1.1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CellTower,
                                    contentDescription = null,
                                    tint = iconTintColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Connection",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = connectionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor,
                                modifier = Modifier.padding(start = 20.dp)
                            )
                        }

                        // Sent Time
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = iconTintColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                        }
                    }
                }
            }

            // FOOTER SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(footerBgColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PriorityHigh,
                        contentDescription = "Warning icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Immediate Assistance Required",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "SOS Card Light", showBackground = true)
@Composable
private fun SosEmergencyCardLightPreview() {
    com.meshlink.ui.designsystem.theme.MeshTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SosEmergencyCard(
                message = Message(
                    messageId = "msg_1",
                    chatId = "chat_1",
                    text = "SOS",
                    senderId = "user_1",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = false,
                    status = DeliveryStatus.DELIVERED,
                    messageType = com.meshlink.domain.model.MessageType.SOS,
                    latitude = 12.794620,
                    longitude = 75.174420,
                    batteryPercent = 62
                ),
                onLocationClick = { _, _ -> }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "SOS Card Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SosEmergencyCardDarkPreview() {
    com.meshlink.ui.designsystem.theme.MeshTheme(themeMode = "DARK") {
        Box(modifier = Modifier.padding(16.dp)) {
            SosEmergencyCard(
                message = Message(
                    messageId = "msg_2",
                    chatId = "chat_1",
                    text = "SOS",
                    senderId = "user_2",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    status = DeliveryStatus.RELAYED,
                    messageType = com.meshlink.domain.model.MessageType.SOS,
                    latitude = 12.794620,
                    longitude = 75.174420,
                    batteryPercent = 62
                ),
                onLocationClick = { _, _ -> }
            )
        }
    }
}

