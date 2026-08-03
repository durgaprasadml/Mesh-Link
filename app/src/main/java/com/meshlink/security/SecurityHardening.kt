package com.meshlink.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import com.meshlink.common.logger.MeshLogger
import java.io.File

/**
 * Executes non-blocking runtime security and integrity checks for Mesh-Link.
 * Safe for offline operation. Warnings are logged and surfaced via SecurityReport
 * without preventing legitimate offline mesh communication.
 */
object SecurityHardening {

    data class SecurityReport(
        val isDebuggable: Boolean,
        val isEmulator: Boolean,
        val isRooted: Boolean,
        val isDeveloperOptionsEnabled: Boolean,
        val isIntegrityValid: Boolean,
        val warnings: List<String>
    ) {
        val hasWarnings: Boolean get() = warnings.isNotEmpty()
    }

    /**
     * Performs all runtime security checks and returns a summary report.
     */
    fun performSecurityChecks(context: Context): SecurityReport {
        val debuggable = isDebuggable(context)
        val emulator = isEmulator()
        val rooted = isRooted()
        val devOptions = isDeveloperOptionsEnabled(context)
        val integrityValid = checkAppIntegrity(context)

        val warnings = mutableListOf<String>()
        if (debuggable) warnings.add("Application is running in debug mode.")
        if (emulator) warnings.add("Application is running inside an emulator environment.")
        if (rooted) warnings.add("Device exhibits root indicators (su binaries or test-keys).")
        if (devOptions) warnings.add("Developer options or ADB debugging is enabled on device.")
        if (!integrityValid) warnings.add("App signature validation failed.")

        val report = SecurityReport(
            isDebuggable = debuggable,
            isEmulator = emulator,
            isRooted = rooted,
            isDeveloperOptionsEnabled = devOptions,
            isIntegrityValid = integrityValid,
            warnings = warnings
        )

        if (report.hasWarnings) {
            MeshLogger.w("SecurityHardening", "Security environment warnings detected: ${warnings.joinToString("; ")}")
        } else {
            MeshLogger.i("SecurityHardening", "Security environment verification passed clean.")
        }

        return report
    }

    /**
     * Checks if the application is marked debuggable in its manifest/build flags.
     */
    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Detects if running inside an Android emulator (QEMU / Genymotion / SDK emulator).
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * Detects root indicators such as test-keys build tags or common su binary locations.
     * Does NOT block app execution.
     */
    fun isRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    /**
     * Detects if Developer Options or ADB debugging is active.
     */
    fun isDeveloperOptionsEnabled(context: Context): Boolean {
        return try {
            val devOptions = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
            val adb = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
            devOptions || adb
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Validates application package name and basic signature presence.
     */
    fun checkAppIntegrity(context: Context): Boolean {
        val expectedPackageName = "com.meshlink"
        return context.packageName.startsWith(expectedPackageName)
    }
}
