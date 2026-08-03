package com.meshlink.routing.engine

import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.RouteType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Log levels supported for structured transport diagnostic output.
 */
enum class DiagnosticLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Structured, privacy-preserving transport logger.
 * Sanitizes log output to ensure no encrypted payloads, text contents, or secret keys are ever logged.
 */
@Singleton
class TransportDiagnostics @Inject constructor() {

    companion object {
        private const val TAG = "TransportDiagnostics"
        var isEnabled: Boolean = true
        var minLogLevel: DiagnosticLogLevel = DiagnosticLogLevel.DEBUG
    }

    fun logTransportSelection(
        packetId: String,
        packetType: PacketType,
        category: TransportCategory,
        selectedRoute: RouteType,
        reason: String
    ) {
        log(
            DiagnosticLogLevel.DEBUG,
            "SELECT | PktID=$packetId | Type=$packetType | Cat=$category | SelectedRoute=$selectedRoute | Reason=$reason"
        )
    }

    fun logTransportFallback(
        packetId: String,
        packetType: PacketType,
        primaryRoute: RouteType,
        fallbackRoute: RouteType,
        reason: String
    ) {
        log(
            DiagnosticLogLevel.WARN,
            "FALLBACK | PktID=$packetId | Type=$packetType | Primary=$primaryRoute -> Fallback=$fallbackRoute | Reason=$reason"
        )
    }

    fun logTransportUnavailable(
        packetId: String,
        packetType: PacketType,
        requestedRoute: RouteType,
        reason: String
    ) {
        log(
            DiagnosticLogLevel.WARN,
            "UNAVAILABLE | PktID=$packetId | Type=$packetType | Requested=$requestedRoute | Reason=$reason"
        )
    }

    fun logTransferStart(
        transferId: String,
        fileName: String,
        totalBytes: Long,
        transport: RouteType
    ) {
        // Sanitize filename to avoid logging sensitive user PII (only log extension / file size)
        val ext = fileName.substringAfterLast('.', "bin")
        log(
            DiagnosticLogLevel.INFO,
            "TRANSFER_START | XferID=$transferId | Type=.$ext | Size=${totalBytes}B | Transport=$transport"
        )
    }

    fun logTransferCompletion(
        transferId: String,
        totalBytes: Long,
        durationMs: Long,
        avgThroughputBps: Double
    ) {
        log(
            DiagnosticLogLevel.INFO,
            "TRANSFER_COMPLETE | XferID=$transferId | Size=${totalBytes}B | Duration=${durationMs}ms | Speed=${avgThroughputBps.toLong()}B/s"
        )
    }

    fun logRetry(
        packetId: String,
        packetType: PacketType,
        attempt: Int,
        transport: RouteType,
        reason: String
    ) {
        log(
            DiagnosticLogLevel.WARN,
            "RETRY | PktID=$packetId | Type=$packetType | Attempt=$attempt | Transport=$transport | Reason=$reason"
        )
    }

    fun logFailure(
        packetId: String,
        packetType: PacketType,
        transport: RouteType,
        reason: String
    ) {
        log(
            DiagnosticLogLevel.ERROR,
            "FAILURE | PktID=$packetId | Type=$packetType | Transport=$transport | Reason=$reason"
        )
    }

    private fun log(level: DiagnosticLogLevel, message: String) {
        if (!isEnabled || level.ordinal < minLogLevel.ordinal) return

        when (level) {
            DiagnosticLogLevel.DEBUG -> MeshLogger.d(TAG, message)
            DiagnosticLogLevel.INFO -> MeshLogger.i(TAG, message)
            DiagnosticLogLevel.WARN -> MeshLogger.w(TAG, message)
            DiagnosticLogLevel.ERROR -> MeshLogger.e(TAG, message)
        }
    }
}
