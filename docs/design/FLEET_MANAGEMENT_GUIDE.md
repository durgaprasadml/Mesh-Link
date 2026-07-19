# Fleet Management Guide

## Offline Fleet Visibility
Because Mesh Link is designed for disconnected environments, we cannot rely on cloud-based telemetry dashboards.

### `FleetManagementManager`
- Each node compiles its own `FleetStatus` (OS version, App version, Battery, Storage, Compliance).
- Support units can walk into a disaster zone and request an offline `.csv` dump from any node to audit the exact health of the fleet without needing an internet connection.
