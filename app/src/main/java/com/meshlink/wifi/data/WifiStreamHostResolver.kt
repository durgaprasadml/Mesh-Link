package com.meshlink.wifi.data

import com.meshlink.util.MeshIdNormalizer

/**
 * Picks which connected Wi-Fi Direct socket a media transfer must be written to.
 *
 * Connected sockets are keyed by remote IP address while transfers are addressed by Mesh ID,
 * so a lookup is required. The previous image/SOS streaming code fell back to writing the file to
 * EVERY connected socket whenever the Mesh ID was not itself a socket key (which was always), which
 * delivered private images to unrelated peers and produced duplicate/foreign SOS attachments.
 *
 * This resolver never fans out: it returns exactly one host or null.
 */
object WifiStreamHostResolver {

    fun resolve(
        requested: String?,
        connectedHosts: Set<String>,
        peerHostsByMeshId: Map<String, String>,
        canonicalize: (String) -> String = MeshIdNormalizer::canonicalize
    ): String? {
        if (connectedHosts.isEmpty()) return null

        if (requested.isNullOrBlank()) {
            return connectedHosts.singleOrNull()
        }

        if (requested in connectedHosts) return requested

        val canonicalRequested = canonicalize(requested)
        val mapped = peerHostsByMeshId[requested] ?: peerHostsByMeshId[canonicalRequested]
        if (mapped != null && mapped in connectedHosts) return mapped

        // Unknown target. A single live socket is unambiguous (the 2-device Wi-Fi Direct group),
        // but only if that socket has not already been identified as a *different* peer.
        val soleHost = connectedHosts.singleOrNull() ?: return null
        val claimedByOtherPeer = peerHostsByMeshId.any { (meshId, host) ->
            host == soleHost && canonicalize(meshId) != canonicalRequested
        }
        return if (claimedByOtherPeer) null else soleHost
    }
}
