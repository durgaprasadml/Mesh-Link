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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        private const val HEARTBEAT_INTERVAL_MS = 15_000L
        private const val PING_PAYLOAD = "__PING__"
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var listenJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private var lastHostAddress: String? = null
    private var backoffDelayMs = 2000L
    private var reconnectAttempts = 0

    // Pool of connected client sockets and resources when acting as Group Owner
    private val clientSockets = ConcurrentHashMap<String, Socket>()
    private val clientWriters = ConcurrentHashMap<String, PrintWriter>()
    private val clientReadJobs = ConcurrentHashMap<String, Job>()
    private val clientHeartbeatJobs = ConcurrentHashMap<String, Job>()

    // Callback when a MeshPacket is received over Wi-Fi Direct
    var onPacketReceived: ((MeshPacket) -> Unit)? = null

    // Callback when a socket connects / reconnects
    var onSocketConnected: (() -> Unit)? = null

    fun startServer() {
        if (serverSocket != null && !serverSocket!!.isClosed) return
        applicationScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(PORT)
                MeshLogger.d(TAG, "ServerSocket started on port $PORT. Listening for incoming multi-peer connections...")

                while (isActive && serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    val clientHost = client.inetAddress?.hostAddress ?: "unknown"
                    MeshLogger.d(TAG, "Client connected to Group Owner: $clientHost")
                    
                    clientSockets[clientHost] = client
                    handleSocketConnection(client, isServerMode = true)
                }
            } catch (e: Exception) {
                if (isActive) {
                    MeshLogger.e(TAG, "ServerSocket error: ${e.message}")
                }
            }
        }
    }

    fun stopServer() {
        try {
            clientHeartbeatJobs.values.forEach { try { it.cancel() } catch (_: Exception) {} }
            clientReadJobs.values.forEach { try { it.cancel() } catch (_: Exception) {} }
            clientWriters.values.forEach { try { it.close() } catch (_: Exception) {} }
            clientSockets.values.forEach { try { it.close() } catch (_: Exception) {} }
            
            clientHeartbeatJobs.clear()
            clientReadJobs.clear()
            clientWriters.clear()
            clientSockets.clear()

            serverSocket?.close()
            serverSocket = null
            MeshLogger.d(TAG, "ServerSocket stopped successfully")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping ServerSocket: ${e.message}")
        }
    }

    fun connectAsClient(hostAddress: String) {
        lastHostAddress = hostAddress
        applicationScope.launch(Dispatchers.IO) {
            try {
                MeshLogger.d(TAG, "Connecting to Group Owner at $hostAddress:$PORT...")
                val socket = Socket()
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, PORT), CONNECT_TIMEOUT_MS)
                MeshLogger.d(TAG, "Socket Opened: Connected to Group Owner $hostAddress:$PORT")
                
                // Reset backoff and attempt counter on successful connection
                backoffDelayMs = 2000L
                reconnectAttempts = 0
                handleSocketConnection(socket, isServerMode = false)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Client socket error: ${e.message}")
                scheduleReconnect()
            }
        }
    }

    private fun handleSocketConnection(socket: Socket, isServerMode: Boolean) {
        val clientHost = socket.inetAddress?.hostAddress ?: "unknown"

        if (!isServerMode) {
            gracefulCloseActiveSocket()
            activeSocket = socket
        } else {
            // Cancel existing jobs for this client host if re-connecting
            clientReadJobs[clientHost]?.cancel()
            clientHeartbeatJobs[clientHost]?.cancel()
        }

        socket.tcpNoDelay = true
        socket.sendBufferSize = 2 * 1024 * 1024 // 2 MB buffer
        socket.receiveBufferSize = 2 * 1024 * 1024 // 2 MB buffer

        try {
            val bufferedOut = BufferedOutputStream(socket.getOutputStream(), 128 * 1024)
            val bufferedIn = BufferedInputStream(socket.getInputStream(), 128 * 1024)

            val currentWriter = PrintWriter(OutputStreamWriter(bufferedOut, Charsets.UTF_8), true)
            val currentReader = BufferedReader(InputStreamReader(bufferedIn, Charsets.UTF_8))

            if (isServerMode) {
                clientWriters[clientHost] = currentWriter
            } else {
                writer = currentWriter
                reader = currentReader
            }

            // Trigger connection notification
            onSocketConnected?.invoke()

            // Start dedicated heartbeat monitoring for this connection
            startHeartbeat(socket, currentWriter, isServerMode, clientHost)

            // Dedicated read loop coroutine per client connection
            val readJob = applicationScope.launch(Dispatchers.IO) {
                try {
                    while (isActive && !socket.isClosed) {
                        val line = currentReader.readLine() ?: break
                        if (line.isNotEmpty()) {
                            if (line == PING_PAYLOAD) {
                                MeshLogger.d(TAG, "Received Heartbeat Ping from $clientHost")
                            } else {
                                val packet = MeshPacketParser.fromJson(line)
                                if (packet != null) {
                                    onPacketReceived?.invoke(packet)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Socket read error on $clientHost: ${e.message}")
                } finally {
                    MeshLogger.d(TAG, "Socket Closed: Stream ended for $clientHost")
                    if (!isServerMode) {
                        disconnect()
                        scheduleReconnect()
                    } else {
                        clientReadJobs[clientHost]?.cancel()
                        clientHeartbeatJobs[clientHost]?.cancel()
                        clientReadJobs.remove(clientHost)
                        clientHeartbeatJobs.remove(clientHost)
                        clientWriters.remove(clientHost)
                        clientSockets.remove(clientHost)
                        try { socket.close() } catch (_: Exception) {}
                    }
                }
            }

            if (isServerMode) {
                clientReadJobs[clientHost] = readJob
            } else {
                listenJob = readJob
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to setup socket streams for $clientHost: ${e.message}")
            if (!isServerMode) {
                disconnect()
                scheduleReconnect()
            }
        }
    }

    private fun startHeartbeat(socket: Socket, outWriter: PrintWriter, isServerMode: Boolean, clientHost: String) {
        val heartbeat = applicationScope.launch(Dispatchers.IO) {
            while (isActive && !socket.isClosed) {
                delay(HEARTBEAT_INTERVAL_MS)
                try {
                    outWriter.println(PING_PAYLOAD)
                    MeshLogger.d(TAG, "Heartbeat ping sent to $clientHost")
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "Heartbeat Failed to $clientHost: ${e.message}")
                    break
                }
            }
        }

        if (isServerMode) {
            clientHeartbeatJobs[clientHost] = heartbeat
        } else {
            heartbeatJob?.cancel()
            heartbeatJob = heartbeat
        }
    }

    private fun scheduleReconnect() {
        val targetHost = lastHostAddress ?: return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            MeshLogger.w(TAG, "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached for $targetHost. Ceasing reconnect attempts.")
            reconnectAttempts = 0
            return
        }

        reconnectJob?.cancel()
        reconnectJob = applicationScope.launch(Dispatchers.IO) {
            reconnectAttempts++
            MeshLogger.d(TAG, "Scheduling reconnect attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS to $targetHost in ${backoffDelayMs}ms...")
            delay(backoffDelayMs)
            
            // Exponential backoff
            backoffDelayMs = (backoffDelayMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            connectAsClient(targetHost)
        }
    }

    suspend fun sendPacket(packet: MeshPacket) = withContext(Dispatchers.IO) {
        var packetToSend = packet
        if (!packetToSend.encrypted && packetToSend.targetId != "BROADCAST") {
            val requirement = PacketEncryptionPolicy.getRequirement(packetToSend.type)
            if (requirement == EncryptionRequirement.REQUIRED || requirement == EncryptionRequirement.OPTIONAL) {
                val aadResult = sessionManager.generateAad(packetToSend.targetId)
                val aadBytes = aadResult?.first
                val aadPrefix = aadResult?.second ?: ""
                val encryptedResult = cryptoManager.encryptOrPassthrough(
                    packetToSend.payload,
                    packetToSend.targetId,
                    true,
                    packetToSend.packetId,
                    0,
                    aadBytes
                )
                if (encryptedResult != null && encryptedResult.second) {
                    val finalPayload = if (aadPrefix.isNotEmpty()) "$aadPrefix${encryptedResult.first}" else encryptedResult.first
                    packetToSend = packetToSend.copy(payload = finalPayload, encrypted = true)
                }
            }
        }

        val json = MeshPacketParser.toJson(packetToSend)

        // Send over active client socket if acting as Client
        val currentWriter = writer
        if (currentWriter != null) {
            try {
                currentWriter.println(json)
                MeshLogger.d(TAG, "Sent packet over Wi-Fi Direct socket: ${packetToSend.packetId}")
                return@withContext
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to send packet to server: ${e.message}")
            }
        }

        // Send to all connected client sockets if acting as Group Owner
        if (clientWriters.isNotEmpty()) {
            clientWriters.forEach { (host, w) ->
                try {
                    w.println(json)
                    MeshLogger.d(TAG, "Sent packet to client $host: ${packetToSend.packetId}")
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to send packet to client $host: ${e.message}")
                }
            }
        } else if (currentWriter == null) {
            MeshLogger.w(TAG, "Cannot send packet: No active socket connections")
        }
    }

    fun isConnected(): Boolean {
        val clientConnected = activeSocket?.isConnected == true && activeSocket?.isClosed == false
        val serverHasClients = clientSockets.values.any { it.isConnected && !it.isClosed }
        return clientConnected || serverHasClients
    }

    fun disconnect() {
        try {
            reconnectJob?.cancel()
            heartbeatJob?.cancel()
            listenJob?.cancel()

            gracefulCloseActiveSocket()
            stopServer()

            MeshLogger.d(TAG, "Socket layer disconnected cleanly")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error during socket disconnect: ${e.message}")
        }
    }

    private fun gracefulCloseActiveSocket() {
        try {
            writer?.close()
            reader?.close()
            activeSocket?.close()
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Error closing active socket: ${e.message}")
        } finally {
            writer = null
            reader = null
            activeSocket = null
        }
    }
}
