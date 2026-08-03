package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import com.meshlink.routing.engine.TransportDiagnostics
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job

/**
 * Centralized manager responsible for tracking and deterministic cleanup of transfer session resources.
 * Manages open streams, temporary files, coroutine jobs, and buffer pool allocations per transfer session.
 */
@Singleton
class TransferResourceManager @Inject constructor(
    private val diagnostics: TransportDiagnostics
) {
    companion object {
        private const val TAG = "TransferResourceManager"
    }

    private val sessionStreams = ConcurrentHashMap<String, MutableSet<AutoCloseable>>()
    private val sessionTempFiles = ConcurrentHashMap<String, MutableSet<File>>()
    private val sessionJobs = ConcurrentHashMap<String, MutableSet<Job>>()
    private val sessionBorrowedBuffers = ConcurrentHashMap<String, AtomicInteger>()

    /**
     * Registers an open stream/descriptor associated with a transfer session.
     */
    fun registerStream(transferId: String, stream: AutoCloseable) {
        sessionStreams.computeIfAbsent(transferId) { ConcurrentHashMap.newKeySet() }.add(stream)
    }

    /**
     * Unregisters a stream once safely closed.
     */
    fun unregisterStream(transferId: String, stream: AutoCloseable) {
        sessionStreams[transferId]?.remove(stream)
    }

    /**
     * Registers a temporary file created for a transfer session.
     */
    fun registerTempFile(transferId: String, file: File) {
        sessionTempFiles.computeIfAbsent(transferId) { ConcurrentHashMap.newKeySet() }.add(file)
    }

    /**
     * Unregisters a temporary file.
     */
    fun unregisterTempFile(transferId: String, file: File) {
        sessionTempFiles[transferId]?.remove(file)
    }

    /**
     * Registers a coroutine job associated with a transfer session.
     */
    fun registerJob(transferId: String, job: Job) {
        sessionJobs.computeIfAbsent(transferId) { ConcurrentHashMap.newKeySet() }.add(job)
    }

    /**
     * Unregisters a coroutine job upon completion.
     */
    fun unregisterJob(transferId: String, job: Job) {
        sessionJobs[transferId]?.remove(job)
    }

    /**
     * Tracks a borrowed buffer count for session memory usage accounting.
     */
    fun trackBufferBorrow(transferId: String, bufferSize: Int) {
        sessionBorrowedBuffers.computeIfAbsent(transferId) { AtomicInteger(0) }.incrementAndGet()
    }

    /**
     * Tracks a returned buffer count.
     */
    fun trackBufferReturn(transferId: String, bufferSize: Int) {
        sessionBorrowedBuffers[transferId]?.decrementAndGet()
    }

    /**
     * Deterministically releases all resources (streams, jobs, temp files) associated with a transfer session.
     */
    fun releaseSessionResources(transferId: String) {
        // 1. Cancel registered coroutine jobs
        sessionJobs.remove(transferId)?.let { jobs ->
            for (job in jobs) {
                try {
                    if (job.isActive) {
                        job.cancel()
                    }
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "Failed to cancel job for $transferId: ${e.message}")
                }
            }
        }

        // 2. Close registered streams
        sessionStreams.remove(transferId)?.let { streams ->
            for (stream in streams) {
                try {
                    stream.close()
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "Failed to close stream for $transferId: ${e.message}")
                    diagnostics.logResourceLeak(transferId, "Failed stream closure: ${e.message}")
                }
            }
        }

        // 3. Delete registered temp files
        sessionTempFiles.remove(transferId)?.let { files ->
            for (file in files) {
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    MeshLogger.w(TAG, "Failed to delete temp file ${file.name} for $transferId: ${e.message}")
                }
            }
        }

        // 4. Log buffer count discrepancies if any
        sessionBorrowedBuffers.remove(transferId)?.let { counter ->
            val netUnreturned = counter.get()
            if (netUnreturned > 0) {
                MeshLogger.w(TAG, "Session $transferId ended with $netUnreturned unreturned buffers")
                diagnostics.logResourceLeak(transferId, "Unreturned buffers: $netUnreturned")
            }
        }

        diagnostics.logCleanupEvent("SESSION_RESOURCES_RELEASED", transferId, true)
    }

    /**
     * Cleans up all tracked resources across all sessions (used on manager shutdown or test reset).
     */
    fun closeAll() {
        val allSessionIds = sessionStreams.keys + sessionTempFiles.keys + sessionJobs.keys
        for (transferId in allSessionIds) {
            releaseSessionResources(transferId)
        }
        sessionStreams.clear()
        sessionTempFiles.clear()
        sessionJobs.clear()
        sessionBorrowedBuffers.clear()
    }

    /**
     * Returns total number of currently registered open streams.
     */
    fun getOpenStreamCount(): Int {
        return sessionStreams.values.sumOf { it.size }
    }

    /**
     * Returns total number of currently registered active jobs.
     */
    fun getActiveJobCount(): Int {
        return sessionJobs.values.sumOf { it.size }
    }
}
