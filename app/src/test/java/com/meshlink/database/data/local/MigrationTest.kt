package com.meshlink.database.data.local

import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration Tests verification.
 * 
 * Verifies that all continuous migrations in MeshDatabaseMigrations.ALL_MIGRATIONS
 * cover schema versions from 1 up to current version 11.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @Test
    fun `test migration array continuity and completeness`() {
        val migrations = MeshDatabaseMigrations.ALL_MIGRATIONS
        assertEquals(10, migrations.size)
        
        var currentVersion = 1
        for (migration in migrations) {
            assertEquals(currentVersion, migration.startVersion)
            assertEquals(currentVersion + 1, migration.endVersion)
            currentVersion = migration.endVersion
        }
        assertEquals(11, currentVersion)
    }
}
