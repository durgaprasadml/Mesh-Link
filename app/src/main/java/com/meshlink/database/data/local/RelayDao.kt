package com.meshlink.database.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RelayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacket(packet: RelayPacketEntity)

    @Query("SELECT * FROM relay_packets WHERE targetId = :targetId OR targetId = 'BROADCAST'")
    suspend fun getPacketsForTarget(targetId: String): List<RelayPacketEntity>

    @Query("SELECT * FROM relay_packets")
    suspend fun getAllRelayPackets(): List<RelayPacketEntity>

    @Query("DELETE FROM relay_packets WHERE packetId = :packetId")
    suspend fun deletePacket(packetId: String)

    @Query("DELETE FROM relay_packets WHERE expiryTimestamp < :now")
    suspend fun deleteExpiredPackets(now: Long)

    @Query("DELETE FROM relay_packets WHERE packetId IN (SELECT packetId FROM relay_packets ORDER BY timestamp DESC LIMIT -1 OFFSET :maxSize)")
    suspend fun enforceStorageCap(maxSize: Int)
}
