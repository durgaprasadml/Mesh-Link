package com.meshlink.data.media

import android.content.Context
import android.util.Base64
import android.util.Log
import com.meshlink.data.ble.MeshPacket
import com.meshlink.data.ble.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Reliable chunked media transfer over BLE mesh.
 *
 * Requirements implemented:
 *  1. Image processing — handled by ImageCompressor (800px, ≤200KB, JPEG 30–45)
 *  2. Chunked transfer — 300-byte Base64 chunks, META first, then sequential chunks
 *  3. Controlled sending — 30ms inter-chunk delay to prevent BLE buffer overflow
 *  4. ACK + retry — receiver sends MEDIA_ACK per chunk; sender retries on MEDIA_NACK
 *  5. Reassembly — strict ordered reassembly, all chunks validated before assembly
 *  6. Progress tracking — per-transferId StateFlow (0.0 → 1.0)
 *  7. Error handling — timeout after 60s, automatic NACK-based retry (max 3 per chunk)
 *  8. Performance — IO dispatcher, no main-thread blocking, bounded memory
 */
class MediaTransferManager(
    private val context: Context
) {
    companion object {
        // 300 bytes of Base64 text per chunk.
        // At 512-byte MTU: 300B B64 + ~80B JSON envelope = 380B < 512B ✓
        private const val CHUNK_SIZE = 300
        private const val TAG = "MediaTransfer"

        // ACK timeout per transfer — if no completion in 60s, declare failed
        private const val TRANSFER_TIMEOUT_MS = 60_000L

        // Inter-chunk delay to prevent BLE TX buffer overflow (Req. 3)
        private const val INTER_CHUNK_DELAY_MS = 30L

        // Max retries per chunk on NACK (Req. 4 / Req. 7)
        private const val MAX_CHUNK_RETRIES = 3

        private val ALLOWED_MIME_PREFIXES = listOf("image/", "audio/")
    }

    // ─────────────────── Public API surfaces ───────────────────

    /** Req. 6: Progress per active transferId (0.0 → 1.0). */
    private val _transferProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val transferProgress: StateFlow<Map<String, Float>> = _transferProgress.asStateFlow()

    /**
     * Callback invoked by BleRepository when a chunk packet needs to be dispatched.
     * Set by BleRepository after construction (avoids circular DI).
     */
    var onSendPacket: ((MeshPacket) -> Unit)? = null

    // ─────────────────── Internal state ───────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Receiver side
    private val incomingBuffers  = ConcurrentHashMap<String, ConcurrentHashMap<Int, String>>()
    private val transferMeta     = ConcurrentHashMap<String, TransferMeta>()
    private val timeoutJobs      = ConcurrentHashMap<String, Job>()

    // Sender side — for ACK tracking and NACK retry
    private val pendingTransfers = ConcurrentHashMap<String, PendingOutbound>()
    private val chunkRetryCount  = ConcurrentHashMap<String, Int>() // "$transferId:$chunkIndex" → retryCount

    // ─────────────────── Data classes ───────────────────

    data class TransferMeta(
        val senderId: String,
        val targetId: String,
        val totalChunks: Int,
        val mimeType: String,
        val sha256: String? = null
    )

    data class CompletedTransfer(
        val transferId: String,
        val senderId: String,
        val filePath: String,
        val mimeType: String
    )

    /** Holds the full packet list for a sent transfer so we can retry on NACK. */
    private data class PendingOutbound(
        val packets: List<MeshPacket>,   // index 0 = META, 1..n = chunks
        val targetId: String
    )

    // ─────────────────── Sender: create + dispatch ───────────────────

    /**
     * Req. 2 & 3: Build chunk packets and dispatch them sequentially with inter-chunk delay.
     * Returns the transferId so the caller can track progress.
     */
    fun createAndSendChunked(
        data: ByteArray,
        senderId: String,
        targetId: String,
        mimeType: String,
        transferId: String = UUID.randomUUID().toString()
    ): String {
        if (ALLOWED_MIME_PREFIXES.none { mimeType.startsWith(it) }) {
            Log.w(TAG, "Rejected send: unsupported MIME '$mimeType'")
            return transferId
        }

        val packets = buildPacketList(data, senderId, targetId, mimeType, transferId)
        if (packets.isEmpty()) return transferId

        pendingTransfers[transferId] = PendingOutbound(packets, targetId)
        updateProgress(transferId, 0f)

        scope.launch {
            dispatchAllPackets(transferId, packets)
        }

        return transferId
    }

    /**
     * Legacy API used by BleRepository.dispatchMediaPackets — kept for compatibility.
     * Creates packet list but does NOT dispatch (caller dispatches via meshRouter).
     */
    fun createChunkedPackets(
        data: ByteArray,
        senderId: String,
        targetId: String,
        mimeType: String,
        transferId: String = UUID.randomUUID().toString()
    ): List<MeshPacket> {
        if (ALLOWED_MIME_PREFIXES.none { mimeType.startsWith(it) }) {
            Log.w(TAG, "Rejected: unsupported MIME '$mimeType'")
            return emptyList()
        }
        return buildPacketList(data, senderId, targetId, mimeType, transferId)
    }

    /**
     * Req. 3 (CRITICAL): Dispatch all packets with INTER_CHUNK_DELAY_MS between each.
     */
    private suspend fun dispatchAllPackets(transferId: String, packets: List<MeshPacket>) {
        val send = onSendPacket ?: run {
            Log.w(TAG, "onSendPacket not set — cannot dispatch $transferId")
            return
        }

        Log.d(TAG, "[$transferId] Dispatching ${packets.size} packets (${packets.size - 1} chunks) to ${packets.first().targetId.takeLast(6)}")
        updateProgress(transferId, 0f)

        packets.forEachIndexed { i, pkt ->
            if (!currentCoroutineContext().isActive) return
            send(pkt)
            if (i < packets.size - 1) {
                delay(INTER_CHUNK_DELAY_MS)
            }
            if (pkt.type == PacketType.MEDIA_CHUNK) {
                val chunksDone = i // i=0 is META
                val total = packets.size - 1
                updateProgress(transferId, chunksDone.toFloat() / total.toFloat())
            }
        }

        Log.d(TAG, "[$transferId] All ${packets.size} packets dispatched")
    }

    private fun buildPacketList(
        data: ByteArray,
        senderId: String,
        targetId: String,
        mimeType: String,
        transferId: String
    ): List<MeshPacket> {
        val base64 = Base64.encodeToString(data, Base64.NO_WRAP)
        val chunks = base64.chunked(CHUNK_SIZE)
        val totalChunks = chunks.size

        Log.d(TAG, "[$transferId] Building: ${data.size}B raw → ${base64.length}B B64 → $totalChunks chunks")

        val packets = mutableListOf<MeshPacket>()

        // Compute SHA-256 of raw data
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val sha256 = md.digest(data).joinToString("") { "%02x".format(it) }

        // Req. 2: META packet first
        packets.add(MeshPacket(
            senderId    = senderId,
            targetId    = targetId,
            payload     = "MEDIA:$mimeType:$sha256",
            type        = PacketType.MEDIA_META,
            transferId  = transferId,
            chunkIndex  = 0,
            totalChunks = totalChunks,
            mimeType    = mimeType,
            ttl         = 10
        ))

        // Chunk packets
        chunks.forEachIndexed { index, chunk ->
            packets.add(MeshPacket(
                senderId    = senderId,
                targetId    = targetId,
                payload     = chunk,
                type        = PacketType.MEDIA_CHUNK,
                transferId  = transferId,
                chunkIndex  = index,
                totalChunks = totalChunks,
                mimeType    = mimeType,
                ttl         = 10
            ))
        }

        return packets
    }

    // ─────────────────── Receiver: handle incoming ───────────────────

    /**
     * Req. 5, 6, 7: Handle all incoming media-related packets.
     * Returns CompletedTransfer when all chunks are assembled successfully.
     */
    fun handleIncomingMediaPacket(packet: MeshPacket): CompletedTransfer? {
        val transferId = packet.transferId ?: run {
            Log.w(TAG, "Received media packet with null transferId, dropping")
            return null
        }

        val mimeType = packet.mimeType ?: "application/octet-stream"

        return when (packet.type) {
            PacketType.MEDIA_META -> handleMeta(packet, transferId, mimeType)
            PacketType.MEDIA_CHUNK -> handleChunk(packet, transferId, mimeType)
            PacketType.MEDIA_ACK -> handleAck(packet, transferId)
            PacketType.MEDIA_NACK -> handleNack(packet, transferId)
            else -> null
        }
    }

    private fun handleMeta(packet: MeshPacket, transferId: String, mimeType: String): CompletedTransfer? {
        if (ALLOWED_MIME_PREFIXES.none { mimeType.startsWith(it) }) {
            Log.w(TAG, "Rejected incoming $transferId: unsupported MIME '$mimeType'")
            return null
        }
        val parts = packet.payload.split(":")
        val parsedSha256 = if (parts.size >= 3) parts[2] else null

        transferMeta[transferId] = TransferMeta(
            senderId    = packet.senderId,
            targetId    = packet.targetId,
            totalChunks = packet.totalChunks,
            mimeType    = mimeType,
            sha256      = parsedSha256
        )
        incomingBuffers.getOrPut(transferId) { ConcurrentHashMap() }
        updateProgress(transferId, 0f)
        startTimeoutWatcher(transferId)
        Log.d(TAG, "[$transferId] META: totalChunks=${packet.totalChunks} mime=$mimeType")
        return null
    }

    private fun handleChunk(packet: MeshPacket, transferId: String, mimeType: String): CompletedTransfer? {
        // Lazily init buffer if META was dropped
        val buffer = incomingBuffers.getOrPut(transferId) { ConcurrentHashMap() }
        if (transferMeta[transferId] == null) {
            transferMeta[transferId] = TransferMeta(
                senderId    = packet.senderId,
                targetId    = packet.targetId,
                totalChunks = packet.totalChunks,
                mimeType    = mimeType
            )
            startTimeoutWatcher(transferId)
            Log.w(TAG, "[$transferId] META was missing, reconstructed from chunk ${packet.chunkIndex}")
        }

        buffer[packet.chunkIndex] = packet.payload

        val meta = transferMeta[transferId] ?: return null
        val progress = buffer.size.toFloat() / meta.totalChunks.coerceAtLeast(1)
        updateProgress(transferId, progress)

        Log.d(TAG, "[$transferId] Chunk ${packet.chunkIndex + 1}/${meta.totalChunks} (${buffer.size} buffered, ${String.format(java.util.Locale.US, "%.0f", progress * 100)}%)")

        // Req. 4: Send ACK back to sender
        sendAck(packet.senderId, packet.targetId, transferId, packet.chunkIndex)

        // Check if complete
        return if (buffer.size >= meta.totalChunks) {
            assembleTransfer(transferId, meta, buffer)
        } else null
    }

    /** Req. 4: Sender received an ACK — update outbound progress tracking. */
    private fun handleAck(packet: MeshPacket, transferId: String): CompletedTransfer? {
        val pending = pendingTransfers[transferId] ?: return null
        val ackedIndex = packet.chunkIndex
        val total = pending.packets.size - 1 // exclude META
        val progress = (ackedIndex + 1).toFloat() / total.toFloat()
        updateProgress(transferId, progress.coerceAtMost(1f))

        if (ackedIndex >= total - 1) {
            // All chunks ACKed
            pendingTransfers.remove(transferId)
            Log.d(TAG, "[$transferId] All $total chunks ACKed by receiver")
        }
        return null
    }

    /**
     * Req. 4 & 7: Sender received a NACK for specific missing chunk indices.
     * Payload format: comma-separated chunk indices, e.g. "3,7,12"
     */
    private fun handleNack(packet: MeshPacket, transferId: String): CompletedTransfer? {
        val pending = pendingTransfers[transferId] ?: run {
            Log.w(TAG, "[$transferId] NACK received but no pending outbound transfer")
            return null
        }

        val missingIndices = packet.payload.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (missingIndices.isEmpty()) return null

        Log.d(TAG, "[$transferId] NACK: retrying chunks $missingIndices")

        scope.launch {
            val send = onSendPacket ?: return@launch
            missingIndices.forEach { idx ->
                val retryKey = "$transferId:$idx"
                val retries = chunkRetryCount.getOrDefault(retryKey, 0)
                if (retries >= MAX_CHUNK_RETRIES) {
                    Log.e(TAG, "[$transferId] Chunk $idx exceeded max retries ($MAX_CHUNK_RETRIES), giving up")
                    return@forEach
                }
                chunkRetryCount[retryKey] = retries + 1

                // Packet at index idx+1 (index 0 is META)
                val chunkPacket = pending.packets.getOrNull(idx + 1)
                if (chunkPacket != null) {
                    send(chunkPacket)
                    delay(INTER_CHUNK_DELAY_MS)
                    Log.d(TAG, "[$transferId] Retried chunk $idx (attempt ${retries + 1})")
                }
            }
        }
        return null
    }

    // ─────────────────── ACK/NACK senders ───────────────────

    /**
     * Req. 4: Send ACK packet back to the original sender confirming receipt of a chunk.
     */
    private fun sendAck(
        originalSenderId: String,
        localId: String,
        transferId: String,
        chunkIndex: Int
    ) {
        val ack = MeshPacket(
            senderId   = localId,
            targetId   = originalSenderId,
            payload    = chunkIndex.toString(),
            type       = PacketType.MEDIA_ACK,
            transferId = transferId,
            chunkIndex = chunkIndex,
            ttl        = 5
        )
        onSendPacket?.invoke(ack)
    }

    /**
     * Req. 4 & 7: Send NACK with missing chunk indices to request retransmission.
     */
    private fun sendNack(
        originalSenderId: String,
        localId: String,
        transferId: String,
        missingIndices: List<Int>
    ) {
        if (missingIndices.isEmpty()) return
        val nack = MeshPacket(
            senderId   = localId,
            targetId   = originalSenderId,
            payload    = missingIndices.joinToString(","),
            type       = PacketType.MEDIA_NACK,
            transferId = transferId,
            ttl        = 5
        )
        onSendPacket?.invoke(nack)
        Log.d(TAG, "[$transferId] Sent NACK for missing chunks: $missingIndices")
    }

    // ─────────────────── Req. 7: Timeout watcher ───────────────────

    /**
     * Req. 7: Start a timeout coroutine per transfer.
     * After TRANSFER_TIMEOUT_MS, check for missing chunks and send NACK or clean up.
     */
    private fun startTimeoutWatcher(transferId: String) {
        timeoutJobs[transferId]?.cancel()
        timeoutJobs[transferId] = scope.launch {
            delay(TRANSFER_TIMEOUT_MS)

            val meta   = transferMeta[transferId] ?: return@launch
            val buffer = incomingBuffers[transferId] ?: return@launch

            val missing = (0 until meta.totalChunks).filter { !buffer.containsKey(it) }
            if (missing.isEmpty()) {
                // Might have assembled already; clean up silently
                cleanupIncoming(transferId)
                return@launch
            }

            Log.w(TAG, "[$transferId] Transfer timed out — missing ${missing.size}/${meta.totalChunks} chunks: ${missing.take(10)}")

            // Send NACK for all missing chunks (Req. 7 automatic retry)
            sendNack(meta.senderId, meta.targetId, transferId, missing)

            // Give 30 more seconds for the retry, then hard-fail
            delay(30_000L)
            if (incomingBuffers.containsKey(transferId)) {
                Log.e(TAG, "[$transferId] Transfer permanently failed after timeout + retry")
                cleanupIncoming(transferId)
                updateProgress(transferId, -1f) // Signal error to UI
            }
        }
    }

    // ─────────────────── Req. 5: Reassembly ───────────────────

    /**
     * Req. 5: Assemble chunks in strict order, validate all present, decode Base64 safely.
     */
    private fun assembleTransfer(
        transferId: String,
        meta: TransferMeta,
        buffer: ConcurrentHashMap<Int, String>
    ): CompletedTransfer? {
        return try {
            // Validate every chunk 0..totalChunks-1 is present
            val missing = (0 until meta.totalChunks).filter { !buffer.containsKey(it) }
            if (missing.isNotEmpty()) {
                Log.e(TAG, "[$transferId] Cannot assemble: missing chunks $missing")
                sendNack(meta.senderId, meta.targetId, transferId, missing)
                return null
            }

            // Req. 5: Reassemble in strict order
            val fullBase64 = buildString(capacity = meta.totalChunks * CHUNK_SIZE) {
                for (i in 0 until meta.totalChunks) {
                    append(buffer[i]!!)
                }
            }

            // Req. 5: Decode Base64 safely
            val fileBytes = try {
                Base64.decode(fullBase64, Base64.NO_WRAP)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "[$transferId] Base64 decode failed: ${e.message}")
                cleanupIncoming(transferId)
                return null
            }

            if (fileBytes.isEmpty()) {
                Log.e(TAG, "[$transferId] Decoded bytes are empty")
                cleanupIncoming(transferId)
                return null
            }

            // Verify checksum if available
            if (meta.sha256 != null) {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val computedSha256 = md.digest(fileBytes).joinToString("") { "%02x".format(it) }
                if (computedSha256 != meta.sha256) {
                    Log.e(TAG, "[$transferId] Checksum mismatch: expected ${meta.sha256}, got $computedSha256")
                    cleanupIncoming(transferId)
                    return null
                }
            }

            val extension = mimeToExtension(meta.mimeType)
            val mediaDir = File(context.filesDir, "mesh_media").also { if (!it.exists()) it.mkdirs() }
            val outputFile = File(mediaDir, "$transferId$extension")
            outputFile.writeBytes(fileBytes)

            cleanupIncoming(transferId)
            updateProgress(transferId, 1f)

            Log.d(TAG, "[$transferId] Assembled ${fileBytes.size}B → ${outputFile.absolutePath}")

            CompletedTransfer(
                transferId = transferId,
                senderId   = meta.senderId,
                filePath   = outputFile.absolutePath,
                mimeType   = meta.mimeType
            )
        } catch (e: Exception) {
            Log.e(TAG, "[$transferId] Assembly exception: ${e.message}", e)
            cleanupIncoming(transferId)
            null
        }
    }

    // ─────────────────── Helpers ───────────────────

    private fun mimeToExtension(mime: String): String = when {
        mime.contains("jpeg") || mime.contains("jpg") -> ".jpg"
        mime.contains("png")  -> ".png"
        mime.contains("webp") -> ".webp"
        mime.contains("image")-> ".jpg"
        mime.contains("audio")-> ".m4a"
        else                  -> ".bin"
    }

    private fun cleanupIncoming(transferId: String) {
        incomingBuffers.remove(transferId)
        transferMeta.remove(transferId)
        timeoutJobs.remove(transferId)?.cancel()
    }

    /** Req. 6: Update progress map on main-safe StateFlow. */
    private fun updateProgress(transferId: String, progress: Float) {
        _transferProgress.update { current ->
            current.toMutableMap().apply {
                this[transferId] = progress
            }
        }
    }
}
