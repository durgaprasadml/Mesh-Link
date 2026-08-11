package com.meshlink.wifi.manager

import android.content.Context
import android.net.wifi.WifiManager
import com.meshlink.service.RadioState
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WifiDirectManagerTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockWifiManager = mockk<WifiManager>(relaxed = true)
    private val mockFacade = mockk<WifiP2pManagerFacade>(relaxed = true)

    private lateinit var manager: WifiDirectManager

    @Before
    fun setup() {
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSystemService(Context.WIFI_SERVICE) } returns mockWifiManager
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `startWifiDirect when Wi-Fi is enabled starts peer discovery and sets state to RUNNING`() {
        every { mockWifiManager.isWifiEnabled } returns true

        manager = WifiDirectManager(mockContext, mockFacade)
        manager.startWifiDirect()

        assertEquals(RadioState.RUNNING, manager.radioState.value)
        verify(exactly = 1) { mockFacade.discoverPeers() }
    }

    @Test
    fun `startWifiDirect when Wi-Fi is disabled remains STOPPED without discovering peers`() {
        every { mockWifiManager.isWifiEnabled } returns false

        manager = WifiDirectManager(mockContext, mockFacade)
        manager.startWifiDirect()

        assertEquals(RadioState.STOPPED, manager.radioState.value)
        verify(exactly = 0) { mockFacade.discoverPeers() }
    }

    @Test
    fun `stopWifiDirect stops peer discovery and transitions state to STOPPED`() {
        every { mockWifiManager.isWifiEnabled } returns true

        manager = WifiDirectManager(mockContext, mockFacade)
        manager.startWifiDirect()
        manager.stopWifiDirect()

        assertEquals(RadioState.STOPPED, manager.radioState.value)
        verify(exactly = 1) { mockFacade.stopPeerDiscovery() }
    }

    @Test
    fun `duplicate startWifiDirect calls when already RUNNING are idempotent`() {
        every { mockWifiManager.isWifiEnabled } returns true

        manager = WifiDirectManager(mockContext, mockFacade)
        manager.startWifiDirect()
        manager.startWifiDirect()

        assertEquals(RadioState.RUNNING, manager.radioState.value)
        verify(exactly = 1) { mockFacade.discoverPeers() }
    }

    @Test
    fun `restartWifiDirect when Wi-Fi is disabled stops and remains STOPPED`() {
        every { mockWifiManager.isWifiEnabled } returns false

        manager = WifiDirectManager(mockContext, mockFacade)
        manager.restartWifiDirect()

        assertEquals(RadioState.STOPPED, manager.radioState.value)
    }
}
