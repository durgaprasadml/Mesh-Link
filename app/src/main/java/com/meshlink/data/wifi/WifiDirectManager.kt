package com.meshlink.data.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel = manager.initialize(context, Looper.getMainLooper(), null)

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var activeSocket: Socket? = null
    private var isGroupOwner = false
    private var serverSocket: ServerSocket? = null
    
    // Fallback indicator
    var isTransportActive: Boolean = false

    private val _incomingStreams = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val incomingStreams: SharedFlow<String> = _incomingStreams.asSharedFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager.requestConnectionInfo(channel) { info ->
                            establishSocketConnection(info)
                        }
                    } else {
                        isTransportActive = false
                        activeSocket?.close()
                        activeSocket = null
                    }
                }
            }
        }
    }

    fun start() {
        context.registerReceiver(receiver, intentFilter)
    }

    fun stop() {
        context.unregisterReceiver(receiver)
        activeSocket?.close()
        serverSocket?.close()
        manager.removeGroup(channel, null)
    }

    // A fast background blind-discovery to populate Android's internal P2P tables natively
    // We execute this concurrently with BLE discovery to satisfy the hardware OS locks
    fun igniteBackgroundDiscovery() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })
    }
    
    // Explicit Handshake requested by MeshRouter
    // Mac is fetched over BLE mapping or manual connection
    fun connectToPeer(deviceMac: String) {
        val config = WifiP2pConfig().apply {
            deviceAddress = deviceMac
            // Ensure fast connections bypass prompts where historically possible
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WifiDirect", "Negotiation bridging successfully to P2P Link...")
            }
            override fun onFailure(reason: Int) {
                Log.e("WifiDirect", "Failed P2P Handshake: $reason")
            }
        })
    }

    private fun establishSocketConnection(info: WifiP2pInfo) {
        isTransportActive = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (info.groupFormed && info.isGroupOwner) {
                    isGroupOwner = true
                    serverSocket = ServerSocket(8888)
                    activeSocket = serverSocket?.accept() // Blocking wait
                } else if (info.groupFormed) {
                    isGroupOwner = false
                    val socket = Socket()
                    socket.bind(null)
                    // Connect directly to the Group Owner's known explicit IP
                    socket.connect(InetSocketAddress(info.groupOwnerAddress, 8888), 5000)
                    activeSocket = socket
                }
                
                maintainSocketStream()
                
            } catch (e: Exception) {
                isTransportActive = false
                Log.e("WifiDirect", "Socket collapse: ${e.message}")
            }
        }
    }

    private fun maintainSocketStream() {
        val socket = activeSocket ?: return
        val inputStream: InputStream = socket.getInputStream()
        val buffer = ByteArray(8192) // 8KB Wide-Band Chunking

        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isTransportActive) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    
                    val jsonPacket = String(buffer, 0, bytesRead, Charsets.UTF_8)
                    _incomingStreams.tryEmit(jsonPacket)
                }
            } catch (e: Exception) {
                isTransportActive = false
            } finally {
                socket.close()
            }
        }
    }

    fun broadcastThroughput(jsonPacket: String): Boolean {
        if (!isTransportActive || activeSocket == null) return false
        
        return try {
            val outputStream: OutputStream = activeSocket!!.getOutputStream()
            outputStream.write(jsonPacket.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            true
        } catch (e: Exception) {
            isTransportActive = false
            false
        }
    }
}
