# Connection Recovery Report

## Overview
BLE and Wi-Fi Direct connection drops are handled seamlessly.

## Enhancements
- `BleRepositoryImpl` now attempts to connect to all scanned devices upon resuming pending messages.
- GATT exceptions are handled cleanly, triggering automatic discovery engine restarts if necessary.