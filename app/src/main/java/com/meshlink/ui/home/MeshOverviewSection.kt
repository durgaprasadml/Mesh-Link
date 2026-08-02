package com.meshlink.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.domain.model.BleDevice

/**
 * Legacy Dashboard Overview Widget - Deprecated in Phase 3 Messaging Dashboard.
 */
@Deprecated(
    message = "Home screen has been refactored into a WhatsApp/Signal messaging-first dashboard. Dashboard widgets are removed.",
    level = DeprecationLevel.WARNING
)
@Composable
fun MeshOverviewSection(
    nearbyDevices: List<BleDevice>,
    onNavigateToNearby: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Legacy component removed in Phase 3
}
