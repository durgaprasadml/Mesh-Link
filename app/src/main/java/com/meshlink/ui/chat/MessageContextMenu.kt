package com.meshlink.ui.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.Message

/**
 * Material 3 Context Dropdown Menu displayed upon long-pressing a message bubble.
 */
@Composable
fun MessageContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    message: Message,
    onReply: (Message) -> Unit = {},
    onCopy: (Message) -> Unit = {},
    onForward: (Message) -> Unit = {},
    onDelete: (Message) -> Unit = {},
    onShare: (Message) -> Unit = {},
    onInfo: (Message) -> Unit = {},
    onSave: (Message) -> Unit = {},
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text("Reply") },
            onClick = {
                onDismissRequest()
                onReply(message)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Reply, contentDescription = "Reply")
            }
        )
        if (message.text.isNotBlank()) {
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    onDismissRequest()
                    onCopy(message)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Forward") },
            onClick = {
                onDismissRequest()
                onForward(message)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Forward, contentDescription = "Forward")
            }
        )
        DropdownMenuItem(
            text = { Text("Share") },
            onClick = {
                onDismissRequest()
                onShare(message)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
            }
        )
        if (message.mediaPath != null) {
            DropdownMenuItem(
                text = { Text("Save") },
                onClick = {
                    onDismissRequest()
                    onSave(message)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save media")
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Message Info") },
            onClick = {
                onDismissRequest()
                onInfo(message)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Info")
            }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismissRequest()
                onDelete(message)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}
