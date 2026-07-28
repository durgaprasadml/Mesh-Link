# Mesh Link Protocol Threat Model

## 1. Attacker Assumptions
- **Dolev-Yao Attacker Model**: The attacker can eavesdrop, intercept, modify, and inject any packets on the mesh network.
- **Physical Proximity**: The attacker may be physically close enough to any node to communicate via BLE/Wi-Fi Direct.
- **Node Compromise**: The attacker does not have physical access or root access to a trusted node's secure enclave or storage. Compromised nodes are treated as unverified peers.

## 2. Trusted Components
- **Local Keystore**: The Android hardware-backed Keystore / EncryptedSharedPreferences where identity keys and session keys are stored.
- **Security Manager**: The local node's `MeshCryptoManager` and `MeshSecurityMonitor` processes which enforce policies.
- **Secure Clock**: System clocks used for timestamps and replay protection.

## 3. Protected Assets
- **Payload Confidentiality**: User messages, file transfers, and sensitive commands must remain unreadable by intermediate nodes and passive eavesdroppers.
- **Payload Integrity**: Any modification to the ciphertext or routing metadata MUST invalidate the payload.
- **Session Keys**: Ephemeral session keys established via ECDH must remain secret.

## 4. Attack Surface
- **BLE Advertisement Payloads**: Eavesdropping and spoofing identity.
- **GATT Characteristics**: Sending malformed, oversized, or replayed packets to the Rx characteristic.
- **Wi-Fi Direct Sockets**: Flooding sockets with connection attempts or garbage data.
- **Mesh Routing Protocol**: Maliciously dropping packets (blackholing) or manipulating TTL/Hop counts to disrupt routing.

## 5. Excluded Threats
- **Denial of Service (Radio Jamming)**: Physical layer jamming of 2.4GHz bands is out of scope for the software protocol.
- **Hardware Extraction**: Advanced physical extraction of keys from the SoC/Secure Enclave (e.g., using electron microscopes) is out of scope.
- **Compromised OS**: Root-level malware on the user's Android device reading memory buffers before encryption.

## 6. Protection Mechanisms
- **Replay Protection**: Strict 64-packet sliding window (similar to IPsec/WireGuard).
- **Encryption**: AES-256-GCM ensuring confidentiality and authenticated encryption (AEAD).
- **Downgrade Protection**: Rejecting unencrypted packets unless the protocol mode explicitly permits it (e.g., SOS mode).
- **Session Expiry**: Sessions expire after 30 minutes of inactivity to limit the blast radius of a compromised key.
