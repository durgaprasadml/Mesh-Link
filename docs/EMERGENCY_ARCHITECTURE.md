# Emergency Architecture (Phase E9)

Mesh Link has been upgraded to a mission-critical emergency communication platform capable of offline disaster response. 

## Architectural Foundations

### 1. Preemptive Priority Queueing
The `QueueOptimizer` now evaluates packets using a strict priority queue. Packets are evaluated against `PacketPriority`:
- **CRITICAL**: SOS, Beacons, Command Broadcasts
- **HIGH**: Medical Tags, Incident Reports, Forms
- **NORMAL**: Standard Text, Location
- **LOW**: File Transfers
- **BACKGROUND**: Route discovery, background sync

CRITICAL packets skip the line entirely and are transmitted immediately over all available transports (BLE + Wi-Fi Direct).

### 2. Emergency Mode Override
The `EmergencyManager` handles global state overrides. When active:
- Standard battery throttling in `BatteryAwareNetworking` is bypassed.
- Background media syncing is paused.
- The radio amplifiers are sustained to ensure maximum transmission range.

### 3. Dedicated Subsystems
- **Incident Management:** `IncidentManager` and `EmergencyFormSync` handle structured data dissemination (Damage reports, Medical status).
- **Search & Rescue:** `EmergencyBeacon` emits periodic ultra-low overhead location broadcasts. `TeamTracker` maintains real-time offline status of team members.
- **Logistics:** `ResourceManager` manages offline inventory synchronization.

### 4. Disaster Recovery Engine
The `DisasterRecoveryEngine` detects mesh partitions and commands mass-reconnects, firing temporary high-power discovery spikes to re-establish broken links after sudden disruptions (e.g., EMP, physical barriers).

## Offline-First Guarantee
Everything remains strictly local, peer-to-peer, and encrypted. The emergency modules utilize the exact same `TrustManager` and AES-256-GCM encryption as standard operations. No cloud dependencies are introduced.
