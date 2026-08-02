package com.meshlink.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.designsystem.theme.MeshTheme

@Preview(name = "Compact Header", showBackground = true)
@Composable
fun HomeHeroSectionPreview() {
    MeshTheme {
        HomeHeroSection(
            userIdentity = UserIdentity(
                userId = "node_alpha_123",
                displayName = "Durga",
                lastUpdated = System.currentTimeMillis()
            ),
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Search Bar & Quick Actions", showBackground = true)
@Composable
fun HomeSearchAndQuickActionsPreview() {
    MeshTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            HomeSearchBar(
                query = "",
                onQueryChange = {},
                onOpenOverlay = {}
            )
            QuickActionsSection(
                onNavigateToNearby = {},
                onNavigateToBroadcast = {},
                onNavigateToSos = {},
                nearbyCount = 3
            )
        }
    }
}

@Preview(name = "Recent Chat Row", showBackground = true)
@Composable
fun RecentChatRowPreview() {
    MeshTheme {
        RecentChatRow(
            chat = Chat(
                id = "node_alpha",
                name = "Alpha Operator",
                lastMessage = "Secured link established over BLE.",
                lastMessageAt = System.currentTimeMillis() - 120_000,
                unreadCount = 2
            ),
            onClick = {}
        )
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
fun RecentChatsEmptyStatePreview() {
    MeshTheme {
        RecentChatsEmptyState(
            searchQuery = "",
            onStartChatting = {}
        )
    }
}
