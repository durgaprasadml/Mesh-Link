package com.meshlink.data.security

import com.meshlink.data.local.TrustDao
import com.meshlink.data.local.TrustEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrustManagerTest {

    private lateinit var trustDao: TrustDao
    private lateinit var securityMonitor: MeshSecurityMonitor
    private lateinit var trustManager: TrustManager

    @Before
    fun setup() {
        trustDao = mockk(relaxed = true)
        securityMonitor = mockk(relaxed = true)
        
        coEvery { trustDao.getAllPeers() } returns emptyList()
        coEvery { trustDao.getPeerByFingerprint(any()) } returns null
        
        trustManager = TrustManager(trustDao, securityMonitor, UnconfinedTestDispatcher())
    }

    @Test
    fun `test new peer is DISCOVERED and starts with neutral score`() = runTest {
        trustManager.updatePeerIdentity("peer1", "fingerprint1", "uuid1")
        
        val slot = slot<TrustEntity>()
        coVerify(timeout = 1000) { trustDao.insertOrUpdatePeerTrust(capture(slot)) }
        
        assertEquals("peer1", slot.captured.peerId)
        assertEquals("fingerprint1", slot.captured.fingerprint)
        assertEquals(TrustLevel.DISCOVERED.name, slot.captured.trustLevel)
        assertEquals(50, slot.captured.trustScore)
    }

    @Test
    fun `test identity change detection`() = runTest {
        val existingEntity = TrustEntity(
            peerId = "peer1",
            deviceUUID = "uuid1",
            fingerprint = "fingerprint1",
            firstSeen = 0L,
            lastSeen = 0L,
            lastIPAddress = null,
            lastBLEAddress = null,
            keyVersion = 1,
            trustLevel = TrustLevel.DISCOVERED.name,
            verificationStatus = VerificationStatus.NOT_VERIFIED.name,
            trustScore = 50,
            identityHistory = "[]"
        )
        coEvery { trustDao.getAllPeers() } returns listOf(existingEntity)
        trustManager = TrustManager(trustDao, securityMonitor, UnconfinedTestDispatcher()) // re-init to load cache
        
        // Present new fingerprint
        trustManager.updatePeerIdentity("peer1", "fingerprint2", "uuid1")
        
        val slot = slot<TrustEntity>()
        coVerify(timeout = 1000) { trustDao.updatePeerTrust(capture(slot)) }
        
        assertEquals(TrustLevel.REVOKED.name, slot.captured.trustLevel)
        assertEquals(0, slot.captured.trustScore)
        coVerify(timeout = 1000) { securityMonitor.reportEvent("peer1", any<SecurityEvent.IdentityChanged>()) }
    }

    @Test
    fun `test trust score increase to TRUSTED when verified`() = runTest {
        val existingEntity = TrustEntity(
            peerId = "peer1",
            deviceUUID = "uuid1",
            fingerprint = "fingerprint1",
            firstSeen = 0L,
            lastSeen = 0L,
            lastIPAddress = null,
            lastBLEAddress = null,
            keyVersion = 1,
            trustLevel = TrustLevel.VERIFIED.name,
            verificationStatus = VerificationStatus.VERIFIED.name,
            trustScore = 78,
            identityHistory = "[]"
        )
        coEvery { trustDao.getAllPeers() } returns listOf(existingEntity)
        trustManager = TrustManager(trustDao, securityMonitor, UnconfinedTestDispatcher())
        
        trustManager.increaseTrustScore("peer1", 5) // Should cross 80
        
        coVerify(timeout = 1000) { trustDao.updateTrustScoreAndLevel("peer1", 83, TrustLevel.TRUSTED.name) }
        assertEquals(TrustLevel.TRUSTED, trustManager.getTrustLevel("peer1"))
    }
}
