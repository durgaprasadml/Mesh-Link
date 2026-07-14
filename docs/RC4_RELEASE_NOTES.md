# Mesh Link RC4 - Release Notes

## Version: Release Candidate 4 (RC4)
**Objective:** Large-Scale Mesh Validation, Chaos Engineering & Disaster Simulation

## Overview
RC4 certifies Mesh Link for extreme environments, disaster recovery (earthquake/flood), and massive node topology scales (100+ users). We simulated aggressive network partitioning, random node failures, transport failovers, broadcast storms, and active security interception.

## Architectural Limit Bumps
To accommodate massive scale without crashing, two internal constraints were expanded:
1. **Queue Optimizer:** Expanded `MAX_QUEUE_SIZE` from `1000` to `10000` to safely buffer massive text bursts.
2. **Deduplication Cache:** Expanded `DEDUP_CACHE_SIZE` from `2000` to `20000` to ensure complete immunity to broadcast storms during 100-node full-mesh floods.

## Validated Core Behaviors
- **Transport Switchover:** BLE to Wi-Fi Direct (and back) fails over without data loss or UI interruption.
- **Partition Resilience:** Sub-meshes correctly queue messages offline and auto-sync when physical proximity is restored.
- **Database ACIDity:** Resilient to ungraceful power loss during transactions.
- **Security Immunity:** Rogue malformed JSON and spoofed ECDSA packets are safely dropped.

**Status:** Certified Fault-Tolerant for Real-World Deployment.
