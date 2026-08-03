package com.meshlink.routing.engine

import com.meshlink.domain.transport.TransportHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportHealthMonitorTest {

    private val monitor = TransportHealthMonitor()

    @Test
    fun `updateBleState updates BLE metrics and diagnostics`() {
        monitor.updateBleState(TransportHealth.CONNECTED, activePeers = 3, rssi = -65, mtu = 512)

        val status = monitor.healthStatus.value
        val diagnostics = monitor.diagnostics.value

        assertTrue(status.bleMetrics.isConnected)
        assertEquals(-65, status.bleMetrics.rssi)
        assertEquals(512, status.bleMetrics.mtu)
        assertEquals(3, diagnostics.activeBlePeers)
        assertEquals(TransportHealth.CONNECTED, diagnostics.bleStatus)
    }

    @Test
    fun `updateWifiState updates Wi-Fi Direct metrics and diagnostics`() {
        monitor.updateWifiState(TransportHealth.CONNECTED, activePeers = 1, socketState = "ESTABLISHED", estimatedThroughputBps = 10_000_000.0)

        val status = monitor.healthStatus.value
        val diagnostics = monitor.diagnostics.value

        assertTrue(status.wifiMetrics.isConnected)
        assertEquals("ESTABLISHED", status.wifiMetrics.socketState)
        assertEquals(10_000_000.0, status.wifiMetrics.estimatedThroughputBps, 0.01)
        assertEquals(1, diagnostics.activeWifiPeers)
        assertEquals(TransportHealth.CONNECTED, diagnostics.wifiStatus)
    }
}
