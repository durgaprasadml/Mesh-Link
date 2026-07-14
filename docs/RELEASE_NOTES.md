# Release Notes (v1.0.0 Production Candidate)

## Overview
Mesh Link v1.0.0 marks our transition from experimental Beta to a Production Candidate. Over the past 6 phases, we have heavily fortified the application architecture, networking stack, and security layers.

## Highlights
- **100% Offline Communication**: Send text, images, and voice notes without any internet dependency.
- **True Mesh Routing**: Multi-hop routing allows communication to extend far beyond the physical range of a single Bluetooth radio.
- **Uncompromised Security**: Hardware-backed Keystore, SQLCipher encrypted persistence, and ECDH ephemeral session keys ensure total privacy.

## Known Issues
- Samsung devices on Android 14 may experience delayed Wi-Fi Direct Group Owner election due to aggressive MAC randomization. The app will gracefully fall back to BLE in this scenario.
