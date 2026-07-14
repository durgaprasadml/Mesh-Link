# Voice Protocol

The Mesh Link Voice Protocol relies on the `MeshPacket` structure, utilizing two new `PacketType` enums:
1. `VOICE_SIGNAL`
2. `VOICE_FRAME`

## Voice Signal Packets
Signaling handles the setup, teardown, and configuration of voice sessions.

**Payload Format (JSON):**
```json
{
  "type": "INVITE | ACCEPT | REJECT | END | PTT_START | PTT_STOP",
  "callId": "uuid-string",
  "targetId": "peer-id",
  "codec": "AAC",
  "isPtt": false
}
```

## Voice Frame Packets
Streaming packets contain the actual encoded audio. They must be compact to survive the strict MTU limits of Bluetooth Low Energy (BLE).

**Payload Format (String Split):**
`{callId}:{sequenceNumber}:{Base64EncodedAACFrame}`

- **callId**: Associates the frame with the active `VoiceSession`.
- **sequenceNumber**: A monotonically increasing Long. Used by the `JitterBuffer` to reorder packets arriving out of sync over the mesh.
- **Base64EncodedAACFrame**: The raw output from Android's `MediaCodec`, encoded in Base64 for safe JSON/String transport across the legacy routing layer.

## Transport Selection
- **BLE**: Extremely low bandwidth. `VoiceCodecManager` caps AAC bitrate to **16kbps**. Voice frames are sent rapidly. Multi-hop voice relay works, but latency increases by ~200ms per hop.
- **Wi-Fi Direct**: High bandwidth. Bitrate scales to **64kbps** automatically. Ideal for full-duplex Live Voice Calls and Group Calls.
