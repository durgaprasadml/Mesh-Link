package com.meshlink.database.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UserDaoTest {

    private lateinit var database: MeshDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MeshDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and retrieve user`() = runTest {
        val user = UserEntity("mesh_1", "Alice", "12345", "hash")
        
        userDao.insertUser(user)
        
        val retrieved = userDao.getUser("mesh_1")
        assertEquals(user, retrieved)
    }

    @Test
    fun `getLocalUser returns the first user`() = runTest {
        val user1 = UserEntity("mesh_1", "Alice", "12345", "hash")
        userDao.insertUser(user1)
        
        val retrieved = userDao.getLocalUser()
        assertEquals(user1, retrieved)
    }

    @Test
    fun `clearUsers deletes all users`() = runTest {
        val user = UserEntity("mesh_1", "Alice", "12345", "hash")
        userDao.insertUser(user)
        
        userDao.clearUsers()
        
        val retrieved = userDao.getUser("mesh_1")
        assertNull(retrieved)
    }
    
    @Test
    fun `insert replaces on conflict`() = runTest {
        val user = UserEntity("mesh_1", "Alice", "12345", "hash")
        userDao.insertUser(user)
        
        val updatedUser = UserEntity("mesh_1", "Alice Updated", "12345", "hash")
        userDao.insertUser(updatedUser)
        
        val retrieved = userDao.getUser("mesh_1")
        assertEquals("Alice Updated", retrieved?.name)
    }
}
