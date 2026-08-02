package com.meshlink.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Developer Showcase Screen for Mesh-Link Phase 15.
 * Interactive showcase displaying adaptive components, empty states, error states,
 * loading states, permission rationales, haptic patterns, and motion curves.
 */

@Composable
fun MeshProductionShowcaseScreen(
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf("States") }
    val haptics = rememberMeshHaptics()
    val scrollState = rememberScrollState()

    MeshAdaptiveScreenScaffold(
        modifier = modifier,
        topBar = {
            MeshResponsiveToolbar(
                title = {
                    Text(
                        text = "Production UI Showcase (Phase 15)",
                        style = MeshTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MeshTheme.colors.primary
                    )
                }
            )
        }
    ) { windowInfo ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("States", "Loading", "Errors", "Permissions").forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = {
                            haptics.perform(MeshHapticPattern.SELECTION)
                            selectedSection = section
                        },
                        label = { Text(section) }
                    )
                }
            }

            // Window Profile Info Card
            MeshResponsiveCard(
                windowInfo = windowInfo,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Adaptive Window Profile",
                    style = MeshTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeshTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "FormFactor: ${windowInfo.profile.formFactor} | Width: ${windowInfo.profile.widthDp} | Orientation: ${windowInfo.profile.orientation}",
                    style = MeshTheme.typography.bodySmall,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }

            when (selectedSection) {
                "States" -> {
                    Text(
                        text = "Standardized Empty States",
                        style = MeshTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        MeshCategoryEmptyState(
                            category = MeshEmptyStateCategory.CHATS,
                            onPrimaryAction = { haptics.perform(MeshHapticPattern.SUCCESS) }
                        )
                    }
                }
                "Loading" -> {
                    Text(
                        text = "Shimmer & Skeleton Loading",
                        style = MeshTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    MeshSkeletonList(itemCount = 3)
                    Spacer(modifier = Modifier.height(16.dp))
                    MeshInlineLoader(message = "Synchronizing mesh state...")
                }
                "Errors" -> {
                    Text(
                        text = "Unified Presentation Errors",
                        style = MeshTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    MeshErrorCard(
                        title = "Mesh Connection Lost",
                        message = "Nearby peer node disconnected unexpectedly. Signal lost.",
                        onRetry = { haptics.perform(MeshHapticPattern.WARNING) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MeshErrorBanner(
                        message = "Bluetooth hardware scanning disabled.",
                        actionLabel = "Enable",
                        onActionClick = { haptics.perform(MeshHapticPattern.SELECTION) }
                    )
                }
                "Permissions" -> {
                    Text(
                        text = "Permission Rationale UX",
                        style = MeshTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        MeshPermissionRationaleScreen(
                            type = MeshPermissionType.BLUETOOTH,
                            onGrantRequested = { haptics.perform(MeshHapticPattern.SUCCESS) },
                            onDismiss = { haptics.perform(MeshHapticPattern.SELECTION) }
                        )
                    }
                }
            }
        }
    }
}
