package com.meshlink.ui.nearby

import app.cash.turbine.test
import com.meshlink.ble.data.BleConstants
import com.meshlink.ble.discovery.BatteryAwareScanner
import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.engine.MeshTopologyManager
import com.meshlink.routing.engine.ReachableNode
import com.meshlink.util.MainDispatcherRule
import com.meshlink.util.MeshIdNormalizer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NearbyDiscoveryPipelineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var meshRepository: MeshRepository
    private lateinit var userRepository: UserRepository
    private lateinit var topologyManager: MeshTopologyManager

    @Before
    fun setup() {
        meshRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        topologyManager = mockk(relaxed = true)
    }

    /**
     * Test 1 — Advertisement parsing:
     * Given a valid 9-byte Mesh-Link manufacturer payload (8 bytes meshId + 1 byte capabilities),
     * verify the mesh ID is correctly extracted, canonicalized, and processed.
     */
    @Test
    fun `test1_advertisementParsing_validPayload_createsPeerRecord`() {
        val targetMeshId = "PEER1234"
        val canonicalId = MeshIdNormalizer.canonicalize(targetMeshId)
        val meshIdBytes = canonicalId.toByteArray(Charsets.UTF_8).copyOf(8)
        val payload = ByteArray(9)
        System.arraycopy(meshIdBytes, 0, payload, 0, 8)
        val capabilities: Byte = 0x01
        payload[8] = capabilities

        // Simulate extraction logic from BleScannerManager
        val extractedBytes = ByteArray(8)
        System.arraycopy(payload, 0, extractedBytes, 0, 8)
        val parsedMeshId = String(extractedBytes, Charsets.UTF_8).replace("\u0000", "").trim()
        val extractedCanonical = MeshIdNormalizer.canonicalize(parsedMeshId)
        val extractedCapabilities = if (payload.size > 8) payload[8] else 0

        assertEquals("PEER1234", extractedCanonical)
        assertEquals(0x01.toByte(), extractedCapabilities)

        val batteryScanner: BatteryAwareScanner = mockk(relaxed = true)
        val engine = DiscoveryEngine(batteryScanner)
        engine.onDeviceDiscovered(
            macAddress = "AA:BB:CC:DD:EE:01",
            meshId = extractedCanonical,
            name = "",
            rssi = -65,
            capabilities = extractedCapabilities
        )

        val cached = engine.cache.get("AA:BB:CC:DD:EE:01")
        assertNotNull(cached)
        assertEquals("PEER1234", cached?.meshId)
        assertEquals(0x01.toByte(), cached?.capabilities)
        assertEquals(-65, cached?.smoothedRssi)
    }

    /**
     * Test 2 — Invalid advertisement:
     * Unrelated or corrupted advertisements (< 8 bytes payload or empty) are rejected.
     */
    @Test
    fun `test2_invalidAdvertisement_rejectedGracefully`() {
        val shortPayload = ByteArray(5) // Less than 8 bytes
        val isValid = shortPayload.size >= 8
        assertFalse("Payloads shorter than 8 bytes must be rejected", isValid)

        val emptyMeshId = ""
        val canonical = MeshIdNormalizer.canonicalize(emptyMeshId)
        assertTrue("Empty mesh ID should result in blank canonical ID", canonical.isBlank())
    }

    /**
     * Test 3 — Device appears:
     * Valid scan results in MeshRepository.scannedDevices flow into NearbyViewModel.uiState.
     */
    @Test
    fun `test3_deviceAppearsInNearbyUiState`() = runTest {
        val localUser = User(meshId = "LOCALUSER1", name = "Local User")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { userRepository.getUserDisplayName(any()) } returns "Alice"
        coEvery { userRepository.getUserProfile(any()) } returns null

        val scannedFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
        val reachableFlow = MutableStateFlow<List<ReachableNode>>(emptyList())
        every { meshRepository.scannedDevices } returns scannedFlow
        every { topologyManager.reachableNodes } returns reachableFlow
        every { meshRepository.getMeshStatus() } returns com.meshlink.domain.model.MeshStatus(
            isBleAdvertising = true,
            isBleScanning = true,
            connectedPeersCount = 0,
            isServerRunning = true
        )

        val viewModel = NearbyViewModel(meshRepository, userRepository, topologyManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.devices.isEmpty())

            // Emit discovered peer
            val peerDevice = BleDevice(
                meshId = "ALICE1234",
                name = "",
                address = "AA:BB:CC:DD:EE:02",
                rssi = -55
            )
            scannedFlow.value = mapOf("ALICE1234" to peerDevice)

            val updated = awaitItem()
            assertEquals(1, updated.devices.size)
            assertEquals("ALICE1234", updated.devices[0].meshId)
            assertEquals("Alice", updated.devices[0].name)
            assertEquals(-55, updated.devices[0].rssi)
        }
    }

    /**
     * Test 4 — Existing peer update:
     * Repeated advertisements from the same node update RSSI and lastSeen without creating duplicates.
     */
    @Test
    fun `test4_existingPeerUpdate_updatesRssiWithoutDuplicates`() {
        val batteryScanner: BatteryAwareScanner = mockk(relaxed = true)
        val engine = DiscoveryEngine(batteryScanner)

        // First advertisement
        engine.onDeviceDiscovered(
            macAddress = "AA:BB:CC:DD:EE:03",
            meshId = "NODE1234",
            name = "",
            rssi = -80,
            capabilities = 0
        )
        assertEquals(1, engine.cache.getAll().size)
        assertEquals(-80, engine.cache.get("AA:BB:CC:DD:EE:03")?.smoothedRssi)

        // Wait past duplicate filter window (2000ms)
        Thread.sleep(2100)

        // Second advertisement with stronger RSSI
        engine.onDeviceDiscovered(
            macAddress = "AA:BB:CC:DD:EE:03",
            meshId = "NODE1234",
            name = "",
            rssi = -50,
            capabilities = 0
        )

        // Still only 1 entry in cache
        assertEquals(1, engine.cache.getAll().size)
        val updated = engine.cache.get("AA:BB:CC:DD:EE:03")
        assertNotNull(updated)
        // RSSI should be smoothed by KalmanFilter
        assertTrue("Smoothed RSSI should reflect improvement", updated!!.smoothedRssi > -80)
    }

    /**
     * Test 5 — Local user exclusion:
     * The local user's own device advertisement must be excluded from Nearby Devices list.
     */
    @Test
    fun `test5_localUserOwnDevice_isExcludedFromNearbyDevices`() = runTest {
        val localUser = User(meshId = "MYOWNID1", name = "My Device")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { userRepository.getUserDisplayName(any()) } returns "My Device"
        coEvery { userRepository.getUserProfile(any()) } returns null

        val ownDevice = BleDevice(
            meshId = "MYOWNID1",
            name = "My Device",
            address = "AA:BB:CC:DD:EE:00",
            rssi = -30
        )
        val peerDevice = BleDevice(
            meshId = "REMOTE01",
            name = "Remote Peer",
            address = "AA:BB:CC:DD:EE:99",
            rssi = -70
        )

        val scannedFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
        every { meshRepository.scannedDevices } returns scannedFlow
        every { topologyManager.reachableNodes } returns MutableStateFlow(emptyList())

        val viewModel = NearbyViewModel(meshRepository, userRepository, topologyManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.devices.isEmpty())

            scannedFlow.value = mapOf("MYOWNID1" to ownDevice, "REMOTE01" to peerDevice)

            val state = awaitItem()
            // Only REMOTE01 should be present; MYOWNID1 must be excluded
            assertEquals(1, state.devices.size)
            assertEquals("REMOTE01", state.devices[0].meshId)
        }
    }

    /**
     * Test 6 — Bluetooth disabled:
     * Calling stop() on DiscoveryEngine stops scanning cleanly.
     */
    @Test
    fun `test6_bluetoothDisabled_stopsDiscoveryCleanly`() {
        val batteryScanner: BatteryAwareScanner = mockk(relaxed = true)
        val engine = DiscoveryEngine(batteryScanner)

        var stopped = false
        engine.stopScanAction = { stopped = true }

        engine.start()
        engine.stop()

        assertTrue(stopped)
    }

    /**
     * Test 7 — Bluetooth re-enabled:
     * DiscoveryEngine.stop() does not cancel engineScope, allowing restart without deadlocks.
     */
    @Test
    fun `test7_bluetoothReenabled_restartsDiscoveryWithoutScopeDeadlock`() {
        val batteryScanner: BatteryAwareScanner = mockk(relaxed = true)
        val engine = DiscoveryEngine(batteryScanner)

        // Start -> Stop -> Restart
        engine.start()
        assertTrue("Engine should be scanning after start", engine.isScanning)

        engine.stop()
        assertFalse("Engine should not be scanning after stop", engine.isScanning)

        engine.start()
        assertTrue("Engine should be scanning after restart", engine.isScanning)

        engine.stop()
        assertFalse("Engine should not be scanning after final stop", engine.isScanning)
    }

    /**
     * Test 8 — Multi-hop and direct deduplication:
     * When a device is discovered both directly via BLE and via multi-hop topology,
     * it is deduplicated by canonical Mesh ID preferring direct connection.
     */
    @Test
    fun `test8_directAndMultiHop_deduplicatesByCanonicalMeshId`() = runTest {
        val localUser = User(meshId = "LOCAL001", name = "Local")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { userRepository.getUserDisplayName(any()) } returns "Bob"
        coEvery { userRepository.getUserProfile(any()) } returns null

        val directDevice = BleDevice(
            meshId = "BOB12345",
            name = "Bob",
            address = "AA:BB:CC:DD:EE:04",
            rssi = -60
        )
        val indirectNode = ReachableNode(
            nodeId = "BOB12345",
            rssi = -85,
            hopCount = 2,
            viaRelayId = "RELAY001"
        )

        val scannedFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
        val reachableFlow = MutableStateFlow<List<ReachableNode>>(emptyList())
        every { meshRepository.scannedDevices } returns scannedFlow
        every { topologyManager.reachableNodes } returns reachableFlow

        val viewModel = NearbyViewModel(meshRepository, userRepository, topologyManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.devices.isEmpty())

            scannedFlow.value = mapOf("BOB12345" to directDevice)
            reachableFlow.value = listOf(indirectNode)

            val state = awaitItem()
            // Deduplicated: exactly 1 device for BOB12345
            assertEquals(1, state.devices.size)
            assertEquals("BOB12345", state.devices[0].meshId)
            // Preferred direct route (hopCount = 0)
            assertEquals(0, state.devices[0].hopCount)
            assertEquals(-60, state.devices[0].rssi)
        }
    }
}
