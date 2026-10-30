package com.meshlink.ui.components.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.util.MeshIdNormalizer
import java.io.File
import java.util.Locale

/**
 * Resolves the emergency message to display on the SOS card.
 * If the sender wrote a custom message, returns it prominently.
 * If the message text only contains standard boilerplate coordinates/status or is blank/SOS,
 * returns a clean message such as "Emergency assistance requested".
 */
fun resolveEmergencyMessage(rawText: String): String {
    val trimmed = rawText.trim()
    if (trimmed.isBlank() || trimmed.equals("SOS", ignoreCase = true)) {
        return "Emergency assistance requested"
    }

    if (trimmed.startsWith("🚨 SOS EMERGENCY") || trimmed.contains("— Lat:") || trimmed.contains("— Direct Range Alert")) {
        val segments = trimmed.split("—").map { it.trim() }
        val customSegments = segments.drop(1).filter { seg ->
            !seg.startsWith("Lat:", ignoreCase = true) &&
            !seg.startsWith("Battery:", ignoreCase = true) &&
            !seg.contains("Direct Range Alert", ignoreCase = true)
        }
        if (customSegments.isNotEmpty()) {
            val custom = customSegments.joinToString(" — ").trim()
            if (custom.isNotBlank()) return custom
        }
        if (trimmed.contains("Awaiting full details via Wi-Fi Direct", ignoreCase = true)) {
            return "Emergency assistance requested (Awaiting full details via Wi-Fi Direct)"
        }
        return "Emergency assistance requested"
    }

    return trimmed
}

/**
 * Resolves the sender display name from passed name, local status, or message text.
 */
fun resolveSosSenderDisplayName(
    message: Message,
    senderDisplayName: String?
): String {
    if (message.isFromMe) {
        return "You"
    }
    if (!senderDisplayName.isNullOrBlank() && senderDisplayName != "Unknown") {
        return senderDisplayName.removePrefix("🚨 ").trim()
    }
    val fromRegex = Regex("""from\s+([^(—]+?)(?:\s*\(Unverified Sender\))?(?:\s*—|$)""")
    val match = fromRegex.find(message.text)
    val extracted = match?.groupValues?.get(1)?.trim()
    return when {
        !extracted.isNullOrBlank() -> extracted.removePrefix("🚨 ").trim()
        message.senderId.isNotBlank() -> MeshIdNormalizer.canonicalize(message.senderId)
        else -> "Unknown Sender"
    }
}

/**
 * Resolves whether the sender's identity has succeeded cryptographic verification.
 * Does not falsely show an identity as verified when cryptographic verification has not succeeded.
 */
fun resolveSosSenderVerification(
    message: Message,
    isSenderVerified: Boolean?
): Boolean {
    if (message.isFromMe) {
        return true
    }
    if (isSenderVerified != null) {
        return isSenderVerified
    }
    if (message.text.contains("Unverified Sender", ignoreCase = true)) {
        return false
    }
    if (message.text.contains("Verified Sender", ignoreCase = true)) {
        return true
    }
    // Default to unverified when cryptographic verification is not explicitly confirmed
    return false
}

/**
 * Fully visible, non-collapsible Material 3 SOS Emergency Alert Card component.
 * Immediately displays all available emergency information (header, custom message,
 * verified sender badge, location, battery, front & rear camera photographs, and timestamp)
 * without requiring the user to expand or hide details.
 */
