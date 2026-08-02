package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Storage Overview card displaying Total Storage, Used, Available, and Cached Media volume.
 */
@Composable
fun StorageOverview(
    usedBytes: Long = 1_450_000_000L,
    totalBytes: Long = 64_000_000_000L,
    cacheBytes: Long = 230_000_000L,
    onClearCacheClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val usedPercent = ((usedBytes.toFloat() / totalBytes.toFloat())).coerceIn(0f, 1f)
    val formattedUsed = String.format("%.1f GB", usedBytes / 1_000_000_000f)
    val formattedTotal = String.format("%.0f GB", totalBytes / 1_000_000_000f)
    val formattedCache = String.format("%.0f MB", cacheBytes / 1_000_000f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage Usage",
                        style = MeshTheme.customTypography.subtitle.copy(fontWeight = FontWeight.Bold),
                        color = MeshTheme.colors.onSurface
                    )
                }

                if (onClearCacheClick != null) {
                    TextButton(onClick = onClearCacheClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Cache",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear Cache ($formattedCache)",
                            style = MeshTheme.customTypography.caption,
                            color = MeshTheme.colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Progress Bar
            LinearProgressIndicator(
                progress = { usedPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MeshTheme.colors.primary,
                trackColor = MeshTheme.colors.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$formattedUsed used of $formattedTotal",
                    style = MeshTheme.customTypography.caption,
                    color = MeshTheme.colors.onSurfaceVariant
                )
                Text(
                    text = "${(usedPercent * 100).toInt()}% Used",
                    style = MeshTheme.customTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = MeshTheme.colors.primary
                )
            }
        }
    }
}
