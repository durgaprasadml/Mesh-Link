package com.meshlink.ble.discovery

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for peer freshness / expiry in the discovery registry that feeds the
 * Nearby Devices list.
 */
class DiscoveryRegistryFreshnessTest {

    private lateinit var engine: DiscoveryEngine
    private val mac = "AA:BB:CC:DD:EE:01"
    private val meshId = "PEER1234"

    @Before
    fun setup() {
        engine = DiscoveryEngine(mockk<BatteryAwareScanner>(relaxed = true))
    }

    private fun advertise(address: String = mac, id: String = meshId) {
        engine.onDeviceDiscovered(macAddress = address, meshId = id, name = "", rssi = -60, capabilities = 1)
    }

    @Test
    fun `duplicate-suppressed advertisement still refreshes last seen`() {
        advertise()
        val record = engine.cache.get(mac)!!
        record.lastSeenMillis = 1_000L // pretend the last accepted advertisement was long ago

        advertise() // inside the 2s duplicate window -> suppressed by DuplicateFilter

        assertTrue("suppressed advertisement must refresh freshness", record.lastSeenMillis > 1_000L)
        assertEquals(1, engine.analytics.metrics.value.duplicateSuppressedCount)
    }

    @Test
    fun `peer stays visible while valid advertisements keep arriving`() {
        advertise()
        val record = engine.cache.get(mac)!!
        val now = System.currentTimeMillis()

        // Advertisement 50s after the previous one, then another 50s later: never > 60s of silence.
        repeat(3) {
            record.lastSeenMillis = System.currentTimeMillis() - 50_000L
            advertise()
            assertEquals("fresh advertisement keeps the peer", 0, engine.evictStalePeers(now = System.currentTimeMillis() + 10_000L))
            assertNotNull(engine.scannedDevices.value[meshId])
        }
        assertTrue(record.lastSeenMillis >= now)
    }

    @Test
    fun `genuinely stale peer expires and disappears from the published list`() {
        advertise()
        assertNotNull(engine.scannedDevices.value[meshId])
        val record = engine.cache.get(mac)!!
        val now = System.currentTimeMillis()
        record.lastSeenMillis = now - (DiscoveryEngine.PEER_STALE_TIMEOUT_MS + 1_000L)

        assertEquals(1, engine.evictStalePeers(now))
        assertFalse(engine.scannedDevices.value.containsKey(meshId))
    }

    @Test
    fun `repeated discovery never creates duplicate records`() {
        repeat(10) { advertise() }
        assertEquals(1, engine.cache.getAll().size)
        assertEquals(1, engine.scannedDevices.value.size)
    }

    @Test
    fun `peer with a live GATT link is not expired for not advertising`() {
        // A linked peer's connectable advertisement is consumed by the link, so lastSeen is frozen.
        engine.linkProbe = { true }
        advertise()
        val record = engine.cache.get(mac)!!
        engine.notifyConnectionSuccess(mac)
        val now = System.currentTimeMillis()
        record.lastSeenMillis = now - 10 * 60_000L

        assertEquals(0, engine.evictStalePeers(now))
        assertNotNull(engine.scannedDevices.value[meshId])
    }

    @Test
    fun `closing a live link restarts the freshness window instead of expiring immediately`() {
        engine.linkProbe = { true }
        advertise()
        val record = engine.cache.get(mac)!!
        engine.notifyConnectionSuccess(mac)
        record.lastSeenMillis = System.currentTimeMillis() - 10 * 60_000L

        engine.notifyConnectionFailure(mac) // reported disconnect of a previously linked peer

        val now = System.currentTimeMillis()
        assertEquals("peer gets a fresh window to be re-heard", 0, engine.evictStalePeers(now))
        assertEquals("but still expires if never heard again", 1,
            engine.evictStalePeers(now + DiscoveryEngine.PEER_STALE_TIMEOUT_MS + 1_000L))
    }

    @Test
    fun `link flag without a real GATT link does not pin the peer forever`() {
        engine.linkProbe = { false } // e.g. client was closed locally; no disconnect callback fired
        advertise()
        val record = engine.cache.get(mac)!!
        engine.notifyConnectionSuccess(mac)
        record.lastSeenMillis = System.currentTimeMillis() - 10 * 60_000L

        val now = System.currentTimeMillis()
        assertEquals("reconciled and given a fresh window", 0, engine.evictStalePeers(now))
        assertFalse(record.linkActive)
        assertEquals("then expires normally", 1,
            engine.evictStalePeers(now + DiscoveryEngine.PEER_STALE_TIMEOUT_MS + 1_000L))
    }

    @Test
    fun `failed connection attempt to a never-linked peer does not refresh freshness`() {
        advertise()
        val record = engine.cache.get(mac)!!
        record.lastSeenMillis = 5_000L

        engine.notifyConnectionFailure(mac)

        assertEquals(5_000L, record.lastSeenMillis)
    }

    @Test
    fun `rotated BLE address publishes the freshest record whatever the insertion order`() {
        listOf(true, false).forEach { staleFirst ->
            val e = DiscoveryEngine(mockk<BatteryAwareScanner>(relaxed = true))
            val staleMac = "AA:00:00:00:00:01"
            val freshMac = "BB:00:00:00:00:02"
            val order = if (staleFirst) listOf(staleMac, freshMac) else listOf(freshMac, staleMac)
            order.forEach { e.onDeviceDiscovered(it, meshId, "", -60, 1) }
            e.cache.get(staleMac)!!.lastSeenMillis = 1_000L
            e.cache.get(freshMac)!!.lastSeenMillis = System.currentTimeMillis()
            // Force a re-publish of the registry after the timestamps were set.
            e.updatePeerIdentity(freshMac, "Peer", meshId)

            val published = e.scannedDevices.value
            assertEquals("one entry per peer (staleFirst=$staleFirst)", 1, published.size)
            assertEquals(freshMac, published[meshId]!!.address)
        }
    }
}
