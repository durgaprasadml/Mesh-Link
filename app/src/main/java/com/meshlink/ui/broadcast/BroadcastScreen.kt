package com.meshlink.ui.broadcast

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.ui.designsystem.theme.MeshTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class BroadcastUiState(
    val messages: List<Message> = emptyList()
)

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val getBroadcastMessagesUseCase: com.meshlink.domain.usecase.messaging.GetBroadcastMessagesUseCase
) : ViewModel() {

    fun sendBroadcast(message: String) {
        viewModelScope.launch {
            meshRepository.broadcastMessage(message)
        }
    }

    // FIX ERROR 3: Expose live broadcast messages from Room
    val uiState: StateFlow<BroadcastUiState> =
        getBroadcastMessagesUseCase()
            .map { BroadcastUiState(messages = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BroadcastUiState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val maxChars = 500
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll to top (newest) when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                title = {
                    Column {
                        Text("Broadcast", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("All nearby devices", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = MeshTheme.elevation.level1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MeshTheme.spacing.medium)
                        .navigationBarsPadding()
                ) {
                    // Info pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MeshTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.mediumSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(MeshTheme.spacing.mediumLarge))
                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                        Text(
                            "Message will be sent to all nearby devices",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))

                    Row(verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { if (it.length <= maxChars) messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a broadcast...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = MeshTheme.shapes.large,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))

                        IconButton(
                            onClick = {
                                val msg = messageText.trim()
                                if (msg.isNotBlank()) {
                                    viewModel.sendBroadcast(msg)
                                    messageText = ""
                                }
                            },
                            modifier = Modifier
                                .size(MeshTheme.spacing.giant)
                                .clip(MeshTheme.shapes.pill)
                                .background(if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${messageText.length}/$maxChars",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End).padding(top = MeshTheme.spacing.extraSmall)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📡", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
                    Text("No broadcasts yet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                    Text("Messages you send will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.mediumSmall),
                verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.mediumSmall),
                reverseLayout = false
            ) {
                items(uiState.messages, key = { it.messageId }) { msg ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        BroadcastBubble(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastBubble(msg: Message) {
    val isMe = msg.isFromMe
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    
    val shape = if (isMe) {
        RoundedCornerShape(
            topStart = MeshTheme.spacing.mediumLarge, 
            topEnd = MeshTheme.spacing.mediumLarge, 
            bottomStart = MeshTheme.spacing.mediumLarge, 
            bottomEnd = MeshTheme.spacing.small
        )
    } else {
        RoundedCornerShape(
            topStart = MeshTheme.spacing.mediumLarge, 
            topEnd = MeshTheme.spacing.mediumLarge, 
            bottomStart = MeshTheme.spacing.small, 
            bottomEnd = MeshTheme.spacing.mediumLarge
        )
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.mediumSmall)
        ) {
            if (!isMe) {
                Text(
                    text = msg.senderId.takeLast(8),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            }
            Text(text = msg.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                color = textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
