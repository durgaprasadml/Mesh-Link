# Route Scoring System

Every possible route is ranked between `0` and `100`. The engine always forwards packets to the highest-scoring neighbor for a given destination.

## Configurable Weights
The algorithm relies on six configurable weights mapped to real-time telemetry:

1. **Link Quality (35%)**
   - Derived from raw RSSI (normalized between -100 and -40 dBm).
2. **Reliability (20%)**
   - Derived from Historical Success Rate. If 9 out of 10 packets are successfully acknowledged, reliability is 90%.
3. **Battery Level (15%)**
   - Direct translation. A phone at 10% battery yields a very low score to prevent the mesh from draining dying devices.
4. **Congestion (15%)**
   - Measured against the `CongestionMonitor`. Deep queues incur severe score penalties.
5. **Trust Score (10%)**
   - Integrated with `TrustManager`. "Verified" devices score higher than unknown devices, preventing rogue nodes from acting as preferred man-in-the-middle relays.
6. **Hop Count (5%)**
   - Prevents packets from bouncing endlessly through long relay chains when shorter paths exist. Max limit: 15 hops.

## Tie Breaking
When scores are identical, routes are ordered arbitrarily, but the cache always preserves the most recently seen active route.
