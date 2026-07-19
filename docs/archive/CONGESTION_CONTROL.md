# Congestion Control

The `CongestionMonitor` keeps track of local queue sizes to prevent the device from crashing under load (e.g., during a massive broadcast storm or huge file transfer).

## Congestion Levels
- **LOW** (< 50 packets): Normal operation.
- **MEDIUM** (50-150 packets): Elevated load, some latency expected.
- **HIGH** (150-300 packets): Deep queues. Non-critical packets are dropped or cached locally (Store-and-Forward) rather than being immediately broadcasted over BLE, allowing the Bluetooth stack time to recover.
- **CRITICAL** (> 300 packets): The node refuses to act as a relay for standard packets. Only `SOS` or `KEY_EXCHANGE` packets are processed.

## Recovery
When the queue drops below thresholds, the state downgrades dynamically. The Store-and-Forward cache loop will re-evaluate conditions and dispatch paused packets once the network returns to LOW or MEDIUM.
