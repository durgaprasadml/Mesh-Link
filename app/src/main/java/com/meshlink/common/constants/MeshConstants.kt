package com.meshlink.common.constants



object MeshConstants {
    const val NETWORK_ID_LENGTH = 8
    
    fun toNetworkId(meshId: String): String {
        return meshId.trim().take(NETWORK_ID_LENGTH)
    }
}
