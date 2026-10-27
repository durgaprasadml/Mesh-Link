package com.meshlink.ble.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.os.PowerManager
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.meshlink.di.ApplicationScope
import com.meshlink.core.permissions.BluetoothPermissionChecker
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class BleAdvertiserManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val permissionChecker: BluetoothPermissionChecker,
    private val restartCoordinator: BleRestartCoordinator
) {
    companion object {
        private const val TAG = "BleAdvertiser"
        private const val NAME_PREVIEW_LENGTH = 3

        /** Let the stack finish tearing down / settling a GATT link before re-arming. */
        private const val REARM_SETTLE_MS = 2_000L
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /** Truth-source for "is the advertisement really running" and "what should be advertised". */
    private val tracker = BleAdvertisingTracker()

    /** Serialises every platform start/stop so two callers can never orphan an advertiser. */
    private val lifecycleMutex = Mutex()

    /** Guarded by [lifecycleMutex]. */
    private var advertiseCallback: AdvertiseCallback? = null

    private val rearmLock = Any()
    private var rearmJob: Job? = null
    private val restartCounter = AtomicInteger(0)

    /**
     * True only after Android confirmed the current advertisement via `onStartSuccess` and no
     * stop/failure followed. (Previously `advertiseCallback != null`, which stayed true after an
     * `onStartFailure` and therefore hid a dead advertiser from the watchdog and `refreshMesh`.)
     */
    val isAdvertising: Boolean
        get() = tracker.isActive

    val advertisingState: AdvertisingState
        get() = tracker.state

    private fun shortId(id: String): String =
        com.meshlink.util.MeshIdNormalizer.canonicalize(id).take(4).ifEmpty { "?" } + "…"

    fun startAdvertising(name: String, meshId: String, capabilities: Byte = 0) {
        // The request is registered synchronously so a later stopAdvertising()/startAdvertising()
        // deterministically supersedes this one, whatever order the coroutines get scheduled in.
        val token = tracker.request(AdvertisingRequest(name, meshId, capabilities))
        MeshLogger.i(
            TAG,
            "[BLE_ADVERTISE_START] requested node=${shortId(meshId)} state=${tracker.state} restarts=${restartCounter.get()}"
        )
        applicationScope.launch {
            lifecycleMutex.withLock { startAdvertisingLocked(token) }
        }
    }

    private suspend fun startAdvertisingLocked(token: Long) {
        if (!tracker.isCurrent(token)) {
            MeshLogger.d(TAG, "[BLE_ADVERTISE_START] superseded or cancelled before execution; skipped")
            return
        }
        val request = tracker.desired ?: return

        if (!settingsRepository.bleAdvertisingEnabled.first() || !settingsRepository.isBleEnabled.first()) {
            MeshLogger.d(TAG, "BLE Advertising disabled in settings. Skipping.")
            // Not wanted, so nothing for the recovery paths to restore.
            if (tracker.isCurrent(token)) tracker.onStopped(byCaller = true)
            return
        }

        if (!permissionChecker.hasRequiredPermissions(context)) {
            MeshLogger.w(TAG, "Missing permissions for BLE advertising")
            if (tracker.isCurrent(token)) tracker.onStopped(byCaller = false)
            return
        }

        // Always tear down whatever this manager previously started before starting again.
        stopPlatformAdvertisementLocked("restart")

        val adapter = bluetoothAdapter
        val advertiser = adapter?.bluetoothLeAdvertiser
        if (advertiser == null || adapter.isEnabled != true) {
            MeshLogger.w(TAG, "Bluetooth is disabled or advertiser unavailable; skipping advertising")
            tracker.onStopped(byCaller = false)
            return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSave = powerManager.isPowerSaveMode
        val userTxPower = settingsRepository.bleTxPower.first()

        val advMode = if (isPowerSave) {
            AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
        } else {
            AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
        }

        val txPower = if (isPowerSave) {
            AdvertiseSettings.ADVERTISE_TX_POWER_LOW
        } else {
            when (userTxPower) {
                0 -> AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW
                1 -> AdvertiseSettings.ADVERTISE_TX_POWER_LOW
                2 -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
                3 -> AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                else -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
            }
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(advMode)
            .setTxPowerLevel(txPower)
            .setConnectable(true)
            .build()

        // 8-byte canonical Mesh ID directly embedded in manufacturer data
        val canonicalMeshId = com.meshlink.util.MeshIdNormalizer.canonicalize(request.meshId)
        val canonicalBytes = canonicalMeshId.toByteArray(Charsets.UTF_8)
        val meshIdBytes = ByteArray(8)
        System.arraycopy(canonicalBytes, 0, meshIdBytes, 0, minOf(canonicalBytes.size, 8))
        val combinedData = ByteArray(9)
        System.arraycopy(meshIdBytes, 0, combinedData, 0, 8)
        combinedData[8] = request.capabilities

        MeshLogger.i(TAG, "[NearbyDiscovery] BLE advertise START: meshId=${request.meshId}, canonical=$canonicalMeshId, txPower=$txPower, mode=$advMode")

        // Primary advertising packet: contains manufacturer data with Mesh ID payload (13 bytes total)
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(BleConstants.MANUFACTURER_ID, combinedData)
            .build()

        // Scan response packet: contains Service UUID
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
            .build()

        // A superseding start/stop may have arrived while we were reading settings.
        if (!tracker.isCurrent(token)) {
            MeshLogger.d(TAG, "[BLE_ADVERTISE_START] superseded while preparing; skipped")
            return
        }

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                if (!tracker.onStartSuccess(token)) {
                    MeshLogger.d(TAG, "[BLE_ADVERTISE_SUCCESS] ignored: callback belongs to a superseded session")
                    return
                }
                MeshLogger.i(TAG, "[NearbyDiscovery] BLE advertise SUCCESS: active with meshId=$canonicalMeshId")
                MeshLogger.i(TAG, "[BLE_ADVERTISE_SUCCESS] node=${shortId(canonicalMeshId)} state=${tracker.state}")
                applicationScope.launch {
                    restartCoordinator.resetRetry(RestartComponent.ADVERTISER)
                }
            }

            override fun onStartFailure(errorCode: Int) {
                if (!tracker.onStartFailure(token)) {
                    MeshLogger.d(TAG, "[BLE_ADVERTISE_FAILURE] ignored: callback belongs to a superseded session (code=$errorCode)")
                    return
                }
                if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                    // The platform says this very advertisement is already running.
                    tracker.onStartSuccess(token)
                    MeshLogger.w(TAG, "[BLE_ADVERTISE_SUCCESS] platform reports already started; treating as active")
                    return
                }
                MeshLogger.e(
                    TAG,
                    "[BLE_ADVERTISE_FAILURE] node=${shortId(canonicalMeshId)} code=$errorCode (${describeAdvertiseError(errorCode)}) state=${tracker.state}"
                )
                MeshLogger.e(TAG, "[NearbyDiscovery] BLE advertise FAILED with error code: $errorCode")
                val self = this
                applicationScope.launch {
                    // The failed callback no longer represents a running advertisement.
                    lifecycleMutex.withLock {
                        if (advertiseCallback === self) advertiseCallback = null
                    }
                }
                val cause = if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                    BleNonRetryableException("Advertise failed", errorCode)
                } else {
                    BleException("Advertise failed", errorCode)
                }
                scheduleRecovery(cause)
            }
        }
        advertiseCallback = callback
        tracker.onStarting(token)

        try {
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker at start of method
            val ignored = advertiser.startAdvertising(settings, data, scanResponse, callback)
        } catch (e: SecurityException) {
            MeshLogger.e(TAG, "[NearbyDiscovery] SecurityException: Missing BLE advertise permission", e)
            advertiseCallback = null
            tracker.onStartFailure(token)
        } catch (e: Exception) {
            MeshLogger.e(TAG, "[BLE_ADVERTISE_FAILURE] exception starting advertising: ${e.message}", e)
            advertiseCallback = null
            tracker.onStartFailure(token)
            scheduleRecovery(e)
        }
    }

    /**
     * Bounded, back-off retry for a failed start, driven by the real failure signal. Executes only
     * if advertising is still wanted and still not running when the delay elapses.
     */
    private fun scheduleRecovery(cause: Throwable) {
        applicationScope.launch {
            if (!settingsRepository.bleAutoRestart.first()) return@launch
            restartCoordinator.scheduleRestart(applicationScope, RestartComponent.ADVERTISER, cause) {
                val request = tracker.desired
                if (request != null && tracker.needsRecovery()) {
                    val n = restartCounter.incrementAndGet()
                    MeshLogger.w(
                        TAG,
                        "[BLE_ADVERTISE_RESTART] reason=start_failure_retry restarts=$n cause=${cause.message}"
                    )
                    startAdvertising(request.name, request.meshId, request.capabilities)
                } else {
                    MeshLogger.d(TAG, "[BLE_ADVERTISE_RESTART] retry skipped: advertising no longer wanted or already active")
                }
            }
        }
    }

    fun stopAdvertising() {
        // Registered synchronously: drops the desired request and invalidates any queued start.
        tracker.onStopped(byCaller = true)
        applicationScope.launch {
            lifecycleMutex.withLock {
                // If a newer startAdvertising() already registered itself, it owns the radio and
                // tears down the previous callback itself; do not kill the new advertisement.
                if (tracker.desired == null) {
                    restartCoordinator.cancelRestart(RestartComponent.ADVERTISER)
                    stopPlatformAdvertisementLocked("stop_requested")
                }
            }
        }
    }

    /**
     * Stops the advertisement this manager started, if any. Clears the local handle even when the
     * radio, adapter or permission is already gone: in that case the platform advertisement is
     * dead and keeping a stale handle would make [isAdvertising] lie. Must hold [lifecycleMutex].
     */
    private fun stopPlatformAdvertisementLocked(reason: String) {
        val callback = advertiseCallback ?: run {
            tracker.onStopped(byCaller = false)
            return
        }
        advertiseCallback = null
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null || !permissionChecker.hasRequiredPermissions(context)) {
            MeshLogger.w(TAG, "[BLE_ADVERTISE_STOP] reason=$reason advertiser/permission unavailable; local handle cleared")
            tracker.onStopped(byCaller = false)
            return
        }
        try {
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker above
            val ignored = advertiser.stopAdvertising(callback)
            MeshLogger.i(TAG, "[NearbyDiscovery] BLE advertise STOPPED")
            MeshLogger.i(TAG, "[BLE_ADVERTISE_STOP] reason=$reason")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "[NearbyDiscovery] Error stopping advertising: ${e.message}", e)
        }
        // Only the radio state changes here: a restart keeps the desired request and a caller
        // stop has already cleared it synchronously in stopAdvertising().
        tracker.onStopped(byCaller = false)
    }

    fun restartAdvertising(name: String, meshId: String, capabilities: Byte = 0) {
        MeshLogger.d(TAG, "Restarting BLE advertising")
        stopAdvertising()
        startAdvertising(name, meshId, capabilities)
    }

    /**
     * Restarts advertising with the last requested identity, without a profile lookup.
     * Used by the watchdog. Returns false when nothing is wanted (so the caller can fall back to
     * a full mesh refresh that re-reads the profile).
     */
    fun recoverAdvertising(reason: String): Boolean {
        val request = tracker.desired ?: return false
        val n = restartCounter.incrementAndGet()
        MeshLogger.w(
            TAG,
            "[DISCOVERY_RECOVERY] advertiser reason=$reason state=${tracker.state} restarts=$n"
        )
        MeshLogger.w(TAG, "[BLE_ADVERTISE_RESTART] reason=$reason restarts=$n")
        startAdvertising(request.name, request.meshId, request.capabilities)
        return true
    }

    /**
     * Called when a remote central connects to / disconnects from our GATT server.
     *
     * Mesh-Link advertises *connectable* legacy advertisements. Android gives the app no callback
     * when such an advertisement is consumed by an inbound connection, so [isAdvertising] cannot
     * detect it. This event-driven hook re-arms the advertisement after the link settles so the
     * device stays discoverable to *other* peers while linked. It is coalesced (trailing
     * debounce) and rate-limited by [BleAdvertisingTracker.MIN_REARM_INTERVAL_MS], so
     * connection churn cannot cause a restart storm.
     */
    fun onPeripheralLinkChanged(connected: Boolean) {
        if (tracker.desired == null) return
        synchronized(rearmLock) {
            rearmJob?.cancel()
            rearmJob = applicationScope.launch {
                delay(REARM_SETTLE_MS)
                val wait = tracker.rearmWaitMs()
                if (wait > 0) delay(wait)
                if (!tracker.tryAcquireRearm()) {
                    MeshLogger.d(TAG, "[BLE_ADVERTISE_RESTART] link re-arm skipped (not wanted or start in flight)")
                    return@launch
                }
                val request = tracker.desired ?: return@launch
                val n = restartCounter.incrementAndGet()
                MeshLogger.i(
                    TAG,
                    "[BLE_ADVERTISE_RESTART] reason=gatt_server_link_${if (connected) "connected" else "disconnected"} " +
                        "state=${tracker.state} restarts=$n"
                )
                startAdvertising(request.name, request.meshId, request.capabilities)
            }
        }
    }

    private fun describeAdvertiseError(code: Int): String = when (code) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
        else -> "UNKNOWN"
    }
}
