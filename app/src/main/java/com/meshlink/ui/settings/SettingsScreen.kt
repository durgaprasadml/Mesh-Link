package com.meshlink.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.screens.*
import kotlinx.coroutines.flow.collectLatest

enum class SettingsDestination {
    HOME, PROFILE, NETWORK, MESSAGING, EMERGENCY, STORAGE, APPEARANCE, NOTIFICATIONS, PRIVACY, DEVELOPER, ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by remember { mutableStateOf(SettingsDestination.HOME) }
    val userName = uiState.user?.name ?: "Mesh User"
    val nodeId = uiState.user?.meshId?.take(8)?.uppercase() ?: "NODE-8A9F"
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SettingsEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is SettingsEvent.SuccessMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
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
                        nodeId = nodeId,
                        uiState = uiState,
                        onNavigate = { currentDestination = it },
                        onBack = onBack
                    )
                    SettingsDestination.PROFILE -> com.meshlink.ui.profile.ProfileScreen(
                        onNavigateBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.NETWORK -> NetworkSettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.MESSAGING -> MessagingSettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.EMERGENCY -> EmergencySettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.STORAGE -> StorageSettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.APPEARANCE -> AppearanceSettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.NOTIFICATIONS -> NotificationsSettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.PRIVACY -> PrivacySettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.DEVELOPER -> DeveloperSettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                    SettingsDestination.ABOUT -> AboutSettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.HOME }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHome(
    userName: String,
    nodeId: String,
    uiState: SettingsUiState,
    onNavigate: (SettingsDestination) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back to Main Screen")
                    }
                },
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search Settings"
                        )
                    }
                },
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
            // Search Bar Filter
            if (isSearchActive) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search settings (e.g. BLE, Theme, SOS)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = MeshTheme.spacing.small)
                    )
                }
            }

            // Profile Header Card
            if (searchQuery.isEmpty()) {
                item {
                    ElevatedCard(
                        onClick = { onNavigate(SettingsDestination.PROFILE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Profile Card for $userName. Node ID $nodeId. Edit Profile."
                                role = Role.Button
                            },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MeshTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MeshTheme.spacing.mediumLarge)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.meshlink.ui.components.UserAvatarImage(
                                    avatarUri = uiState.user?.avatarUri,
                                    displayName = userName,
                                    size = 64.dp
                                )
                                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumLarge))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = userName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                                    Text(
                                        text = "Node ID: $nodeId",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                                    Row(horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)) {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text("Mesh Node Active", style = MaterialTheme.typography.labelSmall) },
                                            leadingIcon = { Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                        AssistChip(
                                            onClick = { },
                                            label = { Text("Relay ON", style = MaterialTheme.typography.labelSmall) },
                                            leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Categorized Settings Cards
            val allSettings = listOf(
                SettingCategory(
                    title = "Profile & Identity",
                    items = listOf(
                        SettingRowData("User Profile", "Display name, Node ID & status", Icons.Default.Person, SettingsDestination.PROFILE)
                    )
                ),
                SettingCategory(
                    title = "Network & Connectivity",
                    items = listOf(
                        SettingRowData("Network & Transport", "BLE, Wi-Fi Direct, Relaying & Hops", Icons.Default.WifiTethering, SettingsDestination.NETWORK)
                    )
                ),
                SettingCategory(
                    title = "Messaging & Media",
                    items = listOf(
                        SettingRowData("Messaging & Media", "Quality, Retry, Receipts & Retention", Icons.Default.Chat, SettingsDestination.MESSAGING)
                    )
                ),
                SettingCategory(
                    title = "Safety & Emergency",
                    items = listOf(
                        SettingRowData("Emergency SOS", "Hotline (112), Broadcasts & Live Location", Icons.Default.WarningAmber, SettingsDestination.EMERGENCY)
                    )
                ),
                SettingCategory(
                    title = "Storage & Data",
                    items = listOf(
                        SettingRowData("Storage & Data", "Database, Cache, Backup & Export", Icons.Default.Storage, SettingsDestination.STORAGE)
                    )
                ),
                SettingCategory(
                    title = "Appearance & Customization",
                    items = listOf(
                        SettingRowData("Appearance", "Theme, Dynamic Color, Font Scale & Motion", Icons.Default.Palette, SettingsDestination.APPEARANCE)
                    )
                ),
                SettingCategory(
                    title = "Notifications & Alerts",
                    items = listOf(
                        SettingRowData("Notifications", "Messages, SOS Alerts, Sounds & Vibration", Icons.Default.Notifications, SettingsDestination.NOTIFICATIONS)
                    )
                ),
                SettingCategory(
                    title = "Privacy & Security",
                    items = listOf(
                        SettingRowData("Privacy & Security", "AES-256 E2EE, Biometrics & Trusted Keys", Icons.Default.Security, SettingsDestination.PRIVACY)
                    )
                ),
                SettingCategory(
                    title = "Diagnostics & Tools",
                    items = listOf(
                        SettingRowData("Developer Options", "Mesh Topology Graph, Logs & BLE Specs", Icons.Default.BugReport, SettingsDestination.DEVELOPER)
                    )
                ),
                SettingCategory(
                    title = "App Info & Support",
                    items = listOf(
                        SettingRowData("About Mesh Link", "Version 1.4.0, Open Source Licenses & GitHub", Icons.Default.Info, SettingsDestination.ABOUT)
                    )
                )
            )

            allSettings.forEach { category ->
                val filteredItems = category.items.filter {
                    searchQuery.isEmpty() ||
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.subtitle.contains(searchQuery, ignoreCase = true)
                }

                if (filteredItems.isNotEmpty()) {
                    item {
                        Text(
                            text = category.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = MeshTheme.spacing.small)
                        )
                        Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MeshTheme.shapes.large
                        ) {
                            Column {
                                filteredItems.forEachIndexed { index, itemData ->
                                    SettingsItemRow(
                                        title = itemData.title,
                                        subtitle = itemData.subtitle,
                                        icon = itemData.icon,
                                        onClick = { onNavigate(itemData.destination) }
                                    )
                                    if (index < filteredItems.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
        }
    }
}

private data class SettingCategory(
    val title: String,
    val items: List<SettingRowData>
)

private data class SettingRowData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: SettingsDestination
)
