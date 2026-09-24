package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.model.User
import com.meshlink.database.data.local.UserEntity
import com.meshlink.trust.MeshIdentityManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val identityManager: MeshIdentityManager,
    private val discoveryEngineProvider: javax.inject.Provider<com.meshlink.ble.discovery.DiscoveryEngine>? = null
) : UserRepository {

    companion object {
        private val GENERIC_NAMES = setOf(
            "man", "device", "peer", "nearby node", "unknown", "unknown user", "android", "null", "-", "user", "mesh peer", "unknown mesh node"
        )

        fun isGenericOrInvalidName(name: String?, meshIdOrAddress: String? = null): Boolean {
            if (name.isNullOrBlank()) return true
            val trimmed = name.trim().lowercase()
            if (GENERIC_NAMES.contains(trimmed)) return true
            if (meshIdOrAddress != null) {
                val canonicalTarget = com.meshlink.util.MeshIdNormalizer.canonicalize(meshIdOrAddress).lowercase()
                if (trimmed == canonicalTarget || trimmed == meshIdOrAddress.trim().lowercase()) return true
            }
            return false
        }
    }

    override val hasProfile: Flow<Boolean> = localDataSource.hasProfile

    override val localUser: Flow<User?> = run {
        val identity = identityManager.getOrCreateIdentity()
        val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(identity.meshId)
        localDataSource.observeUser(canonicalId).map { entity ->
            val finalEntity = entity ?: localDataSource.getUser(identity.meshId)
            if (finalEntity != null) {
                User(
                    meshId = finalEntity.meshId,
                    name = finalEntity.name.ifBlank { identity.displayName },
                    avatarUri = finalEntity.avatarUri,
                    aboutMe = finalEntity.aboutMe,
                    profilePhotoPath = finalEntity.profilePhotoPath,
                    profilePhotoHash = finalEntity.profilePhotoHash,
                    profilePhotoVersion = finalEntity.profilePhotoVersion,
                    profileLastUpdated = finalEntity.profileLastUpdated
                )
            } else if (identity.displayName.isNotBlank()) {
                User(
                    meshId = identity.meshId,
                    name = identity.displayName
                )
            } else {
                null
            }
        }
    }
    
    private val displayNameCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val profileCache = java.util.concurrent.ConcurrentHashMap<String, User>()

    private fun invalidateCaches() {
        displayNameCache.clear()
        profileCache.clear()
    }

    @Deprecated("Use setupProfile instead", ReplaceWith("setupProfile(name, avatarUri)"))
    override suspend fun createProfile(name: String, avatarUri: String?): Result<Unit> {
        return try {
            invalidateCaches()
            val identity = identityManager.getOrCreateIdentity()
            identityManager.updateDisplayName(name)
            val user = UserEntity(meshId = identity.meshId, name = name, avatarUri = avatarUri)
            localDataSource.insertUser(user)
            localDataSource.setProfileCreated(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setupProfile(name: String, avatarUri: String?): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            invalidateCaches()
            val identity = identityManager.getOrCreateIdentity()
            identityManager.updateDisplayName(name)
            val user = UserEntity(meshId = identity.meshId, name = name, avatarUri = avatarUri)
            localDataSource.insertUser(user)
            localDataSource.setProfileCreated(true)
            com.meshlink.common.logger.MeshLogger.i("UserRepository", "[MeshStartup] IDENTITY_READY: MeshID=${identity.meshId}, Name=$name")
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.UnknownError("Failed to setup profile", e))
        }
    }

    override val isEncryptionEnabled: Flow<Boolean> = localDataSource.isEncryptionEnabled
    
    override suspend fun setEncryptionEnabled(enabled: Boolean) {
        localDataSource.setEncryptionEnabled(enabled)
    }
    
    override val isOnlineVisible: Flow<Boolean> = localDataSource.isOnlineVisible
    
    override suspend fun setOnlineVisible(visible: Boolean) {
        localDataSource.setOnlineVisible(visible)
    }
    
    override val meshMode: Flow<String> = localDataSource.meshMode
    
    override suspend fun setMeshMode(mode: String) {
        localDataSource.setMeshMode(mode)
    }

    private fun findMatchingUserEntity(allUsers: List<UserEntity>, targetMeshId: String): UserEntity? {
        if (targetMeshId.isBlank()) return null
        val targetTrimmed = targetMeshId.trim()
        val targetNorm = com.meshlink.util.MeshIdNormalizer.canonicalize(targetTrimmed).lowercase()
        val targetLower = targetTrimmed.lowercase()

        return allUsers.firstOrNull { user ->
            val userMeshId = user.meshId.trim()
            if (userMeshId.equals(targetTrimmed, ignoreCase = true)) return@firstOrNull true
            val userNorm = com.meshlink.util.MeshIdNormalizer.canonicalize(userMeshId).lowercase()
            if (userNorm == targetNorm || userNorm == targetLower || userMeshId.lowercase() == targetNorm) return@firstOrNull true
            
            // Check SHA-256 shortMeshId match (first 8 bytes hex)
            try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(com.meshlink.util.MeshIdNormalizer.canonicalize(userMeshId).toByteArray(Charsets.UTF_8))
                val shortIdHex = hashBytes.copyOf(8).joinToString("") { b -> "%02x".format(b) }
                shortIdHex.equals(targetLower, ignoreCase = true) || shortIdHex.endsWith(targetLower, ignoreCase = true)
            } catch (_: Exception) {
                false
            }
        }
    }

    override suspend fun getLocalUser(): User? {
        val identity = identityManager.getOrCreateIdentity()
        val canonicalId = com.meshlink.util.MeshIdNormalizer.canonicalize(identity.meshId)
        val userEntity = localDataSource.getUser(canonicalId)
            ?: localDataSource.getUser(identity.meshId)
            ?: (localDataSource.getAllUsers().firstOrNull { it.meshId == canonicalId || it.meshId == identity.meshId })

        return if (userEntity != null) {
            User(
                meshId = userEntity.meshId,
                name = userEntity.name.ifBlank { identity.displayName },
                avatarUri = userEntity.avatarUri,
                aboutMe = userEntity.aboutMe,
                profilePhotoPath = userEntity.profilePhotoPath,
                profilePhotoHash = userEntity.profilePhotoHash,
                profilePhotoVersion = userEntity.profilePhotoVersion,
                profileLastUpdated = userEntity.profileLastUpdated
            )
        } else if (identity.displayName.isNotBlank()) {
            User(
                meshId = identity.meshId,
                name = identity.displayName
            )
        } else {
            null
        }
    }

    override suspend fun updateUserName(name: String) {
        invalidateCaches()
        val identity = identityManager.getOrCreateIdentity()
        val userEntity = localDataSource.getUser(identity.meshId) ?: localDataSource.getLocalUser()
        if (userEntity != null) {
            localDataSource.insertUser(userEntity.copy(name = name))
            identityManager.updateDisplayName(name)
        }
    }

    override suspend fun updateProfile(name: String, aboutMe: String?, avatarUri: String?) {
        invalidateCaches()
        val identity = identityManager.getOrCreateIdentity()
        val userEntity = localDataSource.getUser(identity.meshId) ?: localDataSource.getLocalUser()
        if (userEntity != null) {
            localDataSource.insertUser(userEntity.copy(name = name, aboutMe = aboutMe, avatarUri = avatarUri))
            identityManager.updateDisplayName(name)
        }
    }

    override suspend fun getUserDisplayName(meshId: String): String {
        if (meshId.isBlank()) return "Mesh Peer"
        displayNameCache[meshId]?.let { return it }
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        if (canonicalTargetId != meshId) {
            displayNameCache[canonicalTargetId]?.let { return it }
        }
        val localUser = getLocalUser()
        if (localUser != null) {
            val localCanonical = com.meshlink.util.MeshIdNormalizer.canonicalize(localUser.meshId)
            val localDigest = java.security.MessageDigest.getInstance("SHA-256")
            val localHashBytes = localDigest.digest(localCanonical.toByteArray(Charsets.UTF_8))
            val localShortId = localHashBytes.copyOf(8).joinToString("") { b -> "%02x".format(b) }

            if (localCanonical == canonicalTargetId || localShortId.equals(meshId, ignoreCase = true) || localUser.meshId.equals(meshId, ignoreCase = true)) {
                val localName = localUser.name.trim()
                if (!isGenericOrInvalidName(localName, canonicalTargetId)) {
                    displayNameCache[meshId] = localName
                    displayNameCache[canonicalTargetId] = localName
                    return localName
                }
                return "Mesh Peer"
            }
        }
        var userEntity = localDataSource.getUser(meshId) ?: localDataSource.getUser(canonicalTargetId)
        if (userEntity == null) {
            val allUsers = localDataSource.getAllUsers()
            userEntity = findMatchingUserEntity(allUsers, meshId)
        }
        val name = userEntity?.name?.trim()
        if (!name.isNullOrBlank() && !isGenericOrInvalidName(name, canonicalTargetId)) {
            displayNameCache[meshId] = name
            displayNameCache[canonicalTargetId] = name
            return name
        }
        // Do NOT cache generic or unresolved placeholder names!
        return "Mesh Peer"
    }

    override suspend fun getUserProfile(meshId: String): User? {
        if (meshId.isBlank()) return null
        profileCache[meshId]?.let { return it }
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        if (canonicalTargetId != meshId) {
            profileCache[canonicalTargetId]?.let { return it }
        }
        var entity = localDataSource.getUser(meshId) ?: localDataSource.getUser(canonicalTargetId)
        if (entity == null) {
            val allUsers = localDataSource.getAllUsers()
            entity = findMatchingUserEntity(allUsers, meshId)
        }
        val result = entity?.let {
            User(
                meshId = it.meshId,
                name = it.name,
                avatarUri = it.avatarUri,
                aboutMe = it.aboutMe,
                profilePhotoPath = it.profilePhotoPath,
                profilePhotoHash = it.profilePhotoHash,
                profilePhotoVersion = it.profilePhotoVersion,
                profileLastUpdated = it.profileLastUpdated
            )
        }
        if (result != null) {
            profileCache[meshId] = result
            profileCache[canonicalTargetId] = result
        }
        return result
    }

    override fun observeUserProfile(meshId: String): Flow<User?> {
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        return localDataSource.observeUser(canonicalTargetId).map { entity ->
            val finalEntity = entity ?: run {
                val allUsers = localDataSource.getAllUsers()
                findMatchingUserEntity(allUsers, meshId)
            }
            finalEntity?.let {
                User(
                    meshId = it.meshId,
                    name = it.name,
                    avatarUri = it.avatarUri,
                    aboutMe = it.aboutMe,
                    profilePhotoPath = it.profilePhotoPath,
                    profilePhotoHash = it.profilePhotoHash,
                    profilePhotoVersion = it.profilePhotoVersion,
                    profileLastUpdated = it.profileLastUpdated
                )
            }
        }
    }

    override fun observeAllUsers(): Flow<List<User>> = localDataSource.observeAllUsers().map { entities ->
        entities.map { entity ->
            User(
                meshId = entity.meshId,
                name = entity.name,
                avatarUri = entity.avatarUri,
                aboutMe = entity.aboutMe,
                profilePhotoPath = entity.profilePhotoPath,
                profilePhotoHash = entity.profilePhotoHash,
                profilePhotoVersion = entity.profilePhotoVersion,
                profileLastUpdated = entity.profileLastUpdated
            )
        }
    }

    override suspend fun saveOrUpdatePeerProfile(
        meshId: String,
        name: String,
        publicKey: String?,
        lastSeen: Long,
        rssi: Int
    ) {
        if (meshId.isBlank()) return
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        val cleanName = name.trim()
        if (isGenericOrInvalidName(cleanName, canonicalTargetId) || isGenericOrInvalidName(cleanName, meshId)) {
            return
        }

        val existingUser = localDataSource.getUser(canonicalTargetId)
            ?: localDataSource.getUser(meshId)
            ?: findMatchingUserEntity(localDataSource.getAllUsers(), meshId)

        if (existingUser != null) {
            val updated = existingUser.copy(
                name = cleanName,
                publicKey = publicKey ?: existingUser.publicKey,
                lastSeen = lastSeen,
                rssi = if (rssi != 0) rssi else existingUser.rssi
            )
            localDataSource.insertUser(updated)
            if (existingUser.meshId != canonicalTargetId) {
                localDataSource.insertUser(updated.copy(meshId = canonicalTargetId))
            }
        } else {
            val newUser = UserEntity(
                meshId = canonicalTargetId,
                name = cleanName,
                publicKey = publicKey,
                lastSeen = lastSeen,
                rssi = rssi
            )
            localDataSource.insertUser(newUser)
            if (meshId != canonicalTargetId) {
                localDataSource.insertUser(newUser.copy(meshId = meshId))
            }
        }

        // Immediately update in-memory caches
        displayNameCache[canonicalTargetId] = cleanName
        displayNameCache[meshId] = cleanName
        profileCache.remove(canonicalTargetId)
        profileCache.remove(meshId)

        // Notify discovery engine if available
        try {
            discoveryEngineProvider?.get()?.updatePeerIdentity(canonicalTargetId, cleanName, canonicalTargetId)
            if (meshId != canonicalTargetId) {
                discoveryEngineProvider?.get()?.updatePeerIdentity(meshId, cleanName, canonicalTargetId)
            }
        } catch (_: Exception) {
            // Optional integration
        }
    }

    override suspend fun updateProfilePhoto(meshId: String, photoPath: String, photoHash: String, version: Long, lastUpdated: Long) {
        invalidateCaches()
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        val existingUser = localDataSource.getUser(canonicalTargetId) ?: localDataSource.getUser(meshId)
        if (existingUser != null) {
            localDataSource.updateProfilePhoto(existingUser.meshId, photoPath, photoHash, version, lastUpdated)
        } else {
            val newUser = UserEntity(
                meshId = canonicalTargetId,
                name = "User",
                profilePhotoPath = photoPath,
                profilePhotoHash = photoHash,
                profilePhotoVersion = version,
                profileLastUpdated = lastUpdated
            )
            localDataSource.insertUser(newUser)
        }
    }
}
