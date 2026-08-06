package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.model.User
import com.meshlink.trust.MeshIdentityManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private val localDataSource = mockk<UserLocalDataSource>(relaxed = true)
    private val identityManager = mockk<MeshIdentityManager>(relaxed = true)

    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setUp() {
        userRepository = UserRepositoryImpl(localDataSource, identityManager)
    }

    @Test
    fun `isGenericOrInvalidName correctly identifies placeholder and generic names`() {
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName("Man"))
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName("Device"))
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName("Peer"))
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName("Nearby Node"))
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName("Unknown User"))
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName(""))
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName(null))
        assertTrue(UserRepositoryImpl.isGenericOrInvalidName("node-12345", "node-12345"))

        assertFalse(UserRepositoryImpl.isGenericOrInvalidName("Durga Prasad"))
        assertFalse(UserRepositoryImpl.isGenericOrInvalidName("Rahul Kumar"))
    }

    @Test
    fun `getUserDisplayName returns registered name for valid remote peer`() = runBlocking {
        val peerId = "mesh-peer-123"
        coEvery { localDataSource.getLocalUser() } returns UserEntity("mesh-local-456", "Durga Prasad")
        coEvery { localDataSource.getUser(peerId) } returns UserEntity(peerId, "Rahul Kumar")

        val name = userRepository.getUserDisplayName(peerId)

        assertEquals("Rahul Kumar", name)
    }

    @Test
    fun `getUserDisplayName returns Unknown User for remote peer with generic name`() = runBlocking {
        val peerId = "mesh-peer-789"
        coEvery { localDataSource.getLocalUser() } returns UserEntity("mesh-local-456", "Durga Prasad")
        coEvery { localDataSource.getUser(peerId) } returns UserEntity(peerId, "Man")

        val name = userRepository.getUserDisplayName(peerId)

        assertEquals("Unknown User", name)
    }

    @Test
    fun `getUserDisplayName returns Unknown User for unrecorded peer`() = runBlocking {
        val peerId = "mesh-unknown-999"
        coEvery { localDataSource.getLocalUser() } returns UserEntity("mesh-local-456", "Durga Prasad")
        coEvery { localDataSource.getUser(peerId) } returns null

        val name = userRepository.getUserDisplayName(peerId)

        assertEquals("Unknown User", name)
    }
}
