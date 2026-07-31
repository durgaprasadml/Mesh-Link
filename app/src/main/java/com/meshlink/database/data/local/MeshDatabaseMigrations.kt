package com.meshlink.database.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All Room database migrations for MeshLink.
 *
 * Migration naming convention: MIGRATION_<from>_<to>
 *
 * EMPTY MIGRATIONS (1→2 through 7→8):
 * ─────────────────────────────────────────────────────────────────────────────
 * These migrations are intentional no-ops. During pre-release development, the
 * database version was bumped to invalidate stale development builds and force a
 * clean install, without any actual schema changes being needed at that point.
 *
 * An empty migrate() body is valid in Room — it instructs the framework that the
 * schema is identical between the two versions. Room's migration graph still
 * requires the migration object to be registered to avoid an IllegalStateException
 * on upgrade from those historical versions.
 *
 * DO NOT remove these empty migrations — devices that were on internal/beta builds
 * at versions 1–8 require a migration path to reach version 12.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * SCHEMA-CHANGING MIGRATIONS (8→9 and beyond):
 * See each migration body for the specific DDL changes applied.
 */
object MeshDatabaseMigrations {

    // ────────── No-op development-cycle version bumps (v1 – v8) ──────────

    /** No schema change — version bumped to invalidate stale dev builds at v1. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }

    /** No schema change — version bumped to invalidate stale dev builds at v2. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }

    /** No schema change — version bumped to invalidate stale dev builds at v3. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }

    /** No schema change — version bumped to invalidate stale dev builds at v4. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }

    /** No schema change — version bumped to invalidate stale dev builds at v5. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }

    /** No schema change — version bumped to invalidate stale dev builds at v6. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }

    /** No schema change — version bumped to invalidate stale dev builds at v7. */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }

    // ────────── Schema-changing migrations (v8 → v12) ──────────

    /** v8→v9: Adds avatarUri and aboutMe columns to the users table. */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN avatarUri TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN aboutMe TEXT")
        }
    }

    /** v9→v10: Adds media message support columns to the messages table. */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN mimeType TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN mediaWidth INTEGER")
            db.execSQL("ALTER TABLE messages ADD COLUMN mediaHeight INTEGER")
            db.execSQL("ALTER TABLE messages ADD COLUMN mediaSize INTEGER")
            db.execSQL("ALTER TABLE messages ADD COLUMN mediaChecksum TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN thumbnailBase64 TEXT")
        }
    }

    /**
     * v10→v11: Rebuilds the users table to enforce meshId as a proper SQLite PRIMARY KEY
     * (SQLite requires a full table rebuild to add/change primary key constraints).
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE users_new (meshId TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, avatarUri TEXT, aboutMe TEXT)")
            db.execSQL("INSERT INTO users_new (meshId, name, avatarUri, aboutMe) SELECT meshId, name, avatarUri, aboutMe FROM users")
            db.execSQL("DROP TABLE users")
            db.execSQL("ALTER TABLE users_new RENAME TO users")
        }
    }

    /**
     * v11→v12: Creates the 'transfers' table for tracking active and historical
     * media transfer sessions (MediaTransferSessionManager persistence layer).
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transfers (
                    transferId TEXT NOT NULL PRIMARY KEY,
                    senderId TEXT NOT NULL,
                    targetId TEXT NOT NULL,
                    fileName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    totalBytes INTEGER NOT NULL,
                    totalChunks INTEGER NOT NULL,
                    chunksTransferred INTEGER NOT NULL,
                    bytesTransferred INTEGER NOT NULL,
                    direction TEXT NOT NULL,
                    state TEXT NOT NULL,
                    priority TEXT NOT NULL,
                    transportUsed TEXT NOT NULL,
                    sha256Checksum TEXT,
                    startTimeMs INTEGER NOT NULL,
                    endTimeMs INTEGER NOT NULL,
                    lastUpdatedMs INTEGER NOT NULL,
                    retries INTEGER NOT NULL,
                    filePath TEXT,
                    compressionType TEXT NOT NULL,
                    compressedSize INTEGER NOT NULL
                )
                """.trimIndent()
            )
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
        MIGRATION_11_12
    )
}
