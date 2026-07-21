package com.meshlink.wifi.data

import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.meshlink.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.InetAddress

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WifiDirectManagerTest {

    private lateinit var context: Context
    private lateinit var mockWifiP2pManager: WifiP2pManager
    private lateinit var mockChannel: WifiP2pManager.Channel
    private lateinit var mockAnalytics: FirebaseAnalytics
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var manager: WifiDirectManager
    private lateinit var receiver: android.content.BroadcastReceiver

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockWifiP2pManager = mockk(relaxed = true)
        mockChannel = mockk(relaxed = true)
        mockAnalytics = mockk(relaxed = true)
        mockSettingsRepository = mockk(relaxed = true)
        io.mockk.every { mockSettingsRepository.isWifiDirectEnabled } returns kotlinx.coroutines.flow.flowOf(true)
        io.mockk.every { mockSettingsRepository.wifiAutoConnect } returns kotlinx.coroutines.flow.flowOf(true)
        io.mockk.every { mockSettingsRepository.wifiPreferredGroupOwner } returns kotlinx.coroutines.flow.flowOf(false)
        
        every { mockWifiP2pManager.initialize(any(), any(), any()) } returns mockChannel

        // Inject mock WifiP2pManager into the context using shadow
        val shadowApplication = shadowOf(context as android.app.Application)
        shadowApplication.setSystemService(Context.WIFI_P2P_SERVICE, mockWifiP2pManager)

        manager = WifiDirectManager(context, mockAnalytics, mockSettingsRepository)
        
        val receiverField = WifiDirectManager::class.java.getDeclaredField("receiver")
        receiverField.isAccessible = true
        receiver = receiverField.get(manager) as android.content.BroadcastReceiver
    }

    @Test
    fun `when peers changed intent received, requests peers and updates flow`() = runTest {
        val listenerSlot = slot<WifiP2pManager.PeerListListener>()
        val deviceList = mockk<WifiP2pDeviceList>()
        val device1 = WifiP2pDevice().apply { 
            deviceAddress = "00:11:22:33:44:55"
            deviceName = "Test Device"
        }
        every { deviceList.deviceList } returns listOf(device1)

        val intent = Intent(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        receiver.onReceive(context, intent)

        verify { mockWifiP2pManager.requestPeers(any(), capture(listenerSlot)) }
        listenerSlot.captured.onPeersAvailable(deviceList)

        val peers = manager.discoveredPeers.value
        assertEquals(1, peers.size)
        assertEquals("00:11:22:33:44:55", peers["00:11:22:33:44:55"]?.deviceAddress)
    }

    @Test
    fun `when connection changed intent received with connected true, updates connection info`() = runTest {
        val infoListenerSlot = slot<WifiP2pManager.ConnectionInfoListener>()
        val groupListenerSlot = slot<WifiP2pManager.GroupInfoListener>()
        
        val mockNetworkInfo = mockk<NetworkInfo>()
        every { mockNetworkInfo.isConnected } returns true
        
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
        every { intent.getParcelableExtra<NetworkInfo>(any()) } returns mockNetworkInfo
        every { intent.getParcelableExtra(any(), eq(NetworkInfo::class.java)) } returns mockNetworkInfo
        
        receiver.onReceive(context, intent)

        verify { mockWifiP2pManager.requestConnectionInfo(any(), capture(infoListenerSlot)) }
        
        val mockInfo = WifiP2pInfo().apply {
            groupFormed = true
            isGroupOwner = true
            groupOwnerAddress = mockk()
        }
        infoListenerSlot.captured.onConnectionInfoAvailable(mockInfo)

        assertEquals(true, manager.connectionInfo.value?.groupFormed)
        assertEquals(true, manager.connectionInfo.value?.isGroupOwner)

        verify { mockWifiP2pManager.requestGroupInfo(any(), capture(groupListenerSlot)) }
        
        val mockGroup = mockk<WifiP2pGroup>()
        val clientDevice = WifiP2pDevice().apply { deviceAddress = "AA:BB:CC:DD:EE:FF" }
        every { mockGroup.isGroupOwner } returns true
        every { mockGroup.clientList } returns listOf(clientDevice)
        
        groupListenerSlot.captured.onGroupInfoAvailable(mockGroup)

        assertEquals("AA:BB:CC:DD:EE:FF", manager.connectedPeerMac.value)
    }

    @Test
    fun `when connection changed intent received with connected false, clears connection info`() = runTest {
        val mockNetworkInfo = mockk<NetworkInfo>()
        every { mockNetworkInfo.isConnected } returns false
        
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
        every { intent.getParcelableExtra<NetworkInfo>(any()) } returns mockNetworkInfo
        every { intent.getParcelableExtra(any(), eq(NetworkInfo::class.java)) } returns mockNetworkInfo
        receiver.onReceive(context, intent)

        assertNull(manager.connectionInfo.value)
        assertNull(manager.connectedPeerMac.value)
    }

    @Test
    fun `connectToPeer calculates GO intent based on MAC addresses`() = runTest {
        // Set local MAC
        val localDevice = WifiP2pDevice().apply { deviceAddress = "22:22:22:22:22:22" }
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
        every { intent.getParcelableExtra<WifiP2pDevice>(any()) } returns localDevice
        every { intent.getParcelableExtra(any(), eq(WifiP2pDevice::class.java)) } returns localDevice
        receiver.onReceive(context, intent)

        val configList = mutableListOf<WifiP2pConfig>()
        
        // Target MAC is smaller -> local should have GO intent 15
        manager.connectToPeer("11:11:11:11:11:11")
        verify(timeout = 1000) { mockWifiP2pManager.connect(any(), capture(configList), any()) }
        assertEquals(15, configList.last().groupOwnerIntent)

        // Target MAC is larger -> local should have GO intent 0
        manager.connectToPeer("33:33:33:33:33:33")
        verify(timeout = 1000, exactly = 2) { mockWifiP2pManager.connect(any(), capture(configList), any()) }
        assertEquals(0, configList.last().groupOwnerIntent)
    }
}
