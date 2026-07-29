package com.meshlink.ui.components.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Material 3 Location Message Card for Mesh Link chat.
 * 
 * Features:
 * - Premium Material 3 elevated card container with rounded corners (20dp)
 * - Clear header with location icon badge, title, and "Shared via Mesh Link" subtitle
 * - Aspect-ratio (16:9) Map Preview supporting cached bitmaps with canvas vector grid & animated pin fallback
 * - Structured information section presenting Latitude, Longitude, Battery, Connection, and Timestamp rows
 * - WCAG AA accessible "Open in Maps" action button (≥48dp touch target) with graceful clipboard fallback
 * - Dark mode, high contrast, dynamic color, and TalkBack accessibility semantics
 */
@Composable
fun LocationMessageCard(
    message: Message,
    onLocationClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val lat = message.latitude
    val lng = message.longitude
    val hasCoords = lat != null && lng != null

    val formattedLat = remember(lat) {
        lat?.let { String.format(Locale.US, "%.6f", it) } ?: "Unavailable"
    }

    val formattedLng = remember(lng) {
        lng?.let { String.format(Locale.US, "%.6f", it) } ?: "Unavailable"
    }

    val batteryText = remember(message.batteryPercent) {
        if (message.batteryPercent != null && message.batteryPercent >= 0) {
            "${message.batteryPercent}%"
        } else {
            "Unknown"
        }
    }

    val connectionText = remember(message.status) {
        when (message.status) {
            DeliveryStatus.RELAYED -> "Mesh Relayed"
            else -> "Direct Mesh"
        }
    }

    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    // Accessible screen reader narrative
    val talkBackDescription = remember(formattedLat, formattedLng, batteryText, connectionText, formattedTime) {
        "Shared Location message. Latitude: $formattedLat, Longitude: $formattedLng, Battery: $batteryText, Connection: $connectionText, Shared at: $formattedTime."
    }

    // Cached map thumbnail preview decoding
    val mapBitmap = remember(message.thumbnailBase64, message.mediaPath) {
        try {
            if (!message.thumbnailBase64.isNullOrEmpty()) {
                val bytes = Base64.decode(message.thumbnailBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } else if (!message.mediaPath.isNullOrEmpty() && File(message.mediaPath).exists()) {
                BitmapFactory.decodeFile(message.mediaPath)?.asImageBitmap()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // Card styling tokens
    val cardBgColor = when {
        message.isFromMe -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.25f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val cardBorderColor = if (isDark) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MeshTheme.spacing.large))
            .border(
                width = 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(MeshTheme.spacing.large)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = talkBackDescription
            },
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = MeshTheme.elevation.level1
        ),
        shape = RoundedCornerShape(MeshTheme.spacing.large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.mediumLarge)
        ) {
            // HEADER SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(MeshTheme.spacing.huge)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MeshTheme.spacing.extraLarge)
                    )
                }

                Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shared Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Location shared via Mesh Link",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // MAP PREVIEW SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(MeshTheme.spacing.medium))
                    .clickable(enabled = hasCoords) {
                        if (lat != null && lng != null) {
                            onLocationClick(lat, lng)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (mapBitmap != null) {
                    Image(
                        bitmap = mapBitmap,
                        contentDescription = "Map preview snapshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Styled Canvas Map Vector Graphic Fallback
                    StyledMapCanvasPreview(isDark = isDark, latText = formattedLat, lngText = formattedLng)
                }

                // Coordinate Chip Overlay
                if (hasCoords) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(MeshTheme.spacing.mediumSmall),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(MeshTheme.spacing.small)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$formattedLat°, $formattedLng°",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // INFORMATION SECTION (Structured Rows)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.35f else 0.5f),
                shape = RoundedCornerShape(MeshTheme.spacing.medium)
            ) {
                Column(
                    modifier = Modifier.padding(MeshTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.mediumSmall)
                ) {
                    LocationInfoRow(
                        icon = Icons.Default.Explore,
                        label = "Latitude",
                        value = formattedLat
                    )
                    LocationInfoRow(
                        icon = Icons.Default.Place,
                        label = "Longitude",
                        value = formattedLng
                    )
                    LocationInfoRow(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Battery",
                        value = batteryText
                    )
                    LocationInfoRow(
                        icon = Icons.Default.CellTower,
                        label = "Connection",
                        value = connectionText
                    )
                    LocationInfoRow(
                        icon = Icons.Default.Schedule,
                        label = "Shared",
                        value = formattedTime
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // PRIMARY ACTION BUTTON (Open in Maps / Clipboard Fallback)
            FilledTonalButton(
                onClick = {
                    if (lat != null && lng != null) {
                        onLocationClick(lat, lng)
                    } else {
                        // Fallback: Copy to Clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Location Coordinates", "$formattedLat, $formattedLng")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MeshTheme.spacing.giant) // ≥48dp Touch Target
                    .semantics {
                        role = Role.Button
                        contentDescription = "Open location in maps application"
                    },
                shape = RoundedCornerShape(MeshTheme.spacing.medium)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(MeshTheme.spacing.large)
                )
                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                Text(
                    text = "Open in Maps",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Styled Vector Map Canvas Preview composable.
 * Renders topographic grid lines, simulated road vectors, compass ring, and animated pulsing location pin.
 */
@Composable
private fun StyledMapCanvasPreview(
    isDark: Boolean,
    latText: String,
    lngText: String,
    modifier: Modifier = Modifier
) {
    // Pulse animation for location marker pin
    val infiniteTransition = rememberInfiniteTransition(label = "mapPinPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val bgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val gridColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val roadColor = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
    val primaryPinColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw grid lines
            val step = 32.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += step
            }

            // Draw stylized road path 1
            val roadPath1 = Path().apply {
                moveTo(0f, height * 0.7f)
                cubicTo(width * 0.3f, height * 0.8f, width * 0.6f, height * 0.3f, width, height * 0.4f)
            }
            drawPath(
                path = roadPath1,
                color = roadColor,
                style = Stroke(width = 6.dp.toPx())
            )

            // Draw stylized road path 2
            val roadPath2 = Path().apply {
                moveTo(width * 0.4f, 0f)
                cubicTo(width * 0.45f, height * 0.5f, width * 0.55f, height * 0.6f, width * 0.7f, height)
            }
            drawPath(
                path = roadPath2,
                color = roadColor.copy(alpha = 0.8f),
                style = Stroke(width = 4.dp.toPx())
            )

            // Animated pulsing radar aura at center
            drawCircle(
                color = primaryPinColor.copy(alpha = pulseAlpha),
                radius = 28.dp.toPx() * pulseScale,
                center = Offset(width / 2f, height / 2f)
            )
        }

        // Center Location Pin Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(36.dp)
                    .offset(y = (-4).dp)
            )
        }
    }
}

/**
 * Individual information row component displaying icon, label, and formatted value.
 */
@Composable
private fun LocationInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
