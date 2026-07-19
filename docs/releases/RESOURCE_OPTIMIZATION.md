# Resource Optimization

## Guarding Local Hardware
High-density mesh networks can easily exhaust an Android device's heap memory if topology events are spammed.

### `ResourceOptimizationManager`
- Monitors Heap usage and Battery level.
- If Heap > 85%, or Battery < 15%, it flags the system to delay non-critical tasks.
- Non-critical tasks include building analytical graphs, deep cleanup sweeps, and fetching secondary route optimizations.
- Warns the `QueueManager` to drop stale packets if queue depth exceeds 500 items.
