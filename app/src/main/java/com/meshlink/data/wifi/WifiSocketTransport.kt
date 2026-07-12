package com.meshlink.data.wifi

import android.util.Log
import com.meshlink.data.ble.MeshPacket
import com.meshlink.data.ble.MeshPacketParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiSocketTransport @Inject constructor() {
    companion object {
        private const val TAG = "WifiSocketTransport"
        private const val PORT = 8888
        private const val TIMEOUT_MS = 10000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var listenJob: Job? = null

    // Callback when a MeshPacket is received over Wi-Fi Direct
    var onPacketReceived: ((MeshPacket) -> Unit)? = null

    fun startServer() {
        if (serverSocket != null) return
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "ServerSocket started on port $PORT, waiting for client...")
                
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    Log.d(TAG, "Client connected: ${client.inetAddress.hostAddress}")
                    handleSocketConnection(client)
                }
            } catch (e: Exception) {
                Log.e(TAG, "ServerSocket error: ${e.message}")
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ServerSocket: ${e.message}")
        }
    }

    fun connectAsClient(hostAddress: String) {
        scope.launch {
            try {
                Log.d(TAG, "Connecting to Group Owner at $hostAddress:$PORT...")
                val socket = Socket()
                socket.bind(null)
                socket.connect(InetSocketAddress(hostAddress, PORT), TIMEOUT_MS)
                Log.d(TAG, "Connected to Group Owner!")
                handleSocketConnection(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Client socket error: ${e.message}")
            }
        }
    }

    private fun handleSocketConnection(socket: Socket) {
        activeSocket?.close()
        activeSocket = socket
        
        try {
            writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            
            listenJob?.cancel()
            listenJob = scope.launch {
                try {
                    while (isActive) {
                        val line = reader?.readLine() ?: break
                        if (line.isNotEmpty()) {
                            val packet = MeshPacketParser.fromJson(line)
                            if (packet != null) {
                                onPacketReceived?.invoke(packet)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Socket read error: ${e.message}")
                } finally {
                    disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup socket streams: ${e.message}")
            disconnect()
        }
    }

    fun disconnect() {
        try {
            listenJob?.cancel()
            writer?.close()
            reader?.close()
            activeSocket?.close()
            
            writer = null
            reader = null
            activeSocket = null
            Log.d(TAG, "Socket disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}")
        }
    }

    suspend fun sendPacket(packet: MeshPacket) = withContext(Dispatchers.IO) {
        val currentWriter = writer ?: throw IllegalStateException("Socket is not connected")
        val json = MeshPacketParser.toJson(packet)
        currentWriter.println(json)
        Log.d(TAG, "Sent packet over Wi-Fi Direct: ${packet.packetId}")
    }
    
    fun isConnected(): Boolean {
        return activeSocket?.isConnected == true && activeSocket?.isClosed == false
    }
}
