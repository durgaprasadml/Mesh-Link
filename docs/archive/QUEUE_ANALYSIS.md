# Queue Analysis

## Dispatching & Queuing
- `MediaTransferManager`: Caps outbound concurrent chunked transfers to 10. Inter-chunk delay (30ms) ensures BLE TX buffer does not overflow.
- `QueueOptimizer`: Analyzes redundant packets and drops duplicates efficiently. Max items constrained.