package com.meshlink.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.screens.*

enum class SettingsDestination {
    HOME, PROFILE, SECURITY, NETWORK, STORAGE, APPEARANCE, DIAGNOSTICS, DEVELOPER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by remember { mutableStateOf(SettingsDestination.HOME) }

    val userName = uiState.user?.name ?: "User"
    val meshId = uiState.user?.meshId ?: ""

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsEvent.LogoutSuccess -> onLoggedOut()
                else -> {}
            }
        }
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            if (targetState != SettingsDestination.HOME) {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut()
            } else {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut()
            }
        },
        label = "SettingsNav"
    ) { dest ->
        when (dest) {
            SettingsDestination.HOME -> SettingsHome(
                userName = userName,
                meshId = meshId,
                onNavigate = { currentDestination = it },
                onBack = onBack,
                onLogout = { viewModel.logout() }
            )
            SettingsDestination.PROFILE -> ProfileScreen(
                userName = userName,
                meshId = meshId,
                onBack = { currentDestination = SettingsDestination.HOME }
            )
            SettingsDestination.SECURITY -> SecurityCenterScreen(
                onBack = { currentDestination = SettingsDestination.HOME }
            )
            SettingsDestination.NETWORK -> NetworkSettingsScreen(
                onBack = { currentDestination = SettingsDestination.HOME }
            )
            SettingsDestination.STORAGE -> StorageSettingsScreen(
                onBack = { currentDestination = SettingsDestination.HOME }
            )
            SettingsDestination.APPEARANCE -> AppearanceSettingsScreen(
                onBack = { currentDestination = SettingsDestination.HOME }
            )
            SettingsDestination.DIAGNOSTICS -> DiagnosticsScreen(
                onBack = { currentDestination = SettingsDestination.HOME }
            )
            SettingsDestination.DEVELOPER -> DeveloperOptionsScreen(
                onBack = { currentDestination = SettingsDestination.HOME }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHome(
    userName: String,
    meshId: String,
    onNavigate: (SettingsDestination) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.mediumLarge)
        ) {
            item {
                // Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(SettingsDestination.PROFILE) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeshTheme.spacing.mediumLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Mesh ID: ${meshId.take(8).ifBlank { "Unassigned" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                // Main Settings Group
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Security Center",
                            subtitle = "Encryption, Identity, App Lock",
                            icon = Icons.Default.Security,
                            onClick = { onNavigate(SettingsDestination.SECURITY) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Network & Transport",
                            subtitle = "BLE, Wi-Fi Direct, Relaying",
                            icon = Icons.Default.WifiTethering,
                            onClick = { onNavigate(SettingsDestination.NETWORK) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Storage & Data",
                            subtitle = "Database, Media Cache",
                            icon = Icons.Default.Storage,
                            onClick = { onNavigate(SettingsDestination.STORAGE) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Appearance",
                            subtitle = "Theme, Dynamic Color",
                            icon = Icons.Default.Palette,
                            onClick = { onNavigate(SettingsDestination.APPEARANCE) }
                        )
                    }
                }
            }

            item {
                // System Group
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        SettingsItemRow(
                            title = "Diagnostics",
                            subtitle = "Health metrics and exports",
                            icon = Icons.Default.HealthAndSafety,
                            onClick = { onNavigate(SettingsDestination.DIAGNOSTICS) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        SettingsItemRow(
                            title = "Developer Options",
                            subtitle = "Simulators and packet viewers",
                            icon = Icons.Default.Code,
                            onClick = { onNavigate(SettingsDestination.DEVELOPER) }
                        )
                    }
                }
            }

            item {
                // Logout Group
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    SettingsItemRow(
                        title = "Log Out",
                        icon = Icons.Default.ExitToApp,
                        iconTint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = onLogout,
                        trailingContent = null
                    )
                }
                Spacer(modifier = Modifier.height(MeshTheme.spacing.giant))
            }
        }
    }
}
