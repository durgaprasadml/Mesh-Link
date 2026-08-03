package com.meshlink.transfer

import android.content.Context
import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import com.meshlink.di.IoDispatcher
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.routing.engine.IntelligentTransportManager
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.routing.engine.TransportMetrics
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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
    @com.meshlink.di.ApplicationScope private val applicationScope: CoroutineScope,
    // Phase 2 Pipeline Components (Default parameters for 100% backward compatibility)
    val config: TransferConfiguration = TransferConfiguration(),
    val metrics: TransportMetrics = TransportMetrics(),
    val diagnostics: TransportDiagnostics = TransportDiagnostics(),
    val runtimeStateRegistry: TransferRuntimeStateRegistry = TransferRuntimeStateRegistry(),
    val slidingWindowManager: SlidingWindowManager = SlidingWindowManager(config, runtimeStateRegistry),
    val workerPool: ParallelTransferWorkerPool = ParallelTransferWorkerPool(config, ioDispatcher, applicationScope),
    val chunkDispatcher: ChunkDispatcher = ChunkDispatcher(chunkManager, slidingWindowManager, workerPool, runtimeStateRegistry, diagnostics, ioDispatcher),
    val ackManager: TransferAckManager = TransferAckManager(slidingWindowManager, runtimeStateRegistry, metrics, diagnostics),
    val retransmissionScheduler: ChunkRetransmissionScheduler = ChunkRetransmissionScheduler(config, slidingWindowManager, runtimeStateRegistry, chunkDispatcher, metrics, diagnostics, ioDispatcher, applicationScope)
) {
    // Phase 2 extension points (implementing architectural stubs)
    var transferQueue: com.meshlink.transfer.scheduler.TransferQueue? = null
    var slidingWindowBuffer: com.meshlink.transfer.scheduler.SlidingWindowBuffer? = slidingWindowManager
    var parallelWorkerPool: com.meshlink.transfer.scheduler.ParallelTransferWorkerPool? = workerPool

    companion object {
        private const val TAG = "TransferManager"
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
        applicationScope.launch(ioDispatcher) {
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
            applicationScope.launch(ioDispatcher) {
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

    // ─────────────────── Sender (Pipeline Pipeline) ───────────────────

    fun sendFile(
        file: File,
        senderId: String,
        targetId: String,
        priority: TransferPriority = TransferPriority.MEDIUM,
        transferId: String = UUID.randomUUID().toString(),
        thumbnailBase64: String? = null
    ): String {
        val exists = runBlocking(ioDispatcher) { file.exists() }
        if (!exists) {
            MeshLogger.e(TAG, "Cannot send non-existent file: ${file.absolutePath}")
            return transferId
        }

        val mimeType = metaManager.getMimeTypeForFile(file)
        val fileLength = runBlocking(ioDispatcher) { file.length() }
        val selectedRoute = intelligentTransportManager.selectTransportForPayload(
            destinationId = targetId,
            packetType = PacketType.MEDIA_CHUNK,
            payloadSizeBytes = fileLength,
            mimeType = mimeType
        )

        val transport = if (selectedRoute == RouteType.WIFI_DIRECT) TransportType.WIFI_DIRECT else TransportType.BLE
        val checksum = runBlocking(ioDispatcher) { verifier.calculateFileChecksum(file) }
        val totalChunks = chunkManager.getTotalChunks(fileLength, transport)

        val session = TransferSession(
            transferId = transferId,
            senderId = senderId,
            targetId = targetId,
            fileName = file.name,
            mimeType = mimeType,
            totalBytes = fileLength,
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
        applicationScope.launch(ioDispatcher) { cache.persistSession(session) }
        analytics.recordTransferStarted(session)
        diagnostics.logTransferStart(transferId, file.name, fileLength, selectedRoute)

        applicationScope.launch(ioDispatcher) {
            startOutgoingTransfer(session)
        }

        return transferId
    }

    private suspend fun startOutgoingTransfer(session: TransferSession) = withContext(ioDispatcher) {
        val file = File(session.filePath ?: return@withContext)
        if (!file.exists()) {
            failSession(session.transferId, "Source file vanished")
            return@withContext
        }

        scheduler.updateSessionState(session.transferId, TransferState.STREAMING)
        onTransferStateChanged?.invoke(session.transferId, TransferState.STREAMING)
        applicationScope.launch(ioDispatcher) { cache.persistSession(session) }

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

        // Initialize Sliding Window
        val windowSize = slidingWindowManager.initializeSessionWindow(
            session.transferId, session.transportUsed, session.totalChunks
        )
        diagnostics.logWindowCreated(session.transferId, windowSize)

        // Start Selective Retransmission Monitoring
        retransmissionScheduler.startMonitoring(
            session = session,
            file = file,
            onSendPacket = onSendPacket,
            onFailure = { reason -> failSession(session.transferId, reason) }
        )

        // Issue initial sliding window chunk dispatches to ParallelWorkerPool
        chunkDispatcher.dispatchAvailableChunks(session, file, onSendPacket)
    }

    private val lastProgressEmitMs = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val lastProgressEmitPct = java.util.concurrent.ConcurrentHashMap<String, Float>()

    private fun updateProgressThrottled(transferId: String, chunksDone: Int, totalChunks: Int, bytesTransferred: Long) {
        if (totalChunks <= 0) return
        val currentPct = chunksDone.toFloat() / totalChunks.toFloat()
        val lastPct = lastProgressEmitPct[transferId] ?: -1f
        val lastTime = lastProgressEmitMs[transferId] ?: 0L
        val now = System.currentTimeMillis()

        if (chunksDone >= totalChunks || Math.abs(currentPct - lastPct) >= 0.01f || (now - lastTime) >= 100L) {
            lastProgressEmitPct[transferId] = currentPct
            lastProgressEmitMs[transferId] = now
            scheduler.updateSessionProgress(transferId, chunksDone, bytesTransferred)
        }
    }

    // ─────────────────── Receiver ───────────────────

    fun handleIncomingPacket(packet: MeshPacket) {
        val transferId = packet.transferId ?: return

        applicationScope.launch(ioDispatcher) {
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
        applicationScope.launch(ioDispatcher) { cache.persistSession(session) }
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
            applicationScope.launch(ioDispatcher) { cache.persistSession(session) }
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
            updateProgressThrottled(transferId, count, packet.totalChunks, count.toLong() * chunkBytes.size)

            // Send ACK packet (preserving 100% existing packet format and semantics)
            sendPacket(
                packet.targetId, packet.senderId, transferId,
                packet.chunkIndex.toString(), PacketType.MEDIA_ACK, packet.chunkIndex, packet.totalChunks, session.mimeType
            )

            if (count >= packet.totalChunks) {
                assembleAndVerify(session)
            }
        }
    }

    private suspend fun assembleAndVerify(session: TransferSession) = withContext(ioDispatcher) {
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
                
                val durationMs = System.currentTimeMillis() - session.startTimeMs
                metrics.recordMediaTransfer(session.totalBytes, durationMs)
                diagnostics.logTransferCompletion(session.transferId, session.totalBytes, durationMs, session.getAverageSpeedBytesPerSec().toDouble())

                onTransferCompleted?.invoke(session)
            } else {
                MeshLogger.w(TAG, "Checksum verification failed for ${session.transferId}. Requesting chunk recovery.")
                if (session.retries < 3) {
                    session.retries++
                    outputFile.delete()
                    scheduler.updateSessionState(session.transferId, TransferState.RETRYING)
                    onTransferStateChanged?.invoke(session.transferId, TransferState.RETRYING)
                    
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

        if (session.direction == TransferDirection.OUTGOING) {
            val ackResult = ackManager.processAck(packet, session.totalChunks) ?: return

            val chunkSize = chunkManager.calculateChunkSize(session.transportUsed)
            val bytesDone = ackResult.newWindowBase.toLong() * chunkSize
            updateProgressThrottled(transferId, ackResult.newWindowBase, session.totalChunks, bytesDone.coerceAtMost(session.totalBytes))

            if (ackResult.windowAdvancedCount > 0 && session.state == TransferState.STREAMING) {
                val file = File(session.filePath ?: "")
                if (file.exists()) {
                    chunkDispatcher.dispatchAvailableChunks(session, file, onSendPacket)
                }
            }

            if (ackResult.isTransferComplete) {
                scheduler.updateSessionState(transferId, TransferState.COMPLETED)
                retransmissionScheduler.stopMonitoring(transferId)
                slidingWindowManager.closeWindow(transferId)
                chunkDispatcher.clearSession(transferId)

                onTransferStateChanged?.invoke(transferId, TransferState.COMPLETED)
                applicationScope.launch(ioDispatcher) { cache.persistSession(session) }
                analytics.recordTransferCompleted(session)

                val durationMs = System.currentTimeMillis() - session.startTimeMs
                metrics.recordMediaTransfer(session.totalBytes, durationMs)
                diagnostics.logTransferCompletion(transferId, session.totalBytes, durationMs, session.getAverageSpeedBytesPerSec().toDouble())

                onOutgoingTransferCompleted?.invoke(session)
            }
        }
    }

    private fun handleNack(packet: MeshPacket, transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state != TransferState.STREAMING && session.state != TransferState.SENDING) return

        val missing = parseRangePayload(packet.payload)
        applicationScope.launch(ioDispatcher) {
            val file = File(session.filePath ?: return@launch)
            missing.forEach { idx ->
                scheduler.incrementRetry(transferId)
                analytics.recordChunkRetransmission(transferId, idx)
                metrics.recordRetry()
                diagnostics.logRetransmission(transferId, idx, "NACK received")

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
        applicationScope.launch(ioDispatcher) {
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
            retransmissionScheduler.stopMonitoring(transferId)
            onTransferStateChanged?.invoke(transferId, TransferState.PAUSED)
            applicationScope.launch(ioDispatcher) { cache.persistSession(session) }
            MeshLogger.d(TAG, "Paused transfer $transferId")
        }
    }

    fun resumeTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.PAUSED || session.state == TransferState.RETRYING || session.state == TransferState.QUEUED) {
            scheduler.updateSessionState(transferId, TransferState.RESUMING)
            onTransferStateChanged?.invoke(transferId, TransferState.RESUMING)
            if (session.direction == TransferDirection.OUTGOING) {
                applicationScope.launch(ioDispatcher) { startOutgoingTransfer(session) }
            } else {
                applicationScope.launch(ioDispatcher) {
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
        retransmissionScheduler.stopMonitoring(transferId)
        slidingWindowManager.closeWindow(transferId)
        chunkDispatcher.clearSession(transferId)
        onTransferStateChanged?.invoke(transferId, TransferState.CANCELLED)
        val session = scheduler.getSession(transferId)
        applicationScope.launch(ioDispatcher) {
            if (session != null) cache.persistSession(session)
            cache.cleanUpSession(transferId)
        }
    }

    // Optional Stats API for Components
    fun getSession(transferId: String): TransferSession? = scheduler.getSession(transferId)

    fun getTransferSpeedBytesPerSec(transferId: String): Float = scheduler.getSession(transferId)?.getAverageSpeedBytesPerSec() ?: 0f

    fun getTransferEtaSeconds(transferId: String): Long = scheduler.getSession(transferId)?.getEstimatedEtaSeconds() ?: -1L

    fun getRemainingBytes(transferId: String): Long = scheduler.getSession(transferId)?.getRemainingBytes() ?: 0L

    private suspend fun failSession(transferId: String, reason: String) {
        val session = scheduler.getSession(transferId)
        scheduler.updateSessionState(transferId, TransferState.FAILED)
        retransmissionScheduler.stopMonitoring(transferId)
        slidingWindowManager.closeWindow(transferId)
        chunkDispatcher.clearSession(transferId)
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
