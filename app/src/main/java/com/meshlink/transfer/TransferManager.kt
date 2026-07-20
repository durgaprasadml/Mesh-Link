package com.meshlink.transfer

import android.content.Context
import android.util.Base64
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.IoDispatcher
import com.meshlink.routing.engine.IntelligentTransportManager
import com.meshlink.routing.engine.RouteType
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class TransferManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val scheduler: TransferScheduler,
    private val cache: TransferCache,
    private val chunkManager: ChunkManager,
    private val metaManager: FileMetadataManager,
    private val verifier: IntegrityVerifier,
    private val analytics: TransferAnalytics,
    private val intelligentTransportManager: IntelligentTransportManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "TransferManager"
        private const val INTER_CHUNK_DELAY_MS = 30L
        private const val TRANSFER_TIMEOUT_MS = 120_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    var onSendPacket: ((MeshPacket) -> Unit)? = null
    var onTransferCompleted: ((TransferSession) -> Unit)? = null

    // ─────────────────── Initialization ───────────────────

    init {
        scope.launch {
            val persisted = cache.loadPersistedSessions()
            for (session in persisted) {
                if (session.state == TransferState.SENDING || session.state == TransferState.RECEIVING) {
                    session.state = TransferState.PAUSED
                }
                scheduler.addSession(session)
            }
        }
    }

    // ─────────────────── Sender ───────────────────

    fun sendFile(
        file: File,
        senderId: String,
        targetId: String,
        priority: TransferPriority = TransferPriority.MEDIUM,
        transferId: String = UUID.randomUUID().toString()
    ): String {
        if (!file.exists()) {
            MeshLogger.e(TAG, "Cannot send non-existent file: ${file.absolutePath}")
            return transferId
        }

        val routeType = intelligentTransportManager.selectTransportForPayload(targetId, PacketType.MEDIA_CHUNK, file.length())
        val transport = when (routeType) {
            RouteType.BLE -> TransportType.BLE
            RouteType.WIFI_DIRECT -> TransportType.WIFI_DIRECT
            RouteType.HYBRID -> TransportType.HYBRID
        }
        
        val mimeType = metaManager.getMimeTypeForFile(file)
        val checksum = verifier.calculateFileChecksum(file)
        val totalChunks = chunkManager.getTotalChunks(file.length(), transport)

        val session = TransferSession(
            transferId = transferId,
            senderId = senderId,
            targetId = targetId,
            fileName = file.name,
            mimeType = mimeType,
            totalBytes = file.length(),
            totalChunks = totalChunks,
            direction = TransferDirection.OUTGOING,
            priority = priority,
            transportUsed = transport,
            sha256Checksum = checksum,
            filePath = file.absolutePath,
            state = TransferState.WAITING,
            startTimeMs = System.currentTimeMillis()
        )

        scheduler.addSession(session)
        scope.launch { cache.persistSession(session) }
        analytics.recordTransferStarted(session)
        
        scope.launch {
            startOutgoingTransfer(session)
        }

        return transferId
    }

    private suspend fun startOutgoingTransfer(session: TransferSession) {
        val file = File(session.filePath!!)
        if (!file.exists()) {
            failSession(session.transferId, "Source file vanished")
            return
        }

        scheduler.updateSessionState(session.transferId, TransferState.SENDING)
        scope.launch { cache.persistSession(session) }

        // Send META
        val metaPayload = metaManager.generateMetaPayload(
            FileMetadata(session.fileName, session.mimeType, session.totalBytes, session.sha256Checksum)
        )
        sendPacket(
            session.senderId, session.targetId, session.transferId,
            metaPayload, PacketType.MEDIA_META, 0, session.totalChunks, session.mimeType
        )

        // Give receiver time to init cache
        delay(100L)

        // Send Chunks (respecting scheduler queue and QoS)
        var i = session.chunksTransferred
        while (i < session.totalChunks && scope.isActive) {
            
            // Check state (e.g. if paused/cancelled)
            val currentState = scheduler.getSession(session.transferId)?.state
            if (currentState != TransferState.SENDING) {
                MeshLogger.d(TAG, "Stopping outgoing loop for ${session.transferId}. State: $currentState")
                return
            }
            
            // Check scheduler if we are allowed to send (Congestion/Priority limits)
            if (!scheduler.canSendNextChunk(session.transferId)) {
                delay(50L) // Yield to higher priority transfers
                continue
            }

            val chunkSize = chunkManager.calculateChunkSize(session.transportUsed)
            val chunkBytes = chunkManager.readChunkFromFile(file, i, chunkSize)
            
            if (chunkBytes == null) {
                failSession(session.transferId, "Failed to read chunk $i from disk")
                return
            }

            val b64 = Base64.encodeToString(chunkBytes, Base64.NO_WRAP)
            
            sendPacket(
                session.senderId, session.targetId, session.transferId,
                b64, PacketType.MEDIA_CHUNK, i, session.totalChunks, session.mimeType
            )

            scheduler.updateSessionProgress(session.transferId, i + 1, (i + 1).toLong() * chunkSize)
            i++
            
            // Delay to prevent overwhelming BLE buffers
            if (session.transportUsed == TransportType.BLE) {
                delay(INTER_CHUNK_DELAY_MS)
            }
        }
    }

    // ─────────────────── Receiver ───────────────────

    fun handleIncomingPacket(packet: MeshPacket) {
        val transferId = packet.transferId ?: return
        
        scope.launch {
            when (packet.type) {
                PacketType.MEDIA_META -> handleMeta(packet, transferId)
                PacketType.MEDIA_CHUNK -> handleChunk(packet, transferId)
                PacketType.MEDIA_ACK -> handleAck(packet, transferId)
                PacketType.MEDIA_NACK -> handleNack(packet, transferId)
                else -> {}
            }
        }
    }

    private suspend fun handleMeta(packet: MeshPacket, transferId: String) {
        val meta = metaManager.parseMetaPayload(packet.payload)
        if (meta == null) {
            MeshLogger.w(TAG, "Invalid META payload for $transferId")
            return
        }

        if (!cache.initSessionCache(transferId)) {
            MeshLogger.e(TAG, "Failed to init cache for $transferId")
            return
        }

        val session = TransferSession(
            transferId = transferId,
            senderId = packet.senderId,
            targetId = packet.targetId,
            fileName = meta.fileName,
            mimeType = meta.mimeType,
            totalBytes = meta.totalBytes,
            totalChunks = packet.totalChunks,
            direction = TransferDirection.INCOMING,
            sha256Checksum = meta.sha256Checksum,
            state = TransferState.RECEIVING,
            startTimeMs = System.currentTimeMillis()
        )
        
        scheduler.addSession(session)
        scope.launch { cache.persistSession(session) }
        analytics.recordTransferStarted(session)
        
        // Start timeout monitor
        startTimeoutMonitor(transferId)
    }

    private suspend fun handleChunk(packet: MeshPacket, transferId: String) {
        var session = scheduler.getSession(transferId)
        
        // Handle late-joiners (META dropped, but chunks arrived)
        if (session == null) {
            val mime = packet.mimeType ?: "application/octet-stream"
            cache.initSessionCache(transferId)
            session = TransferSession(
                transferId = transferId,
                senderId = packet.senderId,
                targetId = packet.targetId,
                fileName = "recovered_${transferId}.${mime.substringAfter("/")}",
                mimeType = mime,
                totalBytes = 0L,
                totalChunks = packet.totalChunks,
                direction = TransferDirection.INCOMING,
                state = TransferState.RECEIVING,
                startTimeMs = System.currentTimeMillis()
            )
            scheduler.addSession(session)
            scope.launch { cache.persistSession(session) }
            startTimeoutMonitor(transferId)
        }
        
        val chunkBytes = try {
            Base64.decode(packet.payload, Base64.NO_WRAP)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Corrupt base64 in chunk ${packet.chunkIndex} for $transferId")
            return
        }

        val success = cache.writeChunk(transferId, packet.chunkIndex, chunkBytes)
        if (success) {
            val count = cache.getReceivedChunkIndices(transferId).size
            scheduler.updateSessionProgress(transferId, count, count.toLong() * chunkBytes.size)
            
            // Send ACK
            sendPacket(
                packet.targetId, packet.senderId, transferId, 
                packet.chunkIndex.toString(), PacketType.MEDIA_ACK, packet.chunkIndex, packet.totalChunks, session.mimeType
            )

            // Check completion
            if (count >= packet.totalChunks) {
                assembleAndVerify(session)
            }
        }
    }

    private suspend fun assembleAndVerify(session: TransferSession) {
        scheduler.updateSessionState(session.transferId, TransferState.VERIFYING)
        cache.persistSession(session)
        
        val mediaDir = File(context.filesDir, "mesh_media").also { if (!it.exists()) it.mkdirs() }
        val outputFile = File(mediaDir, session.fileName)
        
        val assembled = cache.assembleFile(session.transferId, session.totalChunks, outputFile)
        
        if (assembled) {
            if (verifier.verifyFileChecksum(outputFile, session.sha256Checksum)) {
                session.filePath = outputFile.absolutePath
                scheduler.updateSessionState(session.transferId, TransferState.COMPLETED)
                cache.persistSession(session)
                cache.cleanUpSession(session.transferId)
                analytics.recordTransferCompleted(session)
                onTransferCompleted?.invoke(session)
            } else {
                failSession(session.transferId, "Checksum verification failed")
                outputFile.delete()
            }
        } else {
            failSession(session.transferId, "File assembly failed")
        }
    }

    // ─────────────────── ACK / NACK / Timeouts ───────────────────

    private fun handleAck(packet: MeshPacket, transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.direction == TransferDirection.OUTGOING && packet.chunkIndex == session.totalChunks - 1) {
            scheduler.updateSessionState(transferId, TransferState.COMPLETED)
            scope.launch { cache.persistSession(session) }
            analytics.recordTransferCompleted(session)
        }
    }

    private fun handleNack(packet: MeshPacket, transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state != TransferState.SENDING) return

        val missing = packet.payload.split(",").mapNotNull { it.toIntOrNull() }
        scope.launch {
            val file = File(session.filePath ?: return@launch)
            missing.forEach { idx ->
                scheduler.incrementRetry(transferId)
                analytics.recordChunkRetransmission(transferId, idx)
                
                val chunkSize = chunkManager.calculateChunkSize(session.transportUsed)
                val chunkBytes = chunkManager.readChunkFromFile(file, idx, chunkSize) ?: return@forEach
                val b64 = Base64.encodeToString(chunkBytes, Base64.NO_WRAP)
                
                sendPacket(
                    session.senderId, session.targetId, transferId,
                    b64, PacketType.MEDIA_CHUNK, idx, session.totalChunks, session.mimeType
                )
                delay(INTER_CHUNK_DELAY_MS)
            }
        }
    }

    private fun startTimeoutMonitor(transferId: String) {
        scope.launch {
            delay(TRANSFER_TIMEOUT_MS)
            val session = scheduler.getSession(transferId) ?: return@launch
            if (session.state == TransferState.RECEIVING) {
                val received = cache.getReceivedChunkIndices(transferId)
                val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                
                if (missing.isNotEmpty()) {
                    MeshLogger.w(TAG, "Transfer $transferId timed out. Requesting missing ${missing.size} chunks.")
                    sendPacket(
                        session.targetId, session.senderId, transferId,
                        missing.joinToString(","), PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType
                    )
                    
                    delay(30_000L) // Wait 30s for recovery
                    val newReceived = cache.getReceivedChunkIndices(transferId)
                    if (newReceived.size < session.totalChunks) {
                        failSession(transferId, "Timeout expired, failed to recover.")
                    }
                }
            }
        }
    }

    // ─────────────────── Helpers ───────────────────

    fun pauseTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.SENDING || session.state == TransferState.RECEIVING) {
            scheduler.updateSessionState(transferId, TransferState.PAUSED)
            scope.launch { cache.persistSession(session) }
            MeshLogger.d(TAG, "Paused transfer $transferId")
        }
    }

    fun resumeTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.PAUSED) {
            scheduler.updateSessionState(transferId, TransferState.RESUMING)
            if (session.direction == TransferDirection.OUTGOING) {
                scope.launch { startOutgoingTransfer(session) }
            } else {
                // Incoming relies on sender to resume, or we can send a NACK to pull
                scope.launch {
                    val received = cache.getReceivedChunkIndices(transferId)
                    val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                    if (missing.isNotEmpty()) {
                        sendPacket(
                            session.targetId, session.senderId, transferId,
                            missing.joinToString(","), PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType
                        )
                        scheduler.updateSessionState(transferId, TransferState.RECEIVING)
                        cache.persistSession(session)
                    }
                }
            }
            MeshLogger.d(TAG, "Resumed transfer $transferId")
        }
    }

    fun cancelTransfer(transferId: String) {
        scheduler.updateSessionState(transferId, TransferState.CANCELLED)
        val session = scheduler.getSession(transferId)
        scope.launch { 
            if (session != null) cache.persistSession(session)
            cache.cleanUpSession(transferId) 
        }
    }

    private suspend fun failSession(transferId: String, reason: String) {
        val session = scheduler.getSession(transferId)
        scheduler.updateSessionState(transferId, TransferState.FAILED)
        if (session != null) {
            analytics.recordTransferFailed(session, reason)
            cache.persistSession(session)
        }
        cache.cleanUpSession(transferId)
    }

    private fun sendPacket(
        senderId: String, targetId: String, transferId: String,
        payload: String, type: PacketType, index: Int, total: Int, mime: String
    ) {
        val packet = MeshPacket(
            senderId = senderId,
            targetId = targetId,
            transferId = transferId,
            payload = payload,
            type = type,
            chunkIndex = index,
            totalChunks = total,
            mimeType = mime,
            ttl = 10
        )
        onSendPacket?.invoke(packet)
    }
}
