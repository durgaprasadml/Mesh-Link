package com.meshlink.ui.components.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.meshlink.ui.designsystem.theme.LocalMeshAnimations
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.util.rememberHapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageComposer(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    isRecording: Boolean,
    recordingElapsedMs: Long,
    onStartRecording: () -> Unit,
    onStopRecordingAndSend: () -> Unit,
    onCancelRecording: () -> Unit,
    onSendText: (String) -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = rememberHapticManager()
    val meshAnimations = LocalMeshAnimations.current
    
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            hapticManager.performHeavyClick()
            onStartRecording()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = MeshTheme.elevation.level2
    ) {
        if (isRecording) {
            // Recording UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeshTheme.spacing.mediumLarge, vertical = MeshTheme.spacing.medium)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(MeshTheme.spacing.medium)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
                Text(
                    text = "Recording... ${recordingElapsedMs / 1000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Recording voice note. ${recordingElapsedMs / 1000} seconds elapsed." }
                )
                IconButton(onClick = {
                    hapticManager.performHeavyClick()
                    onCancelRecording()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel recording", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = {
                        hapticManager.performSuccess()
                        onStopRecordingAndSend()
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send voice note", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        } else {
            // Text Input UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = {
                        hapticManager.performLightClick()
                        onAttachClick()
                    },
                    modifier = Modifier.padding(bottom = MeshTheme.spacing.small)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                TextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MeshTheme.spacing.small)
                        .semantics { contentDescription = "Message input field" },
                    placeholder = { Text("Message") },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(MeshTheme.spacing.extraLarge),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.width(MeshTheme.spacing.small))

                AnimatedContent(
                    targetState = inputText.isNotBlank(),
                    transitionSpec = {
                        fadeIn(meshAnimations.fastTransition) togetherWith fadeOut(meshAnimations.fastTransition) using SizeTransform(clip = false)
                    },
                    label = "SendButtonAnimation"
                ) { isTyping ->
                    if (isTyping) {
                        IconButton(
                            onClick = { 
                                hapticManager.performLightClick()
                                onSendText(inputText) 
                            },
                            modifier = Modifier
                                .padding(bottom = MeshTheme.spacing.small)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (hasAudioPermission) {
                                    hapticManager.performHeavyClick()
                                    onStartRecording()
                                } else {
                                    audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .padding(bottom = MeshTheme.spacing.small)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Record voice note", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}
