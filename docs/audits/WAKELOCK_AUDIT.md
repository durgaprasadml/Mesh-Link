# WakeLock Audit

**Date:** July 2026

## Findings
- Previous implementation in `MeshRelayService.kt` risked dangling WakeLocks if an exception bypassed the manual `releaseWakeLock()` call.
- Reference counting was set to `false`, which was correct, but timeout enforcement was weak.

## Resolutions
- Created a robust scoped `withWakeLock(timeoutMs) { ... }` block.
- Applied this block universally around the mesh repository auto-start loop.
- **Leak Status:** 0 potential leaks detected post-refactor. Locks are now guaranteed to release via JVM `finally` execution.
