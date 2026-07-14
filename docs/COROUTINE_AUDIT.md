# Coroutine Audit

## Identified and Fixed Leaks
- `MeshRelayService`: `serviceScope` is now explicitly cancelled in `onDestroy()`.
- `DiscoveryEngine`: `engineScope` is properly cancelled when discovery stops.
- `RouteHealthMonitor`: Cancels its scope to prevent ghost routing updates after being destroyed.