@Composable
fun SosEmergencyCard(
    message: Message,
    onLocationClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    senderDisplayName: String? = null,
    isSenderVerified: Boolean? = null,
    onImageClick: (String) -> Unit = {}
) {
    val isDark = MeshTheme.isDark

    // Emergency Color Palette (High contrast, Material 3 compliant)
    val headerGradient = Brush.horizontalGradient(
        colors = if (isDark) {
            listOf(Color(0xFFC62828), Color(0xFF8E0000))
        } else {
            listOf(Color(0xFFD32F2F), Color(0xFFB71C1C))
        }
    )

    val cardBgColor = if (isDark) Color(0xFF221414) else Color(0xFFFFF9F9)
    val bodyBgColor = if (isDark) Color(0xFF181010) else Color.White
    val sectionBgColor = if (isDark) Color(0xFF261818) else Color(0xFFFAF0F0)
    val cardBorderColor = if (isDark) Color(0xFFEF5350).copy(alpha = 0.4f) else Color(0xFFD32F2F).copy(alpha = 0.3f)
    val sectionBorderColor = if (isDark) Color(0xFF422424) else Color(0xFFF2DEDE)

    val primaryTextColor = if (isDark) Color(0xFFF5EEEE) else MaterialTheme.colorScheme.onSurface
    val labelTextColor = if (isDark) Color(0xFFC4B6B6) else MaterialTheme.colorScheme.onSurfaceVariant
    val emergencyRed = if (isDark) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
    val footerBgColor = if (isDark) Color(0xFF8E0000) else Color(0xFFB71C1C)

    // Breathing pulse animation for emergency header icon
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

    // Resolved Metadata
    val resolvedDisplayName = remember(senderDisplayName, message.isFromMe, message.text, message.senderId) {
        resolveSosSenderDisplayName(message, senderDisplayName)
    }

    val resolvedIsVerified = remember(isSenderVerified, message.isFromMe, message.text) {
        resolveSosSenderVerification(message, isSenderVerified)
    }

    val resolvedEmergencyMessage = remember(message.text) {
        resolveEmergencyMessage(message.text)
    }

    val formattedTime = remember(message.timestamp) {
        com.meshlink.ui.util.DateTimeUtils.formatTime12Hour(message.timestamp)
    }

    val hasLocation = message.latitude != null && message.longitude != null
    val latStr = message.latitude?.let { String.format(Locale.US, "%.6f", it) }
    val lngStr = message.longitude?.let { String.format(Locale.US, "%.6f", it) }

    val hasBattery = message.batteryPercent != null && message.batteryPercent >= 0
    val batteryText = if (hasBattery) "${message.batteryPercent}%" else "Battery information unavailable"

    val frontPath = remember(message.mediaPath) {
        val path = message.mediaPath ?: return@remember null
        if (path.startsWith("{")) {
            try {
                val obj = org.json.JSONObject(path)
                if (obj.has("front")) obj.getString("front").takeIf { it.isNotBlank() } else null
            } catch (_: Exception) { null }
        } else {
            path.takeIf { it.isNotBlank() }
        }
    }
    val rearPath = remember(message.mediaPath) {
        val path = message.mediaPath ?: return@remember null
        if (path.startsWith("{")) {
            try {
                val obj = org.json.JSONObject(path)
                if (obj.has("rear")) obj.getString("rear").takeIf { it.isNotBlank() } else null
            } catch (_: Exception) { null }
        } else {
            null
        }
    }

    val isFrontAvailable = remember(frontPath) {
        frontPath != null && File(frontPath).exists()
    }
    val isRearAvailable = remember(rearPath) {
        rearPath != null && File(rearPath).exists()
    }

    val talkBackDescription = remember(
        resolvedDisplayName,
        resolvedIsVerified,
        resolvedEmergencyMessage,
        hasLocation,
        latStr,
        lngStr,
        hasBattery,
        batteryText,
        isFrontAvailable,
        isRearAvailable,
        formattedTime
    ) {
        buildString {
            append("SOS Emergency Alert. ")
            append("Priority: High Priority Emergency. ")
            append("Sender: $resolvedDisplayName, ${if (resolvedIsVerified) "Verified Sender" else "Unverified Sender"}. ")
            append("Emergency Message: $resolvedEmergencyMessage. ")
            if (hasLocation) {
                append("Location: Latitude $latStr, Longitude $lngStr. ")
            } else {
                append("Location: Location unavailable. ")
            }
            append("Battery: $batteryText. ")
            append("Front camera image: ${if (isFrontAvailable) "Available" else "Unavailable"}. ")
            append("Rear camera image: ${if (isRearAvailable) "Available" else "Unavailable"}. ")
            append("Timestamp: $formattedTime.")
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = talkBackDescription
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. EMERGENCY HEADER SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Emergency Warning Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOS EMERGENCY",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sender: $resolvedDisplayName",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BADGES ROW: PRIORITY & VERIFICATION STATUS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // High Priority Badge
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }

                        // Cryptographic Sender Verification Badge
                        if (resolvedIsVerified) {
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verified Sender",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFE65100).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Unverified Sender",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. BODY SECTION (ALL DETAILS VISIBLE IMMEDIATELY)
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // A. EMERGENCY MESSAGE SECTION
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = sectionBgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, sectionBorderColor)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = emergencyRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EMERGENCY MESSAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = emergencyRed
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resolvedEmergencyMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                        }
                    }

                    // B. LOCATION INFORMATION SECTION
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = sectionBgColor,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, sectionBorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (hasLocation) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                                    contentDescription = null,
                                    tint = if (hasLocation) emergencyRed else labelTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CURRENT LOCATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasLocation) primaryTextColor else labelTextColor
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (hasLocation) {
                                    OutlinedButton(
                                        onClick = { onLocationClick(message.latitude!!, message.longitude!!) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, emergencyRed.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Map,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = emergencyRed
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Open Map",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = emergencyRed
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (hasLocation) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Latitude",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = labelTextColor
                                        )
                                        Text(
                                            text = latStr ?: "Unavailable",
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
                                            text = lngStr ?: "Unavailable",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryTextColor
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Location unavailable",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = labelTextColor
                                )
                            }
                        }
                    }

                    // C. BATTERY & TIMESTAMP SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Battery Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = sectionBgColor,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, sectionBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (hasBattery && message.batteryPercent!! < 20) Icons.Filled.BatteryAlert else Icons.Filled.BatteryStd,
                                        contentDescription = null,
                                        tint = if (hasBattery && message.batteryPercent!! < 20) emergencyRed else emergencyRed.copy(alpha = 0.85f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "BATTERY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = labelTextColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = batteryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasBattery && message.batteryPercent!! < 20) emergencyRed else primaryTextColor
                                )
                            }
                        }

                        // Timestamp Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = sectionBgColor,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, sectionBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = emergencyRed.copy(alpha = 0.85f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (message.isFromMe) "SENT TIME" else "RECEIVED TIME",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = labelTextColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryTextColor
                                )
                            }
                        }
                    }

                    // D. EMERGENCY PHOTOGRAPHS SECTION
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = emergencyRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EMERGENCY SCENE VISUALS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Front Camera Capture
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(135.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isFrontAvailable) Color.Black else sectionBgColor)
                                    .border(1.dp, sectionBorderColor, RoundedCornerShape(12.dp))
                                    .then(
                                        if (isFrontAvailable) {
                                            Modifier.clickable { onImageClick("${message.messageId}#front") }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFrontAvailable) {
                                    AsyncImage(
                                        model = File(frontPath!!),
                                        contentDescription = "Front camera emergency photograph. Tap to view full size.",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Fullscreen,
                                        contentDescription = "View full size",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth(),
                                        color = Color.Black.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "FRONT CAMERA",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NoPhotography,
                                            contentDescription = null,
                                            tint = labelTextColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "FRONT CAMERA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = labelTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Unavailable",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = labelTextColor.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Rear Camera Capture
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(135.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isRearAvailable) Color.Black else sectionBgColor)
                                    .border(1.dp, sectionBorderColor, RoundedCornerShape(12.dp))
                                    .then(
                                        if (isRearAvailable) {
                                            Modifier.clickable { onImageClick("${message.messageId}#rear") }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRearAvailable) {
                                    AsyncImage(
                                        model = File(rearPath!!),
                                        contentDescription = "Rear camera emergency photograph. Tap to view full size.",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Fullscreen,
                                        contentDescription = "View full size",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth(),
                                        color = Color.Black.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "REAR CAMERA",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NoPhotography,
                                            contentDescription = null,
                                            tint = labelTextColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "REAR CAMERA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = labelTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Unavailable",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = labelTextColor.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. FOOTER SECTION
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
                        contentDescription = "Urgent warning icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IMMEDIATE ASSISTANCE REQUIRED",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "SOS Card Light - Complete", showBackground = true)
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
                    timestamp = 1700000000000L,
                    isFromMe = false,
                    status = DeliveryStatus.DELIVERED,
                    messageType = MessageType.SOS,
                    latitude = 12.794620,
                    longitude = 75.174420,
                    batteryPercent = 62
                ),
                senderDisplayName = "Alice Smith",
                isSenderVerified = true,
                onLocationClick = { _, _ -> }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "SOS Card Dark - Complete", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SosEmergencyCardDarkPreview() {
    com.meshlink.ui.designsystem.theme.MeshTheme(themeMode = "DARK") {
        Box(modifier = Modifier.padding(16.dp)) {
            SosEmergencyCard(
                message = Message(
                    messageId = "msg_2",
                    chatId = "chat_1",
                    text = "🚨 SOS EMERGENCY from Bob — Injured on north trail, unable to walk — Lat: 12.794620, Lng: 75.174420 — Battery: 18%",
                    senderId = "user_2",
                    timestamp = 1700000000000L,
                    isFromMe = false,
                    status = DeliveryStatus.DELIVERED,
                    messageType = MessageType.SOS,
                    latitude = 12.794620,
                    longitude = 75.174420,
                    batteryPercent = 18
                ),
                senderDisplayName = "Bob Miller",
                isSenderVerified = false,
                onLocationClick = { _, _ -> }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "SOS Card - Missing Info (Unverified)", showBackground = true)
@Composable
private fun SosEmergencyCardMissingInfoPreview() {
    com.meshlink.ui.designsystem.theme.MeshTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SosEmergencyCard(
                message = Message(
                    messageId = "msg_3",
                    chatId = "chat_2",
                    text = "🚨 SOS EMERGENCY from Charlie (Unverified Sender) — Direct Range Alert Announced. Awaiting full details via Wi-Fi Direct.",
                    senderId = "user_3",
                    timestamp = 1700000000000L,
                    isFromMe = false,
                    status = DeliveryStatus.DELIVERED,
                    messageType = MessageType.SOS,
                    latitude = null,
                    longitude = null,
                    batteryPercent = null
                ),
                senderDisplayName = "Charlie",
                isSenderVerified = false,
                onLocationClick = { _, _ -> }
            )
        }
    }
}
