# RC2 Crypto Latency Report

## Overview
This report covers cryptographic operation latency.

## Optimizations
1. **ECDH Key Caching:**
   - Validated existing in-memory LRU cache (`derivedKeys`) for Shared Secrets.
   - Prevents re-computing Elliptic Curve Diffie-Hellman derivations per packet.
2. **AES-GCM Throughput:**
   - Retained efficient `Cipher` initialization.

## Expected Metrics
- **Per-Packet Crypto Latency:** < 2ms (reduced from ~15ms if un-cached).
