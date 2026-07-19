# Transfer Recovery

## Stateful Large File Transfers
Mesh networks are notoriously unstable. A 50MB video file transfer *will* be interrupted.

### `TransferRecoveryManager`
- Tracks the target `File`, total payload size, and `bytesReceived`.
- Maintained in a `ConcurrentHashMap`.
- If a connection drops, the app can query `getResumeOffset(transferId)` to know exactly where to resume writing without appending duplicate fragments or forcing a restart from 0%.
