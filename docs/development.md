# Development Guide

Welcome to the Mesh Link V3.0 project! This guide will help you set up your environment and start contributing.

## Project Setup

1.  **IDE:** Use Android Studio (latest stable version).
2.  **SDK:** Ensure you have the Android SDK for API level 34+ installed.
3.  **Clone the repository:**
    ```bash
    git clone https://github.com/example/mesh-link.git
    ```

## Build Instructions

Mesh Link uses Gradle. To build the project:

```bash
./gradlew clean build
```

To build and install the sample application on a device:

```bash
./gradlew :app:installDebug
```

## Dependency Management

*   **Language:** Kotlin (1.9+)
*   **Coroutines:** Used extensively for asynchronous operations.
*   **Cryptography:** BouncyCastle or Tink (depending on build variant) for robust security primitives.
*   *Note:* We minimize external dependencies to keep the library footprint small and auditing simple.

## Coding Conventions

*   Follow standard Kotlin coding conventions.
*   Use `ktlint` for formatting (enforced in CI).
*   **Immutability:** Prefer immutable data classes (`val`).
*   **Concurrency:** Use Kotlin Coroutines (`suspend` functions, `Flow`, `Mutex`) rather than raw Threads. Avoid blocking the main thread.

## Debugging

*   **Logging:** Use the internal `com.meshlink.logging` framework. Avoid `android.util.Log` directly to ensure structured output.
*   **Simulator:** Use the Simulator transport for debugging routing logic without needing multiple physical devices.

## Running Locally

For active development, run the `:app` module which provides a simple UI for connecting to peers and sending messages. Use two physical Android devices or two Emulators with Bluetooth support enabled to test real-world transport behavior.
