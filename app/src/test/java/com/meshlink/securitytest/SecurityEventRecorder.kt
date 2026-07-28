package com.meshlink.securitytest

import com.meshlink.simulator.metrics.NetworkRecorder
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Monitors the SimulationEnvironment to capture security-relevant events.
 * Relies on the [NetworkRecorder] to observe dropped packets and their reasons.
 */
class SecurityEventRecorder(private val recorder: NetworkRecorder) {
    
    private val securityEvents = CopyOnWriteArrayList<SecurityEvent>()
    
    data class SecurityEvent(
        val type: EventType,
        val description: String,
        val timestampMs: Long
    )
    
    enum class EventType {
        REPLAY_DETECTED,
        AUTHENTICATION_FAILURE,
        INTEGRITY_FAILURE,
        SESSION_EXPIRED,
        DOWNGRADE_PREVENTED,
        OTHER
    }
    
    /**
     * Examines drops in the recorder and maps them to security events.
     * This is useful for building coverage reports.
     */
    fun pollEvents() {
        val allDrops = recorder.getDroppedPackets()
        
        allDrops.forEach { drop ->
            val eventType = when (drop.dropReason) {
                NetworkRecorder.DropReason.ENCRYPTION_FAILED -> EventType.DOWNGRADE_PREVENTED
                NetworkRecorder.DropReason.TRUST_BLOCKED -> EventType.SESSION_EXPIRED
                NetworkRecorder.DropReason.CORRUPTED -> EventType.INTEGRITY_FAILURE
                NetworkRecorder.DropReason.NO_ROUTE -> EventType.OTHER
                NetworkRecorder.DropReason.TTL_EXPIRED -> EventType.OTHER
                else -> EventType.OTHER
            }
            // If the packet has "REPLAY" in the payload or we want to infer replay, we map it here.
            // For now, we map based on the drop reason from NetworkRecorder.
            // Replay and MAC failures often appear as MALFORMED or ENCRYPTION_FAILED in simulator.
            
            val event = SecurityEvent(
                type = eventType,
                description = "Packet dropped: ${drop.dropReason} at node ${drop.nodeId}",
                timestampMs = drop.virtualTimeMs
            )
            
            if (!securityEvents.contains(event)) {
                securityEvents.add(event)
            }
        }
    }
    
    fun getEvents(type: EventType): List<SecurityEvent> = securityEvents.filter { it.type == type }
    
    fun clear() {
        securityEvents.clear()
    }
}
