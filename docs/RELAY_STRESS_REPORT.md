# Relay Stress Report

## Scenario: Bottleneck / Hub Node
- **Action:** Node C acts as the sole bridge between Sub-Mesh A (50 users) and Sub-Mesh B (50 users).
- **Response:**
  - CPU bounds remain nominal.
  - Relay TTL (Time To Live) is strictly decremented, preventing ghost packets from circulating indefinitely.
  - RAM consumption peaks at 45MB during max throughput, safely avoiding Android's `LowMemoryKiller`.

**Status:** Hub Nodes remain stable under peak traversal loads.
