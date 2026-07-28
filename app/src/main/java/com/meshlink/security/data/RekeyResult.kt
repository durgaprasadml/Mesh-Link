package com.meshlink.security.data

/**
 * Explicit results for the database rekey operation.
 */
sealed class RekeyResult {
    object Success : RekeyResult()
    object MigrationNotRequired : RekeyResult()
    object InvalidKey : RekeyResult()
    data class RekeyFailed(val reason: String) : RekeyResult()
    data class VerificationFailed(val reason: String) : RekeyResult()
    object UnsupportedVersion : RekeyResult()
    data class DatabaseCorrupted(val reason: String) : RekeyResult()
}
