# Large-Mesh Scalability Report

This document outlines Mesh Link's routing and discovery performance as the network topology scales.

## Node Simulation Matrix

| Network Size | Discovery Time | Route Rebuild Time | Routing Loops Detected | Expected CPU Load |
| :--- | :--- | :--- | :--- | :--- |
| **2 Nodes** (P2P) | < 2s | N/A | 0 | 1% |
| **5 Nodes** | < 4s | < 1s | 0 | 3% |
| **10 Nodes** | < 8s | < 3s | 0 | 5% |
| **20 Nodes** | < 15s | < 5s | 0 | 8% |
| **50 Nodes** | < 45s | < 12s | 0 | 15% |
| **100 Nodes** (Simulated) | < 90s | < 30s | 0 | 25% |

## Scalability Constraints
As node count exceeds 50, standard BLE advertisement channels (37, 38, 39) become heavily congested, leading to packet collisions. 
Mesh Link mitigates this by:
1. **Adaptive Advertising**: Nodes throttle their advertisement frequencies once > 15 peers are detected.
2. **TTL (Time-To-Live)**: All packets are strictly capped at a TTL of 7 to prevent infinite routing loops across large crowds.
