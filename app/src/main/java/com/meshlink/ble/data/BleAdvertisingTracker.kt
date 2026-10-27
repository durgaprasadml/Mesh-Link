package com.meshlink.ble.data

/**
 * Truthful lifecycle state of the local BLE advertisement.
 *
 * Android never tells an app when a running advertisement is silently terminated (for example when
 * a legacy connectable advertisement is consumed by an inbound GATT connection, or when the adapter
 * is toggled). The only authoritative signals the app ever receives are
 * `AdvertiseCallback.onStartSuccess` / `onStartFailure`, so [ACTIVE] must be set *only* by a
 * confirmed `onStartSuccess` and cleared by every stop / failure / adapter loss.
 */
enum class AdvertisingState {
    /** Not advertising and not supposed to be (or stopped by the platform / caller). */
    IDLE,

    /** `startAdvertising()` was issued; waiting for `onStartSuccess` / `onStartFailure`. */
    STARTING,

    /** `onStartSuccess` was received for the current session and no stop/failure followed. */
    ACTIVE,

    /** The platform reported a start failure for the current session. */
    FAILED
}

/** Identity that must be advertised while the mesh is running. */
data class AdvertisingRequest(
    val name: String,
    val meshId: String,
    val capabilities: Byte
)

/**
 * Thread-safe, Android-free bookkeeping for [BleAdvertiserManager].
 *
 * Responsibilities:
 *  * `isActive` is true only after a confirmed `onStartSuccess` (previously `isAdvertising` was
 *    `callback != null`, which stayed true after a start failure and hid dead advertisers from
 *    [com.meshlink.service.MeshWatchdog] and `MeshMessagingManager.refreshMesh`).
 *  * Remembers the *desired* advertisement so recovery paths (watchdog, link-change re-arm,
 *    retry scheduler) can restart without re-reading the profile.
 *  * Generation tokens: late/stale callbacks from a superseded session are ignored and a
 *    superseded or cancelled start request never reaches the radio.
 *  * Rate-limits event-driven re-arming so GATT connection churn cannot cause a restart storm.
 */
internal class BleAdvertisingTracker(
    private val monotonicNowMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    companion object {
        /** Minimum spacing between two event-driven (link-change) re-arms. */
        const val MIN_REARM_INTERVAL_MS = 15_000L
    }

    private val lock = Any()
    private var generation = 0L
    private var _state = AdvertisingState.IDLE
    private var _desired: AdvertisingRequest? = null
    private var lastRearmAtMs: Long = Long.MIN_VALUE

    val state: AdvertisingState get() = synchronized(lock) { _state }
    val desired: AdvertisingRequest? get() = synchronized(lock) { _desired }
    val isActive: Boolean get() = synchronized(lock) { _state == AdvertisingState.ACTIVE }

    /**
     * Registers a caller (or recovery) request to advertise [request]. Supersedes any earlier,
     * not-yet-executed request. Returns the generation token the executor must present to
     * [isCurrent] and to the callbacks.
     */
    fun request(request: AdvertisingRequest): Long = synchronized(lock) {
        _desired = request
        generation += 1
        generation
    }

    /** True if [token] is still the newest request (no later start/stop superseded it). */
    fun isCurrent(token: Long): Boolean = synchronized(lock) { token == generation && _desired != null }

    /** The executor is about to hand [token] to the radio. */
    fun onStarting(token: Long): Boolean = synchronized(lock) {
        if (token != generation || _desired == null) return false
        _state = AdvertisingState.STARTING
        true
    }

    /** `onStartSuccess` for [token]. Returns false when the callback belongs to a stale session. */
    fun onStartSuccess(token: Long): Boolean = synchronized(lock) {
        if (token != generation) return false
        _state = AdvertisingState.ACTIVE
        true
    }

    /** `onStartFailure` for [token]. Returns false when the callback belongs to a stale session. */
    fun onStartFailure(token: Long): Boolean = synchronized(lock) {
        if (token != generation) return false
        _state = AdvertisingState.FAILED
        true
    }

    /**
     * Advertising is no longer running. [byCaller] = an explicit stop (mesh stopped, user turned
     * advertising off): the desired request is dropped and any pending start is invalidated.
     * [byCaller] = false is used for the internal stop-before-restart and keeps the desire.
     */
    fun onStopped(byCaller: Boolean) = synchronized(lock) {
        _state = AdvertisingState.IDLE
        if (byCaller) {
            _desired = null
            generation += 1
        }
    }

    /** True when advertising is wanted but is not running and no start is in flight. */
    fun needsRecovery(): Boolean = synchronized(lock) {
        _desired != null && (_state == AdvertisingState.IDLE || _state == AdvertisingState.FAILED)
    }

    /**
     * Decides whether a GATT peripheral link change should re-arm the advertisement now.
     * Allowed only while advertising is wanted, not already starting, and not more often than
     * [MIN_REARM_INTERVAL_MS]. Records the attempt when it returns true.
     */
    fun tryAcquireRearm(): Boolean = synchronized(lock) {
        if (_desired == null || _state == AdvertisingState.STARTING) return false
        val now = monotonicNowMs()
        if (lastRearmAtMs != Long.MIN_VALUE && now - lastRearmAtMs < MIN_REARM_INTERVAL_MS) return false
        lastRearmAtMs = now
        true
    }

    /** Milliseconds until the next re-arm is permitted (0 = now). Used to defer, not drop. */
    fun rearmWaitMs(): Long = synchronized(lock) {
        if (lastRearmAtMs == Long.MIN_VALUE) return 0L
        (MIN_REARM_INTERVAL_MS - (monotonicNowMs() - lastRearmAtMs)).coerceAtLeast(0L)
    }
}
