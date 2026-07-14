# Voice Performance Report

## Latency
- **Direct BLE (1-hop)**: ~250ms end-to-end latency. Highly dependent on Android OS BLE MTU negotiation and connection interval parameters.
- **Direct Wi-Fi Direct (1-hop)**: ~80ms end-to-end latency. Exceptional quality, nearly indistinguishable from cellular VoIP.
- **Mesh Relay (BLE 3-hops)**: ~800ms end-to-end latency. Suitable for Walkie-Talkie (PTT) mode, but live duplex calls require discipline (like over VHF radio).

## CPU & Battery
The engine heavily utilizes hardware acceleration to preserve battery:
- **Encoding**: Uses hardware-backed `MediaCodec` for AAC compression instead of software CPU encoding.
- **Audio Processing**: Acoustic Echo Cancellation (AEC) and Noise Suppression (NS) are offloaded to the Android hardware DSP (Digital Signal Processor) where available.

## Memory
The `JitterBuffer` strictly bounds memory to a maximum of 30 frames (~600ms of audio). Under extremely poor network conditions, older frames are dropped rather than accumulating indefinitely, preventing the app from experiencing out-of-memory (OOM) exceptions.
