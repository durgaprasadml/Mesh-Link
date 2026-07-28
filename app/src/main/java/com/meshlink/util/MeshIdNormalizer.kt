package com.meshlink.util

object MeshIdNormalizer {
    private const val CANONICAL_LENGTH = 8

    /**
     * Canonicalizes a Mesh ID, Peer ID, or MAC Address for routing and identification.
     * Ensures consistent representation across transport, database, and UI layers.
     */
    fun canonicalize(id: String): String {
        val cleaned = id.replace(":", "").trim().uppercase()
        return if (cleaned.length > CANONICAL_LENGTH) {
            cleaned.takeLast(CANONICAL_LENGTH)
        } else {
            cleaned
        }
    }
}
