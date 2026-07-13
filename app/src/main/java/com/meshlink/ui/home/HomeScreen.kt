package com.meshlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.domain.model.Chat
import com.meshlink.messaging.presentation.ChatsListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DarkBackground = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val PrimaryNeonGreen = Color(0xFF00FF88)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val BannerConnected = Color(0xFF00FF88).copy(alpha = 0.15f)
val BannerSearching = Color(0xFFFFC107).copy(alpha = 0.15f)
val BannerNoDevices = Color(0xFFFF5252).copy(alpha = 0.15f)

enum class ConnectionState {
    CONNECTED, SEARCHING, NO_DEVICES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToMeshDebug: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val chatsViewModel: ChatsListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatsState by chatsViewModel.uiState.collectAsStateWithLifecycle()
    val connectionState = when {
        uiState.nearbyDevices.isNotEmpty() -> ConnectionState.CONNECTED
        else -> ConnectionState.SEARCHING
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mesh Link",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToMeshDebug) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Network Status",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToBroadcast) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Broadcast Message",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNearby,
                containerColor = PrimaryNeonGreen,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ConnectionBanner(state = connectionState)
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                if (chatsState.chats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent chats.\nTap + to find nearby devices.",
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                items(chatsState.chats, key = { it.id }) { chat ->
                    ChatItem(
                        chat = chat,
                        onClick = {
                            val safeName = chat.name.ifBlank { chat.id.takeLast(8) }
                            onNavigateToChat(chat.id, safeName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionBanner(state: ConnectionState) {
    val (backgroundColor, dotColor, text) = when (state) {
        ConnectionState.CONNECTED -> Triple(BannerConnected, PrimaryNeonGreen, "Connected to nearby devices")
        ConnectionState.SEARCHING -> Triple(BannerSearching, Color(0xFFFFC107), "Searching for devices...")
        ConnectionState.NO_DEVICES -> Triple(BannerNoDevices, Color(0xFFFF5252), "No devices found")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = dotColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            val displayInitial = chat.name.firstOrNull()?.toString()?.uppercase() ?: "?"
            Text(
                text = displayInitial,
                color = PrimaryNeonGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Message Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.name.ifBlank { chat.id.takeLast(8) },
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage ?: "No messages yet",
                color = if (chat.unreadCount > 0) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Timestamp & Status
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatTime(chat.lastMessageAt),
                color = if (chat.unreadCount > 0) PrimaryNeonGreen else TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(PrimaryNeonGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = DarkBackground,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    if (timeInMillis == 0L) return ""
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}
