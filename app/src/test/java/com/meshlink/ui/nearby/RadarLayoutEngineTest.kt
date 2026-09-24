package com.meshlink.ui.nearby

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.BleDevice
import com.meshlink.ui.components.nearby.RadarLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class RadarLayoutEngineTest {

    private val density = Density(density = 2.5f, fontScale = 1.0f)
    private val canvasSize = Size(width = 800f, height = 550f)

    private fun calculateTestLayout(
        devices: List<BleDevice>,
        searchQuery: String = "",
        selectedAddress: String? = null
    ) = RadarLayoutEngine.calculateLayout(
        devices = devices,
        canvasSize = canvasSize,
        density = density,
        textSizeEstimator = { text, _ -> Size(text.length * 10f, 18f) },
        selectedAddress = selectedAddress,
        searchQuery = searchQuery
    )

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * TEST 1 — ZERO PEERS
     * Clean radar, "You" centered, empty peer list, positive radius.
     */
    @Test
    fun `test1_zeroPeers_returnsEmptyNodesAndCenteredYou`() {
        val scene = calculateTestLayout(emptyList())

        assertEquals(0, scene.nodes.size)
        assertEquals(canvasSize.width / 2f, scene.center.x, 0.01f)
        assertEquals(canvasSize.height / 2f, scene.center.y, 0.01f)
        assertTrue(scene.maxRadius > 0f)
        assertNotNull(scene.youLabelRect)
    }

    /**
     * TEST 2 — ONE PEER
     * Peer clearly separated from center node and "You" pill label.
     */
    @Test
    fun `test2_onePeer_isSeparatedFromCenterAndYou`() {
        val peer = BleDevice(
            meshId = "ALPHA123",
            name = "Durga",
            address = "AA:BB:CC:DD:EE:01",
            rssi = -50
        )

        val scene = calculateTestLayout(listOf(peer))

        assertEquals(1, scene.nodes.size)
        val node = scene.nodes[0]
        assertEquals("Durga", node.displayName)
        assertEquals("D", node.initial)

        val distToCenter = distance(node.centerOffset, scene.center)
        val minCenterExclusion = with(density) { 42.0f }
        assertTrue("Node must be outside center exclusion (got $distToCenter)", distToCenter >= minCenterExclusion)

        // Node center must not intersect "You" label rect
        assertFalse(
            "Node must not overlap You label",
            scene.youLabelRect.contains(node.centerOffset)
        )
    }

    /**
     * TEST 3 — TWO PEERS WITH CLOSE HASH ANGLES
     * Two peers that would otherwise overlap are separated by at least minNodeSeparation.
     */
    @Test
    fun `test3_twoPeers_guaranteeMinimumSeparation`() {
        val peerA = BleDevice(
            meshId = "NODE_001",
            name = "Raju",
            address = "AA:BB:CC:DD:EE:01",
            rssi = -55
        )
        val peerB = BleDevice(
            meshId = "NODE_002",
            name = "Manoj",
            address = "AA:BB:CC:DD:EE:02",
            rssi = -56
        )

        val scene = calculateTestLayout(listOf(peerA, peerB))

        assertEquals(2, scene.nodes.size)
        val nodeA = scene.nodes[0]
        val nodeB = scene.nodes[1]

        val dist = distance(nodeA.centerOffset, nodeB.centerOffset)
        val minSeparationPx = with(density) { 34.dp.toPx() }
        assertTrue(
            "Two peers must be separated by at least min separation ($minSeparationPx px, got $dist px)",
            dist >= minSeparationPx * 0.90f
        )
    }

    /**
     * TEST 4 — THREE TO FIVE PEERS
     * Collision-free markers, readable labels, stable positions.
     */
    @Test
    fun `test4_threeToFivePeers_allMaintainSeparation`() {
        val peers = (1..4).map { i ->
            BleDevice(
                meshId = "PEER_00$i",
                name = "Peer $i",
                address = "AA:BB:CC:DD:EE:0$i",
                rssi = -60 - (i * 5)
            )
        }

        val scene = calculateTestLayout(peers)

        assertEquals(4, scene.nodes.size)

        for (i in 0 until scene.nodes.size) {
            val nodeI = scene.nodes[i]
            // Center check
            assertTrue(distance(nodeI.centerOffset, scene.center) > 50f)

            for (j in i + 1 until scene.nodes.size) {
                val nodeJ = scene.nodes[j]
                val d = distance(nodeI.centerOffset, nodeJ.centerOffset)
                assertTrue("Nodes $i and $j must not overlap (dist=$d)", d >= 50f)
            }
        }
    }

    /**
     * TEST 5 — HIGH DENSITY (10+ PEERS)
     * All peers rendered without crash, positions stay inside canvas boundaries.
     */
    @Test
    fun `test5_highDensity_peersStayInsideCanvasBounds`() {
        val peers = (1..12).map { i ->
            BleDevice(
                meshId = "DENSE_NODE_$i",
                name = "Node $i",
                address = "AA:BB:CC:DD:EE:${if (i < 10) "0$i" else "$i"}",
                rssi = -50 - (i * 3)
            )
        }

        val scene = calculateTestLayout(peers)

        assertEquals(12, scene.nodes.size)
        for (node in scene.nodes) {
            assertTrue("Node X inside canvas", node.centerOffset.x in 0f..canvasSize.width)
            assertTrue("Node Y inside canvas", node.centerOffset.y in 0f..canvasSize.height)
            assertTrue("Node label X inside canvas", node.labelTopLeft.x in 0f..canvasSize.width)
            assertTrue("Node label Y inside canvas", node.labelTopLeft.y in 0f..canvasSize.height)
        }
    }

    /**
     * TEST 6 & 7 — STABILITY ON ADDITION AND REMOVAL
     * Adding Peer B does NOT jump Peer A's position. Removing Peer B restores Peer A's position.
     */
    @Test
    fun `test6_and_7_peerAdditionAndRemoval_preservesStablePosition`() {
        val peerA = BleDevice(
            meshId = "STABLE_A",
            name = "Durga",
            address = "AA:BB:CC:DD:EE:01",
            rssi = -60
        )
        val peerB = BleDevice(
            meshId = "STABLE_B",
            name = "Manoj",
            address = "AA:BB:CC:DD:EE:02",
            rssi = -70
        )

        // 1. Peer A alone
        val scene1 = calculateTestLayout(listOf(peerA))
        val posA1 = scene1.nodes.first { it.canonicalId == "STABLE_A" }.centerOffset

        // 2. Peer A + Peer B
        val scene2 = calculateTestLayout(listOf(peerA, peerB))
        val posA2 = scene2.nodes.first { it.canonicalId == "STABLE_A" }.centerOffset

        // Because placement is sorted by canonicalId ("STABLE_A" is evaluated first),
        // Peer A's placement is identical whether Peer B is present or not!
        assertEquals("Peer A position must remain stable when Peer B appears", posA1.x, posA2.x, 0.01f)
        assertEquals("Peer A position must remain stable when Peer B appears", posA1.y, posA2.y, 0.01f)

        // 3. Peer B removed -> back to Peer A alone
        val scene3 = calculateTestLayout(listOf(peerA))
        val posA3 = scene3.nodes.first { it.canonicalId == "STABLE_A" }.centerOffset
        assertEquals("Peer A position must remain stable when Peer B disappears", posA1.x, posA3.x, 0.01f)
        assertEquals("Peer A position must remain stable when Peer B disappears", posA1.y, posA3.y, 0.01f)
    }

    /**
     * TEST 8 — RSSI STABILITY
     * Small RSSI changes within the same band cause zero coordinate jitter.
     */
    @Test
    fun `test8_rssiMicroFluctuations_causeNoJitter`() {
        val peer1 = BleDevice(
            meshId = "RSSI_TEST",
            name = "Rahul",
            address = "AA:BB:CC:DD:EE:01",
            rssi = -55
        )
        val peer2 = peer1.copy(rssi = -58) // Minor 3 dBm change within strong band

        val scene1 = calculateTestLayout(listOf(peer1))
        val scene2 = calculateTestLayout(listOf(peer2))

        val pos1 = scene1.nodes[0].centerOffset
        val pos2 = scene2.nodes[0].centerOffset

        assertEquals("No jitter on minor RSSI fluctuation", pos1.x, pos2.x, 0.001f)
        assertEquals("No jitter on minor RSSI fluctuation", pos1.y, pos2.y, 0.001f)
    }

    /**
     * TEST 9 — CONNECTION STATE REFLECTED
     */
    @Test
    fun `test9_connectionState_correctlyReflected`() {
        val peerConnected = BleDevice(
            meshId = "CONN_01",
            name = "Connected Node",
            address = "AA:BB:CC:DD:EE:01",
            rssi = -50,
            isConnected = true
        )

        val scene = calculateTestLayout(listOf(peerConnected))

        assertTrue(scene.nodes[0].isConnected)
    }

    /**
     * TEST 10 — SEARCH HIGHLIGHTING
     */
    @Test
    fun `test10_searchHighlighting_matchesQueryWithoutRemovingPeers`() {
        val peer1 = BleDevice(meshId = "NODE_ALICE", name = "Alice", address = "01", rssi = -60)
        val peer2 = BleDevice(meshId = "NODE_BOB", name = "Bob", address = "02", rssi = -60)

        val scene = calculateTestLayout(
            devices = listOf(peer1, peer2),
            searchQuery = "Ali"
        )

        assertEquals(2, scene.nodes.size)
        val alice = scene.nodes.first { it.displayName == "Alice" }
        val bob = scene.nodes.first { it.displayName == "Bob" }

        assertTrue("Alice matches query", alice.isMatchSearch)
        assertFalse("Bob does not match query", bob.isMatchSearch)
    }

    /**
     * TEST 11 — REPEATED DETERMINISTIC EXECUTION
     * Running calculateLayout 100 times produces identical coordinates.
     */
    @Test
    fun `test11_determinism_repeatedRunsProduceExactCoordinates`() {
        val peers = (1..5).map { i ->
            BleDevice(
                meshId = "DET_PEER_$i",
                name = "Det $i",
                address = "AA:BB:CC:DD:EE:0$i",
                rssi = -60
            )
        }

        val baseline = calculateTestLayout(peers)

        repeat(100) {
            val run = calculateTestLayout(peers)

            for (i in baseline.nodes.indices) {
                assertEquals(baseline.nodes[i].centerOffset.x, run.nodes[i].centerOffset.x, 0.0001f)
                assertEquals(baseline.nodes[i].centerOffset.y, run.nodes[i].centerOffset.y, 0.0001f)
            }
        }
    }
}
