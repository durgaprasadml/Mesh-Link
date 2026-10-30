package com.meshlink.database.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration Tests verification.
 *
 * Verifies that all continuous migrations in MeshDatabaseMigrations.ALL_MIGRATIONS
 * cover schema versions from 1 up to current version 13, and that real SQLite databases
 * created on older schemas without newer columns migrate cleanly through Room validation
 * without losing existing rows (ML-AUDIT-004).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MigrationTest {

    @Test
    fun `test migration array continuity and completeness`() {
        val migrations = MeshDatabaseMigrations.ALL_MIGRATIONS
        assertEquals(12, migrations.size)
        
        var currentVersion = 1
        for (migration in migrations) {
            assertEquals(currentVersion, migration.startVersion)
            assertEquals(currentVersion + 1, migration.endVersion)
            currentVersion = migration.endVersion
        }
        assertEquals(13, currentVersion)
    }

    @Test
    fun `test upgrade from legacy v9 schema without mediaDurationMs or location columns preserves rows and passes Room validation (ML-AUDIT-004)`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_v9_to_v13.db"
        context.deleteDatabase(dbName)

        // 1. Create a v9 SQLite database with the minimal historical messages/users/relay_packets columns
        //    (missing mediaDurationMs, latitude, longitude, batteryPercent, mimeType, mediaWidth, etc.)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE `users` (`meshId` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `avatarUri` TEXT, `aboutMe` TEXT)")
                    db.execSQL("CREATE TABLE `chats` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `lastMessage` TEXT, `lastMessageAt` INTEGER NOT NULL, `unreadCount` INTEGER NOT NULL)")
                    db.execSQL("CREATE INDEX `index_chats_lastMessageAt` ON `chats` (`lastMessageAt`)")
                    // Legacy messages table without mimeType, mediaWidth, mediaHeight, mediaSize, mediaChecksum, thumbnailBase64, mediaDurationMs, latitude, longitude, batteryPercent
                    db.execSQL(
                        "CREATE TABLE `messages` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `messageId` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isFromMe` INTEGER NOT NULL, `status` TEXT NOT NULL, `messageType` TEXT NOT NULL, `mediaPath` TEXT)"
                    )
                    // Legacy relay_packets table without priority or broadcastType
                    db.execSQL(
                        "CREATE TABLE `relay_packets` (`packetId` TEXT NOT NULL PRIMARY KEY, `senderId` TEXT NOT NULL, `targetId` TEXT NOT NULL, `payload` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `expiryTimestamp` INTEGER NOT NULL, `ttl` INTEGER NOT NULL, `hopCount` INTEGER NOT NULL, `encrypted` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE `trust_table` (`peerId` TEXT NOT NULL PRIMARY KEY, `deviceUUID` TEXT, `fingerprint` TEXT, `firstSeen` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, `lastIPAddress` TEXT, `lastBLEAddress` TEXT, `keyVersion` INTEGER NOT NULL, `trustLevel` TEXT NOT NULL, `verificationStatus` TEXT NOT NULL, `trustScore` INTEGER NOT NULL, `identityHistory` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE `audit_log_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `peerId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `severity` INTEGER NOT NULL, `details` TEXT NOT NULL, `actionTaken` TEXT NOT NULL)"
                    )

                    // Insert seed rows that must survive migration
                    db.execSQL("INSERT INTO `users` (`meshId`, `name`, `avatarUri`, `aboutMe`) VALUES ('PEER_1', 'Alice', NULL, 'Hello mesh')")
                    db.execSQL("INSERT INTO `chats` (`id`, `name`, `lastMessage`, `lastMessageAt`, `unreadCount`) VALUES ('PEER_1', 'Alice', 'Legacy hello', 1000, 2)")
                    db.execSQL(
                        "INSERT INTO `messages` (`localId`, `messageId`, `chatId`, `senderId`, `text`, `timestamp`, `isFromMe`, `status`, `messageType`, `mediaPath`) VALUES (1, 'msg-legacy-1', 'PEER_1', 'PEER_1', 'Legacy hello', 1000, 0, 'DELIVERED', 'TEXT', NULL)"
                    )
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
            helper.writableDatabase.close()
        }

        // 2. Open with Room at current version (13) with ALL_MIGRATIONS (no destructive migration)
        val roomDb = Room.databaseBuilder(context, MeshDatabase::class.java, dbName)
            .addMigrations(*MeshDatabaseMigrations.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            // Triggers migration 9->10->11->12->13 and Room's full onValidateSchema check
            val migratedUser = roomDb.userDao.getUser("PEER_1")
            assertNotNull(migratedUser)
            assertEquals("Alice", migratedUser?.name)
            assertEquals("Hello mesh", migratedUser?.aboutMe)

            val migratedMsg = roomDb.chatDao.getMessageByUuid("msg-legacy-1")
            assertNotNull(migratedMsg)
            assertEquals("Legacy hello", migratedMsg?.text)
            assertEquals("PEER_1", migratedMsg?.chatId)
            assertEquals(DeliveryStatus.DELIVERED, migratedMsg?.status)

            // Verify new columns can also be written and queried after migration
            val newLocationMsg = MessageEntity(
                messageId = "msg-new-loc",
                chatId = "PEER_1",
                senderId = "PEER_1",
                text = "Location pin",
                timestamp = 2000L,
                isFromMe = false,
                status = DeliveryStatus.DELIVERED,
                messageType = MessageType.LOCATION,
                mediaDurationMs = 5000L,
                latitude = 12.9716,
                longitude = 77.5946,
                batteryPercent = 85
            )
            roomDb.chatDao.insertMessage(newLocationMsg)
            val fetchedLoc = roomDb.chatDao.getMessageByUuid("msg-new-loc")
            assertNotNull(fetchedLoc)
            assertEquals(5000L, fetchedLoc?.mediaDurationMs)
            assertEquals(12.9716, fetchedLoc?.latitude ?: 0.0, 0.0001)
            assertEquals(77.5946, fetchedLoc?.longitude ?: 0.0, 0.0001)
            assertEquals(85, fetchedLoc?.batteryPercent)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
