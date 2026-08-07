package com.meshlink.common.oem

import android.os.Build
import com.meshlink.common.logger.MeshLogger
import javax.inject.Inject
import javax.inject.Singleton

enum class OemVendor {
    SAMSUNG,
    XIAOMI,
    OPPO,
    VIVO,
    HUAWEI,
    ONEPLUS,
    MOTOROLA,
    NOTHING,
    PIXEL,
    UNKNOWN
}

data class OemStrategy(
    val vendor: OemVendor,
    val needsScannerPeriodRefresh: Boolean = false,
    val needsGattResetWorkaround: Boolean = false,
    val requiresAutostartPermissionPrompt: Boolean = false,
    val strictFgsPolicy: Boolean = false
)

@Singleton
class OemCompatibilityManager @Inject constructor() {

    companion object {
        private const val TAG = "OemCompatibility"
    }

    val currentVendor: OemVendor by lazy {
        val manufacturer = try { Build.MANUFACTURER?.lowercase() ?: "" } catch (_: Exception) { "" }
        when {
            manufacturer.contains("samsung") -> OemVendor.SAMSUNG
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> OemVendor.XIAOMI
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> OemVendor.OPPO
            manufacturer.contains("vivo") -> OemVendor.VIVO
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> OemVendor.HUAWEI
            manufacturer.contains("oneplus") -> OemVendor.ONEPLUS
            manufacturer.contains("motorola") -> OemVendor.MOTOROLA
            manufacturer.contains("nothing") -> OemVendor.NOTHING
            manufacturer.contains("google") -> OemVendor.PIXEL
            else -> OemVendor.UNKNOWN
        }
    }

    val strategy: OemStrategy by lazy {
        when (currentVendor) {
            OemVendor.SAMSUNG -> OemStrategy(
                vendor = currentVendor,
                needsScannerPeriodRefresh = true
            )
            OemVendor.XIAOMI -> OemStrategy(
                vendor = currentVendor,
                requiresAutostartPermissionPrompt = true,
                needsScannerPeriodRefresh = true
            )
            OemVendor.OPPO, OemVendor.VIVO, OemVendor.HUAWEI -> OemStrategy(
                vendor = currentVendor,
                requiresAutostartPermissionPrompt = true,
                needsScannerPeriodRefresh = true
            )
            OemVendor.ONEPLUS -> OemStrategy(
                vendor = currentVendor,
                needsGattResetWorkaround = true
            )
            OemVendor.PIXEL -> OemStrategy(
                vendor = currentVendor,
                strictFgsPolicy = true
            )
            OemVendor.MOTOROLA, OemVendor.NOTHING, OemVendor.UNKNOWN -> OemStrategy(
                vendor = currentVendor
            )
        }
    }

    fun applyVendorOptimizations() {
        MeshLogger.d(TAG, "Applied OEM strategies for vendor: $currentVendor -> $strategy")
    }
}
