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
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_FRAME_SIZE_BYTES = 50 * 1024 * 1024 // 50MB safety limit
    }

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var dataOutStream: DataOutputStream? = null
    private var dataInStream: DataInputStream? = null

    private var listenJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private var lastHostAddress: String? = null
    private var backoffDelayMs = 2000L
    private var reconnectAttempts = 0
    private var manualDisconnectRequested = false

    // Pool of connected client sockets and resources when acting as Group Owner
    private val clientSockets = ConcurrentHashMap<String, Socket>()
    private val clientStreams = ConcurrentHashMap<String, DataOutputStream>()
    private val clientReadJobs = ConcurrentHashMap<String, Job>()
    private val clientHeartbeatJobs = ConcurrentHashMap<String, Job>()

    // Callback when a MeshPacket is received over Wi-Fi Direct
    var onPacketReceived: ((MeshPacket) -> Unit)? = null

    // Callback when a socket connects / reconnects
    var onSocketConnected: (() -> Unit)? = null

    fun startServer() {
        manualDisconnectRequested = false
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
            clientStreams.values.forEach { try { it.close() } catch (_: Exception) {} }
            clientSockets.values.forEach { try { it.close() } catch (_: Exception) {} }
            
            clientHeartbeatJobs.clear()
            clientReadJobs.clear()
            clientStreams.clear()
            clientSockets.clear()

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
        applicationScope.launch(Dispatchers.IO) {
            try {
                MeshLogger.d(TAG, "Connecting to Group Owner at $hostAddress:$PORT...")
                val socket = Socket()
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, PORT), CONNECT_TIMEOUT_MS)
                MeshLogger.d(TAG, "Socket Opened: Connected to Group Owner $hostAddress:$PORT")
                
                // Reset backoff on successful connection
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

            val currentOut = DataOutputStream(bufferedOut)
            val currentIn = DataInputStream(bufferedIn)

            if (isServerMode) {
                clientStreams[clientHost] = currentOut
            } else {
                dataOutStream = currentOut
                dataInStream = currentIn
            }

            // Trigger connection notification
            onSocketConnected?.invoke()

            // Start dedicated heartbeat monitoring for this connection
            startHeartbeat(socket, currentOut, isServerMode, clientHost)

            // Dedicated binary read loop coroutine per client connection
            val readJob = applicationScope.launch(Dispatchers.IO) {
                try {
                    while (isActive && !socket.isClosed) {
                        val length = currentIn.readInt()
                        if (length == 0) {
                            // Length = 0 is Heartbeat Ping
                            MeshLogger.d(TAG, "Received Binary Heartbeat Ping from $clientHost")
                        } else if (length > 0) {
                            if (length > MAX_FRAME_SIZE_BYTES) {
                                MeshLogger.e(TAG, "Frame size $length exceeds maximum allowed limit ($MAX_FRAME_SIZE_BYTES). Closing connection to $clientHost")
                                break
                            }
                            val payloadBytes = ByteArray(length)
                            currentIn.readFully(payloadBytes)
                            val jsonString = String(payloadBytes, Charsets.UTF_8)
                            val packet = MeshPacketParser.fromJson(jsonString)
                            if (packet != null) {
                                onPacketReceived?.invoke(packet)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive && !socket.isClosed) {
                        MeshLogger.e(TAG, "Socket binary read error on $clientHost: ${e.message}")
                    }
                } finally {
                    MeshLogger.d(TAG, "Socket Closed: Binary stream ended for $clientHost")
                    if (!isServerMode) {
                        disconnectSocketOnly()
                        scheduleReconnect()
                    } else {
                        clientReadJobs[clientHost]?.cancel()
                        clientHeartbeatJobs[clientHost]?.cancel()
                        clientReadJobs.remove(clientHost)
                        clientHeartbeatJobs.remove(clientHost)
                        clientStreams.remove(clientHost)
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
                disconnectSocketOnly()
                scheduleReconnect()
            }
        }
    }

    private fun startHeartbeat(socket: Socket, outStream: DataOutputStream, isServerMode: Boolean, clientHost: String) {
        val heartbeat = applicationScope.launch(Dispatchers.IO) {
            while (isActive && !socket.isClosed) {
                delay(HEARTBEAT_INTERVAL_MS)
                try {
                    synchronized(outStream) {
                        outStream.writeInt(0) // 0-length frame = heartbeat ping
                        outStream.flush()
                    }
                    MeshLogger.d(TAG, "Binary Heartbeat ping sent to $clientHost")
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
        if (manualDisconnectRequested) {
            MeshLogger.d(TAG, "Manual disconnect was requested. Suppressing reconnect.")
            return
        }
        val targetHost = lastHostAddress ?: return

        reconnectJob?.cancel()
        reconnectJob = applicationScope.launch(Dispatchers.IO) {
            reconnectAttempts++
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
        val payloadBytes = json.toByteArray(Charsets.UTF_8)

        // Send over active client socket if acting as Client
        val currentOut = dataOutStream
        if (currentOut != null) {
            try {
                synchronized(currentOut) {
                    currentOut.writeInt(payloadBytes.size)
                    currentOut.write(payloadBytes)
                    currentOut.flush()
                }
                MeshLogger.d(TAG, "Sent binary packet over Wi-Fi Direct socket: ${packetToSend.packetId} (${payloadBytes.size} bytes)")
                return@withContext
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to send packet to server: ${e.message}")
            }
        }

        // Send to all connected client sockets if acting as Group Owner
        if (clientStreams.isNotEmpty()) {
            clientStreams.forEach { (host, stream) ->
                try {
                    synchronized(stream) {
                        stream.writeInt(payloadBytes.size)
                        stream.write(payloadBytes)
                        stream.flush()
                    }
                    MeshLogger.d(TAG, "Sent binary packet to client $host: ${packetToSend.packetId} (${payloadBytes.size} bytes)")
                } catch (e: Exception) {
                    MeshLogger.e(TAG, "Failed to send packet to client $host: ${e.message}")
                }
            }
        } else if (currentOut == null) {
            MeshLogger.w(TAG, "Cannot send packet: No active socket connections")
        }
    }

    fun isConnected(): Boolean {
        val clientConnected = activeSocket?.isConnected == true && activeSocket?.isClosed == false
        val serverHasClients = clientSockets.values.any { it.isConnected && !it.isClosed }
        return clientConnected || serverHasClients
    }

    fun disconnect() {
        manualDisconnectRequested = true
        disconnectSocketOnly()
        stopServer()
    }

    private fun disconnectSocketOnly() {
        try {
            reconnectJob?.cancel()
            heartbeatJob?.cancel()
            listenJob?.cancel()

            gracefulCloseActiveSocket()
            MeshLogger.d(TAG, "Socket layer disconnected cleanly")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error during socket disconnect: ${e.message}")
        }
    }

    private fun gracefulCloseActiveSocket() {
        try {
            dataOutStream?.close()
            dataInStream?.close()
            activeSocket?.close()
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Error closing active socket: ${e.message}")
        } finally {
            dataOutStream = null
            dataInStream = null
            activeSocket = null
        }
    }
}
