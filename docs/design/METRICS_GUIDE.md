# Metrics Guide

## Runtime Metrics Expansion
The `MetricsManager` now collects critical performance vectors alongside basic counters.

### New Additions
- **Heap Memory (`heap_max_mb`, `heap_used_mb`)**: Extracted from `Runtime.getRuntime()`. Helps detect memory leaks during extended offline operations (e.g. 72-hour burn-in).
- **CPU Time (`activeCpuTimeMs`)**: Extracted from `PowerMetricsManager`.
- **Average Execution Times**: `timers` array now outputs averages dynamically via `getAverageTime()`.
