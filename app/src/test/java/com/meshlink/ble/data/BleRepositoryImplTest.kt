package com.meshlink.ble.data

import android.app.Application
import android.content.Context
import com.meshlink.ble.data.source.BleMeshDataSource
import com.meshlink.data.location.LocationProvider
import com.meshlink.database.data.local.ChatDao
import com.meshlink.domain.repository.UserRepository
import com.meshlink.media.data.MediaTransferManager
import com.meshlink.routing.data.MeshRouter
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.MeshSecurityMonitor
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import com.meshlink.util.MainDispatcherRule
import com.meshlink.wifi.data.WifiDirectManager
import com.meshlink.wifi.data.WifiSocketTransport
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.every
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

class BleRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var application: Application
    private lateinit var bleDataSource: BleMeshDataSource
    private lateinit var meshRouter: MeshRouter
    private lateinit var chatDao: ChatDao
    private lateinit var userRepository: UserRepository
    private lateinit var mediaTransferManager: MediaTransferManager
    private lateinit var locationProvider: LocationProvider
    private lateinit var cryptoManager: MeshCryptoManager
    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var wifiSocketTransport: WifiSocketTransport
    private lateinit var sessionManager: SessionManager
    private lateinit var rekeyManager: RekeyManager
    private lateinit var trustManager: TrustManager
    private lateinit var securityMonitor: MeshSecurityMonitor
    private lateinit var context: Context

    private lateinit var repository: BleRepositoryImpl

    @Before
    fun setup() {
        application = mockk(relaxed = true)
        bleDataSource = mockk(relaxed = true)
        meshRouter = mockk(relaxed = true)
        chatDao = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        mediaTransferManager = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)
        cryptoManager = mockk(relaxed = true)
        wifiDirectManager = mockk(relaxed = true)
        wifiSocketTransport = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        rekeyManager = mockk(relaxed = true)
        trustManager = mockk(relaxed = true)
        securityMonitor = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { bleDataSource.scannedDevices } returns MutableStateFlow(emptyMap())
        every { bleDataSource.connectedServers } returns emptySet()
        every { bleDataSource.activeClients } returns emptySet()

        every { meshRouter.incomingPayloads } returns MutableSharedFlow()
        every { meshRouter.routeTable } returns ConcurrentHashMap()

        every { userRepository.isEncryptionEnabled } returns MutableStateFlow(false)

        every { mediaTransferManager.transferProgress } returns MutableStateFlow(emptyMap())

        every { wifiDirectManager.localDeviceMac } returns MutableStateFlow(null)
        every { wifiDirectManager.connectionInfo } returns MutableStateFlow(null)

        repository = BleRepositoryImpl(
            application, bleDataSource, meshRouter, chatDao, userRepository,
            mockk(relaxed = true), mediaTransferManager, locationProvider, cryptoManager, wifiDirectManager,
            wifiSocketTransport, sessionManager, rekeyManager, trustManager, securityMonitor,
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), context
        )
    }

    @After
    fun teardown() {
        repository.cancelScope()
    }

    @Test
    fun `startAdvertising delegates to bleDataSource`() {
        repository.startAdvertising("TestName", "mesh_1")
        coVerify(exactly = 1) { bleDataSource.startAdvertising("TestName", "mesh_1", 0x01) }
    }

    @Test
    fun `stopAdvertising delegates to bleDataSource`() {
        repository.stopAdvertising()
        coVerify(exactly = 1) { bleDataSource.stopAdvertising() }
    }

    @Test
    fun `startScanning delegates to bleDataSource`() {
        repository.startScanning()
        coVerify(exactly = 1) { bleDataSource.startScanning() }
    }

    @Test
    fun `stopScanning delegates to bleDataSource`() {
        repository.stopScanning()
        coVerify(exactly = 1) { bleDataSource.stopScanning() }
    }

    @Test
    fun `startServer delegates to bleDataSource`() {
        repository.startServer()
        coVerify(exactly = 1) { bleDataSource.startServer() }
    }

    @Test
    fun `stopServer delegates to bleDataSource`() {
        repository.stopServer()
        coVerify(exactly = 1) { bleDataSource.stopServer() }
    }

    @Test
    fun `connectToDevice delegates to bleDataSource`() {
        repository.connectToDevice("00:11:22:33:44:55")
        coVerify(exactly = 1) { bleDataSource.connectToDevice("00:11:22:33:44:55") }
    }
    
    @Test
    fun `stopMesh calls all stop methods`() {
        repository.stopMesh()
        coVerify(exactly = 1) { bleDataSource.stopAdvertising() }
        coVerify(exactly = 1) { bleDataSource.stopScanning() }
        coVerify(exactly = 1) { bleDataSource.stopServer() }
    }
}
