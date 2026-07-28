# Testing Strategy

Mesh Link V3.0 employs a multi-tiered testing strategy to ensure reliability, security, and performance.

## Simulator (`com.meshlink.simulator`)

The Simulator provides an entirely in-memory Transport implementation.

*   **Deterministic Testing:** Run complex multi-node mesh topologies deterministically.
*   **Fault Injection:** Simulate packet loss, latency, and link failures programmatically.
*   **Speed:** Execute integration tests in milliseconds without relying on physical Bluetooth or Wi-Fi hardware.

## Stress Tests

The reliability suite (`com.meshlink.reliability`) includes stress tests designed to push the system to its limits:

*   **High Concurrency:** Flooding the network with thousands of messages simultaneously.
*   **Churn:** Rapidly connecting and disconnecting nodes (simulating users walking in and out of range).
*   **Long-Running Tests:** Executing for extended periods to detect memory leaks and resource exhaustion.

## Security Regression Tests

A dedicated suite ensures cryptographic primitives remain unbroken.

*   **Known Answer Tests (KAT):** Validates AES-GCM and ECDH implementations against standard vectors.
*   **Fuzzing:** Inputs malformed routing headers and payloads to ensure graceful failure.
*   **Replay Simulation:** Attempts to inject previously captured packets to verify the sliding window and Nonce validation.

## Running Tests

All tests can be executed via Gradle:

```bash
# Run standard unit tests
./gradlew testDebugUnitTest

# Run stress and reliability tests (may take longer)
./gradlew connectedAndroidTest -PtestType=reliability
```

## CI Strategy

*   **Pre-commit:** Fast unit tests and linting.
*   **Nightly:** Full stress tests and simulator runs on varied topologies.
*   **Release:** Manual physical device validation (due to hardware variations in Android BLE stacks).

## Test Categories

*   `@SmallTest`: Unit tests (pure Kotlin logic).
*   `@MediumTest`: Integration tests (using Simulator).
*   `@LargeTest`: Stress tests and physical device instrumentation tests.
