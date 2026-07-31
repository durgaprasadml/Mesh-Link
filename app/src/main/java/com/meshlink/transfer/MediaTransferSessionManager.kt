package com.meshlink.transfer

import android.content.Context
import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import com.meshlink.di.ApplicationScope
import com.meshlink.di.IoDispatcher
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.transfer.data.RoomTransferRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Production Media Transfer Session Manager.
 *
 * Acts strictly as a lightweight orchestrator coordinating:
 * - [AdaptiveChunkEngine]: Dynamic chunk sizing and CRC32 header framing
 * - [TransferQueueScheduler]: Priority queueing, concurrency limits, and sliding window flow control
 * - [CompressionEngine]: Format-aware intelligent compression
 * - [IntegrityVerifier]: Per-chunk CRC32 & zero-copy streaming SHA-256 validation
 * - [ResumeManager]: ACK tracking and range request recovery
 * - [ProgressTracker]: Detailed StateFlow metrics calculation
 * - [RoomTransferRepository]: Persistent Room database metadata queue
 * - [TransportExecutor]: Transport-aware dispatch with mid-transfer BLE fallback
 */
@Singleton
class MediaTransferSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val adaptiveChunkEngine: AdaptiveChunkEngine,
    val scheduler: TransferQueueScheduler,
    val compressionEngine: CompressionEngine,
    val integrityVerifier: IntegrityVerifier,
    val resumeManager: ResumeManager,
    val progressTracker: ProgressTracker,
    val roomRepository: RoomTransferRepository,
    val transportExecutor: TransportExecutor,
    private val transferCache: TransferCache,
    private val fileMetadataManager: FileMetadataManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MediaTransferSessionManager"
        private const val INTER_CHUNK_DELAY_MS = 20L
        private const val TRANSFER_TIMEOUT_MS = 120_000L
    }

    var onTransferCompleted: ((TransferSession) -> Unit)? = null
    var onTransferFailed: ((TransferSession, String) -> Unit)? = null

    val activeMetrics: StateFlow<Map<String, TransferMetrics>> = progressTracker.transferMetrics

    private val transferJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val timeoutJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    init {
        // Recover pending / paused sessions from Room DB on application startup
        applicationScope.launch(ioDispatcher) {
            val pending = roomRepository.getPendingOrActiveSessions()
            for (session in pending) {
                if (session.state == TransferState.SENDING || session.state == TransferState.RECEIVING) {
                    session.state = TransferState.PAUSED
                    roomRepository.saveSession(session)
                }
                scheduler.addSession(session)
            }
        }
    }

    // ─────────────────── Sender Workflows ───────────────────

    fun startTransfer(
        file: File,
        senderId: String,
        targetId: String,
        priority: TransferPriority = TransferPriority.MEDIUM,
        transferId: String = UUID.randomUUID().toString()
    ): String {
        if (!file.exists() || !file.canRead()) {
            MeshLogger.e(TAG, "Cannot transfer non-existent or unreadable file: ${file.absolutePath}")
            return transferId
        }

        val mimeType = fileMetadataManager.getMimeTypeForFile(file)
        val transport = transportExecutor.selectRouteForTransfer(targetId, PacketType.MEDIA_META, file.length())

        applicationScope.launch(ioDispatcher) {
            val sha256 = integrityVerifier.calculateFileChecksum(file)
            val compressionRes = compressionEngine.compressFileIfNeeded(file, mimeType)
            val fileToStream = if (compressionRes.isCompressed) {
                val tempFile = File(context.cacheDir, "comp_$transferId.tmp")
                tempFile.writeBytes(compressionRes.data)
                tempFile
            } else file

            val totalBytes = fileToStream.length()
            val chunkSize = adaptiveChunkEngine.calculateChunkSize(transport)
            val totalChunks = (Math.ceil(totalBytes.toDouble() / chunkSize)).toInt().coerceAtLeast(1)

            val session = TransferSession(
                transferId = transferId,
                senderId = senderId,
                targetId = targetId,
                fileName = file.name,
                mimeType = mimeType,
                totalBytes = file.length(),
                totalChunks = totalChunks,
                direction = TransferDirection.OUTGOING,
                state = TransferState.WAITING,
                priority = priority,
                transportUsed = transport,
                sha256Checksum = sha256,
                compressionType = compressionRes.compressionType,
                compressedSize = totalBytes,
                filePath = fileToStream.absolutePath
            )

            scheduler.addSession(session)
            roomRepository.saveSession(session)
            resumeManager.initOrRecoverState(transferId, sha256, totalChunks)
            progressTracker.initTransfer(transferId, session.totalBytes, totalChunks, transport)

            startOutgoingLoop(session)
        }

        return transferId
    }

    private fun startOutgoingLoop(session: TransferSession) {
        transferJobs[session.transferId]?.cancel()
        transferJobs[session.transferId] = applicationScope.launch(ioDispatcher) {
            val file = File(session.filePath ?: return@launch)
            if (!file.exists()) {
                failSession(session.transferId, "Source file missing")
                return@launch
            }

            scheduler.updateSessionState(session.transferId, TransferState.SENDING)
            roomRepository.updateState(session.transferId, TransferState.SENDING.name)

            // Send META packet
            val metaPayload = fileMetadataManager.generateMetaPayload(
                FileMetadata(session.fileName, session.mimeType, session.totalBytes, session.sha256Checksum)
            )

            sendControlPacket(
                session.senderId, session.targetId, session.transferId,
                metaPayload, PacketType.MEDIA_META, 0, session.totalChunks, session.mimeType
            )

            delay(100L) // Allow receiver to initialize session cache

            var chunkIdx = session.chunksTransferred
            val resumeState = resumeManager.getResumeState(session.transferId)
            val ackedIndices = resumeState?.ackedChunkIndices ?: emptySet()

            while (chunkIdx < session.totalChunks && isActive) {
                val current = scheduler.getSession(session.transferId)
                if (current == null || current.state != TransferState.SENDING) {
                    MeshLogger.d(TAG, "Stopping outgoing loop for ${session.transferId} (state=${current?.state})")
                    return@launch
                }

                // Check scheduler if allowed to send next chunk
                if (!scheduler.canSendNextChunk(session.transferId)) {
                    delay(40L)
                    continue
                }

                // Skip already ACKed chunks (Resume optimization)
                if (ackedIndices.contains(chunkIdx)) {
                    chunkIdx++
                    continue
                }

                val currentTransport = transportExecutor.selectRouteForTransfer(session.targetId, PacketType.MEDIA_CHUNK, file.length())
                val dynamicChunkSize = adaptiveChunkEngine.calculateChunkSize(
                    transportType = currentTransport,
                    packetLossRate = session.packetLossRate,
                    averageRttMs = session.averageRttMs,
                    retryCount = session.retries
                )

                val rawChunk = readChunkFromFile(file, chunkIdx, dynamicChunkSize)
                if (rawChunk == null) {
                    failSession(session.transferId, "Read chunk $chunkIdx failed")
                    return@launch
                }

                // Attach CRC32 header
                val framedChunk = adaptiveChunkEngine.attachCrc32Header(rawChunk)
                val b64Payload = Base64.encodeToString(framedChunk, Base64.NO_WRAP)
                BufferPool.returnBuffer(rawChunk)

                val packet = MeshPacket(
                    senderId = session.senderId,
                    targetId = session.targetId,
                    transferId = session.transferId,
                    payload = b64Payload,
                    type = PacketType.MEDIA_CHUNK,
                    chunkIndex = chunkIdx,
                    totalChunks = session.totalChunks,
                    mimeType = session.mimeType,
                    ttl = 10
                )

                val (res, actualTransport) = transportExecutor.dispatchPacket(packet)
                if (res is com.meshlink.domain.model.MeshResult.Error) {
                    scheduler.incrementRetry(session.transferId)
                }

                session.transportUsed = actualTransport
                scheduler.updateSessionProgress(session.transferId, chunkIdx + 1, (chunkIdx + 1).toLong() * dynamicChunkSize)
                roomRepository.updateProgress(session.transferId, chunkIdx + 1, (chunkIdx + 1).toLong() * dynamicChunkSize)

                progressTracker.updateProgress(
                    transferId = session.transferId,
                    chunksTransferred = chunkIdx + 1,
                    bytesTransferred = (chunkIdx + 1).toLong() * dynamicChunkSize,
                    totalBytes = session.totalBytes,
                    totalChunks = session.totalChunks,
                    transport = actualTransport,
                    status = TransferState.SENDING,
                    startTimeMs = session.startTimeMs,
                    retries = session.retries,
                    crc32Errors = session.crc32Errors,
                    packetLossRate = session.packetLossRate,
                    compressionRatio = session.compressionRatio,
                    resumeCount = session.resumeCount
                )

                chunkIdx++
                if (INTER_CHUNK_DELAY_MS > 0) delay(INTER_CHUNK_DELAY_MS)
            }
        }
    }

    // ─────────────────── Receiver Workflows ───────────────────

    fun handleIncomingPacket(packet: MeshPacket) {
        val transferId = packet.transferId ?: return

        applicationScope.launch(ioDispatcher) {
            when (packet.type) {
                PacketType.MEDIA_META -> handleIncomingMeta(packet, transferId)
                PacketType.MEDIA_CHUNK -> handleIncomingChunk(packet, transferId)
                PacketType.MEDIA_ACK -> handleIncomingAck(packet, transferId)
                PacketType.MEDIA_NACK -> handleIncomingNack(packet, transferId)
                else -> {}
            }
        }
    }

    private suspend fun handleIncomingMeta(packet: MeshPacket, transferId: String) {
        val meta = fileMetadataManager.parseMetaPayload(packet.payload) ?: return
        if (!resumeManager.isResumeMetadataValid(transferId, meta.sha256Checksum)) {
            transferCache.cleanUpSession(transferId)
        }

        transferCache.initSessionCache(transferId)
        val existingAcked = transferCache.getReceivedChunkIndices(transferId)

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
            chunksTransferred = existingAcked.size,
            bytesTransferred = existingAcked.size.toLong() * 1024,
            startTimeMs = System.currentTimeMillis()
        )

        scheduler.addSession(session)
        roomRepository.saveSession(session)
        resumeManager.initOrRecoverState(transferId, meta.sha256Checksum, packet.totalChunks, existingAcked)
        progressTracker.initTransfer(transferId, meta.totalBytes, packet.totalChunks, TransportType.BLE, TransferState.RECEIVING)

        startTimeoutWatcher(transferId)
    }

    private suspend fun handleIncomingChunk(packet: MeshPacket, transferId: String) {
        var session = scheduler.getSession(transferId)

        // Late-joiner initialization if META packet was dropped
        if (session == null) {
            transferCache.initSessionCache(transferId)
            val mime = packet.mimeType ?: "application/octet-stream"
            val existing = transferCache.getReceivedChunkIndices(transferId)
            session = TransferSession(
                transferId = transferId,
                senderId = packet.senderId,
                targetId = packet.targetId,
                fileName = "recovered_$transferId.${mime.substringAfter("/")}",
                mimeType = mime,
                totalBytes = 0L,
                totalChunks = packet.totalChunks,
                direction = TransferDirection.INCOMING,
                state = TransferState.RECEIVING,
                chunksTransferred = existing.size,
                startTimeMs = System.currentTimeMillis()
            )
            scheduler.addSession(session)
            roomRepository.saveSession(session)
            resumeManager.initOrRecoverState(transferId, null, packet.totalChunks, existing)
            startTimeoutWatcher(transferId)
        }

        val framedBytes = try {
            Base64.decode(packet.payload, Base64.NO_WRAP)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "[$transferId] Base64 decode failed for chunk ${packet.chunkIndex}")
            return
        }

        // Validate & strip CRC32 header
        val rawChunk = adaptiveChunkEngine.validateAndStripCrc32Header(framedBytes)
        if (rawChunk == null) {
            session.crc32Errors++
            MeshLogger.e(TAG, "[$transferId] Chunk ${packet.chunkIndex} failed CRC32 verification!")
            sendControlPacket(packet.targetId, packet.senderId, transferId, packet.chunkIndex.toString(), PacketType.MEDIA_NACK, packet.chunkIndex, packet.totalChunks, session.mimeType)
            return
        }

        val success = transferCache.writeChunk(transferId, packet.chunkIndex, rawChunk)
        if (success) {
            resumeManager.recordChunkAck(transferId, packet.chunkIndex)
            val ackedSet = transferCache.getReceivedChunkIndices(transferId)
            val count = ackedSet.size
            val bytesRecv = count.toLong() * rawChunk.size

            scheduler.updateSessionProgress(transferId, count, bytesRecv)
            roomRepository.updateProgress(transferId, count, bytesRecv)

            progressTracker.updateProgress(
                transferId = transferId,
                chunksTransferred = count,
                bytesTransferred = bytesRecv,
                totalBytes = session.totalBytes,
                totalChunks = packet.totalChunks,
                transport = TransportType.BLE,
                status = TransferState.RECEIVING,
                startTimeMs = session.startTimeMs,
                retries = session.retries,
                crc32Errors = session.crc32Errors,
                resumeCount = session.resumeCount
            )

            // Send ACK back to sender
            sendControlPacket(packet.targetId, packet.senderId, transferId, packet.chunkIndex.toString(), PacketType.MEDIA_ACK, packet.chunkIndex, packet.totalChunks, session.mimeType)

            // Check completion
            if (count >= packet.totalChunks) {
                assembleAndVerify(session)
            }
        }
    }

    private suspend fun assembleAndVerify(session: TransferSession) {
        scheduler.updateSessionState(session.transferId, TransferState.VERIFYING)
        roomRepository.updateState(session.transferId, TransferState.VERIFYING.name)
        progressTracker.updateStatus(session.transferId, TransferState.VERIFYING)

        val mediaDir = File(context.filesDir, "mesh_media").also { if (!it.exists()) it.mkdirs() }
        val outputFile = File(mediaDir, session.fileName)

        val assembled = transferCache.assembleFile(session.transferId, session.totalChunks, outputFile)
        if (!assembled) {
            failSession(session.transferId, "File assembly failed")
            return
        }

        // Decompress if GZIP compressed
        val finalFile = if (session.compressionType == "GZIP") {
            val decompressedBytes = compressionEngine.decompressIfNeeded(outputFile.readBytes(), session.compressionType)
            outputFile.writeBytes(decompressedBytes)
            outputFile
        } else outputFile

        val verified = integrityVerifier.verifyFileChecksum(finalFile, session.sha256Checksum)
        if (verified) {
            session.filePath = finalFile.absolutePath
            session.endTimeMs = System.currentTimeMillis()
            session.state = TransferState.COMPLETED

            scheduler.updateSessionState(session.transferId, TransferState.COMPLETED)
            roomRepository.saveSession(session)
            transferCache.cleanUpSession(session.transferId)
            resumeManager.clearState(session.transferId)

            progressTracker.updateStatus(session.transferId, TransferState.COMPLETED)
            MeshLogger.d(TAG, "Transfer ${session.transferId} successfully assembled & verified: ${finalFile.absolutePath}")

            onTransferCompleted?.invoke(session)
        } else {
            failSession(session.transferId, "SHA-256 checksum verification failed")
            finalFile.delete()
        }
    }

    private fun handleIncomingAck(packet: MeshPacket, transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        scheduler.recordAckArrival(transferId)
        resumeManager.recordChunkAck(transferId, packet.chunkIndex)

        if (session.direction == TransferDirection.OUTGOING && packet.chunkIndex == session.totalChunks - 1) {
            session.state = TransferState.COMPLETED
            session.endTimeMs = System.currentTimeMillis()

            applicationScope.launch(ioDispatcher) {
                scheduler.updateSessionState(transferId, TransferState.COMPLETED)
                roomRepository.saveSession(session)
                resumeManager.clearState(transferId)
                progressTracker.updateStatus(transferId, TransferState.COMPLETED)
                onTransferCompleted?.invoke(session)
            }
        }
    }

    private fun handleIncomingNack(packet: MeshPacket, transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        val missing = packet.payload.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (missing.isEmpty()) return

        MeshLogger.w(TAG, "[$transferId] Received NACK for missing chunks: $missing")
        scheduler.incrementRetry(transferId)

        applicationScope.launch(ioDispatcher) {
            val file = File(session.filePath ?: return@launch)
            if (!file.exists()) return@launch

            missing.forEach { idx ->
                val chunkSize = adaptiveChunkEngine.calculateChunkSize(session.transportUsed)
                val rawBytes = readChunkFromFile(file, idx, chunkSize) ?: return@forEach
                val framed = adaptiveChunkEngine.attachCrc32Header(rawBytes)
                val b64 = Base64.encodeToString(framed, Base64.NO_WRAP)
                BufferPool.returnBuffer(rawBytes)

                val nackPacket = MeshPacket(
                    senderId = session.senderId,
                    targetId = session.targetId,
                    transferId = transferId,
                    payload = b64,
                    type = PacketType.MEDIA_CHUNK,
                    chunkIndex = idx,
                    totalChunks = session.totalChunks,
                    mimeType = session.mimeType,
                    ttl = 10
                )
                transportExecutor.dispatchPacket(nackPacket)
                delay(INTER_CHUNK_DELAY_MS)
            }
        }
    }

    // ─────────────────── Pause / Resume / Cancel Controls ───────────────────

    fun pauseTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.SENDING || session.state == TransferState.RECEIVING) {
            transferJobs[transferId]?.cancel()
            scheduler.updateSessionState(transferId, TransferState.PAUSED)
            applicationScope.launch(ioDispatcher) {
                roomRepository.updateState(transferId, TransferState.PAUSED.name)
                progressTracker.updateStatus(transferId, TransferState.PAUSED)
            }
        }
    }

    fun resumeTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.PAUSED) {
            session.resumeCount++
            scheduler.updateSessionState(transferId, TransferState.RESUMING)
            applicationScope.launch(ioDispatcher) {
                roomRepository.updateState(transferId, TransferState.RESUMING.name)
                if (session.direction == TransferDirection.OUTGOING) {
                    startOutgoingLoop(session)
                } else {
                    val received = transferCache.getReceivedChunkIndices(transferId)
                    val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                    if (missing.isNotEmpty()) {
                        sendControlPacket(
                            session.targetId, session.senderId, transferId,
                            missing.joinToString(","), PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType
                        )
                    }
                    scheduler.updateSessionState(transferId, TransferState.RECEIVING)
                }
            }
        }
    }

    fun cancelTransfer(transferId: String) {
        transferJobs[transferId]?.cancel()
        timeoutJobs.remove(transferId)?.cancel()
        scheduler.updateSessionState(transferId, TransferState.CANCELLED)

        applicationScope.launch(ioDispatcher) {
            roomRepository.updateState(transferId, TransferState.CANCELLED.name)
            transferCache.cleanUpSession(transferId)
            resumeManager.clearState(transferId)
            progressTracker.removeTransfer(transferId)
        }
    }

    private suspend fun failSession(transferId: String, reason: String) {
        val session = scheduler.getSession(transferId)
        MeshLogger.e(TAG, "Session $transferId failed: $reason")

        scheduler.updateSessionState(transferId, TransferState.FAILED)
        if (session != null) {
            session.endTimeMs = System.currentTimeMillis()
            roomRepository.saveSession(session)
            onTransferFailed?.invoke(session, reason)
        }
        transferCache.cleanUpSession(transferId)
        resumeManager.clearState(transferId)
        progressTracker.updateStatus(transferId, TransferState.FAILED)
    }

    private fun startTimeoutWatcher(transferId: String) {
        timeoutJobs[transferId]?.cancel()
        timeoutJobs[transferId] = applicationScope.launch(ioDispatcher) {
            delay(TRANSFER_TIMEOUT_MS)

            val session = scheduler.getSession(transferId) ?: return@launch
            if (session.state == TransferState.RECEIVING) {
                val received = transferCache.getReceivedChunkIndices(transferId)
                val missing = (0 until session.totalChunks).filter { !received.contains(it) }

                if (missing.isNotEmpty()) {
                    MeshLogger.w(TAG, "[$transferId] Transfer timeout reached. Requesting missing ${missing.size} chunks.")
                    sendControlPacket(
                        session.targetId, session.senderId, transferId,
                        missing.joinToString(","), PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType
                    )

                    delay(30_000L) // 30s recovery window
                    val newReceived = transferCache.getReceivedChunkIndices(transferId)
                    if (newReceived.size < session.totalChunks) {
                        failSession(transferId, "Timeout recovery expired")
                    }
                }
            }
        }
    }

    private suspend fun sendControlPacket(
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
        transportExecutor.dispatchPacket(packet)
    }

    private fun readChunkFromFile(file: File, chunkIndex: Int, chunkSize: Int): ByteArray? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val offset = chunkIndex.toLong() * chunkSize
                if (offset >= raf.length()) return null
                raf.seek(offset)
                val bytesToRead = minOf(chunkSize.toLong(), raf.length() - offset).toInt()
                val buffer = BufferPool.borrowBuffer(bytesToRead)
                raf.readFully(buffer)
                buffer
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error reading chunk $chunkIndex from ${file.name}: ${e.message}")
            null
        }
    }
}
