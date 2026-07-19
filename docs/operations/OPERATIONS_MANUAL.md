# Operations Manual (v2.0)

## Overview
This manual provides instructions for system administrators and field operators managing a Mesh Link deployment.

## 1. Log Extraction & Diagnostics
Since Mesh Link is completely offline, logs are not sent to a centralized server.
To diagnose network issues in the field:
1. Navigate to **Settings -> Developer Options -> Export Diagnostics**.
2. This generates a zip file containing:
   - `routing_events.log` (Hop traces, packet drops)
   - `battery_metrics.csv`
   - `crash_dumps.txt`
3. This zip file can be shared locally via Wi-Fi Direct to the admin device.

## 2. Troubleshooting Connectivity
- **Symptom:** Nodes are physically close but not routing packets.
  - **Check 1:** Trust Level. Has a node been accidentally marked as `BLOCKED`?
  - **Check 2:** Battery Level. If a node is below 15% and not in Emergency Mode, it will refuse to relay background traffic.
  - **Check 3:** OS Restrictions. Ensure the OS hasn't placed the app in a "Deep Sleep" bucket. Instruct users to exempt Mesh Link from battery optimization.

## 3. Incident Response (Rogue Nodes)
If a device is compromised or stolen:
1. A Commander must issue a `REVOKE` packet for the stolen device's public key.
2. This packet is broadcast with `CRITICAL` priority.
3. Upon receipt, all valid nodes update their local `TrustManager`. The compromised node is instantly firewalled at the BLE/Wi-Fi layer.
