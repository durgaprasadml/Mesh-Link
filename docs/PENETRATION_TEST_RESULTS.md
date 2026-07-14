# Penetration Test Results (Threat Model Simulation)

This report details simulated adversarial attacks against the Mesh Link infrastructure.

## 1. Local Network Sniffing
- **Threat Vector**: Attacker runs a Wireshark node in physical proximity, capturing all BLE advertisements and Wi-Fi Direct TCP packets.
- **Result**: ✅ Mitigated. The attacker only captures AES-GCM ciphertexts. Without the ECDH negotiated session key, the data is mathematically useless. 

## 2. Packet Replay Attack
- **Threat Vector**: Attacker captures a valid encrypted packet and repeatedly blasts it over the BLE mesh to trigger fake notifications or corrupt the database.
- **Result**: ✅ Mitigated. `TrustManager` validates the AES-GCM nonce and Sequence Number. Sequence numbers must be strictly monotonically increasing. Replays are dropped silently at the transport boundary.

## 3. Man-In-The-Middle (MITM) Identity Spoofing
- **Threat Vector**: Attacker attempts to impersonate a trusted node by broadcasting an identical Mesh ID.
- **Result**: ✅ Mitigated. The Mesh ID is derived from the SHA-256 hash of the node's ECDSA public key. Unless the attacker possesses the private key stored securely in the victim's hardware Keystore, they cannot compute valid signatures. 

## 4. Resource Exhaustion (DoS)
- **Threat Vector**: Attacker floods the BLE network with garbage data to kill battery life and block legitimate traffic.
- **Result**: ✅ Mitigated. Mesh Link implements rate limiting. If a node repeatedly sends unauthenticated garbage, it is added to a temporary Blocklist, and its packets are ignored at the BLE Scanner level.
