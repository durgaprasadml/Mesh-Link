package com.meshlink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val meshId: String,
    val name: String,
    val phoneNumber: String,
    val pinHash: String // We don't store plain PIN for security, even locally
)
