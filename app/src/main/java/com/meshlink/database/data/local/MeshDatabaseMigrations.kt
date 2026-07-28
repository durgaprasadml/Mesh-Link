package com.meshlink.database.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MeshDatabaseMigrations {
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
            // Version 8 schema update
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN avatarUri TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN aboutMe TEXT")
        }
    }

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

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // SQLite doesn't support dropping columns easily, so we recreate the table.
            db.execSQL("CREATE TABLE users_new (meshId TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, avatarUri TEXT, aboutMe TEXT)")
            db.execSQL("INSERT INTO users_new (meshId, name, avatarUri, aboutMe) SELECT meshId, name, avatarUri, aboutMe FROM users")
            db.execSQL("DROP TABLE users")
            db.execSQL("ALTER TABLE users_new RENAME TO users")
        }
    }
}
