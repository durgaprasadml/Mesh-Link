package com.meshlink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String, // Chat specific Mesh ID
    val name: String,
    val lastMessage: String?,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)
