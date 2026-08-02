package com.meshlink.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.LoadingOverlay
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.MeshTopAppBar
import com.meshlink.ui.contacts.ContactDetailSheet
import com.meshlink.ui.contacts.ContactFilters
import com.meshlink.ui.contacts.ContactsList
import com.meshlink.ui.contacts.ContactsSearch
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.settings.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshProfileScreen(
    profileState: ProfileUiState,
    settingsState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onEditAvatarClick: () -> Unit,
    onSaveProfile: (name: String, aboutMe: String?, avatarUri: String?) -> Unit,
    onExportLogs: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isExpandedWidth = configuration.screenWidthDp >= 840
    val isFoldableWidth = configuration.screenWidthDp in 600..839

    val userIdentityUi = UserIdentityUi.fromUser(
        user = profileState.user,
        isOnline = settingsState.isOnlineVisible
    )

    var showQrDialog by remember { mutableStateOf(false) }
    var selectedContactForSheet by remember { mutableStateOf<ContactUi?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ContactFilterOption.ALL) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Profile, 1: Contacts

    MeshScreen(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MeshTopAppBar(
                title = "Profile, Identity & Contacts",
                onBackClick = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .consumeWindowInsets(paddingValues)
        ) {
            if (isExpandedWidth || isFoldableWidth) {
                // Adaptive Split / Dual-Pane Layout for Tablet & Foldables
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Left Pane: Contacts & Search
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Contacts & Peer Network",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ContactsSearch(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ContactFilters(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ContactsList(
                            query = searchQuery,
                            selectedFilter = selectedFilter,
                            onContactClick = { selectedContactForSheet = it },
                            onDiscoverClick = { onShowToast("Scanning for nearby mesh devices...") }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Right Pane: Profile & Identity Hub
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        item {
                            ProfileHero(
                                userIdentity = userIdentityUi,
                                onEditAvatarClick = onEditAvatarClick,
                                onShareQrClick = { showQrDialog = true },
                                onSettingsClick = { onShowToast("Opening settings...") }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            ProfileQuickActions(
                                onEditProfileClick = onEditAvatarClick,
                                onQrCodeClick = { showQrDialog = true },
                                onTrustedDevicesClick = { onShowToast("Viewing trusted device list") },
                                onContactsClick = { selectedTab = 1 }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            IdentityCard(
                                userIdentity = userIdentityUi,
                                onShowQrCode = { showQrDialog = true },
                                onCopyMeshId = { onShowToast("Copied Mesh ID to clipboard") },
                                onShareIdentity = { onShowToast("Sharing Identity...") },
                                onExportIdentity = { onShowToast("Exported Identity Key") }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            TrustedDevices(
                                onDeviceClick = { onShowToast("Device: ${it.deviceName}") },
                                onRemoveDevice = { onShowToast("Removed trust for ${it.deviceName}") },
                                onVerifyDevice = { onShowToast("Verifying key for ${it.deviceName}") }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            VerificationCard(
                                trustLevel = userIdentityUi.trustLevel,
                                onVerifyClick = { onShowToast("Verification center updated") }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            Personalization(
                                currentName = userIdentityUi.displayName,
                                currentAbout = userIdentityUi.aboutMe,
                                selectedTheme = settingsState.themeMode,
                                onSaveProfile = { name, about ->
                                    onSaveProfile(name, about, userIdentityUi.avatarUri)
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            DiagnosticsSection(
                                uiState = settingsState,
                                onExportLogs = onExportLogs,
                                onShowToast = onShowToast
                            )
                        }
                    }
                }
            } else {
                // Phone Layout (Tabbed: Profile Hub vs Contacts)
                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.background
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Profile & Identity") },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Contacts") },
                            icon = { Icon(Icons.Default.Contacts, contentDescription = null) }
                        )
                    }

                    if (selectedTab == 0) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = MeshSpacing.ScreenPadding),
                            contentPadding = PaddingValues(top = 12.dp, bottom = MeshSpacing.ListBottomSpacing)
                        ) {
                            item {
                                ProfileHero(
                                    userIdentity = userIdentityUi,
                                    onEditAvatarClick = onEditAvatarClick,
                                    onShareQrClick = { showQrDialog = true },
                                    onSettingsClick = { onShowToast("Opening settings...") }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }

                            item {
                                ProfileQuickActions(
                                    onEditProfileClick = onEditAvatarClick,
                                    onQrCodeClick = { showQrDialog = true },
                                    onTrustedDevicesClick = { onShowToast("Viewing trusted devices") },
                                    onContactsClick = { selectedTab = 1 }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }

                            item {
                                IdentityCard(
                                    userIdentity = userIdentityUi,
                                    onShowQrCode = { showQrDialog = true },
                                    onCopyMeshId = { onShowToast("Copied Mesh ID to clipboard") },
                                    onShareIdentity = { onShowToast("Sharing Identity...") },
                                    onExportIdentity = { onShowToast("Exported Identity Key") }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }

                            item {
                                TrustedDevices(
                                    onDeviceClick = { onShowToast("Device: ${it.deviceName}") },
                                    onRemoveDevice = { onShowToast("Removed trust for ${it.deviceName}") },
                                    onVerifyDevice = { onShowToast("Verifying key for ${it.deviceName}") }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }

                            item {
                                VerificationCard(
                                    trustLevel = userIdentityUi.trustLevel,
                                    onVerifyClick = { onShowToast("Verification center updated") }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }

                            item {
                                Personalization(
                                    currentName = userIdentityUi.displayName,
                                    currentAbout = userIdentityUi.aboutMe,
                                    selectedTheme = settingsState.themeMode,
                                    onSaveProfile = { name, about ->
                                        onSaveProfile(name, about, userIdentityUi.avatarUri)
                                    }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }

                            item {
                                DiagnosticsSection(
                                    uiState = settingsState,
                                    onExportLogs = onExportLogs,
                                    onShowToast = onShowToast
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            ContactsSearch(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ContactFilters(
                                selectedFilter = selectedFilter,
                                onFilterSelected = { selectedFilter = it }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ContactsList(
                                query = searchQuery,
                                selectedFilter = selectedFilter,
                                onContactClick = { selectedContactForSheet = it },
                                onDiscoverClick = { onShowToast("Scanning for nearby mesh devices...") }
                            )
                        }
                    }
                }
            }

            // QR Code Presentation Modal
            QRIdentityDialog(
                visible = showQrDialog,
                userIdentity = userIdentityUi,
                onDismiss = { showQrDialog = false },
                onCopyMeshId = { onShowToast("Copied Mesh ID to clipboard") },
                onShare = { onShowToast("Sharing QR Code...") },
                onSaveImage = { onShowToast("Saved QR Code to gallery") },
                onPrintQr = { onShowToast("Sending QR Code to printer...") }
            )

            // Contact Detail Bottom Sheet
            ContactDetailSheet(
                contact = selectedContactForSheet,
                visible = selectedContactForSheet != null,
                onDismiss = { selectedContactForSheet = null },
                onStartChat = {
                    onShowToast("Starting direct chat with ${it.displayName}")
                    selectedContactForSheet = null
                },
                onVerifyFingerprint = {
                    onShowToast("Verified key fingerprint for ${it.displayName}")
                },
                onShareContact = {
                    onShowToast("Shared contact info for ${it.displayName}")
                },
                onViewQr = {
                    showQrDialog = true
                    selectedContactForSheet = null
                }
            )

            LoadingOverlay(isLoading = profileState.isLoading)
        }
    }
}
