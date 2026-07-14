package com.meshlink.database.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TrustDaoTest {

    private lateinit var database: MeshDatabase
    private lateinit var trustDao: TrustDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MeshDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trustDao = database.trustDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and retrieve peer trust`() = runTest {
        val trust = TrustEntity(
            peerId = "peer_1", deviceUUID = "uuid_1", fingerprint = "fp_1",
            firstSeen = 100L, lastSeen = 200L, lastIPAddress = "192.168.1.1", lastBLEAddress = "AA:BB:CC",
            keyVersion = 1, trustLevel = "TRUSTED", verificationStatus = "VERIFIED", trustScore = 100, identityHistory = "[]"
        )
        
        trustDao.insertOrUpdatePeerTrust(trust)
        
        val retrievedById = trustDao.getPeerTrust("peer_1")
        assertEquals(trust, retrievedById)
        
        val retrievedByFingerprint = trustDao.getPeerByFingerprint("fp_1")
        assertEquals(trust, retrievedByFingerprint)
    }

    @Test
    fun `update peer trust updates existing record`() = runTest {
        val trust = TrustEntity(
            peerId = "peer_1", deviceUUID = "uuid_1", fingerprint = "fp_1",
            firstSeen = 100L, lastSeen = 200L, lastIPAddress = "192.168.1.1", lastBLEAddress = "AA:BB:CC",
            keyVersion = 1, trustLevel = "TRUSTED", verificationStatus = "VERIFIED", trustScore = 100, identityHistory = "[]"
        )
        trustDao.insertOrUpdatePeerTrust(trust)
        
        val updatedTrust = trust.copy(trustScore = 150)
        trustDao.updatePeerTrust(updatedTrust)
        
        val retrieved = trustDao.getPeerTrust("peer_1")
        assertEquals(150, retrieved?.trustScore)
    }

    @Test
    fun `update trust score and level`() = runTest {
        val trust = TrustEntity(
            peerId = "peer_1", deviceUUID = "uuid_1", fingerprint = "fp_1",
            firstSeen = 100L, lastSeen = 200L, lastIPAddress = "192.168.1.1", lastBLEAddress = "AA:BB:CC",
            keyVersion = 1, trustLevel = "UNKNOWN", verificationStatus = "UNVERIFIED", trustScore = 0, identityHistory = "[]"
        )
        trustDao.insertOrUpdatePeerTrust(trust)
        
        trustDao.updateTrustScoreAndLevel("peer_1", 200, "VERIFIED")
        
        val retrieved = trustDao.getPeerTrust("peer_1")
        assertEquals(200, retrieved?.trustScore)
        assertEquals("VERIFIED", retrieved?.trustLevel)
    }

    @Test
    fun `get all peers returns all inserted peers`() = runTest {
        val trust1 = TrustEntity(
            peerId = "peer_1", deviceUUID = "uuid_1", fingerprint = "fp_1",
            firstSeen = 100L, lastSeen = 200L, lastIPAddress = "192.168.1.1", lastBLEAddress = "AA:BB:CC",
            keyVersion = 1, trustLevel = "UNKNOWN", verificationStatus = "UNVERIFIED", trustScore = 0, identityHistory = "[]"
        )
        val trust2 = trust1.copy(peerId = "peer_2", fingerprint = "fp_2")
        
        trustDao.insertOrUpdatePeerTrust(trust1)
        trustDao.insertOrUpdatePeerTrust(trust2)
        
        val allPeers = trustDao.getAllPeers()
        assertEquals(2, allPeers.size)
    }
}
