package com.meshlink.core.domain



sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError, val exception: Throwable? = null) : AppResult<Nothing>()
}

sealed interface AppError {
    enum class Network : AppError { TIMEOUT, NO_CONNECTION, UNKNOWN }
    enum class Ble : AppError { DISCONNECTED, GATT_ERROR, UNKNOWN, DEVICE_NOT_FOUND }
    enum class Crypto : AppError { KEY_NOT_FOUND, DECRYPTION_FAILED, SIGNATURE_INVALID, UNKNOWN }
    enum class Database : AppError { INSERT_FAILED, QUERY_FAILED, UNKNOWN }
    enum class General : AppError { UNKNOWN }
}
