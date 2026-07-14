# Voice Security

All voice communication in Mesh Link adheres to strict Enterprise Security standards.

## Encryption
Unlike standard analog radios (which can be easily intercepted by SDRs), every single `VOICE_FRAME` and `VOICE_SIGNAL` packet is encrypted using the existing `MeshCryptoManager`.

### Algorithm
- **Cipher**: AES-256-GCM.
- **Key Exchange**: ECDH (Elliptic Curve Diffie-Hellman) ensures that the symmetric AES key is never transmitted over the air.
- **Forward Secrecy**: Enforced through session re-keying.

## Streaming Security
Because the `AudioStreamer` converts PCM into AAC frames, and passes them to `VoiceTransport`, the encryption occurs at the very edge of the application layer before touching the network layer (`MeshRouter`).

No plaintext audio frames are ever buffered to disk, and memory containing PCM arrays is overwritten rapidly by the `AudioEngine` continuous read loop.
