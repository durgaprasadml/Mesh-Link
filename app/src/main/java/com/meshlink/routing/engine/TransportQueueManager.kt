package com.meshlink.routing.engine

import com.meshlink.ble.api.BleTransport
import com.meshlink.di.ApplicationScope
import com.meshlink.di.IoDispatcher
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.RouteType
import com.meshlink.wifi.api.WifiTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Lightweight queue dispatcher separating Control (BLE) and Media (Wi-Fi Direct) packet streams.
 *
 * Ensures that high-volume media chunks are processed asynchronously in the Wi-Fi Queue
 * and never block or delay instant control packets (ACKs, text, typing indicators, discovery).
 */
@Singleton
class TransportQueueManager @Inject constructor(
    private val bleTransport: BleTransport,
    private val wifiTransport: WifiTransport,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    // Separate channels for Control and Media queues to isolate transport traffic
    private val bleControlQueue = Channel<TransportWorkItem>(Channel.UNLIMITED)
    private val wifiMediaQueue = Channel<TransportWorkItem>(Channel.UNLIMITED)

    data class TransportWorkItem(
        val packet: MeshPacket,
        val includeAddress: String?,
        val excludeAddress: String?,
        val onResult: (MeshResult<Unit>) -> Unit
    )

    init {
        // Start independent background loops for BLE and Wi-Fi Direct packet dispatch
        applicationScope.launch(ioDispatcher) {
            for (item in bleControlQueue) {
                val res = bleTransport.broadcastPacket(
                    item.packet,
                    excludeAddress = item.excludeAddress,
                    includeAddress = item.includeAddress
                )
                item.onResult(res)
            }
        }

        applicationScope.launch(ioDispatcher) {
            for (item in wifiMediaQueue) {
                val res = wifiTransport.broadcastPacket(
                    item.packet,
                    excludeAddress = item.excludeAddress,
                    includeAddress = item.includeAddress
                )
                item.onResult(res)
            }
        }
    }

    /**
     * Enqueues a control packet to the lightweight BLE queue.
     */
    suspend fun dispatchBleControl(
        packet: MeshPacket,
        includeAddress: String? = null,
        excludeAddress: String? = null,
        onResult: (MeshResult<Unit>) -> Unit
    ) {
        bleControlQueue.send(TransportWorkItem(packet, includeAddress, excludeAddress, onResult))
    }

    /**
     * Enqueues a media packet to the lightweight Wi-Fi Direct queue.
     */
    suspend fun dispatchWifiMedia(
        packet: MeshPacket,
        includeAddress: String? = null,
        excludeAddress: String? = null,
        onResult: (MeshResult<Unit>) -> Unit
    ) {
        wifiMediaQueue.send(TransportWorkItem(packet, includeAddress, excludeAddress, onResult))
    }
}
