package com.meshlink.database.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MeshDatabaseMigrations {

    private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex < 0) return false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun addColumnIfMissing(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        columnDefinition: String
    ) {
        if (!hasColumn(db, tableName, columnName)) {
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
        }
    }

    private fun ensureBaseTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `users` (`meshId` TEXT NOT NULL, `name` TEXT NOT NULL, `avatarUri` TEXT, `aboutMe` TEXT, PRIMARY KEY(`meshId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `lastMessage` TEXT, `lastMessageAt` INTEGER NOT NULL, `unreadCount` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chats_lastMessageAt` ON `chats` (`lastMessageAt`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `messages` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `messageId` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isFromMe` INTEGER NOT NULL, `status` TEXT NOT NULL, `messageType` TEXT NOT NULL, `mediaPath` TEXT, `mimeType` TEXT, `mediaWidth` INTEGER, `mediaHeight` INTEGER, `mediaSize` INTEGER, `mediaChecksum` TEXT, `thumbnailBase64` TEXT, `mediaDurationMs` INTEGER, `latitude` REAL, `longitude` REAL, `batteryPercent` INTEGER)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_messageId` ON `messages` (`messageId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_messages_chatId_timestamp` ON `messages` (`chatId`, `timestamp`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_messages_status` ON `messages` (`status`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `relay_packets` (`packetId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `targetId` TEXT NOT NULL, `payload` TEXT NOT NULL, `type` TEXT NOT NULL, `priority` TEXT NOT NULL DEFAULT 'NORMAL', `broadcastType` TEXT NOT NULL DEFAULT 'NONE', `timestamp` INTEGER NOT NULL, `expiryTimestamp` INTEGER NOT NULL, `ttl` INTEGER NOT NULL, `hopCount` INTEGER NOT NULL, `encrypted` INTEGER NOT NULL, `transferId` TEXT, `chunkIndex` INTEGER NOT NULL DEFAULT 0, `totalChunks` INTEGER NOT NULL DEFAULT 0, `mimeType` TEXT, PRIMARY KEY(`packetId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_relay_packets_targetId` ON `relay_packets` (`targetId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_relay_packets_expiryTimestamp` ON `relay_packets` (`expiryTimestamp`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `trust_table` (`peerId` TEXT NOT NULL, `deviceUUID` TEXT, `fingerprint` TEXT, `firstSeen` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, `lastIPAddress` TEXT, `lastBLEAddress` TEXT, `keyVersion` INTEGER NOT NULL, `trustLevel` TEXT NOT NULL, `verificationStatus` TEXT NOT NULL, `trustScore` INTEGER NOT NULL, `identityHistory` TEXT NOT NULL, PRIMARY KEY(`peerId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_trust_table_fingerprint` ON `trust_table` (`fingerprint`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `audit_log_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `peerId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `severity` INTEGER NOT NULL, `details` TEXT NOT NULL, `actionTaken` TEXT NOT NULL)"
        )
    }

    private fun ensureMessageColumnsAndIndices(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "messages", "mediaPath", "TEXT")
        addColumnIfMissing(db, "messages", "mimeType", "TEXT")
        addColumnIfMissing(db, "messages", "mediaWidth", "INTEGER")
        addColumnIfMissing(db, "messages", "mediaHeight", "INTEGER")
        addColumnIfMissing(db, "messages", "mediaSize", "INTEGER")
        addColumnIfMissing(db, "messages", "mediaChecksum", "TEXT")
        addColumnIfMissing(db, "messages", "thumbnailBase64", "TEXT")
        addColumnIfMissing(db, "messages", "mediaDurationMs", "INTEGER")
        addColumnIfMissing(db, "messages", "latitude", "REAL")
        addColumnIfMissing(db, "messages", "longitude", "REAL")
        addColumnIfMissing(db, "messages", "batteryPercent", "INTEGER")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_messageId` ON `messages` (`messageId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_timestamp` ON `messages` (`chatId`, `timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_status` ON `messages` (`status`)")
    }

    private fun ensureRelayColumnsAndIndices(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "relay_packets", "priority", "TEXT NOT NULL DEFAULT 'NORMAL'")
        addColumnIfMissing(db, "relay_packets", "broadcastType", "TEXT NOT NULL DEFAULT 'NONE'")
        addColumnIfMissing(db, "relay_packets", "transferId", "TEXT")
        addColumnIfMissing(db, "relay_packets", "chunkIndex", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "relay_packets", "totalChunks", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "relay_packets", "mimeType", "TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_relay_packets_targetId` ON `relay_packets` (`targetId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_relay_packets_expiryTimestamp` ON `relay_packets` (`expiryTimestamp`)")
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 2 schema update
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 3 schema update
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 4 schema update
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 5 schema update
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 6 schema update
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 7 schema update
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureBaseTables(db)
            ensureMessageColumnsAndIndices(db)
            ensureRelayColumnsAndIndices(db)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureBaseTables(db)
            addColumnIfMissing(db, "users", "avatarUri", "TEXT")
            addColumnIfMissing(db, "users", "aboutMe", "TEXT")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureBaseTables(db)
            ensureMessageColumnsAndIndices(db)
            ensureRelayColumnsAndIndices(db)
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureBaseTables(db)
            addColumnIfMissing(db, "users", "avatarUri", "TEXT")
            addColumnIfMissing(db, "users", "aboutMe", "TEXT")
            // SQLite doesn't support dropping columns easily, so we recreate the table.
            db.execSQL("CREATE TABLE users_new (meshId TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, avatarUri TEXT, aboutMe TEXT)")
            db.execSQL("INSERT INTO users_new (meshId, name, avatarUri, aboutMe) SELECT meshId, name, avatarUri, aboutMe FROM users")
            db.execSQL("DROP TABLE users")
            db.execSQL("ALTER TABLE users_new RENAME TO users")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureBaseTables(db)
            addColumnIfMissing(db, "users", "publicKey", "TEXT")
            addColumnIfMissing(db, "users", "lastSeen", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "rssi", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "trustLevel", "TEXT")
            addColumnIfMissing(db, "users", "capabilities", "INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureBaseTables(db)
            addColumnIfMissing(db, "users", "avatarUri", "TEXT")
            addColumnIfMissing(db, "users", "aboutMe", "TEXT")
            addColumnIfMissing(db, "users", "publicKey", "TEXT")
            addColumnIfMissing(db, "users", "lastSeen", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "rssi", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "trustLevel", "TEXT")
            addColumnIfMissing(db, "users", "capabilities", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "profilePhotoPath", "TEXT")
            addColumnIfMissing(db, "users", "profilePhotoHash", "TEXT")
            addColumnIfMissing(db, "users", "profilePhotoVersion", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "profileLastUpdated", "INTEGER NOT NULL DEFAULT 0")
            ensureMessageColumnsAndIndices(db)
            ensureRelayColumnsAndIndices(db)
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13
    )
}
