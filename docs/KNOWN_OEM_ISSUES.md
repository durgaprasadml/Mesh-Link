# Known OEM Issues & Mitigations

This document serves as the central repository for known vendor-specific anomalies encountered during Mesh Link validation.

## 1. Xiaomi / MIUI
- **Issue:** Very aggressive background task killer. Stops BLE Scanning after ~5 minutes when screen is off.
- **Mitigation:** The application prompts MIUI users to manually set the app to "No Restrictions" in battery settings. The `BootCompletedReceiver` attempts to resurrect the service.

## 2. Samsung (OneUI 5/6)
- **Issue:** Overly restrictive BLE GATT caching. Sometimes causes stale MTU values during rapid reconnects.
- **Mitigation:** Fallback logic chunks down to 20 bytes if the 512-byte MTU request times out or is rejected.

## 3. Oppo / ColorOS
- **Issue:** Background network isolation limits Wi-Fi Direct Group Owner creation when the screen is locked.
- **Mitigation:** Application utilizes a transient `PowerManager.PARTIAL_WAKE_LOCK` while establishing the socket infrastructure.

**All known issues have architectural mitigations in place to ensure app resilience.**
