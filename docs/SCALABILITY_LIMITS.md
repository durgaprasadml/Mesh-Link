# Mesh Link: Scalability Limits

This document outlines the hard architectural limits imposed during RC4 to ensure system stability under extreme loads.

## Hard Constraints
- **Max Hop Count (TTL):** `15`. Packets exceeding 15 hops are dropped to prevent infinite ghost routing.
- **Max Queue Size (`QueueOptimizer`):** `10000` packets. If reached, the system strictly drops new low-priority packets to prevent Out-Of-Memory (OOM) crashes.
- **Dedup Cache Size (`RoutingEngine`):** `20000` packets. The system tracks the last 20,000 UUIDs to prevent redundant broadcasts.
- **Wi-Fi Socket Buffer:** `1MB`. Optimal chunking size for media transfers to balance speed and error-recovery efficiency.
- **Maximum Concurrent BLE Connections:** Dependent on OEM hardware (usually 4 to 7). The app dynamically multiplexes available slots.

## Verified Theoretical Limits
- **Nodes in Mesh:** Safe up to ~100-150 nodes before BLE advertisement spectrum saturation occurs.
- **Concurrent Media Transfers:** Safe up to 3 concurrent active streams (Wi-Fi Direct is 1-to-1 or 1-to-many GO based).
