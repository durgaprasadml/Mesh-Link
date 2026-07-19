# Runtime Health Audit

**Date:** July 14, 2026
**Focus:** Production Runtime Health & Reliability Framework

This audit identifies long-running components and their reliability requirements.

## 1. Foreground Services

### `MeshRelayService`
- **Role**: Maintains the mesh network and background connections.
- **Risks**: Silent death, ANR due to stuck threads, waking up the device too often.
- **Monitoring Strategy**: Ensure heartbeat is maintained via `RuntimeWatchdog`. Monitor foreground service status and wake-lock duration.
- **Recovery Strategy**: Restart via `SelfHealer` if heartbeat is lost and the app is still in a state where it should be running.

## 2. Repositories & Managers

### `BleRepository`
- **Role**: Manages BLE Scanning and Advertising.
- **Risks**: Scanner stalls (stops returning results without throwing errors), Advertiser failures.
- **Monitoring Strategy**: Track scanner callback frequency and advertiser state.
- **Recovery Strategy**: Force stop, flush `BluetoothGatt`, and restart scanning/advertising.

### `WifiDirectManager`
- **Role**: High-bandwidth direct connections.
- **Risks**: P2P group negotiation stalls, socket leaks.
- **Monitoring Strategy**: Track open socket count, group creation timeouts.
- **Recovery Strategy**: Cancel current group negotiation and reset state.

### `SessionManager`
- **Role**: Maintains secure sessions and encryption states.
- **Risks**: Infinite retries on key exchange, memory leaks from stale sessions.
- **Monitoring Strategy**: Monitor active session count and handshake latency.
- **Recovery Strategy**: Evict stale sessions, force re-keying on stalled handshakes.

### `MeshRouter`
- **Role**: Routes packets and maintains topology.
- **Risks**: Stale routes causing black holes, massive memory consumption in routing tables.
- **Monitoring Strategy**: Track active peer count, route resolution latency.
- **Recovery Strategy**: Flush `RouteCache` upon memory pressure.

## 3. Background Workers

### `CleanupWorker`
- **Role**: Prunes old messages, expired chunks, and stale sessions.
- **Risks**: Database locking, excessive I/O causing CPU spikes.
- **Monitoring Strategy**: Track execution duration and DB lock time.
- **Recovery Strategy**: Pause execution during memory pressure or if DB stalls are detected.

### `RetryWorker`
- **Role**: Handles delayed message delivery.
- **Risks**: Infinite retry loops, network congestion.
- **Monitoring Strategy**: Queue size and failure rates.
- **Recovery Strategy**: Exponential backoff and drop if queue exceeds limit.

## 4. Storage & Notifications

### `Database` (`AppDatabase`)
- **Role**: Persistent storage.
- **Risks**: SQLite deadlocks, `OutOfMemoryError` on large queries.
- **Monitoring Strategy**: Query latency and open connection tracking.
- **Recovery Strategy**: Close and re-open connection gracefully if deadlock is suspected.

### `NotificationService` (`NotificationHelper`)
- **Role**: Delivers user-facing alerts.
- **Risks**: Notification spam, dropping critical alerts.
- **Monitoring Strategy**: Track notification post rate.
- **Recovery Strategy**: Throttle notifications if rate is too high.
