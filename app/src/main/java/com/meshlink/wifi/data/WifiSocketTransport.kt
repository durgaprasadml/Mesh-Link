package com.meshlink.wifi.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.util.MeshPacketParser
import com.meshlink.config.WifiConfig
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.policy.EncryptionRequirement
import com.meshlink.security.policy.PacketEncryptionPolicy

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
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
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "WifiSocketTransport"
        private const val PORT = WifiConfig.DEFAULT_PORT
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
        private const val HEARTBEAT_WATCHDOG_TIMEOUT_MS = 30_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_FRAME_SIZE_BYTES = 50 * 1024 * 1024 // 50MB safety limit
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

    // Connection State & Metrics Flow
    private val _connectionState = MutableStateFlow(WifiSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WifiSocketConnectionState> = _connectionState.asStateFlow()

    private val _metricsState = MutableStateFlow(WifiSocketMetrics())
    val metricsState: StateFlow<WifiSocketMetrics> = _metricsState.asStateFlow()

    // Callbacks
    var onPacketReceived: ((MeshPacket) -> Unit)? = null
    var onSocketConnected: (() -> Unit)? = null

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
            serverSocket = ServerSocket(PORT)
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

    fun connectAsClient(hostAddress: String) {
        manualDisconnectRequested = false
        lastHostAddress = hostAddress
        _connectionState.value = WifiSocketConnectionState.CONNECTING

        applicationScope.launch(Dispatchers.IO) {
            try {
                MeshLogger.d(TAG, "Connecting to Group Owner at $hostAddress:$PORT...")
                val socket = Socket()
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, PORT), CONNECT_TIMEOUT_MS)
                MeshLogger.d(TAG, "Socket Opened: Connected to Group Owner $hostAddress:$PORT")
                
                // Reset backoff on successful connection
                backoffDelayMs = 1000L
                reconnectAttempts = 0
                handleSocketConnection(socket, isServerMode = false, clientHost = hostAddress)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Client socket error connecting to $hostAddress: ${e.message}")
                _connectionState.value = WifiSocketConnectionState.FAILED
                scheduleReconnect()
            }
        }
    }

    private fun handleSocketConnection(socket: Socket, isServerMode: Boolean, clientHost: String) {
        try {
            socket.soTimeout = READ_TIMEOUT_MS
            socket.tcpNoDelay = true
            socket.sendBufferSize = 2 * 1024 * 1024 // 2 MB buffer
            socket.receiveBufferSize = 2 * 1024 * 1024 // 2 MB buffer

            val bufferedOut = BufferedOutputStream(socket.getOutputStream(), 128 * 1024)
            val bufferedIn = BufferedInputStream(socket.getInputStream(), 128 * 1024)

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

                        if (length == 0) {
                            // Length = 0 is Heartbeat Ping
                            clientLastHeartbeatMs[clientHost] = System.currentTimeMillis()
                            val hbCount = heartbeatCounter.incrementAndGet()
                            updateMetrics { it.copy(heartbeatCount = hbCount) }
                            MeshLogger.d(TAG, "Received Binary Heartbeat Ping from $clientHost")
                        } else if (length > 0) {
                            if (length > MAX_FRAME_SIZE_BYTES) {
                                MeshLogger.e(TAG, "Frame size $length exceeds maximum allowed limit ($MAX_FRAME_SIZE_BYTES). Closing connection to $clientHost")
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
                                MeshLogger.d(TAG, "Packet Received over Wi-Fi Direct from $clientHost: ${packet.packetId} (${length}B)")
                                onPacketReceived?.invoke(packet)
                            }
                        } else {
                            MeshLogger.e(TAG, "Invalid negative packet length $length from $clientHost")
                            break
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

        updateMetrics { it.copy(activePeers = clientSockets.size) }
        MeshLogger.d(TAG, "Cleaned up socket resources for peer: $clientHost")
    }
}
