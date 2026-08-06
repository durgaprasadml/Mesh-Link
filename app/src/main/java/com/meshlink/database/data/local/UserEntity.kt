package com.meshlink.database.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val meshId: String,
    val name: String,
    val avatarUri: String? = null,
    val aboutMe: String? = null,
    val publicKey: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val rssi: Int = 0,
    val trustLevel: String? = null,
    val capabilities: Byte = 0,
    val profilePhotoPath: String? = null,
    val profilePhotoHash: String? = null,
    val profilePhotoVersion: Long = 0L,
    val profileLastUpdated: Long = 0L
)
