package com.meshlink.ui.broadcast

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
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
    var isOptionsExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isSendEnabled = text.trim().isNotBlank()
    val charCount = text.length

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = MeshTheme.elevation.level2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MeshTheme.spacing.medium)
        ) {
            // Priority & Mode Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MeshTheme.spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scope indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Broadcast scope",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SCOPE: ALL NEARBY NODES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Priority Badge selector trigger
                Surface(
                    shape = CircleShape,
                    color = Color(selectedPriority.containerColor),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(selectedPriority.badgeColor)
                    ),
                    modifier = Modifier
                        .scaleOnPress(0.95f)
                        .clickable { isOptionsExpanded = !isOptionsExpanded }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedPriority.isEmergency) {
                            EmergencyBeaconPulse(size = 8.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = selectedPriority.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(selectedPriority.badgeColor)
                        )
                    }
                }
            }

            // Expanded Priority Selection Drawer
            AnimatedVisibility(
                visible = isOptionsExpanded,
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
                                isOptionsExpanded = false
                            },
                            label = {
                                Text(
                                    priority.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                )
                            },
                            leadingIcon = if (priority.isEmergency) {
                                { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(priority.badgeColor)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(priority.containerColor),
                                selectedLabelColor = Color(priority.badgeColor)
                            )
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= maxChars) text = it },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Broadcast message input" },
                    placeholder = {
                        Text(
                            text = if (selectedPriority.isEmergency) "Broadcast EMERGENCY SOS to mesh..." else "Broadcast to all nearby devices...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(selectedPriority.badgeColor),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = Color(selectedPriority.badgeColor),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = MeshTheme.shapes.large,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                )

                Spacer(modifier = Modifier.width(MeshTheme.spacing.small))

                // Send Button with Ripple Animation
                Box(contentAlignment = Alignment.Center) {
                    BroadcastSendRipple(
                        modifier = Modifier.size(48.dp),
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
                            .size(44.dp)
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
                            tint = if (isSendEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Footer info: Char count & location toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MeshTheme.spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { includeLocation = !includeLocation },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Attach Location",
                            tint = if (includeLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (includeLocation) {
                        Text(
                            text = "GPS Attached",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Text(
                    text = "$charCount/$maxChars",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (charCount >= maxChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
