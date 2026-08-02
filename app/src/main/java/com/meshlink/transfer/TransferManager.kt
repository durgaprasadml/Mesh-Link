package com.meshlink.transfer
import com.meshlink.domain.model.RouteType

import android.content.Context
import android.util.Base64
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import com.meshlink.di.IoDispatcher
import com.meshlink.routing.engine.IntelligentTransportManager
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
    private val wifiSocketTransport: com.meshlink.wifi.data.WifiSocketTransport,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @com.meshlink.di.ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope
) {
    companion object {
        private const val TAG = "TransferManager"
        private const val BLE_INTER_CHUNK_DELAY_MS = 30L
        private const val WIFI_INTER_CHUNK_DELAY_MS = 2L
        private const val TRANSFER_TIMEOUT_MS = 120_000L
        private const val MAX_NACK_PAYLOAD_BYTES = 150
    }

    var onSendPacket: (suspend (MeshPacket) -> Unit)? = null
    var onTransferCompleted: ((TransferSession) -> Unit)? = null
    var onOutgoingTransferCompleted: ((TransferSession) -> Unit)? = null
    var onTransferStateChanged: ((String, TransferState) -> Unit)? = null

    val transferProgress: StateFlow<Map<String, Float>> = scheduler.activeSessions
        .map { sessions ->
            sessions.associate { it.transferId to it.getProgress() }
        }
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ─────────────────── Initialization ───────────────────

    init {
        applicationScope.launch {
            val persisted = cache.loadPersistedSessions()
            for (session in persisted) {
                if (session.state == TransferState.SENDING || session.state == TransferState.RECEIVING || session.state == TransferState.STREAMING) {
                    session.state = TransferState.PAUSED
                }
                scheduler.addSession(session)
            }
        }

        // Auto-resume active/paused transfers on Wi-Fi Direct socket connection
        wifiSocketTransport.onSocketConnected = {
            applicationScope.launch {
                MeshLogger.d(TAG, "Wi-Fi Direct socket re-connected. Auto-resuming paused transfers...")
                val activeSessions = scheduler.activeSessions.value
                for (session in activeSessions) {
                    if (session.state == TransferState.PAUSED || session.state == TransferState.RETRYING || session.state == TransferState.QUEUED) {
                        resumeTransfer(session.transferId)
                    }
                }
            }
        }
    }

    // ─────────────────── Sender ───────────────────

    fun sendFile(
        file: File,
        senderId: String,
        targetId: String,
        priority: TransferPriority = TransferPriority.MEDIUM,
        transferId: String = UUID.randomUUID().toString(),
        thumbnailBase64: String? = null
    ): String {
        if (!file.exists()) {
            MeshLogger.e(TAG, "Cannot send non-existent file: ${file.absolutePath}")
            return transferId
        }

        val mimeType = metaManager.getMimeTypeForFile(file)
        val selectedRoute = intelligentTransportManager.selectTransportForPayload(
            destinationId = targetId,
            packetType = PacketType.MEDIA_CHUNK,
            payloadSizeBytes = file.length(),
            mimeType = mimeType
        )

        val transport = if (selectedRoute == RouteType.WIFI_DIRECT) TransportType.WIFI_DIRECT else TransportType.BLE
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
            thumbnailBase64 = thumbnailBase64,
            state = TransferState.QUEUED,
            startTimeMs = System.currentTimeMillis()
        )

        scheduler.addSession(session)
        onTransferStateChanged?.invoke(transferId, TransferState.QUEUED)
        applicationScope.launch { cache.persistSession(session) }
        analytics.recordTransferStarted(session)
        
        applicationScope.launch {
            startOutgoingTransfer(session)
        }

        return transferId
    }

    private suspend fun startOutgoingTransfer(session: TransferSession) {
        val file = File(session.filePath ?: return)
        if (!file.exists()) {
            failSession(session.transferId, "Source file vanished")
            return
        }

        scheduler.updateSessionState(session.transferId, TransferState.STREAMING)
        onTransferStateChanged?.invoke(session.transferId, TransferState.STREAMING)
        applicationScope.launch { cache.persistSession(session) }

        // Send META packet
        val metaPayload = metaManager.generateMetaPayload(
            FileMetadata(session.fileName, session.mimeType, session.totalBytes, session.sha256Checksum, session.thumbnailBase64)
        )
        sendPacket(
            session.senderId, session.targetId, session.transferId,
            metaPayload, PacketType.MEDIA_META, 0, session.totalChunks, session.mimeType
        )

        // Give receiver time to init cache
        delay(50L)

        val delayMs = if (session.transportUsed == TransportType.WIFI_DIRECT) WIFI_INTER_CHUNK_DELAY_MS else BLE_INTER_CHUNK_DELAY_MS

        // Send Chunks
        var i = session.chunksTransferred
        while (i < session.totalChunks && applicationScope.isActive) {
            val currentState = scheduler.getSession(session.transferId)?.state
            if (currentState != TransferState.STREAMING && currentState != TransferState.SENDING) {
                MeshLogger.d(TAG, "Stopping outgoing loop for ${session.transferId}. State: $currentState")
                return
            }
            
            if (!scheduler.canSendNextChunk(session.transferId)) {
                delay(20L)
                continue
            }

            val chunkSize = chunkManager.calculateChunkSize(session.transportUsed)
            val chunkBytes = chunkManager.readChunkFromFile(file, i, chunkSize)
            
            if (chunkBytes == null) {
                failSession(session.transferId, "Failed to read chunk $i from disk")
                return
            }

            try {
                val b64 = Base64.encodeToString(chunkBytes, Base64.NO_WRAP)
                sendPacket(
                    session.senderId, session.targetId, session.transferId,
                    b64, PacketType.MEDIA_CHUNK, i, session.totalChunks, session.mimeType
                )
            } finally {
                BufferPool.returnBuffer(chunkBytes)
            }

            val bytesSentSoFar = (i + 1).toLong() * chunkSize
            scheduler.updateSessionProgress(session.transferId, i + 1, bytesSentSoFar.coerceAtMost(session.totalBytes))
            i++
            
            if (delayMs > 0) delay(delayMs)
            kotlinx.coroutines.yield()
        }
    }

    // ─────────────────── Receiver ───────────────────

    fun handleIncomingPacket(packet: MeshPacket) {
        val transferId = packet.transferId ?: return
        
        applicationScope.launch {
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
            thumbnailBase64 = meta.thumbnailBase64,
            state = TransferState.RECEIVING,
            startTimeMs = System.currentTimeMillis()
        )
        
        scheduler.addSession(session)
        onTransferStateChanged?.invoke(transferId, TransferState.RECEIVING)
        applicationScope.launch { cache.persistSession(session) }
        analytics.recordTransferStarted(session)
        
        startTimeoutMonitor(transferId)
    }

    private suspend fun handleChunk(packet: MeshPacket, transferId: String) {
        var session = scheduler.getSession(transferId)
        
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
            onTransferStateChanged?.invoke(transferId, TransferState.RECEIVING)
            applicationScope.launch { cache.persistSession(session) }
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

            if (count >= packet.totalChunks) {
                assembleAndVerify(session)
            }
        }
    }

    private suspend fun assembleAndVerify(session: TransferSession) {
        scheduler.updateSessionState(session.transferId, TransferState.VERIFYING)
        onTransferStateChanged?.invoke(session.transferId, TransferState.VERIFYING)
        cache.persistSession(session)
        
        val mediaDir = File(context.filesDir, "mesh_media").also { if (!it.exists()) it.mkdirs() }
        val outputFile = File(mediaDir, session.fileName)
        
        val assembled = cache.assembleFile(session.transferId, session.totalChunks, outputFile)
        
        if (assembled) {
            if (verifier.verifyFileChecksum(outputFile, session.sha256Checksum)) {
                session.filePath = outputFile.absolutePath
                scheduler.updateSessionState(session.transferId, TransferState.COMPLETED)
                onTransferStateChanged?.invoke(session.transferId, TransferState.COMPLETED)
                cache.persistSession(session)
                cache.cleanUpSession(session.transferId)
                analytics.recordTransferCompleted(session)
                onTransferCompleted?.invoke(session)
            } else {
                MeshLogger.w(TAG, "Checksum verification failed for ${session.transferId}. Requesting chunk recovery.")
                if (session.retries < 3) {
                    session.retries++
                    outputFile.delete()
                    scheduler.updateSessionState(session.transferId, TransferState.RETRYING)
                    onTransferStateChanged?.invoke(session.transferId, TransferState.RETRYING)
                    // Request all missing / corrupted chunks using range-compressed MTU-safe NACK batches
                    val received = cache.getReceivedChunkIndices(session.transferId)
                    val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                    val indicesToRequest = if (missing.isNotEmpty()) missing else (0 until session.totalChunks).toList()
                    val nackBatches = formatMissingIndicesToRanges(indicesToRequest)
                    
                    for (batch in nackBatches) {
                        sendPacket(
                            session.targetId, session.senderId, session.transferId,
                            batch, PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType
                        )
                    }
                } else {
                    failSession(session.transferId, "Checksum verification failed after retries")
                    outputFile.delete()
                }
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
            onTransferStateChanged?.invoke(transferId, TransferState.COMPLETED)
            applicationScope.launch { cache.persistSession(session) }
            analytics.recordTransferCompleted(session)
            onOutgoingTransferCompleted?.invoke(session)
        }
    }

    private fun handleNack(packet: MeshPacket, transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state != TransferState.STREAMING && session.state != TransferState.SENDING) return

        val missing = parseRangePayload(packet.payload)
        applicationScope.launch {
            val file = File(session.filePath ?: return@launch)
            missing.forEach { idx ->
                scheduler.incrementRetry(transferId)
                analytics.recordChunkRetransmission(transferId, idx)
                
                val chunkSize = chunkManager.calculateChunkSize(session.transportUsed)
                val chunkBytes = chunkManager.readChunkFromFile(file, idx, chunkSize) ?: return@forEach
                
                try {
                    val b64 = Base64.encodeToString(chunkBytes, Base64.NO_WRAP)
                    sendPacket(
                        session.senderId, session.targetId, transferId,
                        b64, PacketType.MEDIA_CHUNK, idx, session.totalChunks, session.mimeType
                    )
                } finally {
                    BufferPool.returnBuffer(chunkBytes)
                }
                
                kotlinx.coroutines.yield()
            }
        }
    }

    private fun startTimeoutMonitor(transferId: String) {
        applicationScope.launch {
            delay(TRANSFER_TIMEOUT_MS)
            val session = scheduler.getSession(transferId) ?: return@launch
            if (session.state == TransferState.RECEIVING || session.state == TransferState.STREAMING) {
                val received = cache.getReceivedChunkIndices(transferId)
                val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                
                if (missing.isNotEmpty()) {
                    MeshLogger.w(TAG, "Transfer $transferId timed out. Requesting missing ${missing.size} chunks.")
                    val nackBatches = formatMissingIndicesToRanges(missing)
                    for (batch in nackBatches) {
                        sendPacket(
                            session.targetId, session.senderId, transferId,
                            batch, PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType
                        )
                    }
                    
                    delay(30_000L)
                    val newReceived = cache.getReceivedChunkIndices(transferId)
                    if (newReceived.size < session.totalChunks) {
                        failSession(transferId, "Timeout expired, failed to recover.")
                    }
                }
            }
        }
    }

    // ─────────────────── NACK Range Utilities ───────────────────

    fun formatMissingIndicesToRanges(missing: List<Int>, maxPayloadLength: Int = MAX_NACK_PAYLOAD_BYTES): List<String> {
        if (missing.isEmpty()) return emptyList()
        val sorted = missing.sorted()
        val rawRanges = mutableListOf<String>()
        
        var start = sorted[0]
        var prev = sorted[0]

        for (i in 1 until sorted.size) {
            val curr = sorted[i]
            if (curr == prev + 1) {
                prev = curr
            } else {
                if (start == prev) rawRanges.add("$start") else rawRanges.add("$start-$prev")
                start = curr
                prev = curr
            }
        }
        if (start == prev) rawRanges.add("$start") else rawRanges.add("$start-$prev")

        val resultBatches = mutableListOf<String>()
        var currentBatch = StringBuilder()

        for (rangeStr in rawRanges) {
            if (currentBatch.isNotEmpty() && (currentBatch.length + 1 + rangeStr.length) > maxPayloadLength) {
                resultBatches.add(currentBatch.toString())
                currentBatch = StringBuilder(rangeStr)
            } else {
                if (currentBatch.isNotEmpty()) currentBatch.append(",")
                currentBatch.append(rangeStr)
            }
        }
        if (currentBatch.isNotEmpty()) {
            resultBatches.add(currentBatch.toString())
        }

        return resultBatches
    }

    fun parseRangePayload(payload: String): List<Int> {
        if (payload.isBlank()) return emptyList()
        val indices = mutableSetOf<Int>()
        val tokens = payload.split(",")
        for (token in tokens) {
            val trimmed = token.trim()
            if (trimmed.contains("-")) {
                val parts = trimmed.split("-")
                if (parts.size == 2) {
                    val start = parts[0].toIntOrNull()
                    val end = parts[1].toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        for (i in start..end) indices.add(i)
                    }
                }
            } else {
                val idx = trimmed.toIntOrNull()
                if (idx != null) indices.add(idx)
            }
        }
        return indices.sorted()
    }

    // ─────────────────── Public Control API ───────────────────

    fun pauseTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.SENDING || session.state == TransferState.STREAMING || session.state == TransferState.RECEIVING) {
            scheduler.updateSessionState(transferId, TransferState.PAUSED)
            onTransferStateChanged?.invoke(transferId, TransferState.PAUSED)
            applicationScope.launch { cache.persistSession(session) }
            MeshLogger.d(TAG, "Paused transfer $transferId")
        }
    }

    fun resumeTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.PAUSED || session.state == TransferState.RETRYING || session.state == TransferState.QUEUED) {
            scheduler.updateSessionState(transferId, TransferState.RESUMING)
            onTransferStateChanged?.invoke(transferId, TransferState.RESUMING)
            if (session.direction == TransferDirection.OUTGOING) {
                applicationScope.launch { startOutgoingTransfer(session) }
            } else {
                applicationScope.launch {
                    val received = cache.getReceivedChunkIndices(transferId)
                    val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                    if (missing.isNotEmpty()) {
                        val nackBatches = formatMissingIndicesToRanges(missing)
                        for (batch in nackBatches) {
                            sendPacket(
                                session.targetId, session.senderId, transferId,
                                batch, PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType
                            )
                        }
                        scheduler.updateSessionState(transferId, TransferState.RECEIVING)
                        onTransferStateChanged?.invoke(transferId, TransferState.RECEIVING)
                        cache.persistSession(session)
                    }
                }
            }
            MeshLogger.d(TAG, "Resumed transfer $transferId")
        }
    }

    fun cancelTransfer(transferId: String) {
        scheduler.updateSessionState(transferId, TransferState.CANCELLED)
        onTransferStateChanged?.invoke(transferId, TransferState.CANCELLED)
        val session = scheduler.getSession(transferId)
        applicationScope.launch { 
            if (session != null) cache.persistSession(session)
            cache.cleanUpSession(transferId) 
        }
    }

    // Optional Stats API for Components 3 & 10
    fun getSession(transferId: String): TransferSession? = scheduler.getSession(transferId)

    fun getTransferSpeedBytesPerSec(transferId: String): Float = scheduler.getSession(transferId)?.getAverageSpeedBytesPerSec() ?: 0f

    fun getTransferEtaSeconds(transferId: String): Long = scheduler.getSession(transferId)?.getEstimatedEtaSeconds() ?: -1L

    fun getRemainingBytes(transferId: String): Long = scheduler.getSession(transferId)?.getRemainingBytes() ?: 0L

    private suspend fun failSession(transferId: String, reason: String) {
        val session = scheduler.getSession(transferId)
        scheduler.updateSessionState(transferId, TransferState.FAILED)
        onTransferStateChanged?.invoke(transferId, TransferState.FAILED)
        if (session != null) {
            analytics.recordTransferFailed(session, reason)
            cache.persistSession(session)
        }
        cache.cleanUpSession(transferId)
    }

    private suspend fun sendPacket(
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
