package com.meshlink.messaging.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.chat.ChatScreen
import com.meshlink.util.NotificationHelper
import java.io.File

/**
 * Presentation bridge connecting ChatDetailViewModel state and launchers to the redesigned
 * com.meshlink.ui.chat.ChatScreen component.
 */
@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val peerIdentity by viewModel.peerIdentity.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Media pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { viewModel.sendImage(it) }
        }
    }

    // Auto-mark chat as read when messages change
    LaunchedEffect(uiState.messages.lastOrNull()?.messageId) {
        if (uiState.messages.isNotEmpty()) {
            viewModel.markChatAsRead()
        }
    }

    // Bind current notification chat target
    DisposableEffect(viewModel.address) {
        NotificationHelper.setCurrentChatId(viewModel.address)
        onDispose {
            NotificationHelper.setCurrentChatId(null)
        }
    }

    ChatScreen(
        peerIdentity = peerIdentity,
        peerAddress = viewModel.address,
        fallbackName = viewModel.name,
        uiState = uiState,
        onBack = onBack,
        onSendMessage = { viewModel.sendMessage(it) },
        onSendImage = { viewModel.sendImage(it) },
        onSendLocation = { viewModel.sendLocation() },
        onStartRecording = { viewModel.startRecording() },
        onStopRecordingAndSend = { viewModel.stopRecordingAndSend() },
        onCancelRecording = { viewModel.cancelRecording() },
        onToggleMessageSelection = { viewModel.toggleMessageSelection(it) },
        onClearSelection = { viewModel.clearSelection() },
        onDeleteSelectedMessages = { viewModel.deleteSelectedMessages() },
        onDeleteChat = {
            viewModel.deleteChat()
            onBack()
        },
        onPlayVoice = { viewModel.playVoice(it) },
        onStopPlayback = { viewModel.stopPlayback() },
        onRetryTransfer = { viewModel.retryTransfer(it) },
        onOpenLocation = { lat, lng ->
            try {
                val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Location)")
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                }
            } catch (_: Exception) { /* No map application installed */ }
        },
        onLaunchGallery = {
            imagePickerLauncher.launch("image/*")
        },
        onLaunchCamera = {
            val dir = File(context.cacheDir, "images")
            dir.mkdirs()
            val tempFile = File(dir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    )
}
