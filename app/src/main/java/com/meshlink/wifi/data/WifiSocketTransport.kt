package com.meshlink.wifi.data

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.util.MeshPacketParser
import com.meshlink.config.WifiConfig
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.policy.EncryptionRequirement
import com.meshlink.security.policy.PacketEncryptionPolicy
import com.meshlink.transfer.ReceivedMediaNaming
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed class AudioStreamResult {
    object Success : AudioStreamResult()
    data class Failed(val reason: String) : AudioStreamResult()
}

data class HandshakeResult(val status: Int, val resumeOffset: Long)

private data class AudioReceiveSession(
    val transferId: String,
    val fileName: String,
    val mimeType: String,
    val totalBytes: Long,
    val expectedChecksum: String,
    val senderId: String,
    val tempFile: File,
    val digest: MessageDigest,
    /** Receiver-local, per-transfer unique destination (never derived directly from remote input). */
    val finalFile: File,
    /** Connection that opened this session; chunks/END from any other connection are ignored. */
    val clientHost: String
)

enum class WifiSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

data class WifiSocketMetrics(
    val packetsSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val activePeers: Int = 0,
    val reconnectAttempts: Int = 0,
    val heartbeatCount: Long = 0L,
    val averageLatencyMs: Long = 0L
)

@Singleton
class WifiSocketTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "WifiSocketTransport"
        private const val PORT = WifiConfig.DEFAULT_PORT
        private const val CONNECT_TIMEOUT_MS = 2_500
        private const val READ_TIMEOUT_MS = 10_000
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
        private const val HEARTBEAT_WATCHDOG_TIMEOUT_MS = 30_000L
        private const val MAX_BACKOFF_MS = 30_000L
        const val MAX_FRAME_SIZE_BYTES = 4 * 1024 * 1024 // 4MB strict limit for single packet frames
        const val MAX_MEDIA_STREAM_BYTES = 256L * 1024 * 1024 // reject absurd advertised sizes
        // AES-GCM adds a 12 byte IV and a 16 byte tag to every plaintext chunk.
        const val MAX_ENCRYPTED_CHUNK_BYTES = 16 * 1024 + 256

        fun isValidFrameLength(length: Int): Boolean = length in 1..MAX_FRAME_SIZE_BYTES

        fun isValidEncryptedChunkLength(length: Int): Boolean = length in 1..MAX_ENCRYPTED_CHUNK_BYTES

        // High-Speed Binary Streaming Magic Protocol Constants
        const val MAGIC_MEDIA_STREAM_START = -0x5354524D // "STRM" (-1398035021)
        const val MAGIC_MEDIA_STREAM_END = -0x454E445F   // "END_" (-1162757217)
        const val MAGIC_STREAM_HANDSHAKE = -0x5348414B   // "SHAK" (-1287077302)
        const val MAGIC_STREAM_READY = -0x5244595F       // "RDY_" (-1370881953)
        const val MAGIC_STREAM_ACK = -0x41434B5F         // "ACK_" (-1145391265)
        const val MAGIC_STREAM_RESUME_REQ = -0x5253554D  // "RSUM" (-1381116605)
        const val MAGIC_AUDIO_CHUNK = -0x4155444F        // "AUDO" (-1096449968)
        const val STREAM_BUFFER_SIZE = 128 * 1024        // 128 KB high-throughput buffer
        const val AUDIO_CHUNK_SIZE = 16 * 1024           // 16 KB voice chunk size
    }

    private var serverSocket: ServerSocket? = null
    private var reconnectJob: Job? = null
    private var lastHostAddress: String? = null
    private var backoffDelayMs = 1000L
    private var reconnectAttempts = 0
    private var manualDisconnectRequested = false

    // Concurrent multi-peer maps
    private val clientSockets = ConcurrentHashMap<String, Socket>()
    private val clientStreamsOut = ConcurrentHashMap<String, DataOutputStream>()
    private val clientStreamsIn = ConcurrentHashMap<String, DataInputStream>()
    private val clientReadJobs = ConcurrentHashMap<String, Job>()
    private val clientHeartbeatJobs = ConcurrentHashMap<String, Job>()
    private val clientLastHeartbeatMs = ConcurrentHashMap<String, Long>()
    private val peerHostsByMeshId = ConcurrentHashMap<String, String>()

    // Audio stream synchronization and session maps
    private val pendingHandshakes = ConcurrentHashMap<String, CompletableDeferred<HandshakeResult>>()
    private val pendingAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val activeAudioReceivers = ConcurrentHashMap<String, AudioReceiveSession>()
    // transferId -> host the pending handshake/ack is riding on, so a socket failure only aborts its own transfers
    private val pendingTransferHosts = ConcurrentHashMap<String, String>()

    // Connection State & Metrics Flow
    private val _connectionState = MutableStateFlow(WifiSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WifiSocketConnectionState> = _connectionState.asStateFlow()

    private val _metricsState = MutableStateFlow(WifiSocketMetrics())
    val metricsState: StateFlow<WifiSocketMetrics> = _metricsState.asStateFlow()

    // Callbacks
    var onPacketReceived: ((MeshPacket) -> Unit)? = null
    var onSocketConnected: (() -> Unit)? = null

    // High-speed Media Stream Callbacks
    var onMediaStreamStarted: ((transferId: String, fileName: String, mimeType: String, totalBytes: Long, checksum: String, senderId: String) -> Unit)? = null
    var onMediaStreamProgress: ((transferId: String, bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    var onMediaStreamCompleted: ((transferId: String, filePath: String, mimeType: String, senderId: String, totalBytes: Long) -> Unit)? = null
    var onMediaStreamFailed: ((transferId: String, reason: String) -> Unit)? = null

    // Metric Counters
    private val packetsSentCounter = AtomicLong(0L)
    private val packetsReceivedCounter = AtomicLong(0L)
    private val bytesSentCounter = AtomicLong(0L)
    private val bytesReceivedCounter = AtomicLong(0L)
    private val heartbeatCounter = AtomicLong(0L)

    private fun updateMetrics(transform: (WifiSocketMetrics) -> WifiSocketMetrics) {
        _metricsState.update(transform)
    }

    fun startServer() {
        manualDisconnectRequested = false
        if (serverSocket != null && !serverSocket!!.isClosed) return
        try {
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(PORT))
            }
            MeshLogger.i("AUDIO_SOCKET_CONNECTING", "[AUDIO_SOCKET_CONNECTING] ServerSocket listening on port $PORT (reuseAddress=true)")
            MeshLogger.d(TAG, "ServerSocket started on port $PORT. Listening for multi-peer connections...")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start ServerSocket on port $PORT: ${e.message}")
            _connectionState.value = WifiSocketConnectionState.FAILED
            return
        }

        _connectionState.value = WifiSocketConnectionState.CONNECTING

        applicationScope.launch(Dispatchers.IO) {
            try {
                while (isActive && serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    val clientHost = client.inetAddress?.hostAddress ?: "unknown"
                    MeshLogger.i("AUDIO_SOCKET_CONNECTED", "[AUDIO_SOCKET_CONNECTED] Client connected to Group Owner: $clientHost")
                    MeshLogger.d(TAG, "Client connected to Group Owner: $clientHost")
                    
                    handleSocketConnection(client, isServerMode = true, clientHost = clientHost)
                }
            } catch (e: Exception) {
                if (isActive) {
                    MeshLogger.e(TAG, "ServerSocket accept loop error: ${e.message}")
                }
            }
        }
    }

    fun stopServer() {
        try {
            clientSockets.keys.toList().forEach { host ->
                cleanupPeer(host)
            }

            serverSocket?.close()
            serverSocket = null
            MeshLogger.d(TAG, "ServerSocket stopped successfully")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping ServerSocket: ${e.message}")
        }
    }

    fun registerPeerMeshHost(meshId: String, hostAddress: String) {
        val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        if (canonicalId.isNotBlank() && hostAddress.isNotBlank()) {
            peerHostsByMeshId[canonicalId] = hostAddress
            peerHostsByMeshId[meshId] = hostAddress
            MeshLogger.d(TAG, "Registered peer mesh host: $canonicalId -> $hostAddress")
        }
    }

    fun connectAsClient(hostAddress: String) {
        manualDisconnectRequested = false
        lastHostAddress = hostAddress
        backoffDelayMs = 1000L
        reconnectJob?.cancel()
        reconnectJob = null
        _connectionState.value = WifiSocketConnectionState.CONNECTING

        applicationScope.launch(Dispatchers.IO) {
            MeshLogger.i("AUDIO_SOCKET_CONNECTING", "[AUDIO_SOCKET_CONNECTING] Connecting to Group Owner at $hostAddress:$PORT...")
            var lastException: Exception? = null
            for (attempt in 1..3) {
                try {
                    val socket = Socket()
                    socket.reuseAddress = true
                    socket.bind(null)
                    socket.connect(InetSocketAddress(hostAddress, PORT), CONNECT_TIMEOUT_MS)
                    MeshLogger.i("AUDIO_SOCKET_CONNECTED", "[AUDIO_SOCKET_CONNECTED] Connected to Group Owner $hostAddress:$PORT (attempt $attempt)")
                    MeshLogger.d(TAG, "Socket Opened: Connected to Group Owner $hostAddress:$PORT (attempt $attempt)")

                    // Reset backoff on successful connection
                    backoffDelayMs = 1000L
                    reconnectAttempts = 0
                    handleSocketConnection(socket, isServerMode = false, clientHost = hostAddress)
                    return@launch
                } catch (e: Exception) {
                    lastException = e
                    MeshLogger.d(TAG, "Client socket attempt $attempt to $hostAddress failed: ${e.message}")
                    if (attempt < 3 && isActive) {
                        delay(300L)
                    }
                }
            }
            MeshLogger.e(TAG, "Client socket error connecting to $hostAddress: ${lastException?.message}")
            _connectionState.value = WifiSocketConnectionState.FAILED
            scheduleReconnect()
        }
    }

    private fun handleSocketConnection(socket: Socket, isServerMode: Boolean, clientHost: String) {
        try {
            socket.soTimeout = READ_TIMEOUT_MS
            socket.tcpNoDelay = true
            socket.sendBufferSize = 2 * 1024 * 1024 // 2 MB buffer
            socket.receiveBufferSize = 2 * 1024 * 1024 // 2 MB buffer

            val bufferedOut = BufferedOutputStream(socket.getOutputStream(), STREAM_BUFFER_SIZE)
            val bufferedIn = BufferedInputStream(socket.getInputStream(), STREAM_BUFFER_SIZE)

            val currentOut = DataOutputStream(bufferedOut)
            val currentIn = DataInputStream(bufferedIn)

            clientSockets[clientHost] = socket
            clientStreamsOut[clientHost] = currentOut
            clientStreamsIn[clientHost] = currentIn
            clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()

            _connectionState.value = WifiSocketConnectionState.CONNECTED
            updateMetrics { it.copy(activePeers = clientSockets.size) }

            // Trigger connection notification
            onSocketConnected?.invoke()

            // Start dedicated heartbeat monitoring for this connection
            startHeartbeat(socket, currentOut, clientHost)

            // Dedicated binary read loop coroutine per client connection
            val readJob = applicationScope.launch(Dispatchers.IO) {
                try {
                    while (isActive && !socket.isClosed) {
                        val length = try {
                            currentIn.readInt()
                        } catch (e: SocketTimeoutException) {
                            // Check heartbeat watchdog timeout (30s)
                            val lastHb = clientLastHeartbeatMs[clientHost] ?: System.currentTimeMillis()
                            if (System.currentTimeMillis() - lastHb > HEARTBEAT_WATCHDOG_TIMEOUT_MS) {
                                MeshLogger.w(TAG, "Heartbeat Watchdog Timeout (>30s) for $clientHost")
                                break
                            }
                            continue
                        } catch (e: EOFException) {
                            MeshLogger.d(TAG, "EOF reached for $clientHost")
                            break
                        } catch (e: Exception) {
                            if (isActive && !socket.isClosed) {
                                MeshLogger.e(TAG, "Socket binary read error on $clientHost: ${e.message}")
                            }
                            break
                        }

                        when (length) {
                            0 -> {
                                // Length = 0 is Heartbeat Ping
                                clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                                val hbCount = heartbeatCounter.incrementAndGet()
                                updateMetrics { it.copy(heartbeatCount = hbCount) }
                                MeshLogger.d(TAG, "Received Binary Heartbeat Ping from $clientHost")
                            }
                            MAGIC_STREAM_HANDSHAKE -> {
                                clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                                val transferId = currentIn.readUTF()
                                val fileName = currentIn.readUTF()
                                val mimeType = currentIn.readUTF()
                                val totalBytes = currentIn.readLong()
                                val expectedChecksum = currentIn.readUTF()
                                val senderId = currentIn.readUTF()

                                if (totalBytes <= 0L || totalBytes > MAX_MEDIA_STREAM_BYTES) {
                                    // Header was fully consumed, so the stream stays in sync; just refuse the transfer.
                                    MeshLogger.w(TAG, "Rejecting media handshake $transferId from $clientHost: invalid size ${totalBytes}B")
                                    continue
                                }
                                peerHostsByMeshId[senderId] = clientHost

                                val isAudio = mimeType.startsWith("audio/")
                                if (isAudio) {
                                    MeshLogger.i("AUDIO_SESSION", "[AUDIO_SESSION] session=$transferId file=$fileName size=${totalBytes}B mime=$mimeType transport=WIFI_DIRECT")
                                    MeshLogger.i("AUDIO_HANDSHAKE", "[AUDIO_HANDSHAKE] session=$transferId received handshake from $senderId file=$fileName size=${totalBytes}B")
                                }

                                val mediaDir = File(context.filesDir, "mesh_media").apply { if (!exists()) mkdirs() }
                                // fileName / transferId come from the remote peer: never use them as raw path components.
                                val finalFile = ReceivedMediaNaming.resolveInside(
                                    mediaDir,
                                    ReceivedMediaNaming.receivedFileName(transferId, fileName)
                                )

                                if (finalFile.exists() && finalFile.length() == totalBytes) {
                                    if (isAudio) {
                                        MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId already complete. Sending immediate ACK to $clientHost")
                                    }
                                    synchronized(currentOut) {
                                        currentOut.writeInt(MAGIC_STREAM_ACK)
                                        currentOut.writeUTF(transferId)
                                        currentOut.flush()
                                    }
                                    try {
                                        onMediaStreamCompleted?.invoke(transferId, finalFile.absolutePath, mimeType, senderId, totalBytes)
                                    } catch (e: Exception) {
                                        MeshLogger.e(TAG, "Error in onMediaStreamCompleted for handshake: ${e.message}", e)
                                    }
                                } else {
                                    val tempDir = File(context.cacheDir, "wifi_media_temp").apply { if (!exists()) mkdirs() }
                                    val tempFile = ReceivedMediaNaming.resolveInside(
                                        tempDir,
                                        "stream_${ReceivedMediaNaming.safeTransferId(transferId)}.tmp"
                                    )
                                    var resumeOffset = if (tempFile.exists() && tempFile.length() in 1 until totalBytes) {
                                        tempFile.length()
                                    } else {
                                        if (tempFile.exists()) tempFile.delete()
                                        0L
                                    }

                                    val digest = MessageDigest.getInstance("SHA-256")
                                    if (resumeOffset > 0L && tempFile.exists()) {
                                        try {
                                            FileInputStream(tempFile).use { fis ->
                                                val sBuf = ByteArray(STREAM_BUFFER_SIZE)
                                                var readSoFar = 0L
                                                while (readSoFar < resumeOffset) {
                                                    val toRead = minOf(sBuf.size.toLong(), resumeOffset - readSoFar).toInt()
                                                    val r = fis.read(sBuf, 0, toRead)
                                                    if (r <= 0) break
                                                    digest.update(sBuf, 0, r)
                                                    readSoFar += r
                                                }
                                            }
                                        } catch (_: Exception) {
                                            tempFile.delete()
                                            resumeOffset = 0L
                                        }
                                    }

                                    activeAudioReceivers[transferId] = AudioReceiveSession(
                                        transferId = transferId,
                                        fileName = fileName,
                                        mimeType = mimeType,
                                        totalBytes = totalBytes,
                                        expectedChecksum = expectedChecksum,
                                        senderId = senderId,
                                        tempFile = tempFile,
                                        digest = digest,
                                        finalFile = finalFile,
                                        clientHost = clientHost
                                    )

                                    if (isAudio) {
                                        MeshLogger.i("AUDIO_RECEIVER", "[AUDIO_RECEIVER] session=$transferId ready. resumeOffset=$resumeOffset tempPath=${tempFile.absolutePath}")
                                    }

                                    onMediaStreamStarted?.invoke(transferId, fileName, mimeType, totalBytes, expectedChecksum, senderId)
                                    if (resumeOffset > 0L) {
                                        onMediaStreamProgress?.invoke(transferId, resumeOffset, totalBytes)
                                    }

                                    val responseMagic = if (resumeOffset > 0L) MAGIC_STREAM_RESUME_REQ else MAGIC_STREAM_READY
                                    synchronized(currentOut) {
                                        currentOut.writeInt(responseMagic)
                                        currentOut.writeUTF(transferId)
                                        currentOut.writeLong(resumeOffset)
                                        currentOut.flush()
                                    }
                                }
                            }
                            MAGIC_STREAM_READY, MAGIC_STREAM_RESUME_REQ -> {
                                clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                                val transferId = currentIn.readUTF()
                                val resumeOffset = currentIn.readLong()
                                pendingHandshakes[transferId]?.complete(HandshakeResult(length, resumeOffset))
                            }
                            MAGIC_STREAM_ACK -> {
                                clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                                val transferId = currentIn.readUTF()
                                MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId ACK received from $clientHost")
                                pendingAcks[transferId]?.complete(true)
                                pendingHandshakes[transferId]?.complete(HandshakeResult(MAGIC_STREAM_ACK, 0L))
                            }
                            MAGIC_AUDIO_CHUNK -> {
                                clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                                val transferId = currentIn.readUTF()
                                val chunkIndex = currentIn.readInt()
                                val totalChunks = currentIn.readInt()
                                val offset = currentIn.readLong()
                                val encryptedLen = currentIn.readInt()
                                if (chunkIndex < 0 || totalChunks <= 0 || chunkIndex >= totalChunks || offset < 0L || !isValidEncryptedChunkLength(encryptedLen)) {
                                    // An attacker-controlled length or metadata must never size an allocation (OOM / negative size).
                                    MeshLogger.e(TAG, "Invalid chunk framing (idx=$chunkIndex/$totalChunks, offset=$offset, len=$encryptedLen) from $clientHost. Closing connection.")
                                    break
                                }
                                val encryptedBytes = ByteArray(encryptedLen)
                                currentIn.readFully(encryptedBytes)

                                // Only the connection that opened the session may feed it.
                                val session = activeAudioReceivers[transferId]?.takeIf { it.clientHost == clientHost }
                                if (session != null) {
                                    try {
                                        val plainBytes = cryptoManager.decryptBytes(encryptedBytes, session.senderId)
                                        if (session.tempFile.length() + plainBytes.size.toLong() > session.totalBytes) {
                                            MeshLogger.e(TAG, "Chunk write would exceed declared totalBytes (${session.totalBytes}) for $transferId. Aborting session.")
                                            activeAudioReceivers.remove(transferId, session)
                                            session.tempFile.delete()
                                            break
                                        }
                                        FileOutputStream(session.tempFile, true).use { fos ->
                                            fos.write(plainBytes)
                                            fos.flush()
                                        }
                                        session.digest.update(plainBytes)

                                        val currentLen = session.tempFile.length()
                                        val rxBytes = bytesReceivedCounter.addAndGet(encryptedLen.toLong() + 20L)
                                        updateMetrics { it.copy(bytesReceived = rxBytes) }

                                        val pct = if (session.totalBytes > 0) ((currentLen * 100) / session.totalBytes).toInt() else 0
                                        MeshLogger.d("AUDIO_CHUNK", "[AUDIO_CHUNK] session=$transferId chunk=$chunkIndex/$totalChunks bytes=${plainBytes.size} received=$currentLen/${session.totalBytes} ($pct%)")
                                        MeshLogger.d("AUDIO_RECEIVER", "[AUDIO_RECEIVER] session=$transferId chunk=$chunkIndex receivedBytes=$currentLen")
                                        onMediaStreamProgress?.invoke(transferId, currentLen, session.totalBytes)
                                    } catch (e: Exception) {
                                        MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId chunk decryption/write error: ${e.message}", e)
                                    }
                                } else {
                                    MeshLogger.w(TAG, "Received chunk for unknown/uninitialized session $transferId from $clientHost")
                                }
                            }
                            MAGIC_MEDIA_STREAM_END -> {
                                clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                                val transferId = currentIn.readUTF()
                                val senderChecksum = currentIn.readUTF()

                                val audioSession = activeAudioReceivers[transferId]
                                    ?.takeIf { it.clientHost == clientHost }
                                    ?.also { activeAudioReceivers.remove(transferId, it) }
                                if (audioSession != null) {
                                    val localChecksum = audioSession.digest.digest().joinToString("") { "%02x".format(it) }
                                    val isChecksumMatch = localChecksum.equals(audioSession.expectedChecksum, ignoreCase = true) ||
                                            localChecksum.equals(senderChecksum, ignoreCase = true) ||
                                            audioSession.expectedChecksum.isBlank()
                                    val isLengthMatch = audioSession.tempFile.length() == audioSession.totalBytes

                                    if (isLengthMatch && isChecksumMatch) {
                                        val finalFile = audioSession.finalFile
                                        if (!audioSession.tempFile.renameTo(finalFile)) {
                                            audioSession.tempFile.copyTo(finalFile, overwrite = true)
                                            audioSession.tempFile.delete()
                                        }

                                        // Send stream ACK immediately so sender never times out
                                        synchronized(currentOut) {
                                            currentOut.writeInt(MAGIC_STREAM_ACK)
                                            currentOut.writeUTF(transferId)
                                            currentOut.flush()
                                        }

                                        MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId sent stream ACK to $clientHost")
                                        MeshLogger.i("AUDIO_COMPLETE", "[AUDIO_COMPLETE] session=$transferId file=${finalFile.name} size=${finalFile.length()}B verified=true")

                                        try {
                                            onMediaStreamCompleted?.invoke(transferId, finalFile.absolutePath, audioSession.mimeType, audioSession.senderId, audioSession.totalBytes)
                                        } catch (e: Exception) {
                                            MeshLogger.e(TAG, "Error in onMediaStreamCompleted: ${e.message}", e)
                                        }
                                    } else {
                                        val err = "Validation failed: lengthMatch=$isLengthMatch (got=${audioSession.tempFile.length()}, expected=${audioSession.totalBytes}), checksumMatch=$isChecksumMatch"
                                        MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId $err")
                                        audioSession.tempFile.delete()
                                        try {
                                            onMediaStreamFailed?.invoke(transferId, err)
                                        } catch (e: Exception) {
                                            MeshLogger.e(TAG, "Error in onMediaStreamFailed: ${e.message}", e)
                                        }
                                    }
                                } else {
                                    // audioSession was null: check if final file was already received & verified
                                    val mediaDir = File(context.filesDir, "mesh_media")
                                    val idPrefix = ReceivedMediaNaming.safeTransferId(transferId) + "__"
                                    val existingFile = mediaDir.listFiles()?.firstOrNull { it.name.startsWith(idPrefix) }
                                    if (existingFile != null && existingFile.exists() && existingFile.length() > 0L) {
                                        MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId already complete on disk. Sending ACK to $clientHost")
                                        synchronized(currentOut) {
                                            currentOut.writeInt(MAGIC_STREAM_ACK)
                                            currentOut.writeUTF(transferId)
                                            currentOut.flush()
                                        }
                                    } else {
                                        MeshLogger.w(TAG, "MAGIC_MEDIA_STREAM_END received for unknown session $transferId from $clientHost")
                                    }
                                }
                            }
                            MAGIC_MEDIA_STREAM_START -> {
                                // Legacy raw stream: plaintext, unauthenticated and never acknowledged. Every sender in
                                // this build uses the encrypted MAGIC_STREAM_HANDSHAKE/MAGIC_AUDIO_CHUNK protocol, so
                                // this frame can only come from a peer trying to bypass the encryption pipeline.
                                // Its body length is unknown, so the only safe response is to drop the connection.
                                MeshLogger.w(TAG, "Rejecting legacy unencrypted media stream from $clientHost. Closing connection.")
                                break
                            }
                            else -> {
                                if (!isValidFrameLength(length)) {
                                    MeshLogger.e(TAG, "Invalid or oversized frame length $length (max=$MAX_FRAME_SIZE_BYTES) from $clientHost. Closing connection.")
                                    break
                                }
                                val payloadBytes = ByteArray(length)
                                currentIn.readFully(payloadBytes)

                                clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                                val rxBytes = bytesReceivedCounter.addAndGet(length.toLong() + 4L)
                                val rxPkts = packetsReceivedCounter.incrementAndGet()
                                updateMetrics { it.copy(bytesReceived = rxBytes, packetsReceived = rxPkts) }

                                val jsonString = String(payloadBytes, Charsets.UTF_8)
                                val packet = MeshPacketParser.fromJson(jsonString)
                                if (packet != null) {
                                    peerHostsByMeshId[packet.senderId] = clientHost
                                    MeshLogger.d(TAG, "Packet Received over Wi-Fi Direct from $clientHost: ${packet.packetId} (${length}B)")
                                    onPacketReceived?.invoke(packet)
                                }
                            }
                        }
                    }
                } finally {
                    MeshLogger.d(TAG, "Socket Closed: Binary stream ended for $clientHost")
                    cleanupPeer(clientHost)

                    if (!isServerMode) {
                        _connectionState.value = WifiSocketConnectionState.DISCONNECTED
                        scheduleReconnect()
                    }
                }
            }

            clientReadJobs[clientHost] = readJob
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to setup socket streams for $clientHost: ${e.message}")
            cleanupPeer(clientHost)
            if (!isServerMode) {
                _connectionState.value = WifiSocketConnectionState.FAILED
                scheduleReconnect()
            }
        }
    }

    private fun startHeartbeat(socket: Socket, outStream: DataOutputStream, clientHost: String) {
        val heartbeat = applicationScope.launch(Dispatchers.IO) {
            while (isActive && !socket.isClosed) {
                delay(HEARTBEAT_INTERVAL_MS)
                try {
                    synchronized(outStream) {
                        outStream.writeInt(0) // 0-length frame = heartbeat ping
                        outStream.flush()
                    }
                    val txBytes = bytesSentCounter.addAndGet(4L)
                    updateMetrics { it.copy(bytesSent = txBytes) }
                    MeshLogger.d(TAG, "Binary Heartbeat ping sent to $clientHost")
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "Heartbeat Failed to $clientHost: ${e.message}")
                    break
                }
            }
        }

        clientHeartbeatJobs[clientHost]?.cancel()
        clientHeartbeatJobs[clientHost] = heartbeat
    }

    private fun scheduleReconnect() {
        if (manualDisconnectRequested) {
            MeshLogger.d(TAG, "Manual disconnect was requested. Suppressing reconnect.")
            _connectionState.value = WifiSocketConnectionState.DISCONNECTED
            return
        }
        if (isConnected()) {
            MeshLogger.d(TAG, "Already connected to an active peer. Suppressing reconnect.")
            return
        }
        if (reconnectAttempts >= 3) {
            MeshLogger.d(TAG, "Max reconnect attempts ($reconnectAttempts) reached. Halting persistent reconnect.")
            return
        }
        val targetHost = lastHostAddress ?: return

        reconnectJob?.cancel()
        reconnectJob = applicationScope.launch(Dispatchers.IO) {
            _connectionState.value = WifiSocketConnectionState.RECONNECTING
            reconnectAttempts++
            updateMetrics { it.copy(reconnectAttempts = reconnectAttempts) }
            MeshLogger.d(TAG, "Scheduling persistent reconnect attempt #$reconnectAttempts to $targetHost in ${backoffDelayMs}ms...")
            delay(backoffDelayMs)
            
            // Persistent exponential backoff capped at 30 seconds
            backoffDelayMs = (backoffDelayMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            connectAsClient(targetHost)
        }
    }

    suspend fun streamAudioFile(
        transferId: String,
        file: File,
        mimeType: String,
        expectedChecksum: String,
        senderId: String,
        targetPeerAddress: String? = null,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): AudioStreamResult = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead()) {
            val err = "Cannot stream audio: ${file.absolutePath} does not exist or cannot be read"
            MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId $err")
            return@withContext AudioStreamResult.Failed(err)
        }

        if (clientStreamsOut.isEmpty()) {
            val err = "No active Wi-Fi Direct socket streams available for session $transferId"
            MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId $err")
            return@withContext AudioStreamResult.Failed(err)
        }

        val totalBytes = file.length()
        val fileName = file.name

        // Exactly one socket, or none. (The previous 192.168.49.x "first host" rule picked an arbitrary
        // client when the Group Owner had several, delivering media to the wrong peer.)
        val resolvedHost = WifiStreamHostResolver.resolve(
            requested = targetPeerAddress,
            connectedHosts = clientStreamsOut.keys.toSet(),
            peerHostsByMeshId = peerHostsByMeshId
        )
        if (resolvedHost != null && !targetPeerAddress.isNullOrBlank()) {
            registerPeerMeshHost(targetPeerAddress, resolvedHost)
        }
        val targetStreams = resolvedHost?.let { host ->
            clientStreamsOut[host]?.let { stream -> listOf(host to stream) }
        } ?: emptyList()

        if (targetStreams.isEmpty()) {
            val err = "Target peer stream not found for selected peer: $targetPeerAddress (connectedHosts=${clientStreamsOut.keys})"
            MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId $err")
            return@withContext AudioStreamResult.Failed(err)
        }

        for ((host, stream) in targetStreams) {
            try {
                MeshLogger.i("AUDIO_SESSION", "[AUDIO_SESSION] session=$transferId file=$fileName size=${totalBytes}B mime=$mimeType transport=WIFI_DIRECT")
                MeshLogger.i("AUDIO_WIFI", "[AUDIO_WIFI] session=$transferId socket=CONNECTING host=$host")
                MeshLogger.i("AUDIO_SOCKET", "[AUDIO_SOCKET] session=$transferId socket=CONNECTED host=$host")
                MeshLogger.i("AUDIO_SOCKET_CONNECTED", "[AUDIO_SOCKET_CONNECTED] session=$transferId host=$host")

                // The handshake itself is serialized so no writer can split its header.
                val ackDeferred = CompletableDeferred<Boolean>()
                val handshakeDeferred = CompletableDeferred<HandshakeResult>()
                synchronized(stream) {
                // 1. Handshake phase
                MeshLogger.i("AUDIO_HANDSHAKE", "[AUDIO_HANDSHAKE] session=$transferId state=HANDSHAKING sending handshake to $host")
                pendingHandshakes[transferId] = handshakeDeferred
                pendingTransferHosts[transferId] = host

                    stream.writeInt(MAGIC_STREAM_HANDSHAKE)
                    stream.writeUTF(transferId)
                    stream.writeUTF(fileName)
                    stream.writeUTF(mimeType)
                    stream.writeLong(totalBytes)
                    stream.writeUTF(expectedChecksum)
                    stream.writeUTF(senderId)
                    stream.flush()
                }

                val handshakeResponse = withTimeoutOrNull(2_000L) {
                    handshakeDeferred.await()
                }
                pendingHandshakes.remove(transferId)

                if (handshakeResponse == null) {
                    pendingTransferHosts.remove(transferId)
                    val err = "Handshake timeout waiting for receiver response"
                    MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId $err")
                    MeshLogger.e("AUDIO_TRANSFER_FAILURE", "[AUDIO_TRANSFER_FAILURE] stage=HANDSHAKE_TIMEOUT exception=TimeoutException message='$err'")
                    cleanupPeer(host)
                    return@withContext AudioStreamResult.Failed(err)
                }

                if (handshakeResponse.status < 0 && handshakeResponse.status != MAGIC_STREAM_READY && handshakeResponse.status != MAGIC_STREAM_RESUME_REQ && handshakeResponse.status != MAGIC_STREAM_ACK) {
                    // The connection was torn down while we waited (cleanupPeer completes with -1).
                    pendingTransferHosts.remove(transferId)
                    val err = "Connection closed during handshake"
                    MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId $err")
                    return@withContext AudioStreamResult.Failed(err)
                }

                if (handshakeResponse.status == MAGIC_STREAM_ACK) {
                    pendingTransferHosts.remove(transferId)
                    MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId receiver already complete. ACK immediate.")
                    MeshLogger.i("AUDIO_TRANSFER_COMPLETE", "[AUDIO_TRANSFER_COMPLETE] session=$transferId file=$fileName size=${totalBytes}B status=SUCCESS (ALREADY_COMPLETE)")
                    MeshLogger.i("AUDIO_COMPLETE", "[AUDIO_COMPLETE] session=$transferId transfer already complete.")
                    onProgress?.invoke(totalBytes, totalBytes)
                    return@withContext AudioStreamResult.Success
                }

                val resumeOffset = handshakeResponse.resumeOffset.coerceIn(0L, totalBytes)
                MeshLogger.i("AUDIO_HANDSHAKE", "[AUDIO_HANDSHAKE] session=$transferId state=READY resumeOffset=$resumeOffset")

                pendingAcks[transferId] = ackDeferred
                pendingTransferHosts[transferId] = host

                // The audio chunks and end marker form one raw-stream transaction.
                // Heartbeats and mesh packets share this socket, so the lock must cover
                // the complete media write, not individual frames.
                synchronized(stream) {

                // 2. Chunks Transmission with AES-256-GCM encryption
                val totalChunks = ((totalBytes + AUDIO_CHUNK_SIZE - 1) / AUDIO_CHUNK_SIZE).toInt().coerceAtLeast(1)
                var bytesTransferred = resumeOffset
                var currentChunkIndex = (resumeOffset / AUDIO_CHUNK_SIZE).toInt()

                MeshLogger.i("AUDIO_TRANSFER_BEGIN", "[AUDIO_TRANSFER_BEGIN] session=$transferId file=$fileName size=${totalBytes}B totalChunks=$totalChunks resumeOffset=$resumeOffset")
                MeshLogger.i("AUDIO_CHUNK", "[AUDIO_CHUNK] session=$transferId starting from chunk=$currentChunkIndex/$totalChunks resumeOffset=$resumeOffset totalBytes=$totalBytes")

                FileInputStream(file).use { fis ->
                    if (resumeOffset > 0L) {
                        var skipped = 0L
                        while (skipped < resumeOffset) {
                            val s = fis.skip(resumeOffset - skipped)
                            if (s <= 0) break
                            skipped += s
                        }
                    }

                    val buffer = ByteArray(AUDIO_CHUNK_SIZE)
                    while (bytesTransferred < totalBytes) {
                        val toRead = minOf(buffer.size.toLong(), totalBytes - bytesTransferred).toInt()
                        val read = fis.read(buffer, 0, toRead)
                        if (read <= 0) break

                        val plainChunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                        val encryptedChunk = cryptoManager.encryptBytes(plainChunk, targetPeerAddress ?: host)

                            stream.writeInt(MAGIC_AUDIO_CHUNK)
                            stream.writeUTF(transferId)
                            stream.writeInt(currentChunkIndex)
                            stream.writeInt(totalChunks)
                            stream.writeLong(bytesTransferred)
                            stream.writeInt(encryptedChunk.size)
                            stream.write(encryptedChunk)
                            stream.flush()

                        bytesTransferred += read
                        currentChunkIndex++

                        val txBytes = bytesSentCounter.addAndGet(encryptedChunk.size.toLong() + 24L)
                        updateMetrics { it.copy(bytesSent = txBytes) }

                        val pct = if (totalBytes > 0) ((bytesTransferred * 100) / totalBytes).toInt() else 0
                        MeshLogger.d("AUDIO_TRANSFER_PROGRESS", "[AUDIO_TRANSFER_PROGRESS] session=$transferId chunk=$currentChunkIndex/$totalChunks bytesSent=$bytesTransferred/$totalBytes ($pct%)")
                        MeshLogger.d("AUDIO_CHUNK", "[AUDIO_CHUNK] session=$transferId chunk=$currentChunkIndex/$totalChunks bytesSent=$bytesTransferred/$totalBytes ($pct%)")
                        onProgress?.invoke(bytesTransferred, totalBytes)
                    }
                }

                // 3. End of Stream marker
                MeshLogger.i("AUDIO_CHUNK", "[AUDIO_CHUNK] session=$transferId all chunks sent. Sending MAGIC_MEDIA_STREAM_END")
                    stream.writeInt(MAGIC_MEDIA_STREAM_END)
                    stream.writeUTF(transferId)
                    stream.writeUTF(expectedChecksum)
                    stream.flush()
                }

                // 4. Wait for receiver completion ACK
                MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId waiting for receiver completion ACK")
                val ackResult = withTimeoutOrNull(10_000L) {
                    ackDeferred.await()
                }
                pendingAcks.remove(transferId)

                if (ackResult == true) {
                    MeshLogger.i("AUDIO_TRANSFER_COMPLETE", "[AUDIO_TRANSFER_COMPLETE] session=$transferId file=$fileName size=${totalBytes}B status=SUCCESS")
                    MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId ack=RECEIVED verified=true")
                    MeshLogger.i("AUDIO_COMPLETE", "[AUDIO_COMPLETE] session=$transferId file=$fileName size=${totalBytes}B status=SUCCESS")
                    return@withContext AudioStreamResult.Success
                } else {
                    val err = "Receiver did not acknowledge audio completion within timeout"
                    MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId $err")
                    MeshLogger.e("AUDIO_TRANSFER_FAILURE", "[AUDIO_TRANSFER_FAILURE] stage=ACK_TIMEOUT exception=TimeoutException message='$err'")
                    return@withContext AudioStreamResult.Failed(err)
                }
            } catch (e: Exception) {
                pendingHandshakes.remove(transferId)
                pendingAcks.remove(transferId)
                pendingTransferHosts.remove(transferId)
                val err = "Audio stream error on $host: ${e.message}"
                MeshLogger.e("AUDIO_TRANSFER_FAILURE", "[AUDIO_TRANSFER_FAILURE] stage=STREAM_EXCEPTION exception=${e.javaClass.simpleName} message='$err'", e)
                MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId error=$err", e)
                cleanupPeer(host)
                return@withContext AudioStreamResult.Failed(err)
            }
        }

        return@withContext AudioStreamResult.Failed("No target stream completed")
    }

    /**
     * Streams any media file (image, SOS evidence, voice note, ...) to exactly one connected peer using
     * the handshaked, resumable, per-chunk AES-GCM encrypted protocol of [streamAudioFile], and only
     * reports success once the receiver has verified and finalized the file and acknowledged it.
     *
     * Non-audio files used to take a separate raw path that was plaintext, never acknowledged (so a
     * receiver-side checksum failure silently lost the image/SOS photo while the sender reported success)
     * and wrote the file to every connected peer.
     */
    suspend fun streamFile(
        transferId: String,
        file: File,
        mimeType: String,
        expectedChecksum: String,
        senderId: String,
        targetPeerAddress: String? = null,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val res = streamAudioFile(
            transferId = transferId,
            file = file,
            mimeType = mimeType,
            expectedChecksum = expectedChecksum,
            senderId = senderId,
            targetPeerAddress = targetPeerAddress,
            onProgress = onProgress
        )
        return@withContext (res is AudioStreamResult.Success)
    }


    suspend fun sendPacket(packet: MeshPacket) = withContext(Dispatchers.IO) {
        var packetToSend = packet
        if (!packetToSend.encrypted && packetToSend.targetId != "BROADCAST") {
            val requirement = PacketEncryptionPolicy.getRequirement(packetToSend.type)
            if (requirement == EncryptionRequirement.REQUIRED || requirement == EncryptionRequirement.OPTIONAL) {
                val aadResult = try { sessionManager.generateAad(packetToSend.targetId) } catch (_: Throwable) { null }
                val aadBytes = if (aadResult is Pair<*, *>) aadResult.first as? ByteArray else null
                val aadPrefix = if (aadResult is Pair<*, *>) (aadResult.second as? String) ?: "" else ""
                val encryptedResult = try {
                    cryptoManager.encryptOrPassthrough(
                        packetToSend.payload,
                        packetToSend.targetId,
                        true,
                        packetToSend.packetId,
                        0,
                        aadBytes
                    )
                } catch (_: Throwable) { null }
                val encryptedPair = encryptedResult as? Pair<*, *>
                if (encryptedPair != null && encryptedPair.second == true) {
                    val ciphertext = encryptedPair.first as? String
                    if (ciphertext != null) {
                        val finalPayload = if (aadPrefix.isNotEmpty()) "$aadPrefix$ciphertext" else ciphertext
                        packetToSend = packetToSend.copy(payload = finalPayload, encrypted = true)
                    }
                }
            }
        }

        val json = MeshPacketParser.toJson(packetToSend)
        val payloadBytes = json.toByteArray(Charsets.UTF_8)
        if (!isValidFrameLength(payloadBytes.size)) {
            MeshLogger.e(TAG, "Rejecting outgoing packet ${packetToSend.packetId}: payload size ${payloadBytes.size} exceeds MAX_FRAME_SIZE_BYTES ($MAX_FRAME_SIZE_BYTES)")
            return@withContext
        }

        if (clientStreamsOut.isEmpty()) {
            MeshLogger.w(TAG, "Cannot send packet ${packetToSend.packetId}: No active socket streams available")
            return@withContext
        }

        val targetPeerAddress = packetToSend.targetId
        val targetStreams = if (clientStreamsOut.containsKey(targetPeerAddress)) {
            listOf(targetPeerAddress to clientStreamsOut[targetPeerAddress]!!)
        } else {
            // Broadcast or route via all connected client streams
            clientStreamsOut.entries.map { it.key to it.value }
        }

        targetStreams.forEach { (host, stream) ->
            try {
                synchronized(stream) {
                    stream.writeInt(payloadBytes.size)
                    stream.write(payloadBytes)
                    stream.flush()
                }
                val txBytes = bytesSentCounter.addAndGet(payloadBytes.size.toLong() + 4L)
                val txPkts = packetsSentCounter.incrementAndGet()
                updateMetrics { it.copy(bytesSent = txBytes, packetsSent = txPkts) }
                MeshLogger.d(TAG, "Sent binary packet to $host: ${packetToSend.packetId} (${payloadBytes.size} bytes)")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to send packet to $host: ${e.message}")
                cleanupPeer(host)
            }
        }
    }

    fun isConnected(): Boolean {
        val hasActivePeer = clientSockets.values.any { it.isConnected && !it.isClosed }
        return hasActivePeer && _connectionState.value == WifiSocketConnectionState.CONNECTED
    }

    fun isHostConnected(host: String): Boolean {
        val socket = clientSockets[host] ?: return false
        return socket.isConnected && !socket.isClosed
    }

    fun isConnectedToPeer(peerId: String): Boolean {
        val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(peerId)
        val host = peerHostsByMeshId[canonicalId] ?: peerHostsByMeshId[peerId] ?: return false
        return isHostConnected(host)
    }

    fun getConnectedPeers(): List<String> {
        return peerHostsByMeshId.entries
            .filter { isHostConnected(it.value) }
            .map { com.meshlink.util.MeshIdNormalizer.canonicalize(it.key) }
            .distinct()
    }

    fun disconnect() {
        manualDisconnectRequested = true
        reconnectJob?.cancel()
        reconnectJob = null

        clientSockets.keys.toList().forEach { host ->
            cleanupPeer(host)
        }

        stopServer()
        _connectionState.value = WifiSocketConnectionState.DISCONNECTED
        MeshLogger.d(TAG, "WifiSocketTransport disconnected cleanly")
    }

    private fun cleanupPeer(clientHost: String) {
        clientReadJobs.remove(clientHost)?.cancel()
        clientHeartbeatJobs.remove(clientHost)?.cancel()

        val streamOut = clientStreamsOut.remove(clientHost)
        val streamIn = clientStreamsIn.remove(clientHost)
        val socket = clientSockets.remove(clientHost)
        clientLastHeartbeatMs.remove(clientHost)

        try { streamOut?.close() } catch (_: Exception) {}
        try { streamIn?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}

        // Fail only the transfers that were riding on THIS connection. These maps are keyed by
        // transferId and shared by every peer; clearing them wholesale aborted unrelated in-flight
        // transfers to other peers whenever any single socket dropped.
        pendingTransferHosts.filterValues { it == clientHost }.keys.toList().forEach { transferId ->
            pendingHandshakes.remove(transferId)?.complete(HandshakeResult(-1, 0L))
            pendingAcks.remove(transferId)?.complete(false)
            pendingTransferHosts.remove(transferId)
        }

        updateMetrics { it.copy(activePeers = clientSockets.size) }
        MeshLogger.d(TAG, "Cleaned up socket resources for peer: $clientHost")
    }
}
