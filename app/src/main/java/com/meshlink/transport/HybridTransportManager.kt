package com.meshlink.transport

import com.meshlink.ble.api.BleTransport
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.wifi.api.WifiTransport
import com.meshlink.wifi.data.WifiDirectManager
import com.meshlink.wifi.data.WifiP2pConnectionState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
internal class HybridTransportManager @Inject constructor(
    private val bleTransport: BleTransport,
    private val wifiTransport: WifiTransport,
    private val wifiDirectManager: WifiDirectManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : HybridTransport {

    companion object {
        private const val TAG = "HybridTransportManager"
        private const val HIGH_BANDWIDTH_SIZE_BYTES = 1024L // 1 KB threshold
        private const val HYSTERESIS_SCORE_DELTA = 15 // Score must be 15 points higher to switch
        private const val STABILITY_WINDOW_MS = 3000L // 3-second stability window before mode switch
    }

    private val _incomingPackets = MutableSharedFlow<Pair<String, MeshPacket>>(extraBufferCapacity = 400)
    override val incomingPackets: SharedFlow<Pair<String, MeshPacket>> = _incomingPackets.asSharedFlow()

    private val _activeMode = MutableStateFlow(HybridMode.BLE_ONLY)
    override val activeMode: StateFlow<HybridMode> = _activeMode.asStateFlow()

    private val _metrics = MutableStateFlow(HybridTransportMetrics())
    override val metrics: StateFlow<HybridTransportMetrics> = _metrics.asStateFlow()

    override val isWifiConnected: Boolean
        get() = wifiTransport.isConnected

    override val isBleConnected: Boolean
        get() = bleTransport.connectedPeers.isNotEmpty()

    override val connectedPeers: Set<String>
        get() = bleTransport.connectedPeers + wifiTransport.connectedPeers

    private val preferredTransportMode = settingsRepository.preferredTransport
        .stateIn(applicationScope, SharingStarted.Eagerly, "AUTOMATIC")

    // Counters for metrics tracking
    private val fallbackCounter = AtomicLong(0)
    private val upgradeCounter = AtomicLong(0)
    private val downgradeCounter = AtomicLong(0)
    private val retryCounter = AtomicLong(0)
    private val blePacketsSent = AtomicLong(0)
    private val wifiPacketsSent = AtomicLong(0)
    private val totalBytesSent = AtomicLong(0)
    private val packetTypeCountMap = ConcurrentHashMap<String, AtomicLong>()
    private val recentFailuresWifi = AtomicLong(0)

    // Hysteresis tracking — @Volatile ensures cross-thread visibility without lock overhead.
    // These fields are written/read from coroutines on Dispatchers.Default (multi-threaded).
    @Volatile private var candidateMode: HybridMode? = null
    @Volatile private var candidateStartTime: Long = 0L

    init {
        // Parallel listening: Merge incoming BLE stream
        applicationScope.launch {
            bleTransport.incomingPackets.collect { (sender, packet) ->
                MeshLogger.d(TAG, "Incoming BLE packet from $sender (id=${packet.packetId}, type=${packet.type})")
                recordIncomingPacket(packet, isWifi = false)
                _incomingPackets.emit(sender to packet)
            }
        }

        // Parallel listening: Merge incoming Wi-Fi Direct stream
        applicationScope.launch {
            wifiTransport.incomingPackets.collect { (sender, packet) ->
                MeshLogger.d(TAG, "Incoming Wi-Fi packet from $sender (id=${packet.packetId}, type=${packet.type})")
                recordIncomingPacket(packet, isWifi = true)
                _incomingPackets.emit(sender to packet)
            }
        }

        // Monitor transport connectivity with Time + Score Hysteresis
        applicationScope.launch {
            wifiDirectManager.connectionState.collect { connState ->
                val wifiConnected = wifiTransport.isConnected || connState == WifiP2pConnectionState.CONNECTED
                val bleConnected = bleTransport.connectedPeers.isNotEmpty()

                val targetMode = when {
                    wifiConnected && bleConnected -> HybridMode.HYBRID_ACTIVE
                    wifiConnected -> HybridMode.WIFI_DIRECT_ONLY
                    else -> HybridMode.BLE_ONLY
                }
                evaluateHysteresisModeSwitch(targetMode)
            }
        }
    }

    private fun evaluateHysteresisModeSwitch(targetMode: HybridMode) {
        val current = _activeMode.value
        if (targetMode == current) {
            candidateMode = null
            return
        }

        val now = System.currentTimeMillis()
        if (candidateMode != targetMode) {
            candidateMode = targetMode
            candidateStartTime = now
            MeshLogger.d(TAG, "Candidate transport mode changed to $targetMode. Starting stability timer (${STABILITY_WINDOW_MS}ms)")
            return
        }

        if (now - candidateStartTime >= STABILITY_WINDOW_MS) {
            val oldMode = _activeMode.value
            _activeMode.value = targetMode
            if (targetMode == HybridMode.HYBRID_ACTIVE || targetMode == HybridMode.WIFI_DIRECT_ONLY) {
                upgradeCounter.incrementAndGet()
            } else if (oldMode == HybridMode.HYBRID_ACTIVE || oldMode == HybridMode.WIFI_DIRECT_ONLY) {
                downgradeCounter.incrementAndGet()
            }
            MeshLogger.d(TAG, "Transport mode stabilized: $oldMode -> $targetMode")
            candidateMode = null
            updateMetricsSnapshot()
        }
    }

    /**
     * Calculates dynamic transport scores for BLE and Wi-Fi Direct.
     * Returns RouteType.WIFI_DIRECT, RouteType.BLE, or RouteType.HYBRID (for SOS).
     */
    override fun getSelectedRouteType(
        targetId: String,
        packetType: PacketType,
        payloadSize: Long,
        batteryLevel: Int,
        rssi: Int,
        queueSize: Int
    ): RouteType {
        val pref = preferredTransportMode.value
        if (pref == "BLE_ONLY") return RouteType.BLE
        if (pref == "WIFI_ONLY" && wifiTransport.isConnected) return RouteType.WIFI_DIRECT

        // Emergency SOS: Always broadcast on BOTH transports for maximum reachability
        if (packetType == PacketType.SOS) {
            return RouteType.HYBRID
        }

        var bleScore = 50 // Base score for BLE
        var wifiScore = 0 // Base score for Wi-Fi Direct

        val isWifiAvailable = wifiTransport.isConnected &&
                (wifiTransport.connectedPeers.contains(targetId) || wifiTransport.connectedPeers.isNotEmpty())

        if (isWifiAvailable) {
            wifiScore = 60
        }

        // 1. Packet Type & High-Bandwidth Requirements
        val isHighBandwidth = when (packetType) {
            PacketType.VIDEO_FRAME,
            PacketType.VOICE_FRAME,
            PacketType.MEDIA_CHUNK,
            PacketType.MEDIA_META -> true
            else -> payloadSize >= HIGH_BANDWIDTH_SIZE_BYTES
        }

        if (isHighBandwidth) {
            wifiScore += 40
            bleScore -= 10
        } else {
            // Control, Text, ACK, NACK, Location favor BLE
            bleScore += 30
        }

        // 2. Payload Size threshold
        if (payloadSize < HIGH_BANDWIDTH_SIZE_BYTES) {
            bleScore += 20
        } else {
            wifiScore += 25
        }

        // 3. Signal Strength / RSSI
        if (rssi > -70) {
            bleScore += 15
            wifiScore += 15
        } else if (rssi < -85) {
            bleScore -= 15
            wifiScore -= 20
        }

        // 4. Battery Level Heuristic
        if (batteryLevel < 15) {
            // Critical battery penalizes power-hungry Wi-Fi Direct unless mandatory high bandwidth
            if (!isHighBandwidth) {
                wifiScore -= 35
            }
        }

        // 5. Congestion / Queue length penalty
        if (queueSize > 20) {
            bleScore -= 20
        }

        // 6. Recent Wi-Fi Direct failures penalty
        if (recentFailuresWifi.get() > 2) {
            wifiScore -= 30
        }

        // 7. Auto-Upgrade Trigger check
        if (isHighBandwidth && !isWifiAvailable && wifiDirectManager.isP2pEnabled.value && !wifiTransport.isConnected) {
            triggerAutoUpgrade(targetId)
        }

        // Apply Hysteresis score delta requirement (+15 points needed for Wi-Fi)
        return if (wifiScore > bleScore + HYSTERESIS_SCORE_DELTA && isWifiAvailable) {
            RouteType.WIFI_DIRECT
        } else if (isWifiAvailable && isHighBandwidth && wifiScore > bleScore) {
            RouteType.WIFI_DIRECT
        } else {
            RouteType.BLE
        }
    }

    override fun triggerAutoUpgrade(peerAddress: String) {
        if (!wifiDirectManager.isP2pEnabled.value) return
        if (wifiTransport.isConnected || wifiDirectManager.connectionState.value == WifiP2pConnectionState.CONNECTING) return

        applicationScope.launch {
            MeshLogger.d(TAG, "Auto-Upgrade triggered for peer $peerAddress. Initiating silent Wi-Fi Direct connection...")
            try {
                wifiDirectManager.connect(peerAddress)
                upgradeCounter.incrementAndGet()
                updateMetricsSnapshot()
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Auto-Upgrade failed for peer $peerAddress: ${e.message}")
            }
        }
    }

    @Deprecated("Use sendPacket instead", ReplaceWith("sendPacket(packet)"))
    override suspend fun send(packet: MeshPacket) {
        sendPacket(packet)
    }

    override suspend fun sendPacket(packet: MeshPacket): MeshResult<Unit> {
        val payloadSize = packet.payload.length.toLong()
        val selectedRoute = getSelectedRouteType(packet.targetId, packet.type, payloadSize)
        val startTime = System.currentTimeMillis()

        // Emergency SOS: Send over BOTH transports simultaneously
        if (packet.type == PacketType.SOS || selectedRoute == RouteType.HYBRID) {
            MeshLogger.d(TAG, "Emergency SOS packet ${packet.packetId}: Dispatching over BOTH BLE & Wi-Fi Direct")
            val bleJob = applicationScope.launch { bleTransport.sendPacket(packet) }
            val wifiJob = applicationScope.launch {
                if (wifiTransport.isConnected) wifiTransport.sendPacket(packet)
            }
            bleJob.join()
            wifiJob.join()
            recordPacketMetrics(RouteType.HYBRID, System.currentTimeMillis() - startTime, true, payloadSize)
            return MeshResult.Success(Unit)
        }

        return if (selectedRoute == RouteType.WIFI_DIRECT) {
            MeshLogger.d(TAG, "Routing packet ${packet.packetId} via Wi-Fi Direct")
            val wifiResult = wifiTransport.sendPacket(packet)

            if (wifiResult is MeshResult.Success) {
                wifiPacketsSent.incrementAndGet()
                totalBytesSent.addAndGet(payloadSize)
                recordPacketMetrics(RouteType.WIFI_DIRECT, System.currentTimeMillis() - startTime, true, payloadSize)
                wifiResult
            } else {
                // AUTOMATIC DOWNGRADE / FALLBACK: Wi-Fi send failed -> retry on BLE immediately
                retryCounter.incrementAndGet()
                fallbackCounter.incrementAndGet()
                downgradeCounter.incrementAndGet()
                recentFailuresWifi.incrementAndGet()

                MeshLogger.w(TAG, "Wi-Fi Direct send failed for packet ${packet.packetId}. Executing automatic downgrade fallback to BLE...")
                val bleFallbackResult = bleTransport.sendPacket(packet)

                if (bleFallbackResult is MeshResult.Success) {
                    blePacketsSent.incrementAndGet()
                    totalBytesSent.addAndGet(payloadSize)
                    recordPacketMetrics(RouteType.BLE, System.currentTimeMillis() - startTime, true, payloadSize)
                } else {
                    recordPacketMetrics(RouteType.BLE, System.currentTimeMillis() - startTime, false, payloadSize)
                }

                // Silently trigger background Wi-Fi reconnect attempt
                triggerAutoUpgrade(packet.targetId)
                bleFallbackResult
            }
        } else {
            MeshLogger.d(TAG, "Routing packet ${packet.packetId} via BLE")
            val bleResult = bleTransport.sendPacket(packet)

            if (bleResult is MeshResult.Success) {
                blePacketsSent.incrementAndGet()
                totalBytesSent.addAndGet(payloadSize)
                recordPacketMetrics(RouteType.BLE, System.currentTimeMillis() - startTime, true, payloadSize)
            } else {
                recordPacketMetrics(RouteType.BLE, System.currentTimeMillis() - startTime, false, payloadSize)
            }
            bleResult
        }
    }

    @Deprecated("Use broadcastPacket instead", ReplaceWith("broadcastPacket(packet, excludeAddress, includeAddress)"))
    override suspend fun broadcast(packet: MeshPacket, excludeAddress: String?, includeAddress: String?) {
        broadcastPacket(packet, excludeAddress, includeAddress)
    }

    override suspend fun broadcastPacket(
        packet: MeshPacket,
        excludeAddress: String?,
        includeAddress: String?
    ): MeshResult<Unit> {
        val payloadSize = packet.payload.length.toLong()
        val startTime = System.currentTimeMillis()

        // Emergency SOS: Broadcast on BOTH BLE and Wi-Fi Direct simultaneously for maximum reliability
        if (packet.type == PacketType.SOS) {
            MeshLogger.d(TAG, "Broadcasting Emergency SOS packet ${packet.packetId} on BOTH transports simultaneously!")
            applicationScope.launch { bleTransport.broadcastPacket(packet, excludeAddress, includeAddress) }
            if (wifiTransport.isConnected) {
                applicationScope.launch { wifiTransport.broadcastPacket(packet, excludeAddress, includeAddress) }
            }
            recordPacketMetrics(RouteType.HYBRID, System.currentTimeMillis() - startTime, true, payloadSize)
            return MeshResult.Success(Unit)
        }

        val selectedRoute = getSelectedRouteType(includeAddress ?: packet.targetId, packet.type, payloadSize)

        return if (selectedRoute == RouteType.WIFI_DIRECT && wifiTransport.isConnected) {
            MeshLogger.d(TAG, "Broadcasting packet ${packet.packetId} via Wi-Fi Direct")
            val wifiResult = wifiTransport.broadcastPacket(packet, excludeAddress, includeAddress)

            // If in HYBRID_ACTIVE mode, also broadcast over BLE to reach non-Wi-Fi mesh nodes
            if (_activeMode.value == HybridMode.HYBRID_ACTIVE) {
                bleTransport.broadcastPacket(packet, excludeAddress, includeAddress)
            }

            wifiPacketsSent.incrementAndGet()
            totalBytesSent.addAndGet(payloadSize)
            recordPacketMetrics(RouteType.WIFI_DIRECT, System.currentTimeMillis() - startTime, true, payloadSize)
            wifiResult
        } else {
            MeshLogger.d(TAG, "Broadcasting packet ${packet.packetId} via BLE")
            val bleResult = bleTransport.broadcastPacket(packet, excludeAddress, includeAddress)
            if (bleResult is MeshResult.Success) {
                blePacketsSent.incrementAndGet()
                totalBytesSent.addAndGet(payloadSize)
            }
            recordPacketMetrics(RouteType.BLE, System.currentTimeMillis() - startTime, bleResult is MeshResult.Success, payloadSize)
            bleResult
        }
    }

    @Deprecated("Use connectToPeer instead", ReplaceWith("connectToPeer(peerId)"))
    override suspend fun connect(peerId: String) {
        connectToPeer(peerId)
    }

    override suspend fun connectToPeer(peerId: String): MeshResult<Unit> {
        // BLE remains primary connection channel for mesh handshake & continuous discovery
        val bleResult = bleTransport.connectToPeer(peerId)

        // Parallel Wi-Fi Direct upgrade check
        if (wifiDirectManager.isP2pEnabled.value && !wifiTransport.isConnected) {
            triggerAutoUpgrade(peerId)
        }

        return bleResult
    }

    override fun recordPacketMetrics(
        routeType: RouteType,
        latencyMs: Long,
        success: Boolean,
        packetSize: Long
    ) {
        if (!success) {
            // Track per-transport failure for hysteresis score penalty.
            // Wi-Fi failures accumulate a penalty that biases the score toward BLE fallback.
            // Cap at 20 to prevent unbounded penalty on prolonged outages.
            if (routeType == RouteType.WIFI_DIRECT) {
                val current = recentFailuresWifi.get()
                if (current < 20) recentFailuresWifi.incrementAndGet()
                MeshLogger.w(TAG, "Wi-Fi Direct packet delivery failure (consecutive failures: ${recentFailuresWifi.get()}, latency=${latencyMs}ms)")
            } else {
                MeshLogger.w(TAG, "BLE packet delivery failure (latency=${latencyMs}ms)")
            }
        } else {
            // Decrement recent Wi-Fi failure penalty on success (gradual recovery)
            if (routeType == RouteType.WIFI_DIRECT && recentFailuresWifi.get() > 0) {
                recentFailuresWifi.decrementAndGet()
            }
        }
        updateMetricsSnapshot()
    }

    private fun recordIncomingPacket(packet: MeshPacket, isWifi: Boolean) {
        packetTypeCountMap.computeIfAbsent(packet.type.name) { AtomicLong(0) }.incrementAndGet()
        totalBytesSent.addAndGet(packet.payload.length.toLong())
        updateMetricsSnapshot()
    }

    private fun updateMetricsSnapshot() {
        val totalSent = blePacketsSent.get() + wifiPacketsSent.get()
        val countsMap = packetTypeCountMap.mapValues { it.value.get() }

        _metrics.update { current ->
            current.copy(
                activeMode = _activeMode.value,
                throughputBps = totalBytesSent.get(),
                retryCount = retryCounter.get(),
                fallbackCount = fallbackCounter.get(),
                upgradeCount = upgradeCounter.get(),
                downgradeCount = downgradeCounter.get(),
                totalPacketsSentBle = blePacketsSent.get(),
                totalPacketsSentWifi = wifiPacketsSent.get(),
                packetTypeCounts = countsMap
            )
        }
    }
}
