package com.meshlink.domain.model



data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String?,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)
