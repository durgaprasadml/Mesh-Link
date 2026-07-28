package com.meshlink.domain.model

sealed interface DispatchResult {
    data object Queued : DispatchResult
    data object NoPeers : DispatchResult
    data object QueueFull : DispatchResult
    data object Rejected : DispatchResult
    data class Error(val cause: Throwable) : DispatchResult
}
