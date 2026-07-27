package com.meshlink.domain.model

sealed class MeshResult<out T> {
    data class Success<out T>(val data: T) : MeshResult<T>()
    data class Error(val error: MeshError) : MeshResult<Nothing>()
}

inline fun <T, R> MeshResult<T>.map(transform: (T) -> R): MeshResult<R> {
    return when (this) {
        is MeshResult.Success -> MeshResult.Success(transform(data))
        is MeshResult.Error -> this
    }
}
