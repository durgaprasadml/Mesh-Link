package com.meshlink.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccessibilitySettings(
    uiState: SettingsUiState,
    onSetHighContrast: (Boolean) -> Unit = {},
    onSetReduceMotion: (Boolean) -> Unit = {},
    onSetLargeText: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Accessibility & Comfort",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.Contrast,
                    title = "High Contrast Mode",
                    subtitle = "Enhance text contrast and border lines for outdoor readability",
                    isSwitch = true,
                    isChecked = uiState.highContrast,
                    onCheckedChange = onSetHighContrast
                )

                SettingsRow(
                    icon = Icons.Default.Animation,
                    title = "Reduce Motion",
                    subtitle = "Minimize scale and slide animations throughout the app",
                    isSwitch = true,
                    isChecked = uiState.reduceMotionEnabled,
                    onCheckedChange = onSetReduceMotion
                )

                SettingsRow(
                    icon = Icons.Default.FormatSize,
                    title = "Large Tactical Font Scale",
                    subtitle = "Enlarge font sizing for fast text recognition in tactical scenarios",
                    isSwitch = true,
                    isChecked = uiState.largeTextEnabled,
                    onCheckedChange = onSetLargeText
                )

                SettingsRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "TalkBack Screen Reader Optimization",
                    subtitle = "Ensure all interactive targets have 48dp touch bounds & semantic tags",
                    isSwitch = true,
                    isChecked = true,
                    onCheckedChange = {},
                    enabled = false
                )

                SettingsRow(
                    icon = Icons.Default.TouchApp,
                    title = "Minimum 48dp Touch Targets",
                    subtitle = "Enforced across all buttons, chips, and settings items",
                    statusChipText = "Enforced",
                    statusChipColor = MaterialTheme.colorScheme.primaryContainer,
                    statusChipTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    showDivider = false
                )
            }
        }
    }
}
