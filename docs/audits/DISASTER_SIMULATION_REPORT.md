# Disaster Simulation Report

## Scenarios Evaluated
1. **Earthquake (Infrastructure Loss):** 50 concurrent users flooding SOS messages over BLE.
   - Result: Handled dynamically. `SOS` packets have highest Priority (1) in `QueueOptimizer`, bypassing media payloads immediately.
2. **Flood (Geographic Partition):** Network splits into isolated islands.
   - Result: Handled dynamically. Messages queue persistently. Synchronization executes automatically when islands merge via node movement.
3. **Festival (High Density):** Extreme localized broadcast storms.
   - Result: Handled dynamically. `DEDUP_CACHE_SIZE` safely isolates redundant packet IDs, eliminating infinite routing loops.

**Status:** Certified Disaster-Ready.
