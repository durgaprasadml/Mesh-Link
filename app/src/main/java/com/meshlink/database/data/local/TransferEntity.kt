package com.meshlink.database.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meshlink.transfer.TransferDirection
import com.meshlink.transfer.TransferPriority
import com.meshlink.transfer.TransferSession
import com.meshlink.transfer.TransferState
import com.meshlink.transfer.TransportType

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val transferId: String,
    val senderId: String,
    val targetId: String,
    val fileName: String,
    val mimeType: String,
    val totalBytes: Long,
    val totalChunks: Int,
    val chunksTransferred: Int = 0,
    val bytesTransferred: Long = 0L,
    val direction: String,
    val state: String,
    val priority: String,
    val transportUsed: String,
    val sha256Checksum: String? = null,
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long = 0L,
    val lastUpdatedMs: Long = System.currentTimeMillis(),
    val retries: Int = 0,
    val filePath: String? = null,
    val compressionType: String = "NONE",
    val compressedSize: Long = totalBytes
) {
    fun toTransferSession(): TransferSession {
        return TransferSession(
            transferId = transferId,
            senderId = senderId,
            targetId = targetId,
            fileName = fileName,
            mimeType = mimeType,
            totalBytes = totalBytes,
            totalChunks = totalChunks,
            direction = try { TransferDirection.valueOf(direction) } catch (e: Exception) { TransferDirection.OUTGOING },
            state = try { TransferState.valueOf(state) } catch (e: Exception) { TransferState.WAITING },
            priority = try { TransferPriority.valueOf(priority) } catch (e: Exception) { TransferPriority.MEDIUM },
            transportUsed = try { TransportType.valueOf(transportUsed) } catch (e: Exception) { TransportType.UNKNOWN },
            bytesTransferred = bytesTransferred,
            chunksTransferred = chunksTransferred,
            sha256Checksum = sha256Checksum,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            lastUpdatedMs = lastUpdatedMs,
            retries = retries,
            filePath = filePath,
            compressionType = compressionType,
            compressedSize = compressedSize
        )
    }

    companion object {
        fun fromSession(session: TransferSession): TransferEntity {
            return TransferEntity(
                transferId = session.transferId,
                senderId = session.senderId,
                targetId = session.targetId,
                fileName = session.fileName,
                mimeType = session.mimeType,
                totalBytes = session.totalBytes,
                totalChunks = session.totalChunks,
                chunksTransferred = session.chunksTransferred,
                bytesTransferred = session.bytesTransferred,
                direction = session.direction.name,
                state = session.state.name,
                priority = session.priority.name,
                transportUsed = session.transportUsed.name,
                sha256Checksum = session.sha256Checksum,
                startTimeMs = session.startTimeMs,
                endTimeMs = session.endTimeMs,
                lastUpdatedMs = session.lastUpdatedMs,
                retries = session.retries,
                filePath = session.filePath,
                compressionType = session.compressionType,
                compressedSize = session.compressedSize
            )
        }
    }
}
