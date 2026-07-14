package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                Text("Theme", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        var darkMode by remember { mutableStateOf(true) }
                        SettingsItemRow(
                            title = "Dark Theme",
                            subtitle = "Use dark mode across the app.",
                            icon = Icons.Default.DarkMode,
                            trailingContent = { Switch(checked = darkMode, onCheckedChange = { darkMode = it }) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        var dynamicColor by remember { mutableStateOf(true) }
                        SettingsItemRow(
                            title = "Dynamic Color (Material You)",
                            subtitle = "Adapt colors based on your wallpaper.",
                            icon = Icons.Default.ColorLens,
                            trailingContent = { Switch(checked = dynamicColor, onCheckedChange = { dynamicColor = it }) }
                        )
                    }
                }
            }
        }
    }
}
