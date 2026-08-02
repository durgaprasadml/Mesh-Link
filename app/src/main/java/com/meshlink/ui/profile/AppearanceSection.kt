package com.meshlink.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.settings.SettingsUiState

@Composable
fun AppearanceSection(
    uiState: SettingsUiState,
    onSetThemeMode: (String) -> Unit,
    onSetMaterialYou: (Boolean) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetGlassEffects: (Boolean) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetLargeText: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ThemeSelector(
            currentTheme = uiState.themeMode,
            onSelectTheme = onSetThemeMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        val appearanceItems = listOf(
            SettingsItemUi(
                id = "material_you",
                title = "Material You Dynamic Colors",
                subtitle = "Adapt accent palette to device wallpaper",
                icon = Icons.Default.AutoAwesome,
                isChecked = uiState.isMaterialYouEnabled,
                onClick = { onSetMaterialYou(!uiState.isMaterialYouEnabled) }
            ),
            SettingsItemUi(
                id = "high_contrast",
                title = "High Contrast Mode",
                subtitle = "Increase text legibility and edge borders",
                icon = Icons.Default.Contrast,
                isChecked = uiState.highContrast,
                onClick = { onSetHighContrast(!uiState.highContrast) }
            ),
            SettingsItemUi(
                id = "glass_effects",
                title = "Glassmorphism UI Effects",
                subtitle = "Enable translucent visual depth and subtle glows",
                icon = Icons.Default.BlurOn,
                isChecked = uiState.glassEffectsEnabled,
                onClick = { onSetGlassEffects(!uiState.glassEffectsEnabled) }
            ),
            SettingsItemUi(
                id = "reduce_motion",
                title = "Reduce Motion",
                subtitle = "Minimize entrance and scale transitions",
                icon = Icons.Default.Animation,
                isChecked = uiState.reduceMotionEnabled,
                onClick = { onSetReduceMotion(!uiState.reduceMotionEnabled) }
            ),
            SettingsItemUi(
                id = "large_text",
                title = "Large Typography Scale",
                subtitle = "Enlarge font sizing for mesh tactical text",
                icon = Icons.Default.FormatSize,
                isChecked = uiState.largeTextEnabled,
                onClick = { onSetLargeText(!uiState.largeTextEnabled) }
            )
        )

        SettingsGroupCard(
            title = "Visual Customization",
            items = appearanceItems
        )
    }
}
