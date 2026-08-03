package com.meshlink.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.meshlink.ui.profile.AvatarAssets

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

@Composable
fun UserAvatarImage(
    avatarUri: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescriptionText: String? = null
) {
    val description = remember(contentDescriptionText, displayName) {
        contentDescriptionText
            ?: if (!displayName.isNullOrBlank()) "Profile picture of $displayName" else "User profile picture"
    }

    val avatarResId = remember(avatarUri) { AvatarAssets.getAvatarResId(avatarUri) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        when {
            avatarResId != null -> {
                Image(
                    painter = painterResource(id = avatarResId),
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !avatarUri.isNullOrBlank() -> {
                val imageRequest = remember(avatarUri, context) {
                    ImageRequest.Builder(context)
                        .data(avatarUri)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = displayName?.trim()?.firstOrNull()?.uppercaseChar()
                    if (initial != null) {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = (size.value * 0.45f).sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = description,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(size * 0.6f)
                        )
                    }
                }
            }
        }
    }
}
