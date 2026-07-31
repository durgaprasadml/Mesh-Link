package com.meshlink.database.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        RelayPacketEntity::class,
        TrustEntity::class,
        AuditLogEntity::class,
        TransferEntity::class
    ],
    version = 12,
    // exportSchema = true is required for Room's MigrationTestHelper and schema regression tests.
    // Schema JSON files are output to app/schemas/ (configured via room.schemaLocation KSP arg).
    exportSchema = true
)
abstract class MeshDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val chatDao: ChatDao
    abstract val relayDao: RelayDao
    abstract val trustDao: TrustDao
    abstract val auditLogDao: AuditLogDao
    abstract val transferDao: TransferDao
}
