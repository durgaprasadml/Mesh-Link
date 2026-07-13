package com.meshlink.domain.mapper

import com.meshlink.database.data.local.ChatEntity
import com.meshlink.domain.model.Chat

fun ChatEntity.toDomain(): Chat {
    return Chat(
        id = this.id,
        name = this.name,
        lastMessage = this.lastMessage,
        lastMessageAt = this.lastMessageAt,
        unreadCount = this.unreadCount
    )
}

fun Chat.toEntity(): ChatEntity {
    return ChatEntity(
        id = this.id,
        name = this.name,
        lastMessage = this.lastMessage,
        lastMessageAt = this.lastMessageAt,
        unreadCount = this.unreadCount
    )
}
