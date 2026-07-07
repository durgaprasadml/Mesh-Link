package com.meshlink.data.local

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
}
