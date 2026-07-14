package com.meshlink.util

import android.content.Context
import androidx.room.Room
import com.meshlink.database.data.local.MeshDatabase

object RoomTestDatabase {
    fun createInMemoryDb(context: Context): MeshDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            MeshDatabase::class.java
        ).allowMainThreadQueries().build()
    }
}
