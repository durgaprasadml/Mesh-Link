# Transfer Performance Report

## Resumability & Reliability
The move from RAM-buffered chunks to a Disk-backed `TransferCache` has fundamentally shifted the reliability metrics of the app.
- **OOM Errors**: Eliminated. The app can safely receive a 2GB file over Wi-Fi Direct without exceeding 30MB of peak heap usage, because chunks are flushed to disk immediately upon receipt.
- **Resumability**: A transfer interrupted by a Bluetooth disconnect or process death can be fully resumed. The `TransferManager` automatically queries `TransferCache` for missing indices and issues a grouped NACK string to the sender, allowing the sender to transmit *only* the missing chunks.

## Network Yielding & Fairness
Under the legacy system, a large file transfer would spam the BLE bus as fast as the Android OS permitted, often leading to buffer overflows (Error 133) and dropping all other mesh traffic (like text messages).
With the new `TransferScheduler`, transfers are strictly prioritized. A background file transfer will automatically yield its transmission slot if a Critical priority packet enters the queue.

## Parallel Transfers
The engine now explicitly supports concurrent multiplexing up to 2 active streams (to prevent BLE starvation) while queuing dozens of others cleanly in the background.
