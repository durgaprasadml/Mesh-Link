package com.meshlink.security.api

import com.meshlink.domain.model.MeshResult

/**
 * Provides cryptographic operations for mesh communication.
 *
 * Responsibility: Handle encryption, decryption, and key management abstraction.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe.
 * Return Contract: Cryptographic operations return [MeshResult] to indicate success or failure.
 * Failure Conditions: Missing keys, invalid ciphertext, unsupported algorithm.
 */
interface CryptoProvider {
    @Deprecated("Use encryptData instead", ReplaceWith("encryptData(peerId, data)"))
    suspend fun encrypt(data: ByteArray, peerId: String): ByteArray

    /**
     * Encrypts data for a specific peer.
     *
     * @param peerId The ID of the target peer.
     * @param data The plaintext data to encrypt.
     * @return [MeshResult.Success] containing the ciphertext, or [MeshResult.Error] on failure.
     */
    suspend fun encryptData(peerId: String, data: ByteArray): MeshResult<ByteArray>

    @Deprecated("Use decryptData instead", ReplaceWith("decryptData(peerId, data)"))
    suspend fun decrypt(data: ByteArray, peerId: String): ByteArray

    /**
     * Decrypts data from a specific peer.
     *
     * @param peerId The ID of the sending peer.
     * @param data The ciphertext to decrypt.
     * @return [MeshResult.Success] containing the plaintext, or [MeshResult.Error] on failure.
     */
    suspend fun decryptData(peerId: String, data: ByteArray): MeshResult<ByteArray>
}
