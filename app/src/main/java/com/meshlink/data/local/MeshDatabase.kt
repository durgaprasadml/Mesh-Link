package com.meshlink.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, ChatEntity::class, MessageEntity::class, RelayPacketEntity::class], version = 7, exportSchema = false)
abstract class MeshDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val chatDao: ChatDao
    abstract val relayDao: RelayDao
}
