# Battery Optimization Report

## Doze & App Standby Resilience
Mesh Link is designed to survive Android's aggressive Doze modes:
1. **Idle / Deep Doze:** `MeshRelayService` uses `PowerManager.PARTIAL_WAKE_LOCK` for a strictly limited 30-second window during sync pulses, avoiding battery drain penalties.
2. **App Standby Buckets:** Ensures critical routing messages escalate the app bucket priority via high-priority FCM (if internet exists) or local notification triggers.
3. **Battery Saver Mode:** The app gracefully disables proactive BLE advertising to conserve battery, relying only on passive scanning unless the user triggers an active message send.

## Validation
- Screen OFF routing: **PASS**
- Idle overnight recovery: **PASS**
