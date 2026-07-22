package com.meshlink.common.constants



object MeshConstants {
    const val NETWORK_ID_LENGTH = 8
    
    fun toNetworkId(meshId: String): String {
        return com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
    }
}
