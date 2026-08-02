package com.meshlink.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.MeshTopAppBar
import com.meshlink.ui.components.UserAvatar
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress
import com.meshlink.ui.settings.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshSettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onSetThemeMode: (String) -> Unit,
    onSetMaterialYou: (Boolean) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetGlassEffects: (Boolean) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetLargeText: (Boolean) -> Unit,
    onSetEncryptionEnabled: (Boolean) -> Unit,
    onSetOnlineVisible: (Boolean) -> Unit,
    onSetAdvancedEncryption: (Boolean) -> Unit,
    onSetBleEnabled: (Boolean) -> Unit,
    onSetBleAdv: (Boolean) -> Unit,
    onSetBleScan: (Boolean) -> Unit,
    onSetTransport: (String) -> Unit,
    onSetRelayEnabled: (Boolean) -> Unit,
    onSetMaxHops: (Int) -> Unit,
    onExportLogs: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val userName = uiState.user?.name ?: "Mesh User"
    val nodeId = uiState.user?.meshId?.take(8)?.uppercase() ?: "NODE-8A9F"

    MeshScreen(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MeshTopAppBar(
                title = "Tactical Settings",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search Settings"
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshSpacing.ScreenPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = MeshSpacing.ListBottomSpacing)
        ) {
            // Search Bar Filter
            if (isSearchActive) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search settings (e.g. BLE, Theme, E2EE)...") },
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
                            .padding(bottom = 14.dp)
                    )
                }
            }

            // Profile Header Card
            if (searchQuery.isEmpty()) {
                item {
                    ElevatedCard(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scaleOnPress(0.96f)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(MeshSpacing.CardCornerRadius)
                            )
                            .semantics {
                                contentDescription = "Profile Card for $userName. Node ID $nodeId. Click to edit."
                                role = Role.Button
                            },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(MeshSpacing.CardCornerRadius),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MeshSpacing.CardInternalPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                identity = UserIdentity.create(
                                    userId = uiState.user?.meshId ?: "",
                                    displayName = userName,
                                    avatarUri = uiState.user?.avatarUri
                                ),
                                size = 58.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = userName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MeshTheme.colors.online)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Node ID: $nodeId",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(18.dp)) }
            }

            // 1. Appearance Section
            item {
                AppearanceSection(
                    uiState = uiState,
                    onSetThemeMode = onSetThemeMode,
                    onSetMaterialYou = onSetMaterialYou,
                    onSetHighContrast = onSetHighContrast,
                    onSetGlassEffects = onSetGlassEffects,
                    onSetReduceMotion = onSetReduceMotion,
                    onSetLargeText = onSetLargeText
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // 2. Connectivity & Mesh Transport Section
            item {
                ConnectivitySection(
                    uiState = uiState,
                    onSetBleEnabled = onSetBleEnabled,
                    onSetBleAdv = onSetBleAdv,
                    onSetBleScan = onSetBleScan,
                    onSetTransport = onSetTransport,
                    onSetRelayEnabled = onSetRelayEnabled,
                    onSetMaxHops = onSetMaxHops
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // 3. Privacy & Security Section
            item {
                PrivacySection(
                    uiState = uiState,
                    onSetEncryptionEnabled = onSetEncryptionEnabled,
                    onSetOnlineVisible = onSetOnlineVisible,
                    onSetAdvancedEncryption = onSetAdvancedEncryption,
                    onShowToast = onShowToast
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // 4. Notifications & Alerts Section
            item {
                NotificationSection(
                    uiState = uiState,
                    onShowToast = onShowToast
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // 5. Diagnostics & Developer Section
            item {
                DiagnosticsSection(
                    uiState = uiState,
                    onExportLogs = onExportLogs,
                    onShowToast = onShowToast
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // 6. About Section
            item {
                AboutSection(
                    uiState = uiState,
                    onShowToast = onShowToast
                )
            }
        }
    }
}
