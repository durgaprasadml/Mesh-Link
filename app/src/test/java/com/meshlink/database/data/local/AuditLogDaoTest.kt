package com.meshlink.database.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuditLogDaoTest {

    private lateinit var database: MeshDatabase
    private lateinit var auditLogDao: AuditLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MeshDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        auditLogDao = database.auditLogDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and retrieve logs ordered by timestamp desc`() = runTest {
        val log1 = AuditLogEntity(timestamp = 1000L, peerId = "peer_1", eventName = "event", severity = 1, details = "details", actionTaken = "action")
        val log2 = AuditLogEntity(timestamp = 2000L, peerId = "peer_1", eventName = "event", severity = 1, details = "details", actionTaken = "action")
        
        auditLogDao.insertAuditLog(log1)
        auditLogDao.insertAuditLog(log2)
        
        val logs = auditLogDao.getAllAuditLogs()
        assertEquals(2, logs.size)
        assertEquals(2000L, logs[0].timestamp) // Newest first
        assertEquals(1000L, logs[1].timestamp)
    }

    @Test
    fun `get audit log count`() = runTest {
        assertEquals(0, auditLogDao.getAuditLogCount())
        
        val log1 = AuditLogEntity(timestamp = 1000L, peerId = "peer_1", eventName = "event", severity = 1, details = "details", actionTaken = "action")
        auditLogDao.insertAuditLog(log1)
        
        assertEquals(1, auditLogDao.getAuditLogCount())
    }

    @Test
    fun `delete oldest logs`() = runTest {
        val log1 = AuditLogEntity(timestamp = 1000L, peerId = "peer_1", eventName = "event", severity = 1, details = "details", actionTaken = "action")
        val log2 = AuditLogEntity(timestamp = 2000L, peerId = "peer_1", eventName = "event", severity = 1, details = "details", actionTaken = "action")
        val log3 = AuditLogEntity(timestamp = 3000L, peerId = "peer_1", eventName = "event", severity = 1, details = "details", actionTaken = "action")
        
        auditLogDao.insertAuditLog(log1)
        auditLogDao.insertAuditLog(log2)
        auditLogDao.insertAuditLog(log3)
        
        assertEquals(3, auditLogDao.getAuditLogCount())
        
        // Delete the 2 oldest
        auditLogDao.deleteOldestLogs(2)
        
        val remaining = auditLogDao.getAllAuditLogs()
        assertEquals(1, remaining.size)
        // Only the newest should remain
        assertEquals(3000L, remaining[0].timestamp)
    }
}
