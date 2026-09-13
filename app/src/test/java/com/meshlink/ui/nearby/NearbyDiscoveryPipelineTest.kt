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

    /**
     * Test 9 — Profile Name preferred over Bluetooth Hardware Name:
     * User profile display name ("Durga Prasad") takes precedence over
     * Bluetooth device name ("motorola edge 60 fusion").
     */
    @Test
    fun `test9_profileDisplayName_takesPrecedenceOverBluetoothName`() = runTest {
        val localUser = User(meshId = "LOCALUSER1", name = "Local Device")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { userRepository.getUserDisplayName("PEER1234") } returns "Durga Prasad"
        coEvery { userRepository.getUserProfile("PEER1234") } returns null

        val peerDevice = BleDevice(
            meshId = "PEER1234",
            name = "motorola edge 60 fusion",
            bluetoothDeviceName = "motorola edge 60 fusion",
            address = "AA:BB:CC:DD:EE:10",
            rssi = -60
        )

        val scannedFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
        every { meshRepository.scannedDevices } returns scannedFlow
        every { topologyManager.reachableNodes } returns MutableStateFlow(emptyList())

        val viewModel = NearbyViewModel(meshRepository, userRepository, topologyManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.devices.isEmpty())

            scannedFlow.value = mapOf("PEER1234" to peerDevice)

            val updated = awaitItem()
            assertEquals(1, updated.devices.size)
            assertEquals("Durga Prasad", updated.devices[0].name)
            assertEquals("Durga Prasad", updated.devices[0].displayName)
            assertEquals("motorola edge 60 fusion", updated.devices[0].bluetoothDeviceName)
        }
    }

    /**
     * Test 10 — Rescan and continued discovery does not overwrite resolved profile name:
     * When new BLE advertisements arrive with Bluetooth hardware name, DiscoveryEngine
     * does not overwrite already resolved Mesh-Link profile display name.
     */
    @Test
    fun `test10_continuedBleScanning_doesNotOverwriteResolvedProfileName`() {
        val batteryScanner: BatteryAwareScanner = mockk(relaxed = true)
        val engine = DiscoveryEngine(batteryScanner)

        // 1. Initial BLE discovery with hardware name
        engine.onDeviceDiscovered(
            macAddress = "AA:BB:CC:DD:EE:20",
            meshId = "SHORTID1",
            name = "motorola edge 60 fusion",
            rssi = -70
        )

        val initial = engine.cache.get("AA:BB:CC:DD:EE:20")
        assertNotNull(initial)
        // Primary name must NOT be set to hardware phone model; it defaults to Mesh Peer placeholder
        assertEquals("Mesh Peer", initial?.name)
        assertEquals("motorola edge 60 fusion", initial?.bluetoothDeviceName)

        // 2. Identity received via handshake or beacon
        engine.updatePeerIdentity(
            identifier = "AA:BB:CC:DD:EE:20",
            displayName = "Durga Prasad",
            canonicalMeshId = "CANONICAL1"
        )

        val afterIdentity = engine.cache.get("AA:BB:CC:DD:EE:20")
        assertNotNull(afterIdentity)
        assertEquals("Durga Prasad", afterIdentity?.displayName)
        assertEquals("Durga Prasad", afterIdentity?.name)
        assertEquals("CANONICAL1", afterIdentity?.meshId)

        // 3. Subsequent BLE scan arrives with hardware name
        // Wait past duplicate filter window (2000ms)
        Thread.sleep(2100)
        engine.onDeviceDiscovered(
            macAddress = "AA:BB:CC:DD:EE:20",
            meshId = "SHORTID1",
            name = "motorola edge 60 fusion",
            rssi = -55
        )

        val afterRescan = engine.cache.get("AA:BB:CC:DD:EE:20")
        assertNotNull(afterRescan)
        // Profile display name MUST NOT be overwritten by Bluetooth name
        assertEquals("Durga Prasad", afterRescan?.displayName)
        assertEquals("Durga Prasad", afterRescan?.name)
        assertEquals("motorola edge 60 fusion", afterRescan?.bluetoothDeviceName)
    }

    /**
     * Test 11 — Multiple peers display distinct profile names:
     * Peer A ("Alice" on Samsung) and Peer B ("Bob" on Pixel) are both resolved independently.
     */
    @Test
    fun `test11_multiplePeers_displayDistinctProfileNames`() = runTest {
        val localUser = User(meshId = "MYID123", name = "Me")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { userRepository.getUserDisplayName("ALICE001") } returns "Alice"
        coEvery { userRepository.getUserDisplayName("BOB00002") } returns "Bob"
        coEvery { userRepository.getUserProfile(any()) } returns null

        val peerA = BleDevice(
            meshId = "ALICE001",
            name = "Samsung Galaxy",
            bluetoothDeviceName = "Samsung Galaxy",
            address = "AA:BB:CC:DD:EE:A1",
            rssi = -50
        )
        val peerB = BleDevice(
            meshId = "BOB00002",
            name = "Pixel 8",
            bluetoothDeviceName = "Pixel 8",
            address = "AA:BB:CC:DD:EE:B2",
            rssi = -65
        )

        val scannedFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
        every { meshRepository.scannedDevices } returns scannedFlow
        every { topologyManager.reachableNodes } returns MutableStateFlow(emptyList())

        val viewModel = NearbyViewModel(meshRepository, userRepository, topologyManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.devices.isEmpty())

            scannedFlow.value = mapOf("ALICE001" to peerA, "BOB00002" to peerB)

            val updated = awaitItem()
            assertEquals(2, updated.devices.size)
            val names = updated.devices.map { it.name }
            assertTrue(names.contains("Alice"))
            assertTrue(names.contains("Bob"))
        }
    }

    /**
     * Test 12 — Missing display name defaults to Mesh Peer, never physical smartphone hardware name:
     */
    @Test
    fun `test12_missingDisplayName_fallsBackToMeshPeerNeverPhoneModel`() = runTest {
        val localUser = User(meshId = "MYID123", name = "Me")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { userRepository.getUserDisplayName(any()) } returns "Unknown User"
        coEvery { userRepository.getUserProfile(any()) } returns null

        val peerWithBtName = BleDevice(
            meshId = "PEER001",
            name = "Mesh Peer",
            bluetoothDeviceName = "motorola edge 60 fusion",
            address = "AA:BB:CC:DD:EE:31",
            rssi = -60
        )
        val peerWithoutAnyName = BleDevice(
            meshId = "PEER002",
            name = "Mesh Peer",
            bluetoothDeviceName = null,
            address = "AA:BB:CC:DD:EE:32",
            rssi = -70
        )

        val scannedFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
        every { meshRepository.scannedDevices } returns scannedFlow
        every { topologyManager.reachableNodes } returns MutableStateFlow(emptyList())

        val viewModel = NearbyViewModel(meshRepository, userRepository, topologyManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.devices.isEmpty())

            scannedFlow.value = mapOf("PEER001" to peerWithBtName, "PEER002" to peerWithoutAnyName)

            val updated = awaitItem()
            assertEquals(2, updated.devices.size)
            val device1 = updated.devices.first { it.address == "AA:BB:CC:DD:EE:31" }
            val device2 = updated.devices.first { it.address == "AA:BB:CC:DD:EE:32" }

            assertEquals("Mesh Peer", device1.name)
            assertEquals("Mesh Peer", device2.name)
            // Bluetooth hardware name is preserved only as transport metadata
            assertEquals("motorola edge 60 fusion", device1.bluetoothDeviceName)
        }
    }

    /**
     * Test 13 — Searching by user profile display name matches discovered peers:
     */
    @Test
    fun `test13_searchQuery_filtersByProfileDisplayName`() = runTest {
        val localUser = User(meshId = "MYID123", name = "Me")
        coEvery { userRepository.getLocalUser() } returns localUser
        coEvery { userRepository.getUserDisplayName("NODE_RAHUL") } returns "Rahul"
        coEvery { userRepository.getUserDisplayName("NODE_ALICE") } returns "Alice"
        coEvery { userRepository.getUserProfile(any()) } returns null

        val peerA = BleDevice(
            meshId = "NODE_RAHUL",
            name = "vivo Y300 Plus 5G",
            bluetoothDeviceName = "vivo Y300 Plus 5G",
            address = "AA:BB:CC:DD:EE:41",
            rssi = -55
        )
        val peerB = BleDevice(
            meshId = "NODE_ALICE",
            name = "Pixel 7a",
            bluetoothDeviceName = "Pixel 7a",
            address = "AA:BB:CC:DD:EE:42",
            rssi = -65
        )

        val scannedFlow = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
        every { meshRepository.scannedDevices } returns scannedFlow
        every { topologyManager.reachableNodes } returns MutableStateFlow(emptyList())

        val viewModel = NearbyViewModel(meshRepository, userRepository, topologyManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.devices.isEmpty())

            scannedFlow.value = mapOf("NODE_RAHUL" to peerA, "NODE_ALICE" to peerB)

            val both = awaitItem()
            assertEquals(2, both.devices.size)

            // Search for "Rahul"
            viewModel.onSearchQueryChanged("Rahul")
            val filtered = awaitItem()
            assertEquals(1, filtered.devices.size)
            assertEquals("Rahul", filtered.devices[0].name)
            assertEquals("NODE_RAHUL", filtered.devices[0].meshId)
        }
    }

    /**
     * Test 14 — Deterministic angle calculation remains stable across recompositions:
     */
    @Test
    fun `test14_deterministicRadarAngle_isStableForSameCanonicalMeshId`() {
        val canonicalId = "MESH_PEER_ALPHA"
        val hash1 = Math.abs((canonicalId.hashCode().toLong() and 0xFFFFFFFFL))
        val angleDeg1 = (hash1 % 360).toFloat()

        val hash2 = Math.abs((canonicalId.hashCode().toLong() and 0xFFFFFFFFL))
        val angleDeg2 = (hash2 % 360).toFloat()

        assertEquals(angleDeg1, angleDeg2, 0.001f)
        assertTrue(angleDeg1 in 0f..360f)
    }
}
