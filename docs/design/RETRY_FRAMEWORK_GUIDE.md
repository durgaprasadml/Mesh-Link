# Retry Framework Guide

## RetryCoordinator
The central node for backoff retry logic.

### Parameters
- **Base Backoff:** 2,000 ms
- **Max Backoff:** 60,000 ms
- **Max Retries:** 10

### Features
- **Exponential Scaling:** $Delay = 2000 \times 2^{Attempt}$ bounded by 60,000 ms.
- **Deep Sleep:** If 10 retries are exhausted without a connection, the coordinator suspends polling for 5 minutes (`MAX_BACKOFF_MS * 5`) to prevent severe battery drain, acting as a network-aware throttling mechanism.
