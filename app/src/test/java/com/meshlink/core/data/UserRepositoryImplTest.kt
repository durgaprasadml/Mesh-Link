package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.model.User
import com.meshlink.trust.MeshIdentityManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun `getUserDisplayName returns Mesh Peer for remote peer with generic name`() = runBlocking {
        val peerId = "mesh-peer-789"
        coEvery { localDataSource.getLocalUser() } returns UserEntity("mesh-local-456", "Durga Prasad")
        coEvery { localDataSource.getUser(peerId) } returns UserEntity(peerId, "Man")

        val name = userRepository.getUserDisplayName(peerId)

        assertEquals("Mesh Peer", name)
    }

    @Test
    fun `getUserDisplayName returns Mesh Peer for unrecorded peer`() = runBlocking {
        val peerId = "mesh-unknown-999"
        coEvery { localDataSource.getLocalUser() } returns UserEntity("mesh-local-456", "Durga Prasad")
        coEvery { localDataSource.getUser(peerId) } returns null
        coEvery { localDataSource.getAllUsers() } returns emptyList()

        val name = userRepository.getUserDisplayName(peerId)

        assertEquals("Mesh Peer", name)
    }

    @Test
    fun `getUserDisplayName does not poison cache when peer is initially unrecorded`() = runBlocking {
        val peerId = "mesh-unrecorded-peer"
        coEvery { localDataSource.getUser(peerId) } returns null
        coEvery { localDataSource.getAllUsers() } returns emptyList()
        every { identityManager.getOrCreateIdentity() } returns com.meshlink.trust.MeshIdentity(
            meshId = "mesh-local-me",
            publicKey = "pk",
            displayName = "My Identity"
        )

        // First lookup: peer not yet in DB
        val initialLookup = userRepository.getUserDisplayName(peerId)
        assertEquals("Mesh Peer", initialLookup)

        // Peer profile arrives later (e.g., from broadcast senderName or beacon)
        coEvery { localDataSource.getUser(peerId) } returns UserEntity(peerId, "Raju")
        userRepository.saveOrUpdatePeerProfile(peerId, "Raju")

        // Second lookup: MUST resolve to Raju, proving cache was not poisoned
        val subsequentLookup = userRepository.getUserDisplayName(peerId)
        assertEquals("Raju", subsequentLookup)
    }

    @Test
    fun `saveOrUpdatePeerProfile rejects generic names`() = runBlocking {
        val peerId = "mesh-peer-test"
        userRepository.saveOrUpdatePeerProfile(peerId, "Unknown User")
        userRepository.saveOrUpdatePeerProfile(peerId, "Android")
        userRepository.saveOrUpdatePeerProfile(peerId, "Mesh Peer")

        coVerify(exactly = 0) { localDataSource.insertUser(any()) }
    }

    @Test
    fun `getLocalUser strictly anchors to identityManager meshId and ignores rogue localUser query`() = runBlocking {
        val localMeshId = "MYID1234"
        every { identityManager.getOrCreateIdentity() } returns com.meshlink.trust.MeshIdentity(
            meshId = localMeshId,
            publicKey = "pk",
            displayName = "My Real Profile Name"
        )
        // Simulate localDataSource.getLocalUser() returning a peer row due to SQLite row order
        coEvery { localDataSource.getLocalUser() } returns UserEntity("rogue-peer-id", "Rogue Peer")
        coEvery { localDataSource.getUser(localMeshId) } returns UserEntity(localMeshId, "My Real Profile Name")

        val user = userRepository.getLocalUser()

        assertNotNull(user)
        assertEquals(localMeshId, user?.meshId)
        assertEquals("My Real Profile Name", user?.name)
    }

    @Test
    fun `getUserDisplayName resolves name via shortMeshId from allUsers cache`() = runBlocking {
        val canonicalPeerId = "mesh-peer-full-12345"
        val normId = com.meshlink.util.MeshIdNormalizer.canonicalize(canonicalPeerId)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val shortId = digest.digest(normId.toByteArray(Charsets.UTF_8)).copyOf(8).joinToString("") { "%02x".format(it) }

        val peerUser = UserEntity(canonicalPeerId, "Durga Prasad")
        coEvery { localDataSource.getUser(shortId) } returns null
        coEvery { localDataSource.getUser(any()) } returns null
        coEvery { localDataSource.getAllUsers() } returns listOf(peerUser)
        every { identityManager.getOrCreateIdentity() } returns com.meshlink.trust.MeshIdentity(
            meshId = "mesh-local-456",
            publicKey = "pk",
            displayName = "Local User"
        )

        val resolved = userRepository.getUserDisplayName(shortId)

        assertEquals("Durga Prasad", resolved)
    }
}
