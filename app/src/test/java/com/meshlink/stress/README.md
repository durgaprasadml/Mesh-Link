# Mesh Link Stress Testing Framework (HIGH-TEST-02)

This package contains the high-throughput, fault injection, and long-running soak testing framework for the Mesh Link simulator.

## Components

1. **`StressScenarioDSL.kt`**: A declarative way to configure a simulation with parameters such as node count, virtual duration, failure profile, injected traffic volume, and concurrent senders.
2. **`StressScenarioRunner.kt`**: The execution engine that sets up the nodes, applies the given `NetworkFailureProfile`, drives background traffic, and automatically persists failure states (topology, metrics, event timeline).
3. **`FaultInjector.kt`**: Deterministic API for simulating node crashes, link flapping, and network partitions.
4. **`SoakTestEnvironment.kt`**: Specialized orchestrator for 1h, 6h, 12h, and 24h tests that verify bounded resource growth over prolonged execution.
5. **`StressMetricsCollector.kt`**: Evaluates simulation reports against baseline thresholds (`RegressionThresholds`).

## Running Tests Locally

Standard high-throughput and fault injection tests run normally like any JUnit test and use virtual time for speed:
```bash
./gradlew app:testDebugUnitTest --tests "com.meshlink.stress.HighThroughputTest"
```

### Soak Tests
Soak tests are excluded by default to avoid slowing down CI PR builds. To run them locally or in nightly CI runs, export the `RUN_SOAK_TESTS` environment variable:

```bash
export RUN_SOAK_TESTS=true
./gradlew app:testDebugUnitTest --tests "com.meshlink.stress.SoakTest"
```

## Troubleshooting & Debugging

If a test fails (e.g. regression limits exceeded or queue overflow):
1. Check `build/reports/stress/` directory.
2. View the generated `*_failure_*.txt` for the specific metric violation.
3. Open `*_topology_*.json` in the Topology Visualizer to inspect the final network state.
4. Examine the `*_timeline_*.json` for a chronologically ordered dump of every packet send/receive/drop event leading up to the failure.
5. Use the seed from the failure report in the `StressScenarioDSL` configuration to deterministically reproduce the error.
