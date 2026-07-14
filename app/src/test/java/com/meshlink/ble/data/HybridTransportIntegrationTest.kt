package com.meshlink.ble.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meshlink.ble.data.source.BleMeshDataSource
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.repository.UserRepository
import com.meshlink.media.data.MediaTransferManager
import com.meshlink.routing.data.MeshRouter
import com.meshlink.wifi.data.WifiDirectManager
import com.meshlink.wifi.data.WifiSocketTransport
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import io.mockk.spyk

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HybridTransportIntegrationTest {

    private lateinit var repository: BleRepositoryImpl
    private lateinit var meshRouter: MeshRouter
    private lateinit var chatDao: ChatDao
    private lateinit var userRepository: UserRepository
    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var wifiSocketTransport: WifiSocketTransport
    private lateinit var mediaTransferManager: MediaTransferManager
    private lateinit var bleDataSource: BleMeshDataSource

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context as Application

        bleDataSource = mockk(relaxed = true)
        meshRouter = mockk(relaxed = true)
        chatDao = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        mediaTransferManager = mockk(relaxed = true)
        wifiDirectManager = mockk(relaxed = true)
        wifiSocketTransport = spyk(WifiSocketTransport())

        val cryptoManager = mockk<com.meshlink.security.data.MeshCryptoManager>(relaxed = true)
        val sessionManager = mockk<com.meshlink.security.data.SessionManager>(relaxed = true)
        val rekeyManager = mockk<com.meshlink.security.data.RekeyManager>(relaxed = true)
        val trustManager = mockk<com.meshlink.security.data.TrustManager>(relaxed = true)
        val securityMonitor = mockk<com.meshlink.security.data.MeshSecurityMonitor>(relaxed = true)
        val locationProvider = mockk<com.meshlink.data.location.LocationProvider>(relaxed = true)

        val user = UserEntity("localId", "LocalUser", "1234567890", "hash")
        coEvery { userRepository.getLocalUser() } returns user
        every { userRepository.isEncryptionEnabled } returns flowOf(false) // simplify testing
        
        every { bleDataSource.scannedDevices } returns MutableStateFlow(emptyMap())
        every { meshRouter.incomingPayloads } returns MutableSharedFlow()
        every { mediaTransferManager.transferProgress } returns MutableStateFlow(emptyMap())
        
        every { wifiDirectManager.localDeviceMac } returns MutableStateFlow(null)
        every { wifiDirectManager.connectionInfo } returns MutableStateFlow(null)
        every { wifiDirectManager.connectedPeerMac } returns MutableStateFlow(null)
        every { wifiDirectManager.discoveredPeers } returns MutableStateFlow(emptyMap())

        repository = BleRepositoryImpl(
            app, bleDataSource, meshRouter, chatDao, userRepository,
            mediaTransferManager, locationProvider, cryptoManager,
            wifiDirectManager, wifiSocketTransport, sessionManager,
            rekeyManager, trustManager, securityMonitor, context
        )
    }

    @Test
    fun `when wifi socket receives packet, it is handled as incoming payload`() = runTest {
        val packet = MeshPacket(
            packetId = "test-packet",
            senderId = "remote-peer",
            targetId = "localId", // Target is local
            payload = "hello",
            type = PacketType.TEXT,
            encrypted = false
        )

        // Simulate incoming Wi-Fi direct socket data by directly invoking the property
        wifiSocketTransport.onPacketReceived?.invoke(packet)

        // Verify it gets processed by the repository and eventually saved to ChatDao
        io.mockk.coVerify(timeout = 1000) { 
            chatDao.insertMessageAndUpdateChat(
                match { it.messageId == "test-packet" && it.text == "hello" }, 
                any()
            )
        }
    }
}
