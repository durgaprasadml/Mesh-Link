package com.meshlink.database.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTransfer(entity: TransferEntity)

    @Query("UPDATE transfers SET chunksTransferred = :chunksTransferred, bytesTransferred = :bytesTransferred, lastUpdatedMs = :lastUpdatedMs WHERE transferId = :transferId")
    suspend fun updateProgress(transferId: String, chunksTransferred: Int, bytesTransferred: Long, lastUpdatedMs: Long = System.currentTimeMillis())

    @Query("UPDATE transfers SET state = :state, lastUpdatedMs = :lastUpdatedMs WHERE transferId = :transferId")
    suspend fun updateState(transferId: String, state: String, lastUpdatedMs: Long = System.currentTimeMillis())

    @Query("UPDATE transfers SET state = :state, endTimeMs = :endTimeMs, lastUpdatedMs = :lastUpdatedMs WHERE transferId = :transferId")
    suspend fun updateStateAndEndTime(transferId: String, state: String, endTimeMs: Long, lastUpdatedMs: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transfers WHERE transferId = :transferId")
    suspend fun getTransferById(transferId: String): TransferEntity?

    @Query("SELECT * FROM transfers WHERE state IN ('WAITING', 'PREPARING', 'COMPRESSING', 'SENDING', 'RECEIVING', 'VERIFYING', 'RESUMING') ORDER BY startTimeMs DESC")
    fun getActiveTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers ORDER BY startTimeMs DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE state IN ('WAITING', 'PREPARING', 'COMPRESSING', 'SENDING', 'RECEIVING', 'RESUMING', 'PAUSED')")
    suspend fun getPendingOrActiveTransfers(): List<TransferEntity>

    @Query("DELETE FROM transfers WHERE transferId = :transferId")
    suspend fun deleteTransfer(transferId: String)

    @Query("DELETE FROM transfers WHERE state IN ('COMPLETED', 'CANCELLED')")
    suspend fun clearFinishedTransfers()
}
