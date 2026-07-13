package com.meshlink.database.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    indices = [Index(value = ["lastMessageAt"])]
)
data class ChatEntity(
    @PrimaryKey val id: String, // Chat specific Mesh ID
    val name: String,
    val lastMessage: String?,
    val lastMessageAt: Long,
    val unreadCount: Int = 0
)
