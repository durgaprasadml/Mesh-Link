package com.meshlink.messaging.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.domain.model.Chat
import com.meshlink.ui.components.EmptyState
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.meshlink.ui.components.chat.ChatRowItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: ChatsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            Box(modifier = Modifier.padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.small)) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onSearch = { isSearchActive = false },
                            expanded = isSearchActive,
                            onExpandedChange = { isSearchActive = it },
                            placeholder = { Text("Search conversations") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    },
                    expanded = isSearchActive,
                    onExpandedChange = { isSearchActive = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    // Could put real-time search results here, for now it filters the list below
                }
            }

            if (uiState.chats.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = if (uiState.searchQuery.isBlank()) "No Messages" else "No results found",
                    description = if (uiState.searchQuery.isBlank()) "You haven't started any conversations yet." else "Try a different search term.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = MeshTheme.spacing.medium)
                ) {
                    items(uiState.chats, key = { it.id }, contentType = { "chat_item" }) { chat ->
                        ChatRowItem(chat = chat, onClick = {
                            val safeName = chat.name.ifBlank { com.meshlink.util.MeshIdNormalizer.canonicalize(chat.id) }
                            onNavigateToChat(chat.id, safeName)
                        })
                    }
                }
            }
        }
    }
}
