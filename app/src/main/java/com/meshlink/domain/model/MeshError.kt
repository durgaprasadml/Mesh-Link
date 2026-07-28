package com.meshlink.domain.model

sealed class MeshError(open val message: String, open val cause: Throwable? = null) {
    data class TransportError(
        override val message: String,
        val deviceAddress: String? = null,
        val reasonCode: Int? = null,
        override val cause: Throwable? = null
    ) : MeshError(message, cause)

    data class RoutingError(
        override val message: String,
        val targetMeshId: String? = null,
        override val cause: Throwable? = null
    ) : MeshError(message, cause)

    data class SecurityError(
        override val message: String,
        val peerId: String? = null,
        override val cause: Throwable? = null
    ) : MeshError(message, cause)

    data class MediaError(
        override val message: String,
        val uri: String? = null,
        override val cause: Throwable? = null
    ) : MeshError(message, cause)

    data class ValidationError(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null
    ) : MeshError(message, cause)

    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : MeshError(message, cause)
}
