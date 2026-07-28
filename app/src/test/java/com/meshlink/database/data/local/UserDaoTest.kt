package com.meshlink.database.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meshlink.util.RoomTestDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UserDaoTest {

    private lateinit var database: MeshDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = RoomTestDatabase.createInMemoryDb(context)
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
