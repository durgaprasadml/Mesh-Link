package com.meshlink.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated typing indicator with support for direct and mesh relay typing states.
 * Follows Material 3 styling and supports TalkBack / reduced motion accessibility.
 */
@Composable
fun TypingIndicator(
    typingState: TypingState,
    modifier: Modifier = Modifier
) {
    val labelText = if (typingState.isViaRelay) {
        "${typingState.peerName.ifBlank { "Peer" }} typing via mesh"
    } else {
        "${typingState.peerName.ifBlank { "Peer" }} is typing..."
    }

    AnimatedVisibility(
        visible = typingState.isTyping,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp)
                .semantics { contentDescription = labelText }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                TypingDotsAnimation()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TypingDotsAnimation() {
    val pulseAlpha by ChatAnimations.rememberPulseAlpha()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dots = listOf(0, 1, 2)
        dots.forEach { index ->
            val scaleFactor = when (index) {
                0 -> pulseAlpha
                1 -> ((pulseAlpha + 0.3f) % 0.6f) + 0.4f
                else -> ((pulseAlpha + 0.6f) % 0.6f) + 0.4f
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(scaleFactor)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
