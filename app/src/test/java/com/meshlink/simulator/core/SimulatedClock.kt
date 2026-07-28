package com.meshlink.simulator.core

import java.util.concurrent.atomic.AtomicLong

/**
 * Deterministic virtual clock for the mesh simulation framework.
 *
 * All time-sensitive operations in the simulator use this clock instead of
 * [System.currentTimeMillis], ensuring tests are fully deterministic and
 * independent of wall-clock time.
 *
 * Thread Safety: [currentTimeMs] is backed by [AtomicLong] — safe for concurrent reads.
 * Only one thread should advance the clock at a time (the scheduler thread).
 *
 * Usage:
 * ```kotlin
 * val clock = SimulatedClock()
 * clock.advanceBy(1_000) // advance 1 virtual second
 * println(clock.currentTimeMs) // 1000
 * ```
 */
class SimulatedClock(initialTimeMs: Long = 0L) {

    private val _timeMs = AtomicLong(initialTimeMs)

    /** Current virtual time in milliseconds. */
    val currentTimeMs: Long get() = _timeMs.get()

    /**
     * Advances the virtual clock by [ms] milliseconds.
     * @param ms Must be >= 0. Negative values are ignored.
     */
    fun advanceBy(ms: Long) {
        if (ms > 0) _timeMs.addAndGet(ms)
    }

    /**
     * Advances the virtual clock by [ms] and returns the new time.
     */
    fun advanceAndGet(ms: Long): Long {
        if (ms > 0) return _timeMs.addAndGet(ms)
        return _timeMs.get()
    }

    /**
     * Resets the virtual clock to [timeMs].
     */
    fun reset(timeMs: Long = 0L) {
        _timeMs.set(timeMs)
    }

    /**
     * Returns a [TimeProvider] lambda compatible with components that accept
     * a time-provider function.
     */
    fun asProvider(): () -> Long = { currentTimeMs }

    override fun toString(): String = "SimulatedClock(${currentTimeMs}ms)"
}
