# Incident Management Guide

## Overview
The Incident Management subsystem in Mesh Link allows for distributed, offline coordination of disaster scenarios.

## Concepts
1. **Incident Declaration:** Any node with `COMMAND` trust level can declare an incident. 
2. **Incident Types:** Pre-defined types such as `EARTHQUAKE`, `MEDICAL`, `WILDFIRE`.
3. **Emergency Forms:** Structured JSON forms used for Damage Reports, Medical Reports, and Situation Reports (SITREPs).

## Synchronization
Since there is no central server, the `EmergencyFormSync` module uses a flood-fill broadcast with `HIGH` priority. 
- Local forms are stored in memory.
- `MeshRouter` intercepts incoming `FORM_SYNC` packets and routes them to `EmergencyFormSync`.
- A background routine ensures eventual consistency across the entire mesh.

## Team Tracking
The `TeamTracker` passively listens for location and status updates. If a team member fails to check in within 5 minutes (`OFFLINE_THRESHOLD_MS`), they are marked as `OFFLINE`, alerting nearby commanders.
