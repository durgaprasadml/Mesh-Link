package com.meshlink.database.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log_table ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(auditLog: AuditLogEntity)

    @Query("SELECT COUNT(*) FROM audit_log_table")
    suspend fun getAuditLogCount(): Int

    @Query("DELETE FROM audit_log_table WHERE id IN (SELECT id FROM audit_log_table ORDER BY timestamp ASC LIMIT :limit)")
    suspend fun deleteOldestLogs(limit: Int)
}
