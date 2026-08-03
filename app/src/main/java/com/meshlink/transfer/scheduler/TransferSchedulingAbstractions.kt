package com.meshlink.transfer.scheduler

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.RouteType
import com.meshlink.transfer.TransferSession

/**
 * Architectural contract interface for prioritized transfer session queuing (Phase 2 Preparation).
 */
interface TransferQueue {
    fun enqueue(session: TransferSession)
    fun dequeue(): TransferSession?
    fun peek(): TransferSession?
    fun remove(transferId: String): Boolean
    fun clear()
    fun size(): Int
}

/**
 * Architectural contract interface for isolated packet transport queuing (Phase 2 Preparation).
 */
interface PacketQueue {
    fun enqueuePacket(packet: MeshPacket, route: RouteType)
    fun dequeuePacket(route: RouteType): MeshPacket?
    fun size(route: RouteType): Int
    fun clear(route: RouteType)
}

/**
 * Architectural contract interface for sliding window packet buffer management (Phase 2 Preparation).
 */
interface SlidingWindowBuffer {
    fun initializeWindow(windowSize: Int)
    fun addUnacknowledgedChunk(chunkIndex: Int, packet: MeshPacket)
    fun acknowledgeChunk(chunkIndex: Int): Boolean
    fun getExpiredChunks(timeoutMs: Long): List<MeshPacket>
    fun advanceWindow(): Int
}

/**
 * Architectural contract interface for parallel worker pool chunk transmission (Phase 2 Preparation).
 */
interface ParallelTransferWorkerPool {
    fun startWorkers(workerCount: Int)
    fun submitChunkTask(task: suspend () -> Unit)
    fun stopWorkers()
    fun getActiveWorkerCount(): Int
}
