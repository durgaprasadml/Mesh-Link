package com.meshlink.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SettingsUiState
import com.meshlink.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Appearance") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.large)
        ) {
            item {
                Text("Theme & Colors", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(MeshTheme.spacing.medium)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.themeMode == "SYSTEM",
                                onClick = { viewModel.setThemeMode("SYSTEM") },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) { Text("System") }
                            SegmentedButton(
                                selected = uiState.themeMode == "LIGHT",
                                onClick = { viewModel.setThemeMode("LIGHT") },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) { Text("Light") }
                            SegmentedButton(
                                selected = uiState.themeMode == "DARK",
                                onClick = { viewModel.setThemeMode("DARK") },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) { Text("Dark") }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.background)
                    Column {
                        SettingsItemRow(
                            title = "Dynamic Color (Material You)",
                            subtitle = "Adapt colors based on your wallpaper.",
                            icon = Icons.Default.ColorLens,
                            trailingContent = {
                                Switch(
                                    checked = uiState.isMaterialYouEnabled,
                                    onCheckedChange = { viewModel.setMaterialYouEnabled(it) }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "High Contrast",
                            subtitle = "Increase text and element contrast.",
                            icon = Icons.Default.Contrast,
                            trailingContent = {
                                Switch(
                                    checked = uiState.highContrast,
                                    onCheckedChange = { viewModel.setHighContrast(it) }
                                )
                            }
                        )
                    }
                    if (!uiState.isMaterialYouEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        Column(modifier = Modifier.padding(MeshTheme.spacing.medium)) {
                            Text("Accent Color", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                            val colors = listOf(
                                "Blue" to Color(0xFF2196F3),
                                "Green" to Color(0xFF4CAF50),
                                "Purple" to Color(0xFF9C27B0),
                                "Orange" to Color(0xFFFF9800),
                                "Red" to Color(0xFFF44336)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)) {
                                items(
                                    items = colors,
                                    key = { it.first },
                                    contentType = { "color_item" }
                                ) { (name, color) ->
                                    val isSelected = uiState.accentColor == name
                                    Box(
                                        modifier = Modifier
                                            .size(MeshTheme.spacing.giant)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.setAccentColor(name) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Typography", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Large Text",
                            subtitle = "Force larger text size everywhere.",
                            icon = Icons.Default.FormatSize,
                            trailingContent = {
                                Switch(
                                    checked = uiState.largeTextEnabled,
                                    onCheckedChange = { viewModel.setLargeTextEnabled(it) }
                                )
                            }
                        )
                        if (!uiState.largeTextEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            Column(modifier = Modifier.padding(MeshTheme.spacing.medium)) {
                                Text("Font Scale: ${"%.1f".format(uiState.fontScale)}x", style = MaterialTheme.typography.titleMedium)
                                Slider(
                                    value = uiState.fontScale,
                                    onValueChange = { viewModel.setFontScale(it) },
                                    valueRange = 0.8f..1.4f,
                                    steps = 5
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Visual Effects", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Glass Effects",
                            subtitle = "Enable translucent blurs and panels.",
                            icon = Icons.Default.BlurOn,
                            trailingContent = {
                                Switch(
                                    checked = uiState.glassEffectsEnabled,
                                    onCheckedChange = { viewModel.setGlassEffectsEnabled(it) }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Animations",
                            subtitle = "Enable UI transitions and motion.",
                            icon = Icons.Default.Animation,
                            trailingContent = {
                                Switch(
                                    checked = uiState.animationsEnabled,
                                    onCheckedChange = { viewModel.setAnimationsEnabled(it) }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Reduce Motion",
                            subtitle = "Minimize movement to reduce eye strain.",
                            icon = Icons.Default.Accessibility,
                            trailingContent = {
                                Switch(
                                    checked = uiState.reduceMotionEnabled,
                                    onCheckedChange = { viewModel.setReduceMotionEnabled(it) },
                                    enabled = uiState.animationsEnabled
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        Column(modifier = Modifier.padding(MeshTheme.spacing.medium)) {
                            Text("Corner Radius: ${"%.1f".format(uiState.cornerRadiusScale)}x", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.cornerRadiusScale,
                                onValueChange = { viewModel.setCornerRadiusScale(it) },
                                valueRange = 0.0f..2.0f,
                                steps = 9
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MeshTheme.spacing.large))
            }
        }
    }
}
