package com.meshlink.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun DiscoveryControls(
    isScanning: Boolean,
    onToggleScan: () -> Unit,
    onRefresh: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        glowColor = if (isScanning) MeshTheme.colors.connected else MeshTheme.colors.primary,
        glowRadius = 140f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshSpacing.MD, vertical = MeshSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Scan / Stop Action Button
            ElevatedButton(
                onClick = onToggleScan,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = if (isScanning) MeshTheme.colors.danger.copy(alpha = 0.85f) else MeshTheme.colors.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isScanning) "Pause Scan" else "Start Scan",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(MeshSpacing.XS))
                Text(
                    text = if (isScanning) "Pause Scan" else "Scan Mesh",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Quick Control Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(MeshSpacing.XS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh scan",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter mesh peers",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
