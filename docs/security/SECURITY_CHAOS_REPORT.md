# Security Chaos Report

## Scenario: Malicious Interception & Injection
- **Action:** A rogue node injects malformed JSON, invalid signatures, expired TTLs, and altered ciphertexts into the mesh network.
- **Response:**
  - **Malformed Packets:** Deserializer catches `JsonSyntaxException` and drops the packet. No crash.
  - **Altered Ciphertext:** AES-GCM MAC validation fails intrinsically. Packet dropped silently.
  - **Replay Attacks:** Packet UUIDs exist in the dedup cache (`RoutingEngine`), immediately dropping duplicates.
  - **Spoofed Signatures:** ECDSA verification fails against the public key attached to the mesh peer.

**Status:** Mesh Link is structurally immune to passive interception and active packet modification.
