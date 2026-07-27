package com.meshlink.media.api

import com.meshlink.domain.model.MeshResult

/**
 * Handles the transfer of large media files across the mesh.
 *
 * Responsibility: Manage file reading, chunking, and delegating to router/transport.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe for concurrent transfers.
 * Return Contract: Media operations return [MeshResult] to indicate success or failure.
 * Failure Conditions: File not found, permission denied, target unreachable.
 */
interface MediaTransfer {
    @Deprecated("Use transferMedia instead", ReplaceWith("transferMedia(peerId, uri)"))
    suspend fun sendMedia(uri: String, peerId: String)

    /**
     * Transfers media content to a specific peer.
     *
     * @param peerId The target peer's ID.
     * @param uri The local URI of the media to send.
     * @return [MeshResult.Success] on successful transfer, [MeshResult.Error] on failure.
     */
    suspend fun transferMedia(peerId: String, uri: String): MeshResult<Unit>
}
