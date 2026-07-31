package com.meshlink.transfer.data

import com.meshlink.database.data.local.TransferDao
import com.meshlink.database.data.local.TransferEntity
import com.meshlink.di.IoDispatcher
import com.meshlink.transfer.TransferSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTransferRepository @Inject constructor(
    private val transferDao: TransferDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    val activeTransfers: Flow<List<TransferSession>> = transferDao.getActiveTransfers()
        .map { entities -> entities.map { it.toTransferSession() } }

    val allTransfers: Flow<List<TransferSession>> = transferDao.getAllTransfers()
        .map { entities -> entities.map { it.toTransferSession() } }

    suspend fun saveSession(session: TransferSession) = withContext(ioDispatcher) {
        val entity = TransferEntity.fromSession(session)
        transferDao.insertOrUpdateTransfer(entity)
    }

    suspend fun updateProgress(transferId: String, chunksTransferred: Int, bytesTransferred: Long) = withContext(ioDispatcher) {
        transferDao.updateProgress(transferId, chunksTransferred, bytesTransferred)
    }

    suspend fun updateState(transferId: String, state: String) = withContext(ioDispatcher) {
        transferDao.updateState(transferId, state)
    }

    suspend fun updateStateAndEndTime(transferId: String, state: String, endTimeMs: Long) = withContext(ioDispatcher) {
        transferDao.updateStateAndEndTime(transferId, state, endTimeMs)
    }

    suspend fun getSession(transferId: String): TransferSession? = withContext(ioDispatcher) {
        transferDao.getTransferById(transferId)?.toTransferSession()
    }

    suspend fun getPendingOrActiveSessions(): List<TransferSession> = withContext(ioDispatcher) {
        transferDao.getPendingOrActiveTransfers().map { it.toTransferSession() }
    }

    suspend fun deleteSession(transferId: String) = withContext(ioDispatcher) {
        transferDao.deleteTransfer(transferId)
    }

    suspend fun clearFinishedSessions() = withContext(ioDispatcher) {
        transferDao.clearFinishedTransfers()
    }
}
