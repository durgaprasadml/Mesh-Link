package com.meshlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Reusable Material 3 Loading Components for Mesh-Link.
 * Includes Circular, Linear, Full Screen, Card, Chat, List, Grid, Analytics, Media, and Nearby loaders.
 */

@Composable
fun MeshCircularLoading(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = MeshTheme.colors.primary,
    strokeWidth: Dp = 3.dp,
    label: String = "Loading"
) {
    CircularProgressIndicator(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
        color = color,
        strokeWidth = strokeWidth
    )
}

@Composable
fun MeshLinearLoading(
    modifier: Modifier = Modifier,
    color: Color = MeshTheme.colors.primary,
    trackColor: Color = MeshTheme.colors.surfaceVariant,
    label: String = "Progressing"
) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
        color = color,
        trackColor = trackColor
    )
}

@Composable
fun MeshFullScreenLoading(
    message: String = "Loading Mesh-Link...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MeshCircularLoading(size = 48.dp, strokeWidth = 4.dp, label = message)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MeshTheme.typography.bodyMedium,
                color = MeshTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MeshCardLoading(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(16.dp),
        color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            MeshCircularLoading(size = 28.dp, strokeWidth = 2.5.dp)
        }
    }
}

@Composable
fun MeshChatLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) { index ->
            val isOutgoing = index % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOutgoing) MeshTheme.colors.primaryContainer else MeshTheme.colors.surfaceVariant,
                    modifier = Modifier
                        .width(if (isOutgoing) 200.dp else 240.dp)
                        .height(56.dp)
                ) {}
            }
        }
    }
}

@Composable
fun MeshListLoading(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MeshTheme.colors.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MeshTheme.colors.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .background(MeshTheme.colors.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(10.dp)
                            .background(MeshTheme.colors.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun MeshGridLoading(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            MeshCardLoading(height = 140.dp)
        }
    }
}

@Composable
fun MeshAnalyticsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MeshCardLoading(height = 180.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeshCardLoading(modifier = Modifier.weight(1f), height = 100.dp)
            MeshCardLoading(modifier = Modifier.weight(1f), height = 100.dp)
        }
        MeshCardLoading(height = 200.dp)
    }
}

@Composable
fun MeshMediaLoading(modifier: Modifier = Modifier) {
    MeshGridLoading(modifier = modifier, columns = 3, itemCount = 9)
}

@Composable
fun MeshNearbyLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MeshCircularLoading(size = 56.dp, strokeWidth = 4.dp, label = "Scanning nearby mesh peers...")
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scanning nearby devices...",
                style = MeshTheme.typography.bodyLarge,
                color = MeshTheme.colors.onBackground
            )
        }
    }
}
