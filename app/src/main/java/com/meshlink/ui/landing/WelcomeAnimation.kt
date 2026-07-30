package com.meshlink.ui.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal Material 3 Welcome overlay for first-time profile creation users.
 */
@Composable
fun WelcomeAnimation(
    displayName: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.padding(bottom = 96.dp)
        ) {
            Text(
                text = if (displayName.isNotBlank()) "Welcome to Mesh Link, $displayName" else "Welcome to Mesh Link",
                style = MaterialTheme.typography.titleMedium,
                color = AnimationConstants.SoftNeonGreenBright,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

