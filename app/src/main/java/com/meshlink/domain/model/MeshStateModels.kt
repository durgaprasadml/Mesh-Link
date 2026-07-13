package com.meshlink.domain.model

enum class MeshConnectionState {
    DISABLED,
    SCANNING,
    ADVERTISING,
    CONNECTING,
    CONNECTED,
    MESH_READY,
    OFFLINE,
    RECOVERING
}

enum class MediaTransferState {
    QUEUED,
    PREPARING,
    COMPRESSING,
    ENCRYPTING,
    SENDING,
    RECEIVING,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class SecurityState {
    UNVERIFIED,
    AUTHENTICATING,
    SECURE_SESSION,
    REKEYING,
    VERIFIED,
    BLOCKED
}
