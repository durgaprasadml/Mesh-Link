# Broadcast Optimization

## Preventing Packet Storms
In a mesh of 1000 nodes, a single broadcast message can theoretically amplify into 1,000,000 relay attempts if unchecked.

### `BroadcastOptimizationManager`
1. **TTL Enforcement**: Drops packets that exceed the max hop count.
2. **Duplicate Cache**: Tracks packet signatures for 60 seconds. Duplicate relays are aggressively dropped.
3. **Density Suppression**: In clusters of >50 peers, a node has a 30% chance to voluntarily ignore relaying a broadcast, relying on its neighbors to propagate it, slashing network noise dramatically.
