package com.meshlink.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Legacy Activity Timeline Widget - Deprecated in Phase 3 Messaging Dashboard.
 */
@Deprecated(
    message = "Home screen has been refactored into a WhatsApp/Signal messaging-first dashboard. Activity timeline is removed.",
    level = DeprecationLevel.WARNING
)
@Composable
fun ActivityTimelineSection(
    modifier: Modifier = Modifier
) {
    // Legacy component removed in Phase 3
}
