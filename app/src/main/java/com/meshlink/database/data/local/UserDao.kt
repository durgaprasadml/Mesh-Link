package com.meshlink.database.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE meshId = :meshId LIMIT 1")
    suspend fun getUser(meshId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getLocalUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun observeLocalUser(): kotlinx.coroutines.flow.Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE meshId = :meshId LIMIT 1")
    fun observeUser(meshId: String): kotlinx.coroutines.flow.Flow<UserEntity?>

    @Query("UPDATE users SET lastSeen = :lastSeen, rssi = :rssi WHERE meshId = :meshId")
    suspend fun updateLastSeen(meshId: String, lastSeen: Long, rssi: Int)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
