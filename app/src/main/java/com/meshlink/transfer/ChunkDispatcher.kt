package com.meshlink.transfer

import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import com.meshlink.di.IoDispatcher
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.routing.engine.TransportDiagnostics
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Dispatcher responsible for issuing chunk preparation and transmission tasks
 * to the ParallelTransferWorkerPool while strictly enforcing sliding window bounds.
 */
@Singleton
class ChunkDispatcher @Inject constructor(
    private val chunkManager: ChunkManager,
    private val slidingWindowManager: SlidingWindowManager,
    private val workerPool: ParallelTransferWorkerPool,
    private val runtimeStateRegistry: TransferRuntimeStateRegistry,
    private val diagnostics: TransportDiagnostics,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "ChunkDispatcher"
    }

    // Prevents issuing redundant parallel tasks for the same chunk index
    private val inDispatchChunks = ConcurrentHashMap<String, MutableSet<Int>>()

    fun dispatchAvailableChunks(
        session: TransferSession,
        file: File,
        onSendPacket: (suspend (MeshPacket) -> Unit)?
    ) {
        val transferId = session.transferId
        val sendableIndices = slidingWindowManager.getNextSendableIndices(transferId)

        if (sendableIndices.isEmpty()) {
            return
        }

        val inDispatch = inDispatchChunks.computeIfAbsent(transferId) { ConcurrentHashMap.newKeySet() }

        for (chunkIndex in sendableIndices) {
            if (!inDispatch.add(chunkIndex)) {
                continue // Already being processed by a worker
            }

            workerPool.submitChunkTask {
                try {
                    processAndSendChunk(session, file, chunkIndex, onSendPacket)
                } finally {
                    inDispatch.remove(chunkIndex)
                }
            }
        }
    }

    private suspend fun processAndSendChunk(
        session: TransferSession,
        file: File,
        chunkIndex: Int,
        onSendPacket: (suspend (MeshPacket) -> Unit)?
    ) = withContext(ioDispatcher) {
        val transferId = session.transferId
        val runtimeState = runtimeStateRegistry.getState(transferId)

        // Double check ACK status before reading disk
        if (runtimeState?.isChunkAcked(chunkIndex) == true) {
            return@withContext
        }

        val chunkSize = chunkManager.calculateChunkSize(session.transportUsed)
        val chunkBytes = chunkManager.readChunkFromFile(file, chunkIndex, chunkSize)

        if (chunkBytes == null) {
            MeshLogger.e(TAG, "Failed to read chunk $chunkIndex for $transferId from ${file.absolutePath}")
            return@withContext
        }

        try {
            val b64Payload = encodeBase64(chunkBytes)
            val packet = MeshPacket(
                senderId = session.senderId,
                targetId = session.targetId,
                transferId = transferId,
                payload = b64Payload,
                type = PacketType.MEDIA_CHUNK,
                chunkIndex = chunkIndex,
                totalChunks = session.totalChunks,
                mimeType = session.mimeType,
                ttl = 10
            )

            // Register in sliding window buffer and runtime state timestamp
            slidingWindowManager.addUnacknowledgedChunk(chunkIndex, packet)

            // Dispatch packet to transport layer
            onSendPacket?.invoke(packet)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error encoding/sending chunk $chunkIndex for $transferId: ${e.message}", e)
        } finally {
            BufferPool.returnBuffer(chunkBytes)
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            // JVM unit test fallback where android.util.Base64 is not mocked
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    fun clearSession(transferId: String) {
        inDispatchChunks.remove(transferId)
    }

    fun clearAll() {
        inDispatchChunks.clear()
    }
}
