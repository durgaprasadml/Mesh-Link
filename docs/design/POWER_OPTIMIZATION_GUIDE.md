# Power Optimization Guide

## Adaptive Power Scaling
Mesh Link dynamically regulates its BLE advertising and scanning frequencies based on device battery limits.

### Components
1. **`PowerStateManager`**: Listens to Android system intents (`ACTION_POWER_SAVE_MODE_CHANGED`, `ACTION_DEVICE_IDLE_MODE_CHANGED`) and updates an internal `PowerState`.
2. **`AdaptiveMeshPowerManager`**: Collects the `PowerState` Flow. When the system shifts into `BATTERY_SAVER` or `DOZE_MODE`, it throttles mesh discovery. When power resumes to normal, full mesh speed is unlocked.

## Process Lifecycle
The application strictly binds to `ProcessLifecycleOwner`. This allows high-priority coroutines to rest when the app is backgrounded, mitigating CPU starvation.
