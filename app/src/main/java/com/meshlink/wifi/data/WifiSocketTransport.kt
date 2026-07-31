package com.meshlink.wifi.data

import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.util.MeshPacketParser
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WifiSocketTransport @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "WifiSocketTransport"
        private const val PORT = 8888
        private const val SOCKET_TIMEOUT_MS = 15000
        private const val HEARTBEAT_INTERVAL_MS = 10000L
        private const val HEARTBEAT_PING = "__PING__"
        private const val HEARTBEAT_PONG = "__PONG__"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var heartbeatJob: Job? = null

    // Map of Peer IP / Address -> Client Socket Connection state
    private val clientConnections = ConcurrentHashMap<String, ClientConnection>()

    private val _connectedPeers = MutableStateFlow<Set<String>>(emptySet())
    val connectedPeersFlow: StateFlow<Set<String>> = _connectedPeers.asStateFlow()

    val connectedPeers: Set<String>
        get() = clientConnections.keys.toSet()

    // Callback when a MeshPacket is received over Wi-Fi Direct socket
    var onPacketReceived: ((senderAddress: String, packet: MeshPacket) -> Unit)? = null
    var onPeerConnected: ((peerAddress: String) -> Unit)? = null
    var onPeerDisconnected: ((peerAddress: String) -> Unit)? = null

    private inner class ClientConnection(
        val peerAddress: String,
        val socket: Socket,
        val reader: BufferedReader,
        val writer: PrintWriter,
        var readJob: Job? = null,
        var lastActiveTime: Long = System.currentTimeMillis()
    )

    fun startServer() {
        if (serverSocket != null && serverSocket?.isClosed == false) return

        serverJob?.cancel()
        serverJob = applicationScope.launch(Dispatchers.IO) {
            try {
                val ss = ServerSocket(PORT)
                ss.reuseAddress = true
                serverSocket = ss
                MeshLogger.d(TAG, "ServerSocket started on port $PORT, listening for incoming client connections...")

                startHeartbeatLoop()

                while (isActive && !ss.isClosed) {
                    val clientSocket = ss.accept()
                    val remoteIp = clientSocket.inetAddress?.hostAddress ?: continue
                    MeshLogger.d(TAG, "Incoming connection accepted from $remoteIp")
                    setupConnection(remoteIp, clientSocket)
                }
            } catch (e: Exception) {
                if (e !is java.net.SocketException || serverSocket?.isClosed == false) {
                    MeshLogger.e(TAG, "ServerSocket error: ${e.message}")
                }
            }
        }
    }

    fun stopServer() {
        try {
            heartbeatJob?.cancel()
            serverJob?.cancel()
            serverSocket?.close()
            serverSocket = null
            disconnectAll()
            MeshLogger.d(TAG, "ServerSocket stopped and all connections cleared")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping ServerSocket: ${e.message}")
        }
    }

    fun connectAsClient(hostAddress: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (clientConnections.containsKey(hostAddress)) {
            MeshLogger.d(TAG, "Already connected to $hostAddress")
            onComplete?.invoke(true)
            return
        }

        applicationScope.launch(Dispatchers.IO) {
            try {
                MeshLogger.d(TAG, "Connecting as client to Group Owner at $hostAddress:$PORT...")
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.sendBufferSize = 1024 * 1024
                socket.receiveBufferSize = 1024 * 1024
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, PORT), SOCKET_TIMEOUT_MS)

                MeshLogger.d(TAG, "Successfully connected to Group Owner at $hostAddress")
                setupConnection(hostAddress, socket)
                startHeartbeatLoop()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Client socket error connecting to $hostAddress: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    private fun setupConnection(peerAddress: String, socket: Socket) {
        try {
            socket.soTimeout = SOCKET_TIMEOUT_MS
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

            val connection = ClientConnection(
                peerAddress = peerAddress,
                socket = socket,
                reader = reader,
                writer = writer
            )

            val existing = clientConnections.put(peerAddress, connection)
            existing?.let { closeConnectionSilently(it) }

            updateConnectedPeersState()
            onPeerConnected?.invoke(peerAddress)

            connection.readJob = applicationScope.launch(Dispatchers.IO) {
                try {
                    while (isActive && !socket.isClosed) {
                        val line = reader.readLine() ?: break
                        connection.lastActiveTime = System.currentTimeMillis()

                        when (line.trim()) {
                            HEARTBEAT_PING -> {
                                writer.println(HEARTBEAT_PONG)
                            }
                            HEARTBEAT_PONG -> {
                                // Heartbeat ACK received
                            }
                            else -> {
                                if (line.isNotBlank()) {
                                    val packet = MeshPacketParser.fromJson(line)
                                    if (packet != null) {
                                        onPacketReceived?.invoke(peerAddress, packet)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    MeshLogger.d(TAG, "Socket stream ended for $peerAddress: ${e.message}")
                } finally {
                    disconnectPeer(peerAddress)
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to setup socket streams for $peerAddress: ${e.message}")
            disconnectPeer(peerAddress)
        }
    }

    suspend fun sendPacket(packet: MeshPacket): Boolean = withContext(Dispatchers.IO) {
        val targetIp = packet.targetId
        val json = MeshPacketParser.toJson(packet)

        if (targetIp.isNotBlank() && targetIp != "BROADCAST" && clientConnections.containsKey(targetIp)) {
            val conn = clientConnections[targetIp] ?: return@withContext false
            return@withContext sendLineToConnection(conn, json)
        } else {
            // Broadcast to all active socket connections
            return@withContext broadcastLine(json)
        }
    }

    suspend fun broadcastPacket(packet: MeshPacket, excludeAddress: String? = null, includeAddress: String? = null): Boolean = withContext(Dispatchers.IO) {
        val json = MeshPacketParser.toJson(packet)
        if (includeAddress != null && clientConnections.containsKey(includeAddress)) {
            val conn = clientConnections[includeAddress] ?: return@withContext false
            return@withContext sendLineToConnection(conn, json)
        }

        var successCount = 0
        clientConnections.forEach { (peerAddress, conn) ->
            if (peerAddress != excludeAddress) {
                if (sendLineToConnection(conn, json)) {
                    successCount++
                }
            }
        }
        return@withContext successCount > 0 || clientConnections.isEmpty()
    }

    private fun sendLineToConnection(conn: ClientConnection, line: String): Boolean {
        return try {
            conn.writer.println(line)
            true
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Send to ${conn.peerAddress} failed: ${e.message}")
            disconnectPeer(conn.peerAddress)
            false
        }
    }

    private fun broadcastLine(line: String): Boolean {
        var anySuccess = false
        clientConnections.values.forEach { conn ->
            if (sendLineToConnection(conn, line)) {
                anySuccess = true
            }
        }
        return anySuccess
    }

    private fun startHeartbeatLoop() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = applicationScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                val now = System.currentTimeMillis()
                clientConnections.forEach { (peerAddress, conn) ->
                    if (now - conn.lastActiveTime > SOCKET_TIMEOUT_MS * 2) {
                        MeshLogger.w(TAG, "Connection timeout for $peerAddress, disconnecting...")
                        disconnectPeer(peerAddress)
                    } else {
                        sendLineToConnection(conn, HEARTBEAT_PING)
                    }
                }
            }
        }
    }

    fun disconnectPeer(peerAddress: String) {
        val conn = clientConnections.remove(peerAddress) ?: return
        closeConnectionSilently(conn)
        updateConnectedPeersState()
        onPeerDisconnected?.invoke(peerAddress)
        MeshLogger.d(TAG, "Disconnected peer $peerAddress")
    }

    fun disconnectAll() {
        val peers = clientConnections.keys.toList()
        peers.forEach { disconnectPeer(it) }
    }

    private fun closeConnectionSilently(conn: ClientConnection) {
        try {
            conn.readJob?.cancel()
            conn.writer.close()
            conn.reader.close()
            conn.socket.close()
        } catch (e: Exception) {
            // Socket teardown exceptions are non-fatal — the OS will reclaim the socket.
            // Log at WARN so patterns (e.g., repeated double-close) remain visible in diagnostics.
            MeshLogger.w(TAG, "Exception during socket teardown for peer (ignored): ${e.message}")
        }
    }

    private fun updateConnectedPeersState() {
        _connectedPeers.value = clientConnections.keys.toSet()
    }

    fun isConnected(): Boolean {
        return clientConnections.isNotEmpty()
    }

    fun getConnectedPeersCount(): Int {
        return clientConnections.size
    }
}
