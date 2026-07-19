# Discovery Optimization

## Adaptive BLE Scanning
In dense networks, static BLE scanning intervals cause "discovery storms", leading to severe battery drain and radio interference.

### `AdaptiveDiscoveryManager`
- Monitors the current active peer count.
- **SPARSE (0 peers):** Aggressive scanning to find a cluster (5000ms).
- **IDLE (10-50 peers):** Relaxed scanning (15000ms) to conserve battery.
- **DENSE (50+ peers):** Heavily throttled (30000ms) to prevent overlapping advertisement saturation.
- **EMERGENCY:** Max power (1000ms) for disaster-relief broadcast mode.
