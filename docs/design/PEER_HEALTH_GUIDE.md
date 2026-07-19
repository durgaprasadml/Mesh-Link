# Peer Health Guide

## Trust and Stability Scoring
In a MANET, peer reliability is everything. The `PeerIntelligenceManager` tracks the lifecycle of every hashed Peer ID.

### The `PeerHealthScore`
1. **EXCELLENT**: Packet success > 95%, Latency < 100ms. Reliable relay node.
2. **GOOD**: Standard connection.
3. **WARNING**: Packet success < 80% or Latency > 500ms. The peer is likely moving out of range or suffering interference.
4. **CRITICAL**: Reconnects > 10 within a short window, or packet success < 50%. This peer should NOT be used for relaying messages as it creates network partitions.
