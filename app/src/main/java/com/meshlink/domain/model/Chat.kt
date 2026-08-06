package com.meshlink.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String?,
    val lastMessageAt: Long,
    val unreadCount: Int = 0,
    val avatarUri: String? = null,
    val profilePhotoPath: String? = null
)
