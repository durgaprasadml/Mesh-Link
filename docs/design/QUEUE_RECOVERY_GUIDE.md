# Queue Recovery Guide

## Architecture
The Queue Recovery phase delegates responsibility to the `RetryCoordinator`, resolving previous bugs where pending data was lost due to in-memory store destruction.

## Mechanics
1. **Relay Persistence:** `MeshRouter` correctly caches out-of-order or congested packets in `RelayDao`. `RetryCoordinator` forces these to evaluate on boot.
2. **Pending Messages:** Fetched from `ChatDao` using `getPendingMessages()`.
3. **Deduplication Validation:** The existing loop guard and `routingEngine.markPacketProcessed` ensure that resending the queue does NOT create duplicate packets across the mesh.

## Operations Flow
- App Starts -> `RetryCoordinator` boots -> Queries pending records -> Enqueues to `QueueOptimizer` via `MeshRouter` -> Normal routing takes over.
