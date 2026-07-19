package com.meshlink.core.data

import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.model.User

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.security.data.source.CryptoDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private lateinit var localDataSource: UserLocalDataSource
    private lateinit var cryptoDataSource: CryptoDataSource
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setup() {
        localDataSource = mockk(relaxed = true)
        cryptoDataSource = mockk(relaxed = true)
        repository = UserRepositoryImpl(localDataSource, cryptoDataSource)
    }

    @Test
    fun `registerUser should generate hashes and insert user`() = runTest {
        val name = "John Doe"
        val phone = "1234567890"
        val pin = "1234"
        
        coEvery { cryptoDataSource.generateLegacyHash(phone) } returns "mesh_id_1"
        coEvery { cryptoDataSource.generateSalt() } returns "salt_1"
        coEvery { cryptoDataSource.generateSaltedHash(pin, "salt_1") } returns "hash_1"

        val result = repository.registerUser(name, phone, pin)

        assertTrue(result.isSuccess)
        assertEquals("mesh_id_1", result.getOrNull())
        
        coVerify(exactly = 1) { 
            localDataSource.insertUser(match { 
                it.meshId == "mesh_id_1" && 
                it.name == name && 
                it.phoneNumber == phone && 
                it.pinHash == "salt_1:hash_1"
            }) 
        }
        coVerify(exactly = 1) { localDataSource.setLoginState(true) }
    }

    @Test
    fun `loginUser should succeed with valid pin (new format)`() = runTest {
        val phone = "1234567890"
        val pin = "1234"
        
        coEvery { cryptoDataSource.generateLegacyHash(phone) } returns "mesh_id_1"
        val mockUser = UserEntity("mesh_id_1", "John", phone, "salt_1:hash_1")
        coEvery { localDataSource.getUser("mesh_id_1") } returns mockUser
        coEvery { cryptoDataSource.generateSaltedHash(pin, "salt_1") } returns "hash_1"

        val result = repository.loginUser(phone, pin)

        assertTrue(result.isSuccess)
        assertEquals(User(mockUser.meshId, mockUser.name, mockUser.phoneNumber), result.getOrNull())
        coVerify(exactly = 1) { localDataSource.setLoginState(true) }
    }

    @Test
    fun `loginUser should fail with invalid pin`() = runTest {
        val phone = "1234567890"
        val pin = "wrong_pin"
        
        coEvery { cryptoDataSource.generateLegacyHash(phone) } returns "mesh_id_1"
        val mockUser = UserEntity("mesh_id_1", "John", phone, "salt_1:hash_1")
        coEvery { localDataSource.getUser("mesh_id_1") } returns mockUser
        coEvery { cryptoDataSource.generateSaltedHash(pin, "salt_1") } returns "wrong_hash"

        val result = repository.loginUser(phone, pin)

        assertFalse(result.isSuccess)
        coVerify(exactly = 0) { localDataSource.setLoginState(true) }
    }
    
    @Test
    fun `loginUser should fail if user not found`() = runTest {
        val phone = "1234567890"
        
        coEvery { cryptoDataSource.generateLegacyHash(phone) } returns "mesh_id_1"
        coEvery { localDataSource.getUser("mesh_id_1") } returns null

        val result = repository.loginUser(phone, "salt_1:hash_1")

        assertFalse(result.isSuccess)
    }

    @Test
    fun `logout should set login state to false`() = runTest {
        repository.logout()
        coVerify(exactly = 1) { localDataSource.setLoginState(false) }
    }
}
