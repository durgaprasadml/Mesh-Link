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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.meshlink.domain.model.UserIdentity
import com.meshlink.ui.profile.AvatarAssets

/**
 * Shared UserAvatar composable - Single Source of Truth for avatar rendering across Mesh Link.
 *
 * Resolution Order:
 * 1. Gallery Image (Highest priority)
 * 2. Camera Image (If gallery image unavailable)
 * 3. Preset Avatar (If no custom image)
 * 4. Generated Initial (Final fallback only)
 * 5. Unknown User Placeholder Icon (Only if no identity exists)
 */
@Composable
fun UserAvatar(
    identity: UserIdentity?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescriptionText: String? = null
) {
    val description = contentDescriptionText
        ?: if (!identity?.displayName.isNullOrBlank()) "Profile picture of ${identity?.displayName}" else "User profile picture"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        val galleryUri = identity?.galleryImageUri
        val cameraUri = identity?.cameraImageUri
        val avatarResId = identity?.avatarResource

        when {
            // 1. Gallery image (Highest priority)
            !galleryUri.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(galleryUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 2. Camera image (If gallery image unavailable)
            !cameraUri.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cameraUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 3. Preset Avatar (If no custom image)
            avatarResId != null -> {
                Image(
                    painter = painterResource(id = avatarResId),
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 4. Generated Initial (Final fallback only)
            !identity?.initial.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = identity?.initial ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = (size.value * 0.45f).sp
                    )
                }
            }
            // 5. Unknown User Placeholder Icon (Only if no identity exists)
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = description,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Legacy compatibility wrapper for UserAvatarImage.
 */
@Composable
fun UserAvatarImage(
    avatarUri: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescriptionText: String? = null
) {
    val identity = UserIdentity.create(
        userId = displayName ?: "",
        displayName = displayName ?: "",
        avatarUri = avatarUri
    )
    UserAvatar(
        identity = identity,
        modifier = modifier,
        size = size,
        contentDescriptionText = contentDescriptionText
    )
}
