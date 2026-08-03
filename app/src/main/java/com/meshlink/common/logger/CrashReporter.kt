package com.meshlink.common.logger

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Vendor-independent crash reporting abstraction interface for Mesh-Link.
 */
interface CrashReporter {
    fun logNonFatal(throwable: Throwable, metadata: Map<String, String>? = null)
    fun logFatal(throwable: Throwable, metadata: Map<String, String>? = null)
    fun logBreadcrumb(message: String)
    fun setCustomKey(key: String, value: String)
    fun setUserId(userId: String)
}

/**
 * No-op implementation for offline or privacy-strict environments.
 */
class NoOpCrashReporter : CrashReporter {
    override fun logNonFatal(throwable: Throwable, metadata: Map<String, String>?) {}
    override fun logFatal(throwable: Throwable, metadata: Map<String, String>?) {}
    override fun logBreadcrumb(message: String) {}
    override fun setCustomKey(key: String, value: String) {}
    override fun setUserId(userId: String) {}
}

/**
 * Firebase Crashlytics implementation wrapping Firebase SDK gracefully.
 */
class FirebaseCrashReporterImpl(private val context: Context) : CrashReporter {

    private val firebaseCrashlytics by lazy {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    override fun logNonFatal(throwable: Throwable, metadata: Map<String, String>?) {
        metadata?.forEach { (key, value) ->
            firebaseCrashlytics?.setCustomKey(key, value)
        }
        firebaseCrashlytics?.recordException(throwable)
    }

    override fun logFatal(throwable: Throwable, metadata: Map<String, String>?) {
        metadata?.forEach { (key, value) ->
            firebaseCrashlytics?.setCustomKey(key, value)
        }
        firebaseCrashlytics?.recordException(throwable)
    }

    override fun logBreadcrumb(message: String) {
        firebaseCrashlytics?.log(message)
    }

    override fun setCustomKey(key: String, value: String) {
        firebaseCrashlytics?.setCustomKey(key, value)
    }

    override fun setUserId(userId: String) {
        firebaseCrashlytics?.setUserId(userId)
    }
}

/**
 * Self-hosted / local diagnostic crash reporter logging records in memory or local storage.
 */
class SelfHostedCrashReporter(private val context: Context) : CrashReporter {

    private val crashEvents = ConcurrentHashMap<String, String>()

    override fun logNonFatal(throwable: Throwable, metadata: Map<String, String>?) {
        val timestamp = System.currentTimeMillis().toString()
        crashEvents[timestamp] = "NON_FATAL: ${throwable.message} | metadata=$metadata"
    }

    override fun logFatal(throwable: Throwable, metadata: Map<String, String>?) {
        val timestamp = System.currentTimeMillis().toString()
        crashEvents[timestamp] = "FATAL: ${throwable.message} | metadata=$metadata"
    }

    override fun logBreadcrumb(message: String) {
        val timestamp = System.currentTimeMillis().toString()
        crashEvents[timestamp] = "BREADCRUMB: $message"
    }

    override fun setCustomKey(key: String, value: String) {
        crashEvents["KEY_$key"] = value
    }

    override fun setUserId(userId: String) {
        crashEvents["USER_ID"] = userId
    }

    fun getCrashLogs(): Map<String, String> = HashMap(crashEvents)
}
