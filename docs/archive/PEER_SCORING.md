# Peer Scoring Algorithm

The `PeerScoreCalculator` is responsible for evaluating the quality of a discovered node.

## The Algorithm

Every peer receives a score between `0` and `100`. Higher scores dictate higher connection priority.

### Components

1. **RSSI (Weight: 50%)**
   - Normalizes clamped RSSI between `-100 dBm` and `-40 dBm`.
   - Raw RSSI is first smoothed using an Exponential Moving Average (EMA) filter with an alpha of `0.25` to prevent spikes from ruining scores.

2. **Stability / Failure Penalty (Weight: Negative)**
   - For every failed connection attempt, the peer loses 10 points (capped at -30 points).
   - This ensures the engine quickly pivots away from nodes with faulty BLE stacks.

3. **Staleness Penalty (Weight: Negative)**
   - If an advertisement hasn't been seen in 10 seconds, the score drops by 2 points per second (capped at -20 points).
   - This ensures we prioritize devices that are currently broadcasting strongly.

4. **Routing Hop Count Bonus (Weight: 20%)**
   - Direct connections (0 hops) receive a +20 bonus. 
   - 1 hop receives +10.
   - This keeps the mesh topology dense and avoids unnecessarily long relay chains when direct connections are available.

## Tie Breaking
In the event of identical scores, the `SmartConnectionPolicy` exponential backoff acts as the natural tie-breaker. Whichever node's backoff timer expires first will be connected to first.
