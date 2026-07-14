# Mesh Link Roadmap (v3.0)

While v2.0 secured the foundation for enterprise and emergency deployment, v3.0 will focus on extreme distance, hardware integration, and multi-hop stream routing.

## 1. LoRa Hardware Integration
- **Goal:** Extend the mesh range from 100 meters (Wi-Fi/BLE) to 10+ Kilometers.
- **Approach:** Introduce a USB-Serial transport layer to interface with external ESP32/LoRa modules.
- **Challenge:** LoRa bandwidth is incredibly low. The `QueueOptimizer` will need a specialized `LoRaTransport` profile that strictly drops everything except `CRITICAL` text/location packets.

## 2. Multi-Hop Video & Voice Routing
- **Goal:** Allow real-time voice and video to traverse multiple hops without congesting the network.
- **Approach:** Implement dynamic bandwidth allocation and UDP-style packet dropping for stale frames in the `RelayDao`.

## 3. Distributed Mesh AI (Federated Learning)
- **Goal:** Allow the local `BatteryPredictor` and `RouteOptimizer` to learn from the entire mesh without exposing private data.
- **Approach:** Share aggregated, anonymized model weights over the mesh during low-traffic periods.

## 4. Desktop Client (Windows/macOS/Linux)
- **Goal:** Allow command centers to run Mesh Link on laptops.
- **Approach:** Port the core networking engine to Kotlin Multiplatform (KMP), enabling desktop JVM targets.
