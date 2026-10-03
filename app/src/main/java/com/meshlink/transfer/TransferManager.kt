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
import com.meshlink.wifi.data.AudioStreamResult
import com.meshlink.wifi.data.WifiSocketTransport
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    private val wifiDirectManager: com.meshlink.wifi.manager.WifiDirectManager? = null,
    private val beaconHandlerProvider: javax.inject.Provider<com.meshlink.ble.data.handlers.BeaconHandler>? = null,
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
    val retransmissionScheduler: ChunkRetransmissionScheduler = ChunkRetransmissionScheduler(config, slidingWindowManager, runtimeStateRegistry, chunkDispatcher, metrics, diagnostics, ioDispatcher, applicationScope),
    // Phase 4 Resource, Registry & Cleanup Components
    val sessionRegistry: TransferSessionRegistry = TransferSessionRegistry(),
    val resourceManager: TransferResourceManager = TransferResourceManager(diagnostics),
    val cleanupManager: TransferCleanupManager = TransferCleanupManager(context, diagnostics),
    val cacheManager: TransferCacheManager = TransferCacheManager(context, diagnostics)
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

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        MeshLogger.e(TAG, "Unhandled exception in transfer coroutine scope: ${throwable.message}", throwable)
    }

    private val managedScope = CoroutineScope(SupervisorJob() + ioDispatcher + exceptionHandler)

    var onSendPacket: (suspend (MeshPacket) -> Unit)? = null
    var onTransferCompleted: ((TransferSession) -> Unit)? = null
    var onOutgoingTransferCompleted: ((TransferSession) -> Unit)? = null
    var onTransferStateChanged: ((String, TransferState) -> Unit)? = null
    var onIncomingStreamStarted: ((transferId: String, fileName: String, mimeType: String, totalBytes: Long, senderId: String) -> Unit)? = null

    private val activeOutgoingJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    fun isTransferActive(transferId: String): Boolean {
        val job = activeOutgoingJobs[transferId]
        if (job != null && job.isActive) return true
        val session = scheduler.getSession(transferId) ?: sessionRegistry.getSession(transferId)
        val state = session?.state
        return state in listOf(
            TransferState.PREPARING,
            TransferState.CONNECTING,
            TransferState.WAITING_FOR_WIFI,
            TransferState.SOCKET_CONNECTING,
            TransferState.HANDSHAKING,
            TransferState.STREAMING,
            TransferState.TRANSFERRING,
            TransferState.WAITING_FOR_ACK,
            TransferState.RETRYING
        )
    }

    private val transferCompletedListeners = java.util.concurrent.CopyOnWriteArrayList<(TransferSession) -> Unit>()

    fun addTransferCompletedListener(listener: (TransferSession) -> Unit) {
        transferCompletedListeners.add(listener)
    }

    fun removeTransferCompletedListener(listener: (TransferSession) -> Unit) {
        transferCompletedListeners.remove(listener)
    }

    private fun notifyTransferCompleted(session: TransferSession) {
        transferCompletedListeners.forEach { listener ->
            try {
                listener.invoke(session)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Error in transferCompletedListener: ${e.message}", e)
            }
        }
        onTransferCompleted?.invoke(session)
    }

    val transferProgress: StateFlow<Map<String, Float>> = scheduler.activeSessions
        .map { sessions ->
            sessions
                .filter {
                    it.state == TransferState.SENDING ||
                    it.state == TransferState.RECEIVING ||
                    it.state == TransferState.STREAMING ||
                    it.state == TransferState.TRANSFERRING ||
                    it.state == TransferState.WAITING_FOR_ACK ||
                    it.state == TransferState.COMPLETING ||
                    it.state == TransferState.RETRYING
                }
                .associate { it.transferId to it.getProgress() }
        }
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ─────────────────── Initialization ───────────────────

    init {
        applicationScope.launch(ioDispatcher + exceptionHandler) {
            val persisted = cache.loadPersistedSessions()
            for (session in persisted) {
                if (session.state == TransferState.SENDING || session.state == TransferState.RECEIVING || session.state == TransferState.STREAMING) {
                    session.state = TransferState.PAUSED
                }
                scheduler.addSession(session)
                sessionRegistry.registerSession(session)
            }
            // Trigger initial async cleanup sweep
            cleanupManager.runFullCleanup(sessionRegistry.getActiveSessions())
        }

        // Auto-resume active/paused transfers on Wi-Fi Direct socket connection
        wifiSocketTransport.onSocketConnected = {
            applicationScope.launch(ioDispatcher + exceptionHandler) {
                MeshLogger.d(TAG, "Wi-Fi Direct socket re-connected. Auto-resuming paused transfers...")
                val activeSessions = scheduler.activeSessions.value
                for (session in activeSessions) {
                    if (session.state == TransferState.PAUSED || session.state == TransferState.RETRYING || session.state == TransferState.QUEUED) {
                        resumeTransfer(session.transferId)
                    }
                }
            }
        }

        // Wire high-speed media stream reception callbacks
        wifiSocketTransport.onMediaStreamStarted = { transferId, fileName, mimeType, totalBytes, expectedChecksum, senderId ->
            applicationScope.launch(ioDispatcher + exceptionHandler) {
                var session = scheduler.getSession(transferId)
                if (session?.state?.isTerminal() == true) {
                    MeshLogger.d(TAG, "Ignoring onMediaStreamStarted for already terminal transfer: $transferId (state=${session.state})")
                    return@launch
                }
                val isAudio = mimeType.startsWith("audio/")
                if (isAudio) {
                    MeshLogger.i(
                        "AUDIO_RECEIVE_START",
                        "[AUDIO_RECEIVE_START] transferId=$transferId peerId=$senderId fileName=$fileName fileSize=${totalBytes}B mimeType=$mimeType transport=WIFI_DIRECT"
                    )
                }
                if (session == null) {
                    session = TransferSession(
                        transferId = transferId,
                        senderId = senderId,
                        targetId = "LOCAL",
                        fileName = fileName,
                        mimeType = mimeType,
                        totalBytes = totalBytes,
                        totalChunks = 1,
                        direction = TransferDirection.INCOMING,
                        transportUsed = TransportType.WIFI_DIRECT,
                        sha256Checksum = expectedChecksum,
                        state = TransferState.RECEIVING,
                        startTimeMs = System.currentTimeMillis()
                    )
                    scheduler.addSession(session)
                    sessionRegistry.registerSession(session)
                } else if (!session.state.isTerminal()) {
                    session.state = TransferState.RECEIVING
                    session.transportUsed = TransportType.WIFI_DIRECT
                    session.totalBytes = totalBytes
                    session.fileName = fileName
                    session.mimeType = mimeType
                }
                if (!session.state.isTerminal()) {
                    onTransferStateChanged?.invoke(transferId, TransferState.RECEIVING)
                }
                cache.persistSession(session)
                onIncomingStreamStarted?.invoke(transferId, fileName, mimeType, totalBytes, senderId)
            }
        }

        wifiSocketTransport.onMediaStreamProgress = { transferId, bytesTransferred, totalBytes ->
            val session = scheduler.getSession(transferId)
            val isAudio = session?.mimeType?.startsWith("audio/") == true
            if (isAudio) {
                MeshLogger.d(
                    "AUDIO_RECEIVE_PROGRESS",
                    "[AUDIO_RECEIVE_PROGRESS] transferId=$transferId peerId=${session?.senderId} bytesTransferred=$bytesTransferred totalBytes=$totalBytes mimeType=${session?.mimeType} transport=WIFI_DIRECT"
                )
            }
            updateStreamProgressThrottled(transferId, bytesTransferred, totalBytes)
        }

        wifiSocketTransport.onMediaStreamCompleted = { transferId, filePath, mimeType, senderId, totalBytes ->
            applicationScope.launch(ioDispatcher + exceptionHandler) {
                val isAudio = mimeType.startsWith("audio/")
                if (isAudio) {
                    MeshLogger.i(
                        "AUDIO_RECEIVE_COMPLETE",
                        "[AUDIO_RECEIVE_COMPLETE] transferId=$transferId peerId=$senderId fileName=${File(filePath).name} fileSize=${totalBytes}B mimeType=$mimeType transport=WIFI_DIRECT"
                    )
                }
                val session = scheduler.getSession(transferId) ?: TransferSession(
                    transferId = transferId,
                    senderId = senderId,
                    targetId = "LOCAL",
                    fileName = File(filePath).name,
                    mimeType = mimeType,
                    totalBytes = totalBytes,
                    totalChunks = 1,
                    direction = TransferDirection.INCOMING,
                    transportUsed = TransportType.WIFI_DIRECT,
                    filePath = filePath,
                    state = TransferState.COMPLETED,
                    startTimeMs = System.currentTimeMillis()
                )
                session.filePath = filePath
                session.state = TransferState.COMPLETED
                session.bytesTransferred = totalBytes
                session.chunksTransferred = session.totalChunks.coerceAtLeast(1)
                scheduler.addSession(session)
                updateStreamProgressThrottled(transferId, totalBytes, totalBytes)
                updateState(transferId, TransferState.COMPLETED)
                cache.persistSession(session)
                resourceManager.releaseSessionResources(transferId)
                sessionRegistry.unregisterSession(transferId)

                analytics.recordTransferCompleted(session)
                val durationMs = (System.currentTimeMillis() - session.startTimeMs).coerceAtLeast(1L)
                val durationSec = durationMs / 1000.0
                val mbps = (totalBytes / (1024.0 * 1024.0)) / durationSec.coerceAtLeast(0.001)
                MeshLogger.i("MEDIA_TRANSFER", "file=${session.fileName}, size=${totalBytes}B, transport=WIFI_DIRECT, duration=${"%.2f".format(durationSec)}s, throughput=${"%.2f".format(mbps)}MB/s, retries=0, status=SUCCESS")
                metrics.recordMediaTransfer(totalBytes, durationMs)
                diagnostics.logTransferCompletion(transferId, totalBytes, durationMs, session.getAverageSpeedBytesPerSec().toDouble())

                notifyTransferCompleted(session)
            }
        }

        wifiSocketTransport.onMediaStreamFailed = { transferId, reason ->
            applicationScope.launch(ioDispatcher + exceptionHandler) {
                val session = scheduler.getSession(transferId)
                val isAudio = session?.mimeType?.startsWith("audio/") == true
                if (isAudio) {
                    MeshLogger.e(
                        TAG,
                        "[AUDIO_TRANSFER_FAILURE] transferId=$transferId peerId=${session?.senderId} error=$reason mimeType=${session?.mimeType} transport=WIFI_DIRECT"
                    )
                } else {
                    MeshLogger.w(TAG, "Incoming Wi-Fi media stream failed for $transferId: $reason")
                }
                failSession(transferId, reason)
            }
        }
    }

    // ─────────────────── Sender (Pipeline Pipeline) ───────────────────

    suspend fun sendFile(
        file: File,
        senderId: String,
        targetId: String,
        priority: TransferPriority = TransferPriority.MEDIUM,
        transferId: String = UUID.randomUUID().toString(),
        thumbnailBase64: String? = null
    ): String {
        val exists = withContext(ioDispatcher) { file.exists() }
        val canRead = withContext(ioDispatcher) { file.canRead() }
        val fileLength = withContext(ioDispatcher) { file.length() }
        val canOpenStream = withContext(ioDispatcher) {
            try {
                file.inputStream().use { true }
            } catch (e: Exception) {
                false
            }
        }
        val mimeType = metaManager.getMimeTypeForFile(file)
        val isAudio = mimeType.startsWith("audio/")

        if (isAudio) {
            MeshLogger.i(
                "AUDIO_DEBUG",
                "[AUDIO_DEBUG] uri=${file.toURI()} path=${file.absolutePath} mimeType=$mimeType fileSize=${fileLength}B exists=$exists readable=$canRead inputStream=$canOpenStream"
            )
            MeshLogger.i(
                "AUDIO_SEND_START",
                "[AUDIO_SEND_START] transferId=$transferId peerId=$targetId fileName=${file.name} fileSize=${fileLength}B mimeType=$mimeType transport=WIFI_DIRECT"
            )
            MeshLogger.i(
                "AUDIO_FILE_CHECK",
                "[AUDIO_FILE_CHECK] transferId=$transferId peerId=$targetId path=${file.absolutePath} exists=$exists canRead=$canRead size=${fileLength}B"
            )
        }

        if (!exists || !canRead || fileLength == 0L || !canOpenStream) {
            if (isAudio) {
                MeshLogger.e(
                    TAG,
                    "[AUDIO_TRANSFER_FAILURE] transferId=$transferId peerId=$targetId error='Source audio file invalid, unreadable, or empty (exists=$exists, canRead=$canRead, size=${fileLength}B, inputStream=$canOpenStream)' mimeType=$mimeType transport=WIFI_DIRECT"
                )
            }
            MeshLogger.e(TAG, "Cannot send non-existent, unreadable or empty file: ${file.absolutePath}")
            return transferId
        }

        if (isAudio) {
            MeshLogger.i(
                "AUDIO_FILE_READY",
                "[AUDIO_FILE_READY] transferId=$transferId peerId=$targetId fileName=${file.name} fileSize=${fileLength}B mimeType=$mimeType transport=WIFI_DIRECT"
            )
            MeshLogger.i(
                "AUDIO_METADATA",
                "[AUDIO_METADATA] transferId=$transferId peerId=$targetId fileName=${file.name} fileSize=${fileLength}B mimeType=$mimeType transport=WIFI_DIRECT"
            )
        }

        // ── AUDIO TRANSPORT POLICY (HARD REQUIREMENT) ──────────────────────────
        // Audio payloads MUST be transferred exclusively via Wi-Fi Direct.
        // BLE MUST NOT be used for audio payloads under any circumstance.
        // If Wi-Fi Direct is not available when sendFile() is called, the session
        // is created as WIFI_DIRECT and startOutgoingTransfer() will wait/fail-safe.
        // ───────────────────────────────────────────────────────────────────────
        val selectedRoute: RouteType = if (isAudio) {
            MeshLogger.i(
                "AUDIO_TRANSPORT_SELECTION",
                "[AUDIO_TRANSPORT_SELECTION] transferId=$transferId peerId=$targetId selectedTransport=WIFI_DIRECT mimeType=$mimeType fileSize=${fileLength}B"
            )
            RouteType.WIFI_DIRECT
        } else {
            // Non-audio: existing size-based heuristic (unchanged).
            val isWifi = wifiSocketTransport.isConnected() || intelligentTransportManager.isWifiAvailable()
            val isSmallMedia = fileLength <= 50_000L
            when {
                isWifi && !isSmallMedia -> RouteType.WIFI_DIRECT
                wifiSocketTransport.isConnected() -> RouteType.WIFI_DIRECT
                isSmallMedia -> RouteType.BLE
                else -> intelligentTransportManager.selectTransportForPayload(
                    destinationId = targetId,
                    packetType = PacketType.MEDIA_CHUNK,
                    payloadSizeBytes = fileLength,
                    mimeType = mimeType
                )
            }
        }

        val transport = if (selectedRoute == RouteType.WIFI_DIRECT) TransportType.WIFI_DIRECT else TransportType.BLE
        val checksum = withContext(ioDispatcher) { verifier.calculateFileChecksum(file) }
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
        sessionRegistry.registerSession(session)
        onTransferStateChanged?.invoke(transferId, TransferState.QUEUED)
        applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }
        analytics.recordTransferStarted(session)
        diagnostics.logTransferStart(transferId, file.name, fileLength, selectedRoute)

        val existingJob = activeOutgoingJobs[transferId]
        if (existingJob != null && existingJob.isActive) {
            MeshLogger.d(TAG, "Transfer $transferId is already running actively. Skipping duplicate start.")
            return transferId
        }

        val job = applicationScope.launch(ioDispatcher + exceptionHandler) {
            try {
                startOutgoingTransfer(session)
            } finally {
                activeOutgoingJobs.remove(transferId)
            }
        }
        activeOutgoingJobs[transferId] = job
        resourceManager.registerJob(transferId, job)

        return transferId
    }

    private suspend fun executeAudioTransferWithRetry(session: TransferSession, file: File) = withContext(ioDispatcher) {
        val transferId = session.transferId
        val peerId = session.targetId
        val fileName = file.name
        val fileSize = file.length()
        val mimeType = session.mimeType

        MeshLogger.i("AUDIO_SEND_START", "[AUDIO_SEND_START] transferId=$transferId peerId=$peerId file=$fileName size=${fileSize}B mime=$mimeType")
        MeshLogger.i("AUDIO_FILE_READY", "[AUDIO_FILE_READY] transferId=$transferId path=${file.absolutePath} bytes=$fileSize mime=$mimeType checksum=${session.sha256Checksum}")
        MeshLogger.i("AUDIO_TRANSPORT_SELECTION", "[AUDIO_TRANSPORT_SELECTION] transport=WIFI_DIRECT session=$transferId")

        session.transportUsed = TransportType.WIFI_DIRECT
        session.totalBytes = fileSize
        session.totalChunks = ((fileSize + WifiSocketTransport.AUDIO_CHUNK_SIZE - 1) / WifiSocketTransport.AUDIO_CHUNK_SIZE).toInt().coerceAtLeast(1)

        // Send out-of-band control META packet over BLE control mesh so receiver immediately:
        //  1) Displays the incoming voice bubble placeholder in the chat right away
        //  2) Proactively warms its Wi-Fi server socket and connects as client to GO
        try {
            MeshLogger.i("AUDIO_CONTROL_META", "[AUDIO_CONTROL_META] Sending out-of-band META over BLE mesh for audio transfer: $transferId target=$peerId")
            sendMetaPacket(session)
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Failed to send audio control META: ${e.message}")
        }

        val maxAttempts = 6
        val retryDelaysMs = listOf(500L, 1000L, 2000L, 3000L, 4000L, 5000L)
        var lastErrorReason = "Unknown error"

        for (attempt in 1..maxAttempts) {
            val currentSession = scheduler.getSession(transferId) ?: sessionRegistry.getSession(transferId)
            if (currentSession?.state == TransferState.COMPLETED) {
                MeshLogger.i("AUDIO_COMPLETE", "[AUDIO_COMPLETE] session=$transferId already marked COMPLETED. Finishing.")
                return@withContext
            }

            if (attempt > 1) {
                val delayMs = retryDelaysMs.getOrElse(attempt - 2) { 5000L }
                MeshLogger.i("AUDIO_RETRY", "[AUDIO_RETRY] session=$transferId attempt=$attempt/$maxAttempts delay=${delayMs}ms lastError='$lastErrorReason'")
                updateState(transferId, TransferState.RETRYING)
                delay(delayMs)
            }

            // Step 1: Ensure Wi-Fi Direct connection & Socket readiness
            updateState(transferId, TransferState.WAITING_FOR_WIFI)
            MeshLogger.i("AUDIO_WIFI", "[AUDIO_WIFI] session=$transferId socket=CONNECTING peerId=$peerId attempt=$attempt")

            val isP2pConnected = wifiDirectManager?.isConnected == true
            var socketReady = false

            if (isP2pConnected) {
                val pState = wifiDirectManager?.p2pState?.value
                if (pState is com.meshlink.wifi.model.WifiP2pState.Connected) {
                    if (!pState.isGroupOwner && pState.groupOwnerAddress.isNotBlank()) {
                        val goAddress = pState.groupOwnerAddress
                        wifiSocketTransport.registerPeerMeshHost(peerId, goAddress)
                        if (!wifiSocketTransport.isHostConnected(goAddress)) {
                            wifiSocketTransport.connectAsClient(goAddress)
                        }
                    } else if (pState.isGroupOwner) {
                        wifiSocketTransport.startServer()
                    }
                }
                socketReady = wifiSocketTransport.isConnected()
            }

            if (!socketReady) {
                updateState(transferId, TransferState.SOCKET_CONNECTING)
                MeshLogger.i("AUDIO_SOCKET", "[AUDIO_SOCKET] session=$transferId requesting Wi-Fi Direct connection (initiator=true)")

                // Fast Direct Wi-Fi Reachability: Check all candidate IPs concurrently
                val peerDetails = wifiDirectManager?.getPeerWifiDetails(peerId)
                val candidateIps = mutableListOf<String>()
                peerDetails?.ipAddress?.takeIf { it.isNotBlank() }?.let { candidateIps.add(it) }
                candidateIps.addAll(com.meshlink.wifi.util.WifiNetworkUtils.getCandidatePeerIps(context))
                com.meshlink.wifi.util.WifiNetworkUtils.getArpClients().forEach { candidateIps.add(it) }

                val distinctCandidates = candidateIps.distinct().filter { it.isNotBlank() && it != "0.0.0.0" }
                val reachableIp = withContext(ioDispatcher) {
                    val deferreds = distinctCandidates.map { candIp ->
                        async {
                            if (com.meshlink.wifi.util.WifiNetworkUtils.isTcpPortReachable(candIp, 8988, 350)) {
                                candIp
                            } else null
                        }
                    }
                    deferreds.awaitAll().firstOrNull { it != null }
                }

                if (reachableIp != null) {
                    MeshLogger.i("AUDIO_SOCKET", "[AUDIO_SOCKET] Found active TCP listener on $reachableIp:8988! Connecting...")
                    wifiSocketTransport.registerPeerMeshHost(peerId, reachableIp)
                    wifiSocketTransport.connectAsClient(reachableIp)
                    val waitStart = System.currentTimeMillis()
                    while (System.currentTimeMillis() - waitStart < 2000L) {
                        if (wifiSocketTransport.isConnected()) {
                            socketReady = true
                            MeshLogger.i("AUDIO_SOCKET", "[AUDIO_SOCKET] Socket connected successfully to $reachableIp:8988")
                            break
                        }
                        delay(50L)
                    }
                }

                // If not already connected over direct Wi-Fi link, trigger P2P discovery & connection
                if (!socketReady) {
                    // Trigger wake beacon over mesh control plane so remote peer starts discovery
                    try {
                        beaconHandlerProvider?.get()?.let { bh ->
                            val wakePacket = bh.generateBeaconPacket(
                                localMeshId = session.senderId,
                                targetPeerId = peerId,
                                reqWifiConnect = true
                            )
                            onSendPacket?.invoke(wakePacket)
                            MeshLogger.i("AUDIO_WIFI", "[AUDIO_WIFI] Sent Wi-Fi connect wake beacon to peer $peerId")
                            delay(300L)
                        }
                    } catch (e: Exception) {
                        MeshLogger.w("AUDIO_WIFI", "Failed to send wake beacon: ${e.message}")
                    }

                    try {
                        wifiDirectManager?.ensureConnected(peerId, timeoutMs = 25_000L, isInitiator = true)
                    } catch (e: Exception) {
                        MeshLogger.w("AUDIO_WIFI", "[AUDIO_WIFI] wifiDirectManager.ensureConnected error: ${e.message}")
                    }

                    val socketWaitStart = System.currentTimeMillis()
                    while (System.currentTimeMillis() - socketWaitStart < 20_000L) {
                        val pState = wifiDirectManager?.p2pState?.value
                        if (pState is com.meshlink.wifi.model.WifiP2pState.Connected) {
                            if (!pState.isGroupOwner && pState.groupOwnerAddress.isNotBlank()) {
                                val go = pState.groupOwnerAddress
                                wifiSocketTransport.registerPeerMeshHost(peerId, go)
                                if (!wifiSocketTransport.isHostConnected(go)) {
                                    wifiSocketTransport.connectAsClient(go)
                                }
                            } else if (pState.isGroupOwner) {
                                wifiSocketTransport.startServer()
                            }
                        }

                        if (wifiSocketTransport.isConnected()) {
                            socketReady = true
                            break
                        }
                        delay(100L)
                    }
                }
            }

            if (!socketReady) {
                lastErrorReason = "Wi-Fi Direct socket not ready after attempt $attempt"
                MeshLogger.w("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId attempt=$attempt $lastErrorReason")
                continue
            }

            MeshLogger.i("AUDIO_SOCKET", "[AUDIO_SOCKET] session=$transferId socket=CONNECTED")

            // Step 2: Handshake & Transfer Chunks
            updateState(transferId, TransferState.HANDSHAKING)
            MeshLogger.i("AUDIO_HANDSHAKE", "[AUDIO_HANDSHAKE] session=$transferId state=HANDSHAKING")

            updateState(transferId, TransferState.TRANSFERRING)
            val streamStartTime = System.currentTimeMillis()

            val result = wifiSocketTransport.streamAudioFile(
                transferId = transferId,
                file = file,
                mimeType = mimeType,
                expectedChecksum = session.sha256Checksum ?: "",
                senderId = session.senderId,
                targetPeerAddress = peerId,
                onProgress = { bytesTransferred, totalBytes ->
                    session.bytesTransferred = bytesTransferred
                    val pct = if (totalBytes > 0) ((bytesTransferred * 100) / totalBytes).toInt() else 0
                    MeshLogger.d("AUDIO_CHUNK", "[AUDIO_CHUNK] session=$transferId bytes=$bytesTransferred/$totalBytes ($pct%)")
                    updateStreamProgressThrottled(transferId, bytesTransferred, totalBytes)
                }
            )

            when (result) {
                is AudioStreamResult.Success -> {
                    updateState(transferId, TransferState.WAITING_FOR_ACK)
                    MeshLogger.i("AUDIO_ACK", "[AUDIO_ACK] session=$transferId ack=RECEIVED")
                    updateState(transferId, TransferState.COMPLETING)

                    session.bytesTransferred = session.totalBytes
                    session.chunksTransferred = session.totalChunks
                    updateStreamProgressThrottled(transferId, session.totalBytes, session.totalBytes)
                    updateState(transferId, TransferState.COMPLETED)

                    resourceManager.releaseSessionResources(transferId)
                    sessionRegistry.unregisterSession(transferId)
                    applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }

                    val durationMs = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(1L)
                    val durationSec = durationMs / 1000.0
                    val mbps = (session.totalBytes / (1024.0 * 1024.0)) / durationSec.coerceAtLeast(0.001)

                    MeshLogger.i("AUDIO_COMPLETE", "[AUDIO_COMPLETE] session=$transferId file=$fileName size=${session.totalBytes}B duration=${durationMs}ms throughput=${"%.2f".format(mbps)}MB/s retries=${attempt - 1}")
                    metrics.recordMediaTransfer(session.totalBytes, durationMs)
                    diagnostics.logTransferCompletion(transferId, session.totalBytes, durationMs, session.getAverageSpeedBytesPerSec().toDouble())

                    onOutgoingTransferCompleted?.invoke(session)
                    return@withContext
                }
                is AudioStreamResult.Failed -> {
                    lastErrorReason = result.reason
                    MeshLogger.w("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId attempt=$attempt failed: $lastErrorReason")
                }
            }
        }

        // All attempts exhausted
        val finalReason = "Audio transfer failed after $maxAttempts attempts: $lastErrorReason"
        MeshLogger.e("AUDIO_FAILURE", "[AUDIO_FAILURE] session=$transferId all $maxAttempts attempts exhausted. Error: $finalReason")
        diagnostics.logTransportUnavailable(
            packetId = transferId,
            packetType = PacketType.MEDIA_CHUNK,
            requestedRoute = RouteType.WIFI_DIRECT,
            reason = finalReason
        )
        failSession(transferId, finalReason)
    }

    private suspend fun startOutgoingTransfer(session: TransferSession) = withContext(ioDispatcher) {
        val file = File(session.filePath ?: return@withContext)
        val isAudio = session.mimeType.startsWith("audio/")

        if (!file.exists()) {
            if (isAudio) {
                MeshLogger.e(
                    TAG,
                    "[AUDIO_TRANSFER_FAILURE] transferId=${session.transferId} peerId=${session.targetId} error='Source file vanished: ${file.absolutePath}' mimeType=${session.mimeType} transport=WIFI_DIRECT"
                )
            }
            failSession(session.transferId, "Source file vanished")
            return@withContext
        }

        // ── AUDIO HARD SAFETY GUARD ──────────────────────────────────────────────
        // Audio payloads MUST NEVER be transferred over BLE.
        // Audio uses Wi-Fi Direct exclusively with automatic retry and resumability.
        // ────────────────────────────────────────────────────────────────────────
        if (isAudio) {
            executeAudioTransferWithRetry(session, file)
            return@withContext
        }

        // Determine if Wi-Fi Direct streaming can actually be used
        if (session.transportUsed == TransportType.WIFI_DIRECT) {
            updateState(session.transferId, TransferState.STREAMING)
            applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }

            // Send out-of-band control META packet over BLE control mesh BEFORE waiting for socket
            // so the receiver is immediately notified of incoming audio and proactively initiates Wi-Fi Direct connection.
            if (isAudio) {
                MeshLogger.i("AUDIO_CONTROL_META", "[AUDIO_CONTROL_META] Sending out-of-band META over mesh control plane for audio transfer: ${session.transferId} target=${session.targetId}")
                sendMetaPacket(session)
            }

            // Ensure Wi-Fi Direct socket is ready.
            // For audio: wait up to 45s (allow time for Wi-Fi Direct PBC negotiation).
            // For other media: wait up to 3s (existing behaviour).
            val socketWaitTimeoutMs = if (isAudio) 45_000L else 3_000L
            var socketReady = wifiSocketTransport.isConnected()
            if (!socketReady) {
                val peerDetails = wifiDirectManager?.getPeerWifiDetails(session.targetId)
                val candidateIps = mutableListOf<String>()
                peerDetails?.ipAddress?.takeIf { it.isNotBlank() }?.let { candidateIps.add(it) }
                candidateIps.addAll(com.meshlink.wifi.util.WifiNetworkUtils.getCandidatePeerIps(context))
                com.meshlink.wifi.util.WifiNetworkUtils.getArpClients().forEach { candidateIps.add(it) }

                val distinctCandidates = candidateIps.distinct().filter { it.isNotBlank() && it != "0.0.0.0" }
                val reachableIp = withContext(ioDispatcher) {
                    val deferreds = distinctCandidates.map { candIp ->
                        async {
                            if (com.meshlink.wifi.util.WifiNetworkUtils.isTcpPortReachable(candIp, 8988, 400)) {
                                candIp
                            } else null
                        }
                    }
                    deferreds.awaitAll().firstOrNull { it != null }
                }

                if (reachableIp != null) {
                    wifiSocketTransport.registerPeerMeshHost(session.targetId, reachableIp)
                    wifiSocketTransport.connectAsClient(reachableIp)
                    val waitStart = System.currentTimeMillis()
                    while (System.currentTimeMillis() - waitStart < 2000L) {
                        if (wifiSocketTransport.isConnected()) {
                            socketReady = true
                            break
                        }
                        delay(50L)
                    }
                }

                if (!socketReady) {
                    if (isAudio) {
                        MeshLogger.i(
                            "AUDIO_WIFI_WAIT",
                            "[AUDIO_WIFI_WAIT] transferId=${session.transferId} peerId=${session.targetId} timeoutMs=$socketWaitTimeoutMs transport=WIFI_DIRECT"
                        )
                        MeshLogger.i(
                            "AUDIO_WIFI_CONNECTION",
                            "[AUDIO_WIFI_CONNECTION] transferId=${session.transferId} peerId=${session.targetId} requesting Wi-Fi Direct connection"
                        )
                    }
                    // Proactively trigger Wi-Fi Direct connection via WifiDirectManager as INITIATOR
                    applicationScope.launch(ioDispatcher + exceptionHandler) {
                        try {
                            wifiDirectManager?.ensureConnected(session.targetId, timeoutMs = socketWaitTimeoutMs, isInitiator = true)
                        } catch (e: Exception) {
                            MeshLogger.w(TAG, "wifiDirectManager.ensureConnected failed: ${e.message}")
                        }
                    }
                    val connectStartTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - connectStartTime < socketWaitTimeoutMs) {
                        if (wifiSocketTransport.isConnected()) {
                            socketReady = true
                            break
                        }
                        delay(100L)
                    }
                }
            }

            if (socketReady) {
                if (isAudio) {
                    MeshLogger.i(
                        "AUDIO_WIFI_SOCKET",
                        "[AUDIO_WIFI_SOCKET] CONNECTED transferId=${session.transferId} peerId=${session.targetId}"
                    )
                    MeshLogger.i(
                        "AUDIO_WIFI_STREAM",
                        "[AUDIO_WIFI_STREAM] START transferId=${session.transferId} fileSize=${session.totalBytes}B"
                    )
                }
                session.transportUsed = TransportType.WIFI_DIRECT
                session.totalChunks = 1

                val streamStartTime = System.currentTimeMillis()

                if (isAudio) {
                    MeshLogger.i(
                        "AUDIO_TRANSFER_BEGIN",
                        "[AUDIO_TRANSFER_BEGIN] transferId=${session.transferId} peerId=${session.targetId} fileName=${file.name} fileSize=${session.totalBytes}B mimeType=${session.mimeType} transport=WIFI_DIRECT"
                    )
                }

                val success = wifiSocketTransport.streamFile(
                    transferId = session.transferId,
                    file = file,
                    mimeType = session.mimeType,
                    expectedChecksum = session.sha256Checksum ?: "",
                    senderId = session.senderId,
                    targetPeerAddress = session.targetId,
                    onProgress = { bytesTransferred, totalBytes ->
                        if (isAudio) {
                            MeshLogger.d(
                                "AUDIO_TRANSFER_PROGRESS",
                                "[AUDIO_TRANSFER_PROGRESS] transferId=${session.transferId} peerId=${session.targetId} bytesTransferred=$bytesTransferred totalBytes=$totalBytes mimeType=${session.mimeType} transport=WIFI_DIRECT"
                            )
                        }
                        updateStreamProgressThrottled(session.transferId, bytesTransferred, totalBytes)
                    }
                )

                if (success) {
                    session.bytesTransferred = session.totalBytes
                    session.chunksTransferred = 1
                    updateStreamProgressThrottled(session.transferId, session.totalBytes, session.totalBytes)
                    updateState(session.transferId, TransferState.COMPLETED)
                    resourceManager.releaseSessionResources(session.transferId)
                    sessionRegistry.unregisterSession(session.transferId)
                    applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }

                    val durationMs = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(1L)
                    val durationSec = durationMs / 1000.0
                    val mbps = (session.totalBytes / (1024.0 * 1024.0)) / durationSec.coerceAtLeast(0.001)

                    MeshLogger.i("MEDIA_TRANSFER", "file=${file.name}, size=${session.totalBytes}B, transport=WIFI_DIRECT, duration=${"%.2f".format(durationSec)}s, throughput=${"%.2f".format(mbps)}MB/s, retries=0, status=SUCCESS")
                    if (isAudio) {
                        MeshLogger.i(
                            "AUDIO_WIFI_STREAM",
                            "[AUDIO_WIFI_STREAM] COMPLETE transferId=${session.transferId} bytesSent=${session.totalBytes} durationMs=$durationMs"
                        )
                        MeshLogger.i(
                            "AUDIO_TRANSFER_COMPLETE",
                            "[AUDIO_TRANSFER_COMPLETE] transferId=${session.transferId} peerId=${session.targetId} fileName=${file.name} fileSize=${session.totalBytes}B mimeType=${session.mimeType} transport=WIFI_DIRECT duration=${durationMs}ms throughput=${"%.2f".format(mbps)}MB/s"
                        )
                    }
                    metrics.recordMediaTransfer(session.totalBytes, durationMs)
                    diagnostics.logTransferCompletion(session.transferId, session.totalBytes, durationMs, session.getAverageSpeedBytesPerSec().toDouble())

                    onOutgoingTransferCompleted?.invoke(session)
                    return@withContext
                } else {
                    // Wi-Fi stream failed.
                    if (isAudio) {
                        // AUDIO MUST NOT fall back to BLE — fail the transfer.
                        val reason = "Wi-Fi Direct stream failed for audio payload. Audio MUST NOT fall back to BLE."
                        MeshLogger.e(
                            TAG,
                            "[AUDIO_TRANSFER_FAILURE] transferId=${session.transferId} peerId=${session.targetId} error=$reason mimeType=${session.mimeType} transport=WIFI_DIRECT"
                        )
                        diagnostics.logTransportUnavailable(
                            packetId = session.transferId,
                            packetType = PacketType.MEDIA_CHUNK,
                            requestedRoute = RouteType.WIFI_DIRECT,
                            reason = reason
                        )
                        failSession(session.transferId, reason)
                        return@withContext
                    } else {
                        // Non-audio: allow BLE fallback as before.
                        MeshLogger.w(TAG, "Wi-Fi Direct stream failed for ${session.transferId}. Falling back to BLE.")
                        session.transportUsed = TransportType.BLE
                        diagnostics.logTransportFallback(
                            packetId = session.transferId,
                            packetType = PacketType.MEDIA_CHUNK,
                            primaryRoute = RouteType.WIFI_DIRECT,
                            fallbackRoute = RouteType.BLE,
                            reason = "Wi-Fi Direct socket stream failed, falling back to BLE"
                        )
                    }
                }
            } else {
                // Wi-Fi socket not ready after waiting.
                if (isAudio) {
                    // Clean up any empty/stuck Wi-Fi Direct group so discovery is restored
                    applicationScope.launch(ioDispatcher + exceptionHandler) {
                        try {
                            wifiDirectManager?.disconnect()
                        } catch (e: Exception) {
                            MeshLogger.w(TAG, "wifiDirectManager.disconnect cleanup failed: ${e.message}")
                        }
                    }
                    // AUDIO MUST NOT fall back to BLE — fail the transfer.
                    val reason = "Wi-Fi Direct unavailable after ${socketWaitTimeoutMs}ms wait. Audio MUST NOT fall back to BLE."
                    MeshLogger.e(
                        TAG,
                        "[AUDIO_TRANSFER_FAILURE] transferId=${session.transferId} peerId=${session.targetId} error=$reason mimeType=${session.mimeType} transport=WIFI_DIRECT"
                    )
                    MeshLogger.e(TAG, "[AUDIO_WIFI_CONNECTION] state=FAILED transferId=${session.transferId}")
                    diagnostics.logTransportUnavailable(
                        packetId = session.transferId,
                        packetType = PacketType.MEDIA_CHUNK,
                        requestedRoute = RouteType.WIFI_DIRECT,
                        reason = reason
                    )
                    failSession(session.transferId, reason)
                    return@withContext
                } else {
                    // Non-audio: fall back to BLE as before.
                    MeshLogger.w(TAG, "Wi-Fi Direct socket not connected. Falling back to BLE for ${session.transferId}")
                    session.transportUsed = TransportType.BLE
                }
            }
        } else {
            // Session was not WIFI_DIRECT. If audio somehow reached here, that's a violation.
            if (isAudio) {
                val violation = "[AUDIO_TRANSPORT_VIOLATION] Audio payload reached BLE path in startOutgoingTransfer. " +
                    "transferId=${session.transferId} peerId=${session.targetId} mimeType=${session.mimeType} transport=BLE"
                MeshLogger.e(TAG, violation)
                failSession(session.transferId, "Audio transport violation: audio must not use BLE.")
                return@withContext
            }
            session.transportUsed = TransportType.BLE
        }

        // Hard check again before BLE chunking dispatch
        if (isAudio) {
            val violation = "[AUDIO_TRANSPORT_VIOLATION] Audio payload reached BLE chunked dispatch. " +
                "transferId=${session.transferId} peerId=${session.targetId} mimeType=${session.mimeType} transport=BLE"
            MeshLogger.e(TAG, violation)
            failSession(session.transferId, "Audio transport violation: audio must not use BLE.")
            return@withContext
        }

        // BLE Path / Fallback Path (audio is EXCLUDED above — only non-audio reaches here):
        // Set accurate chunk count for BLE and send META packet to receiver
        session.totalChunks = chunkManager.getTotalChunks(session.totalBytes, TransportType.BLE)
        sendMetaPacket(session)

        updateState(session.transferId, TransferState.STREAMING)
        applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }

        // Give receiver time to init cache
        delay(50L)

        // Initialize Sliding Window for BLE chunking
        val windowSize = slidingWindowManager.initializeSessionWindow(
            session.transferId, TransportType.BLE, session.totalChunks
        )
        diagnostics.logWindowCreated(session.transferId, windowSize)

        // Start Selective Retransmission Monitoring
        retransmissionScheduler.startMonitoring(
            session = session,
            file = file,
            onSendPacket = onSendPacket,
            onFailure = { reason ->
                applicationScope.launch(ioDispatcher + exceptionHandler) {
                    failSession(session.transferId, reason)
                }
            }
        )

        // Issue initial sliding window chunk dispatches to ParallelWorkerPool
        chunkDispatcher.dispatchAvailableChunks(session, file, onSendPacket)
    }

    private suspend fun sendMetaPacket(session: TransferSession) {
        val metaPayload = metaManager.generateMetaPayload(
            FileMetadata(session.fileName, session.mimeType, session.totalBytes, session.sha256Checksum, session.thumbnailBase64)
        )
        val metaPriority = if (session.priority == TransferPriority.CRITICAL || session.priority == TransferPriority.HIGH) {
            com.meshlink.domain.model.PacketPriority.CRITICAL
        } else {
            com.meshlink.domain.model.PacketPriority.NORMAL
        }
        sendPacket(
            session.senderId, session.targetId, session.transferId,
            metaPayload, PacketType.MEDIA_META, 0, session.totalChunks, session.mimeType,
            metaPriority
        )
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

    private fun updateStreamProgressThrottled(transferId: String, bytesTransferred: Long, totalBytes: Long) {
        if (totalBytes <= 0L) return
        val currentPct = (bytesTransferred.toDouble() / totalBytes.toDouble()).toFloat()
        val lastPct = lastProgressEmitPct[transferId] ?: -1f
        val lastTime = lastProgressEmitMs[transferId] ?: 0L
        val now = System.currentTimeMillis()

        if (bytesTransferred >= totalBytes || Math.abs(currentPct - lastPct) >= 0.01f || (now - lastTime) >= 100L) {
            lastProgressEmitPct[transferId] = currentPct
            lastProgressEmitMs[transferId] = now
            val chunksDone = if (bytesTransferred >= totalBytes) 1 else 0
            scheduler.updateSessionProgress(transferId, chunksDone, bytesTransferred)
        }
    }

    // ─────────────────── Receiver ───────────────────

    fun prepareForIncomingAudio(peerId: String) {
        applicationScope.launch(ioDispatcher + exceptionHandler) {
            MeshLogger.i("AUDIO_PREPARE", "[AUDIO_PREPARE] Incoming audio notification from peer=$peerId. Warming Wi-Fi Direct socket...")
            wifiSocketTransport.startServer()

            val pState = wifiDirectManager?.p2pState?.value
            if (pState is com.meshlink.wifi.model.WifiP2pState.Connected) {
                if (!pState.isGroupOwner && pState.groupOwnerAddress.isNotBlank()) {
                    MeshLogger.i("AUDIO_PREPARE", "[AUDIO_PREPARE] Client in P2P group. Connecting client socket to GO ${pState.groupOwnerAddress}:8988 immediately...")
                    wifiSocketTransport.connectAsClient(pState.groupOwnerAddress)
                }
            } else {
                val candidateIps = com.meshlink.wifi.util.WifiNetworkUtils.getCandidatePeerIps(context)
                val reachable = withContext(ioDispatcher) {
                    val deferreds = candidateIps.map { ip ->
                        async {
                            if (com.meshlink.wifi.util.WifiNetworkUtils.isTcpPortReachable(ip, 8988, 350)) ip else null
                        }
                    }
                    deferreds.awaitAll().firstOrNull { it != null }
                }
                if (reachable != null) {
                    MeshLogger.i("AUDIO_PREPARE", "[AUDIO_PREPARE] Found reachable peer at $reachable:8988. Auto-connecting client socket...")
                    wifiSocketTransport.connectAsClient(reachable)
                } else {
                    MeshLogger.d("AUDIO_PREPARE", "[AUDIO_PREPARE] P2P not currently connected. Triggering peer discovery...")
                    wifiDirectManager?.discoverPeers()
                }
            }
        }
    }

    fun handleIncomingPacket(packet: MeshPacket) {
        val transferId = packet.transferId ?: return

        applicationScope.launch(ioDispatcher + exceptionHandler) {
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

        val existingSession = scheduler.getSession(transferId)
        if (existingSession != null && (existingSession.transportUsed == TransportType.WIFI_DIRECT || existingSession.state == TransferState.COMPLETED)) {
            MeshLogger.d(TAG, "Ignoring META packet for existing Wi-Fi/completed transfer: $transferId")
            return
        }

        val isAudio = meta.mimeType.startsWith("audio/")
        if (isAudio) {
            MeshLogger.i("AUDIO_PREPARE", "[AUDIO_PREPARE] Received MEDIA_META for incoming audio $transferId from ${packet.senderId}. Warming Wi-Fi socket.")
            prepareForIncomingAudio(packet.senderId)
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
        sessionRegistry.registerSession(session)
        onTransferStateChanged?.invoke(transferId, TransferState.RECEIVING)
        applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }
        analytics.recordTransferStarted(session)

        startTimeoutMonitor(transferId)
    }

    private suspend fun handleChunk(packet: MeshPacket, transferId: String) {
        var session = scheduler.getSession(transferId)

        val isAudio = session?.mimeType?.startsWith("audio/") == true || packet.mimeType?.startsWith("audio/") == true
        if (isAudio) {
            MeshLogger.w(TAG, "[AUDIO_TRANSPORT_VIOLATION] Received audio chunk via BLE for $transferId. Dropping chunk: audio payload must never use BLE.")
            return
        }

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
            sessionRegistry.registerSession(session)
            onTransferStateChanged?.invoke(transferId, TransferState.RECEIVING)
            // Persist initial state for resume-after-disconnect support
            applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }
            startTimeoutMonitor(transferId)
        }

        // Synchronize totalChunks in case route fallback or initial packet desynchronization occurred
        if (packet.totalChunks > 0 && session.totalChunks != packet.totalChunks) {
            session.totalChunks = packet.totalChunks
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

            sendPacket(
                packet.targetId, packet.senderId, transferId,
                packet.chunkIndex.toString(), PacketType.MEDIA_ACK, packet.chunkIndex, session.totalChunks, session.mimeType,
                com.meshlink.domain.model.PacketPriority.CRITICAL
            )

            // Persist session state only at meaningful milestones to avoid per-chunk disk I/O pressure.
            // The session.json is required only for resume-after-disconnect; it does not need to be
            // perfectly up-to-date on every 180-byte BLE packet.
            // Milestones: first chunk (count==1) and every 10% progress step.
            val totalChunks = packet.totalChunks.coerceAtLeast(1)
            val progressPct = (count * 100) / totalChunks
            val prevProgressPct = ((count - 1) * 100) / totalChunks
            val isMilestone = count == 1 || (progressPct / 10) > (prevProgressPct / 10)
            if (isMilestone) {
                applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }
            }

            if (count >= packet.totalChunks) {
                assembleAndVerify(session)
            }
        }
    }

    private suspend fun assembleAndVerify(session: TransferSession) = withContext(ioDispatcher) {
        updateState(session.transferId, TransferState.VERIFYING)
        cache.persistSession(session)

        val mediaDir = File(context.filesDir, "mesh_media").also { if (!it.exists()) it.mkdirs() }
        val outputFile = File(mediaDir, session.fileName)

        val assembled = cache.assembleFile(session.transferId, session.totalChunks, outputFile)

        if (assembled) {
            if (verifier.verifyFileChecksum(outputFile, session.sha256Checksum)) {
                session.filePath = outputFile.absolutePath
                updateState(session.transferId, TransferState.COMPLETED)
                cache.persistSession(session)
                
                // Complete session resource teardown
                resourceManager.releaseSessionResources(session.transferId)
                cache.cleanUpSession(session.transferId)
                sessionRegistry.unregisterSession(session.transferId)

                analytics.recordTransferCompleted(session)
                val durationMs = System.currentTimeMillis() - session.startTimeMs
                metrics.recordMediaTransfer(session.totalBytes, durationMs)
                diagnostics.logTransferCompletion(session.transferId, session.totalBytes, durationMs, session.getAverageSpeedBytesPerSec().toDouble())

                notifyTransferCompleted(session)
            } else {
                MeshLogger.w(TAG, "Checksum verification failed for ${session.transferId}. Requesting chunk recovery.")
                if (session.retries < 3) {
                    session.retries++
                    outputFile.delete()
                    updateState(session.transferId, TransferState.RETRYING)
                    
                    val received = cache.getReceivedChunkIndices(session.transferId)
                    val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                    val indicesToRequest = if (missing.isNotEmpty()) missing else (0 until session.totalChunks).toList()
                    val nackBatches = formatMissingIndicesToRanges(indicesToRequest)

                    for (batch in nackBatches) {
                        sendPacket(
                            session.targetId, session.senderId, session.transferId,
                            batch, PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType,
                            com.meshlink.domain.model.PacketPriority.CRITICAL
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

            if (session.state == TransferState.STREAMING) {
                val file = File(session.filePath ?: "")
                if (file.exists()) {
                    chunkDispatcher.dispatchAvailableChunks(session, file, onSendPacket)
                }
            }

            if (ackResult.isTransferComplete) {
                updateState(transferId, TransferState.COMPLETED)
                retransmissionScheduler.stopMonitoring(transferId)
                slidingWindowManager.closeWindow(transferId)
                chunkDispatcher.clearSession(transferId)

                // Resource teardown on outgoing complete
                resourceManager.releaseSessionResources(transferId)
                sessionRegistry.unregisterSession(transferId)

                applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }
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
        val job = applicationScope.launch(ioDispatcher + exceptionHandler) {
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
        resourceManager.registerJob(transferId, job)
    }

    private fun startTimeoutMonitor(transferId: String) {
        val job = applicationScope.launch(ioDispatcher + exceptionHandler) {
            delay(TRANSFER_TIMEOUT_MS)
            val session = scheduler.getSession(transferId) ?: return@launch
            if (session.transportUsed == TransportType.WIFI_DIRECT) {
                // Wi-Fi Direct stream handles its own socket timeout and lifecycle; do not monitor chunk cache
                return@launch
            }
            if (session.state == TransferState.RECEIVING || session.state == TransferState.STREAMING) {
                val received = cache.getReceivedChunkIndices(transferId)
                val missing = (0 until session.totalChunks).filter { !received.contains(it) }

                if (missing.isNotEmpty()) {
                    MeshLogger.w(TAG, "Transfer $transferId timed out. Requesting missing ${missing.size} chunks.")
                    val nackBatches = formatMissingIndicesToRanges(missing)
                    for (batch in nackBatches) {
                        sendPacket(
                            session.targetId, session.senderId, transferId,
                            batch, PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType,
                            com.meshlink.domain.model.PacketPriority.CRITICAL
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
        resourceManager.registerJob(transferId, job)
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
            updateState(transferId, TransferState.PAUSED)
            retransmissionScheduler.stopMonitoring(transferId)
            applicationScope.launch(ioDispatcher + exceptionHandler) { cache.persistSession(session) }
            MeshLogger.d(TAG, "Paused transfer $transferId")
        }
    }

    fun resumeTransfer(transferId: String) {
        val session = scheduler.getSession(transferId) ?: return
        if (session.state == TransferState.PAUSED || session.state == TransferState.RETRYING || session.state == TransferState.QUEUED) {
            updateState(transferId, TransferState.RESUMING)
            if (session.direction == TransferDirection.OUTGOING) {
                applicationScope.launch(ioDispatcher + exceptionHandler) { startOutgoingTransfer(session) }
            } else {
                applicationScope.launch(ioDispatcher + exceptionHandler) {
                    val received = cache.getReceivedChunkIndices(transferId)
                    val missing = (0 until session.totalChunks).filter { !received.contains(it) }
                    if (missing.isNotEmpty()) {
                        val nackBatches = formatMissingIndicesToRanges(missing)
                        for (batch in nackBatches) {
                            sendPacket(
                                session.targetId, session.senderId, transferId,
                                batch, PacketType.MEDIA_NACK, 0, session.totalChunks, session.mimeType,
                                com.meshlink.domain.model.PacketPriority.CRITICAL
                            )
                        }
                        updateState(transferId, TransferState.RECEIVING)
                        cache.persistSession(session)
                    }
                }
            }
            MeshLogger.d(TAG, "Resumed transfer $transferId")
        }
    }

    fun cancelTransfer(transferId: String) {
        updateState(transferId, TransferState.CANCELLED)
        retransmissionScheduler.stopMonitoring(transferId)
        slidingWindowManager.closeWindow(transferId)
        chunkDispatcher.clearSession(transferId)
        
        resourceManager.releaseSessionResources(transferId)
        sessionRegistry.unregisterSession(transferId)

        val session = scheduler.getSession(transferId)
        applicationScope.launch(ioDispatcher + exceptionHandler) {
            if (session != null) cache.persistSession(session)
            cache.cleanUpSession(transferId)
        }
    }

    // Optional Stats API for Components
    fun getSession(transferId: String): TransferSession? = scheduler.getSession(transferId) ?: sessionRegistry.getSession(transferId)

    fun getTransferSpeedBytesPerSec(transferId: String): Float = getSession(transferId)?.getAverageSpeedBytesPerSec() ?: 0f

    fun getTransferEtaSeconds(transferId: String): Long = getSession(transferId)?.getEstimatedEtaSeconds() ?: -1L

    fun getRemainingBytes(transferId: String): Long = getSession(transferId)?.getRemainingBytes() ?: 0L

    private fun updateState(transferId: String, newState: TransferState) {
        val oldState = scheduler.getSession(transferId)?.state?.name ?: "UNKNOWN"
        scheduler.updateSessionState(transferId, newState)
        onTransferStateChanged?.invoke(transferId, newState)
        diagnostics.logSessionStateTransition(transferId, oldState, newState.name)
        // Release throttle tracking entry and clear session from active progress when terminal
        if (newState.isTerminal()) {
            scheduler.cleanupProgressTracking(transferId)
            scheduler.removeSession(transferId)
        }
    }

    private suspend fun failSession(transferId: String, reason: String) {
        val session = scheduler.getSession(transferId)
        updateState(transferId, TransferState.FAILED)
        retransmissionScheduler.stopMonitoring(transferId)
        slidingWindowManager.closeWindow(transferId)
        chunkDispatcher.clearSession(transferId)

        resourceManager.releaseSessionResources(transferId)
        sessionRegistry.unregisterSession(transferId)

        if (session != null) {
            analytics.recordTransferFailed(session, reason)
            cache.persistSession(session)
        }
        cache.cleanUpSession(transferId)
    }

    /**
     * Gracefully shuts down the TransferManager, cancelling active session jobs and releasing all pooled resources.
     */
    fun shutdown() {
        val activeSessions = sessionRegistry.getActiveSessions()
        for (session in activeSessions) {
            cancelTransfer(session.transferId)
        }
        resourceManager.closeAll()
        sessionRegistry.clearAll()
        BufferPool.trimMemory()
        MeshLogger.i(TAG, "TransferManager successfully shut down.")
    }

    private suspend fun sendPacket(
        senderId: String, targetId: String, transferId: String,
        payload: String, type: PacketType, index: Int, total: Int, mime: String,
        priority: com.meshlink.domain.model.PacketPriority = com.meshlink.domain.model.PacketPriority.NORMAL
    ) {
        val packet = MeshPacket(
            senderId = senderId,
            targetId = targetId,
            transferId = transferId,
            payload = payload,
            type = type,
            priority = priority,
            chunkIndex = index,
            totalChunks = total,
            mimeType = mime,
            ttl = 10
        )
        onSendPacket?.invoke(packet)
    }
}
