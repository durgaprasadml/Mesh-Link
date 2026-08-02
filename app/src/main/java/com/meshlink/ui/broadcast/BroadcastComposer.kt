package com.meshlink.ui.broadcast

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.scaleOnPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastComposer(
    onSendBroadcast: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxChars: Int = 500
) {
    var text by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(BroadcastPriority.NORMAL) }
    var includeLocation by remember { mutableStateOf(false) }
    var isPrioritySelectorOpen by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isSendEnabled = text.trim().isNotBlank()
    val charCount = text.length

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = MeshTheme.elevation.level2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small)
        ) {
            // Priority Drawer Toggle & Active Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MeshTheme.spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scope Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Broadcast scope",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "COMMUNITY BROADCAST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Priority Badge Trigger
                PriorityChip(
                    priority = selectedPriority,
                    onClick = { isPrioritySelectorOpen = !isPrioritySelectorOpen },
                    compact = true
                )
            }

            // Expanded Priority Drawer
            AnimatedVisibility(
                visible = isPrioritySelectorOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = MeshTheme.spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.extraSmall)
                ) {
                    BroadcastPriority.values().forEach { priority ->
                        val isSelected = priority == selectedPriority
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPriority = priority
                                isPrioritySelectorOpen = false
                            },
                            label = {
                                Text(
                                    priority.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                )
                            },
                            leadingIcon = if (priority.isEmergency) {
                                {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(priority.badgeColor)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(priority.containerColor),
                                selectedLabelColor = Color(priority.badgeColor)
                            )
                        )
                    }
                }
            }

            // Multiline Input Field
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= maxChars) text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "What's happening broadcast input" },
                placeholder = {
                    Text(
                        text = if (selectedPriority.isEmergency) "Broadcast EMERGENCY SOS to mesh..." else "What's happening?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(selectedPriority.badgeColor),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    cursorColor = Color(selectedPriority.badgeColor),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = MeshTheme.shapes.large,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            Spacer(modifier = Modifier.height(MeshTheme.spacing.extraSmall))

            // Action Row: Attachments, Char Counter, Send Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachments Shortcuts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { includeLocation = !includeLocation },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Attach Location",
                            tint = if (includeLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Attachment shortcut */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (includeLocation) {
                        Text(
                            text = "Location Attached",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                // Character Counter & Send Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)
                ) {
                    Text(
                        text = "$charCount/$maxChars",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (charCount >= maxChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(contentAlignment = Alignment.Center) {
                        BroadcastSendRipple(
                            modifier = Modifier.size(44.dp),
                            isBroadcasting = isSendEnabled,
                            color = Color(selectedPriority.badgeColor)
                        )

                        IconButton(
                            onClick = {
                                val msgToSend = buildString {
                                    if (selectedPriority != BroadcastPriority.NORMAL) {
                                        append("[${selectedPriority.label.uppercase()}] ")
                                    }
                                    append(text.trim())
                                }
                                if (msgToSend.isNotBlank()) {
                                    onSendBroadcast(msgToSend)
                                    text = ""
                                    keyboardController?.hide()
                                }
                            },
                            enabled = isSendEnabled,
                            modifier = Modifier
                                .size(40.dp)
                                .scaleOnPress(0.92f)
                                .clip(CircleShape)
                                .background(
                                    if (isSendEnabled) Color(selectedPriority.badgeColor)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Broadcast",
                                tint = if (isSendEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
