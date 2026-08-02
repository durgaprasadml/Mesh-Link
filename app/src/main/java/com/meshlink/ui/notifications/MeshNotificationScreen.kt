package com.meshlink.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshNotificationScreen(
    state: NotificationCenterUiState,
    onBackClick: (() -> Unit)? = null,
    onQueryChange: (String) -> Unit = {},
    onClearQuery: () -> Unit = {},
    onCategorySelected: (NotificationCategory) -> Unit = {},
    onDismissAlert: (String) -> Unit = {},
    onAlertAction: (PriorityAlertUi) -> Unit = {},
    onNotificationClick: (NotificationItemUi) -> Unit = {},
    onQuickActionClick: (NotificationItemUi, String) -> Unit = { _, _ -> },
    onToggleService: (String) -> Unit = {},
    onCategoryToggle: (NotificationCategory, Boolean) -> Unit = { _, _ -> },
    onSettingChange: (String, Boolean) -> Unit = { _, _ -> },
    onClearHistory: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onShortcutClick: (String) -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    modifier: Modifier = Modifier
) {
    var isSearching by remember { mutableStateOf(false) }

    val filteredNotifications = remember(state.notifications, state.searchQuery, state.selectedCategory) {
        state.notifications.filter { item ->
            val matchesCategory = state.selectedCategory == NotificationCategory.ALL || item.category == state.selectedCategory
            val matchesSearch = state.searchQuery.isBlank() ||
                    item.title.contains(state.searchQuery, ignoreCase = true) ||
                    item.message.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val unreadCount = remember(state.notifications) {
        state.notifications.count { !it.isRead }
    }

    MeshScreen(
        modifier = modifier,
        topBar = {
            NotificationTopBar(
                unreadCount = unreadCount,
                isMeshConnected = state.meshStatus.isConnected,
                onBackClick = onBackClick,
                onSearchClick = { isSearching = !isSearching },
                onMarkAllReadClick = {
                    state.notifications.forEach { onNotificationClick(it.copy(isRead = true)) }
                },
                onSettingsClick = { /* Scroll to settings or navigate */ }
            )
        }
    ) { paddingValues ->
        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Expanded -> {
                // Expanded Master-Detail Layout (3 columns: Categories | Feed | Details & Services)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = MeshSpacing.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        NotificationCategoriesGrid(
                            categories = state.categories,
                            onCategoryClick = onCategorySelected,
                            onCategoryToggle = onCategoryToggle
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        NotificationSearchBar(
                            query = state.searchQuery,
                            onQueryChange = onQueryChange,
                            onClearQuery = onClearQuery
                        )
                        NotificationFilterChips(
                            selectedCategory = state.selectedCategory,
                            onCategorySelected = onCategorySelected,
                            categories = state.categories
                        )

                        if (filteredNotifications.isEmpty()) {
                            NotificationEmptyState(onRefresh = onRefresh)
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                            ) {
                                item {
                                    NotificationFeedSection(
                                        notifications = filteredNotifications,
                                        onNotificationClick = onNotificationClick,
                                        onQuickActionClick = onQuickActionClick
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        MeshStatusCard(meshStatus = state.meshStatus)
                        BackgroundServicesDashboard(
                            services = state.backgroundServices,
                            onToggleService = onToggleService
                        )
                    }
                }
            }

            WindowWidthSizeClass.Medium -> {
                // Medium Split Layout (Notifications | System & Details)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = MeshSpacing.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        NotificationFilterChips(
                            selectedCategory = state.selectedCategory,
                            onCategorySelected = onCategorySelected,
                            categories = state.categories
                        )
                        NotificationFeedSection(
                            notifications = filteredNotifications,
                            onNotificationClick = onNotificationClick,
                            onQuickActionClick = onQuickActionClick
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                    ) {
                        MeshStatusCard(meshStatus = state.meshStatus)
                        BackgroundServicesDashboard(
                            services = state.backgroundServices,
                            onToggleService = onToggleService
                        )
                    }
                }
            }

            else -> {
                // Compact Single-Column Layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = MeshSpacing.ScreenPadding),
                    contentPadding = PaddingValues(
                        top = MeshSpacing.CardSpacing,
                        bottom = MeshSpacing.ListBottomSpacing
                    ),
                    verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
                ) {
                    item {
                        AnimatedVisibility(
                            visible = isSearching || state.searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            NotificationSearchBar(
                                query = state.searchQuery,
                                onQueryChange = onQueryChange,
                                onClearQuery = onClearQuery
                            )
                        }
                    }

                    item {
                        NotificationFilterChips(
                            selectedCategory = state.selectedCategory,
                            onCategorySelected = onCategorySelected,
                            categories = state.categories
                        )
                    }

                    if (state.priorityAlerts.any { !it.isDismissed }) {
                        item {
                            PriorityAlertsSection(
                                alerts = state.priorityAlerts,
                                onDismissAlert = onDismissAlert,
                                onAlertAction = onAlertAction
                            )
                        }
                    }

                    item {
                        MeshStatusCard(meshStatus = state.meshStatus)
                    }

                    item {
                        BackgroundServicesDashboard(
                            services = state.backgroundServices,
                            onToggleService = onToggleService
                        )
                    }

                    if (filteredNotifications.isEmpty()) {
                        item {
                            NotificationEmptyState(onRefresh = onRefresh)
                        }
                    } else {
                        item {
                            NotificationFeedSection(
                                notifications = filteredNotifications,
                                onNotificationClick = onNotificationClick,
                                onQuickActionClick = onQuickActionClick
                            )
                        }
                    }

                    item {
                        com.meshlink.ui.system.SystemWidgetsPreviewSection()
                    }

                    item {
                        com.meshlink.ui.system.AppShortcutsSection(onShortcutClick = onShortcutClick)
                    }

                    item {
                        NotificationHistorySection(
                            historyItems = state.historyItems,
                            onClearHistory = onClearHistory
                        )
                    }

                    item {
                        NotificationSettingsSection(
                            settings = state.settings,
                            onSettingChange = onSettingChange
                        )
                    }
                }
            }
        }
    }
}
