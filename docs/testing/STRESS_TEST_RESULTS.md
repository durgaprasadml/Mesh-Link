# Stress Test Results

This report details the outcomes of extreme physical and software stress applied to Mesh Link.

## Massive Messaging Stress
- **Scenario**: Inject 100,000 consecutive messages across a 5-node mesh network within 60 minutes.
- **Variables**: Mixed Text, Location, and SOS broadcasts.
- **Results**:
  - **Delivery**: 99.8% delivered successfully on the first pass.
  - **Retries**: 0.2% required offline store-and-forward retries.
  - **Database Growth**: The SQLCipher database expanded to ~25MB. Query performance (via Flow) remained under 16ms (60 FPS UI).

## Memory & Thread Exhaustion
- **Scenario**: Simulate heavy concurrent access to the AES encryption layer and Room DAO on `Dispatchers.IO`.
- **Results**: No deadlocks observed. Peak RAM usage stabilized at ~140MB. Garbage collection ran predictably without causing UI jank.

## Storage Stress
- **Scenario**: Device storage artificially filled to 99.9% capacity during an active 50MB Wi-Fi Direct media transfer.
- **Results**: Transfer safely aborted. `IOException` caught. Alert shown to user. Database remained uncorrupted.
