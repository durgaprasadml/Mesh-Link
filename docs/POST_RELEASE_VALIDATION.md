# Post-Release Validation Checklist

Upon deploying a new release (e.g. v2.0.0), the operations team must monitor the following metrics for 72 hours:

- [ ] Monitor Google Play Console for ANR Spikes (Target < 0.47%).
- [ ] Monitor Google Play Console for Crash Rate Spikes (Target < 1.09%).
- [ ] Verify `Stuck WakeLock` metrics remain stable (ensuring `MeshRelayService` isn't burning user battery).
- [ ] Monitor GitHub Issues for user reports of Database Migration failures (SQLite `IllegalStateException`).
- [ ] Perform manual smoke test on minimum 3 devices (e.g. Pixel, Samsung, Xiaomi) to verify Bluetooth connectivity on the live APK.
