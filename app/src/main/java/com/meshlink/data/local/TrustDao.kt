package com.meshlink.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrustDao {
    @Query("SELECT * FROM trust_table")
    suspend fun getAllPeers(): List<TrustEntity>

    @Query("SELECT * FROM trust_table WHERE peerId = :peerId")
    suspend fun getPeerTrust(peerId: String): TrustEntity?

    @Query("SELECT * FROM trust_table WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getPeerByFingerprint(fingerprint: String): TrustEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePeerTrust(trustEntity: TrustEntity)

    @Update
    suspend fun updatePeerTrust(trustEntity: TrustEntity)

    @Query("UPDATE trust_table SET trustScore = :score, trustLevel = :level WHERE peerId = :peerId")
    suspend fun updateTrustScoreAndLevel(peerId: String, score: Int, level: String)
}
