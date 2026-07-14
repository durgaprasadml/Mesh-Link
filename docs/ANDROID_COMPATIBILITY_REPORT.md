# Android Compatibility & Lifecycle Report

Mesh Link targets API 34 (Android 14) and supports a minimum of API 33 (Android 13).

## OS Version Verification
| Version | Status | Notes |
| :--- | :--- | :--- |
| **Android 13 (API 33)** | 🟢 PASS | Granular media permissions (`READ_MEDIA_IMAGES`) function correctly. |
| **Android 14 (API 34)** | 🟢 PASS | Target SDK. Foreground Service types (`connectedDevice`, `dataSync`) declared properly. |
| **Android 15 (API 35)** | 🟢 PASS | Compatible with edge-to-edge UI enforcements. |
| **Android 16+ (Simulated)** | 🟢 PASS | Tested against strict battery optimization APIs. |

## Lifecycle Resiliency
- **Configuration Changes**: Screen rotations and Dark/Light theme toggles do not interrupt active TCP or BLE sockets thanks to `ViewModel` state hoisting and background Coroutine Scopes.
- **Split Screen / PiP**: Fully responsive UI scales gracefully.
- **Background Restrictions**: The app uses `WorkManager` for guaranteed offline queue flushing and `Foreground Services` to maintain real-time BLE mesh links while the screen is locked.
