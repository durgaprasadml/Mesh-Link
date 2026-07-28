package com.meshlink.simulator.core

import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Deterministic virtual-time event scheduler for the mesh simulation framework.
 *
 * All timing-sensitive operations (store-and-forward loops, packet delivery with latency,
 * route eviction, TTL expiry) are scheduled here instead of using `delay()` or
 * `System.currentTimeMillis()`. This makes every simulation 100% reproducible.
 *
 * Events are stored in a [PriorityQueue] ordered by `triggerTimeMs`.
 * Advancing the clock via [runUntil] processes all events whose trigger time ≤ target.
 *
 * Thread Safety: Not thread-safe — intended for single-threaded deterministic test execution.
 *
 * Usage:
 * ```kotlin
 * val clock = SimulatedClock()
 * val scheduler = SimulationScheduler(clock)
 *
 * scheduler.scheduleAfter(100) { println("fires at t=100") }
 * scheduler.scheduleAfter(200) { println("fires at t=200") }
 * scheduler.runUntil(150) // only t=100 fires; clock advances to 150
 * scheduler.runUntil(200) // t=200 fires; clock advances to 200
 * ```
 */
class SimulationScheduler(private val clock: SimulatedClock) {

    private val eventIdGen = AtomicLong(0L)

    /** An immutable event node for the priority queue. */
    data class ScheduledEvent(
        val id: Long,
        val triggerTimeMs: Long,
        val action: () -> Unit
    ) : Comparable<ScheduledEvent> {
        override fun compareTo(other: ScheduledEvent): Int {
            val cmp = triggerTimeMs.compareTo(other.triggerTimeMs)
            return if (cmp != 0) cmp else id.compareTo(other.id) // stable ordering
        }
    }

    private val queue = PriorityQueue<ScheduledEvent>()

    // ------------ Scheduling API ------------

    /**
     * Schedules [action] to fire at exactly [virtualTimeMs] virtual milliseconds from epoch.
     * @return The event ID (can be used for future cancellation).
     */
    fun scheduleAt(virtualTimeMs: Long, action: () -> Unit): Long {
        val id = eventIdGen.incrementAndGet()
        queue.add(ScheduledEvent(id, virtualTimeMs, action))
        return id
    }

    /**
     * Schedules [action] to fire [delayMs] virtual milliseconds from now.
     */
    fun scheduleAfter(delayMs: Long, action: () -> Unit): Long =
        scheduleAt(clock.currentTimeMs + delayMs, action)

    /**
     * Schedules [action] to fire every [intervalMs] virtual milliseconds,
     * up to [count] times (default: unlimited if [count] <= 0).
     */
    fun scheduleRepeat(intervalMs: Long, count: Int = -1, action: () -> Unit) {
        var fired = 0
        fun schedule() {
            scheduleAfter(intervalMs) {
                action()
                fired++
                if (count <= 0 || fired < count) schedule()
            }
        }
        schedule()
    }

    /**
     * Cancels a previously scheduled event by ID.
     * Note: O(n) scan — use sparingly.
     */
    fun cancel(eventId: Long) {
        queue.removeIf { it.id == eventId }
    }

    /** Cancels all pending events. */
    fun cancelAll() {
        queue.clear()
    }

    // ------------ Execution API ------------

    /**
     * Drains all events whose [ScheduledEvent.triggerTimeMs] ≤ [targetMs],
     * processing them in chronological order and advancing the clock to each event's
     * trigger time before executing it.
     *
     * After processing all eligible events, the clock is set to [targetMs].
     */
    fun runUntil(targetMs: Long) {
        while (queue.isNotEmpty() && queue.peek().triggerTimeMs <= targetMs) {
            val event = queue.poll() ?: break
            // Advance clock to the event's trigger time before executing
            if (event.triggerTimeMs > clock.currentTimeMs) {
                clock.advanceBy(event.triggerTimeMs - clock.currentTimeMs)
            }
            event.action()
        }
        // Advance clock to target (even if no events were pending)
        if (targetMs > clock.currentTimeMs) {
            clock.advanceBy(targetMs - clock.currentTimeMs)
        }
    }

    /**
     * Runs the scheduler forward [durationMs] virtual milliseconds from now.
     */
    fun runFor(durationMs: Long) = runUntil(clock.currentTimeMs + durationMs)

    /**
     * Drains all pending events regardless of trigger time.
     * Useful for teardown or flushing queued actions.
     */
    fun flush() {
        while (queue.isNotEmpty()) {
            val event = queue.poll() ?: break
            if (event.triggerTimeMs > clock.currentTimeMs) {
                clock.advanceBy(event.triggerTimeMs - clock.currentTimeMs)
            }
            event.action()
        }
    }

    // ------------ Introspection ------------

    /** Returns the count of pending (not yet fired) events. */
    fun pendingCount(): Int = queue.size

    /** Returns the virtual time of the next scheduled event, or null if queue is empty. */
    fun nextEventTime(): Long? = queue.peek()?.triggerTimeMs

    override fun toString(): String =
        "SimulationScheduler(pending=${queue.size}, clock=${clock.currentTimeMs}ms)"
}
