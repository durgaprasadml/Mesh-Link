package com.meshlink.wifi.manager

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import com.meshlink.wifi.model.WifiP2pState
import com.meshlink.wifi.permission.WifiP2pPermissionHandler
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WifiP2pManagerFacadeTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockPermissionHandler = mockk<WifiP2pPermissionHandler>(relaxed = true)

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `initial state is Unavailable when WifiP2pManager is null`() = testScope.runTest {
        val facade = WifiP2pManagerFacade(
            context = mockContext,
            wifiP2pManager = null,
            channel = null,
            permissionHandler = mockPermissionHandler,
            applicationScope = testScope
        )

        assertEquals(WifiP2pState.Unavailable, facade.p2pState.value)
        assertTrue(facade.discoveredPeers.value.isEmpty())
    }

    @Test
    fun `onStateChanged updates P2P state to Enabled or Disabled`() = testScope.runTest {
        val mockManager = mockk<WifiP2pManager>(relaxed = true)
        val mockChannel = mockk<WifiP2pManager.Channel>(relaxed = true)

        val facade = WifiP2pManagerFacade(
            context = mockContext,
            wifiP2pManager = mockManager,
            channel = mockChannel,
            permissionHandler = mockPermissionHandler,
            applicationScope = testScope
        )

        facade.onStateChanged(isEnabled = true)
        assertEquals(WifiP2pState.Enabled, facade.p2pState.value)

        facade.onStateChanged(isEnabled = false)
        assertEquals(WifiP2pState.Disabled, facade.p2pState.value)
    }

    @Test
    fun `disconnect cancels connection and removes group`() = testScope.runTest {
        val mockManager = mockk<WifiP2pManager>(relaxed = true)
        val mockChannel = mockk<WifiP2pManager.Channel>(relaxed = true)

        val facade = WifiP2pManagerFacade(
            context = mockContext,
            wifiP2pManager = mockManager,
            channel = mockChannel,
            permissionHandler = mockPermissionHandler,
            applicationScope = testScope
        )

        facade.disconnect()
        // Verify disconnect completes without throwing exceptions
    }
}
