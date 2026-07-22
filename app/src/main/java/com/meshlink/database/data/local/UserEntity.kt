package com.meshlink.database.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val meshId: String,
    val name: String,
    val avatarUri: String? = null,
    val aboutMe: String? = null
)
