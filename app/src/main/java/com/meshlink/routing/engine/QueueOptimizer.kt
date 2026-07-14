package com.meshlink.routing.engine

import com.meshlink.ble.data.MeshPacket
import com.meshlink.ble.data.PacketType
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueOptimizer @Inject constructor() {

    // A priority queue to ensure critical packets get routed first.
    // Higher priority level number means higher priority.
    private val packetQueue = PriorityQueue<MeshPacket> { p1, p2 ->
        // Compare priorities directly. Since PriorityQueue polls the smallest element first,
        // we negate the comparison so that higher priority values are polled first.
        val priorityComparison = p2.priority.level.compareTo(p1.priority.level)
        if (priorityComparison != 0) {
            priorityComparison
        } else {
            // Tie breaker: fallback to packet type heuristics
            getLegacyPriorityScore(p1.type).compareTo(getLegacyPriorityScore(p2.type))
        }
    }

    /**
     * Maps legacy packet types to priority levels as a tie-breaker.
     * 1: Emergency / Critical
     * 2: Real-time Media (Voice/Video)
     * 3: Normal Messages
     * 4: Background Sync / Bulk Transfer
     */
    private fun getLegacyPriorityScore(type: PacketType): Int {
        return when (type) {
            PacketType.SOS -> 1
            PacketType.VOICE_SIGNAL, PacketType.VOICE_FRAME, PacketType.VIDEO_SIGNAL, PacketType.VIDEO_FRAME -> 2
            PacketType.TEXT, PacketType.LOCATION -> 3
            PacketType.MEDIA_CHUNK, PacketType.DELIVERY_ACK, PacketType.READ_RECEIPT -> 4
            else -> 4
        }
    }

    @Synchronized
    fun enqueue(packet: MeshPacket) {
        packetQueue.offer(packet)
    }

    @Synchronized
    fun dequeue(): MeshPacket? {
        return packetQueue.poll()
    }
    
    @Synchronized
    fun clear() {
        packetQueue.clear()
    }

    @Synchronized
    fun size(): Int = packetQueue.size
}
