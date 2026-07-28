# Troubleshooting Guide

This guide helps diagnose and resolve common issues encountered when using or developing Mesh Link V3.0.

## BLE Discovery Failures

**Symptoms:** Devices are not finding each other; no connections are initiated.

**Diagnostic Steps:**
1.  **Permissions:** Ensure `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, and `ACCESS_FINE_LOCATION` are granted.
2.  **Hardware limitations:** Some Android devices cannot act as BLE peripheral (Broadcaster) and Central (Scanner) simultaneously. Check the device's BLE capabilities in the logs.
3.  **Interference:** Ensure devices are within 5-10 meters. Check for heavy 2.4GHz Wi-Fi interference.
4.  **Logging:** Check the `Transport` logs for "Advertising failed" or "Scanner failed to start" errors.

## Connection Instability

**Symptoms:** Connections are established but drop frequently (GATT Error 133 or similar).

**Diagnostic Steps:**
1.  **Connection Parameters:** Android's BLE stack can be aggressive. Ensure you are requesting `CONNECTION_PRIORITY_BALANCED` or `HIGH` during active data transfer.
2.  **MTU Negotiation:** If a connection drops immediately after handshake, verify that both sides support the negotiated MTU size.
3.  **Timeouts:** Check if the security handshake is taking too long, causing the transport layer to time out.

## Routing Issues

**Symptoms:** Node A is connected to Node B, and Node B to Node C, but Node A cannot send a message to Node C.

**Diagnostic Steps:**
1.  **TTL:** Check if the routing TTL is set too low. If TTL=1, packets will not hop.
2.  **Routing Table:** Export the Diagnostics Snapshot and check the routing table on Node B. Does it have a valid route to C?
3.  **Logs:** Look for "Packet dropped" logs on Node B. It might be dropping the packet due to a duplicated Sequence Number or full buffer.

## Message Delivery Failures

**Symptoms:** `sendMessage` returns success, but the destination never receives it.

**Diagnostic Steps:**
1.  **Asynchronous nature:** `sendMessage` success only means the packet was enqueued to the local transport/routing layer. It does not guarantee end-to-end delivery unless an application-level ACK is implemented.
2.  **Security validation:** The destination might be dropping the packet if the signature is invalid or the Nonce is rejected (replay attack prevention). Check the `Security` module logs on the receiving device.

## Session Problems

**Symptoms:** Handshake fails; devices disconnect immediately.

**Diagnostic Steps:**
1.  **Downgrade Attack Detection:** If one node requires V3 security and the other is attempting V2, the handshake will be intentionally aborted.
2.  **Key Rotation:** If devices lose sync on their key rotation schedules, packets will fail decryption. Force a disconnect to trigger a fresh handshake.

## Build Issues

**Symptoms:** Gradle sync fails or build fails.

**Diagnostic Steps:**
1.  **Java Version:** Ensure you are using Java 17+.
2.  **Clean Build:** Run `./gradlew clean` and invalidate IDE caches.
3.  **NDK:** If using Tink or native crypto libraries, ensure the Android NDK is installed.
