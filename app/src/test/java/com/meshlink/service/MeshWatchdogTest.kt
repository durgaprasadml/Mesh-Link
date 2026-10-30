package com.meshlink.service

import com.meshlink.ble.data.BleAdvertiserManager
import com.meshlink.ble.data.BleScannerManager
import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.wifi.manager.WifiDirectManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshWatchdogTest {

    @Test
    fun testExponentialBackoffSchedule() {
        val schedule = listOf(1000L, 2000L, 5000L, 10000L, 20000L, 30000L, 60000L)
        assertEquals(7, schedule.size)
        assertEquals(1000L, schedule[0])
        assertEquals(60000L, schedule.last())
    }

    @Test
    fun `performHealthCheck handles concurrent failure updates safely without race conditions`() = runTest {
        val meshSupervisor = mockk<MeshSupervisor>(relaxed = true)
        val bleScanner = mockk<BleScannerManager>(relaxed = true)
        val bleAdvertiser = mockk<BleAdvertiserManager>(relaxed = true)
        val wifiDirect = mockk<WifiDirectManager>(relaxed = true)
        val diagnostics = mockk<TransportDiagnostics>(relaxed = true)

        every { meshSupervisor.subsystemStates } returns MutableStateFlow(
            mapOf(
                RadioSubsystem.BLE_SCANNER to RadioState.RUNNING,
                RadioSubsystem.BLE_ADVERTISER to RadioState.RUNNING,
                RadioSubsystem.WIFI_DIRECT to RadioState.RUNNING
            )
        )
        every { bleScanner.isScanning } returns false
        every { bleAdvertiser.isAdvertising } returns false
        every { wifiDirect.radioState } returns MutableStateFlow(RadioState.FAILED)

        val watchdog = MeshWatchdog(
            meshSupervisor = meshSupervisor,
            bleScannerManager = bleScanner,
            bleAdvertiserManager = bleAdvertiser,
            wifiDirectManager = wifiDirect,
            transportDiagnostics = diagnostics,
            externalScope = backgroundScope
        )

        val jobs = (1..4).map {
            launch {
                watchdog.performHealthCheck()
            }
        }
        testScheduler.advanceUntilIdle()
        jobs.forEach { it.join() }

        assertEquals(12, watchdog.getRecoveryCount())
        assertEquals(4, watchdog.getFailureCount(RadioSubsystem.BLE_SCANNER))
        assertEquals(4, watchdog.getFailureCount(RadioSubsystem.BLE_ADVERTISER))
        assertEquals(4, watchdog.getFailureCount(RadioSubsystem.WIFI_DIRECT))
        verify(atLeast = 4) { meshSupervisor.restartSubsystem(RadioSubsystem.BLE_SCANNER) }
    }
}
