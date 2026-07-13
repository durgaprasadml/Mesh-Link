package com.meshlink.database.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, ChatEntity::class, MessageEntity::class, RelayPacketEntity::class, TrustEntity::class, AuditLogEntity::class], version = 8, exportSchema = false)
abstract class MeshDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val chatDao: ChatDao
    abstract val relayDao: RelayDao
    abstract val trustDao: TrustDao
    abstract val auditLogDao: AuditLogDao
}
