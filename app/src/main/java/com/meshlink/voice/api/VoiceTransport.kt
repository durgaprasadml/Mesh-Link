package com.meshlink.voice.api

import com.meshlink.domain.model.MeshResult

/**
 * Handles real-time voice call signaling and transport.
 *
 * Responsibility: Initiate, accept, and terminate voice calls.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe for concurrent operations.
 * Return Contract: Call operations return [MeshResult] to indicate success or failure.
 * Failure Conditions: Peer unavailable, timeout, hardware error.
 */
interface VoiceTransport {
    @Deprecated("Use initiateVoiceCall instead", ReplaceWith("initiateVoiceCall(peerId)"))
    suspend fun startVoiceCall(peerId: String)

    /**
     * Initiates a voice call with a target peer.
     *
     * @param peerId The target peer ID.
     * @return [MeshResult.Success] on success, [MeshResult.Error] on failure.
     */
    suspend fun initiateVoiceCall(peerId: String): MeshResult<Unit>

    @Deprecated("Use terminateVoiceCall instead", ReplaceWith("terminateVoiceCall(peerId)"))
    suspend fun endVoiceCall(peerId: String)

    /**
     * Terminates an ongoing voice call with a peer.
     *
     * @param peerId The target peer ID.
     * @return [MeshResult.Success] on success, [MeshResult.Error] on failure.
     */
    suspend fun terminateVoiceCall(peerId: String): MeshResult<Unit>
}
