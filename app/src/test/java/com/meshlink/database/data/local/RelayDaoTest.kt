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
class RelayDaoTest {

    private lateinit var database: MeshDatabase
    private lateinit var relayDao: RelayDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MeshDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        relayDao = database.relayDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and retrieve packets for target`() = runTest {
        val packet1 = RelayPacketEntity("p1", "sender", "target1", "payload", "text", ttl = 3, hopCount = 1, encrypted = true)
        val packetBroadcast = RelayPacketEntity("p2", "sender", "BROADCAST", "payload", "text", ttl = 3, hopCount = 1, encrypted = true)
        val packetOther = RelayPacketEntity("p3", "sender", "target2", "payload", "text", ttl = 3, hopCount = 1, encrypted = true)
        
        relayDao.insertPacket(packet1)
        relayDao.insertPacket(packetBroadcast)
        relayDao.insertPacket(packetOther)
        
        val retrieved = relayDao.getPacketsForTarget("target1")
        assertEquals(2, retrieved.size)
        assert(retrieved.contains(packet1))
        assert(retrieved.contains(packetBroadcast))
    }

    @Test
    fun `delete expired packets`() = runTest {
        val now = System.currentTimeMillis()
        val expiredPacket = RelayPacketEntity("p1", "s", "t", "payload", "text", expiryTimestamp = now - 1000, ttl = 3, hopCount = 1, encrypted = true)
        val validPacket = RelayPacketEntity("p2", "s", "t", "payload", "text", expiryTimestamp = now + 1000, ttl = 3, hopCount = 1, encrypted = true)
        
        relayDao.insertPacket(expiredPacket)
        relayDao.insertPacket(validPacket)
        
        relayDao.deleteExpiredPackets(now)
        
        val allPackets = relayDao.getAllRelayPackets()
        assertEquals(1, allPackets.size)
        assertEquals(validPacket, allPackets[0])
    }

    @Test
    fun `enforce storage cap`() = runTest {
        // Insert 5 packets with sequential timestamps
        for (i in 1..5) {
            relayDao.insertPacket(RelayPacketEntity("p$i", "s", "t", "payload", "text", timestamp = i * 1000L, ttl = 3, hopCount = 1, encrypted = true))
        }
        
        // Enforce cap of 3
        relayDao.enforceStorageCap(3)
        
        val allPackets = relayDao.getAllRelayPackets()
        assertEquals(3, allPackets.size)
        
        // The newest ones should remain (p3, p4, p5)
        assert(allPackets.any { it.packetId == "p3" })
        assert(allPackets.any { it.packetId == "p4" })
        assert(allPackets.any { it.packetId == "p5" })
    }
}
