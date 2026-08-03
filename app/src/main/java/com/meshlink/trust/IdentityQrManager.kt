package com.meshlink.trust

import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

data class QrIdentityPayload(
    val meshId: String,
    val publicKey: String,
    val fingerprint: String,
    val displayName: String,
    val protocolVersion: Int = 1,
    val signature: String = ""
) {
    fun toJson(): String {
        return """{"meshId":"$meshId","publicKey":"$publicKey","fingerprint":"$fingerprint","displayName":"$displayName","protocolVersion":$protocolVersion,"signature":"$signature"}"""
    }

    companion object {
        fun fromJson(jsonStr: String): QrIdentityPayload {
            val meshId = extractValue(jsonStr, "meshId")
            val publicKey = extractValue(jsonStr, "publicKey")
            val fingerprint = extractValue(jsonStr, "fingerprint")
            val displayName = extractValue(jsonStr, "displayName")
            val protocolVersion = extractValue(jsonStr, "protocolVersion").toIntOrNull() ?: 1
            val signature = extractValue(jsonStr, "signature")

            return QrIdentityPayload(
                meshId = meshId,
                publicKey = publicKey,
                fingerprint = fingerprint,
                displayName = if (displayName.isEmpty()) "User" else displayName,
                protocolVersion = protocolVersion,
                signature = signature
            )
        }

        private fun extractValue(json: String, key: String): String {
            val pattern = """"$key"\s*:\s*("(.*?)"|(\d+))""".toRegex()
            val match = pattern.find(json) ?: return ""
            return match.groupValues[2].ifEmpty { match.groupValues[3] }
        }
    }
}

/**
 * Offline QR identity export and exchange manager.
 */
@Singleton
class IdentityQrManager @Inject constructor(
    private val identityManager: MeshIdentityManager,
    private val verificationManager: IdentityVerificationManager
) {
    companion object {
        private const val TAG = "IdentityQrManager"
    }

    /**
     * Generates QR payload string for local Mesh identity.
     */
    fun generateLocalQrPayload(): String {
        val identity = identityManager.getOrCreateIdentity()
        val fingerprint = IdentityFingerprint.compute(identity.publicKey)

        val payload = QrIdentityPayload(
            meshId = identity.meshId,
            publicKey = identity.publicKey,
            fingerprint = fingerprint,
            displayName = identity.displayName,
            protocolVersion = identity.identityVersion,
            signature = identity.signature
        )
        return payload.toJson()
    }

    /**
     * Parses scanned QR payload and verifies target identity offline.
     */
    fun processScannedQr(qrData: String): VerificationResult {
        return try {
            val payload = QrIdentityPayload.fromJson(qrData)
            val identity = MeshIdentity(
                meshId = payload.meshId,
                publicKey = payload.publicKey,
                identityVersion = payload.protocolVersion,
                displayName = payload.displayName,
                signature = payload.signature
            )

            verificationManager.verifyIdentity(
                targetIdentity = identity,
                method = VerificationMethod.QR_CODE,
                targetLevel = TrustLevel.VERIFIED,
                notes = "Scanned QR exchange payload"
            )
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to parse or verify scanned QR payload: ${e.message}")
            VerificationResult.Failure("Malformed QR payload: ${e.message}")
        }
    }
}
