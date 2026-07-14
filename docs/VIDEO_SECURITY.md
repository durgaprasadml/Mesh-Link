# Video Security

## End-to-End Encryption
Like all Mesh Link communications, video is heavily encrypted before it hits the network.

1. **Hardware Encoder**: Generates H.265 NAL units.
2. **Video Transport**: Extracts the NAL units and passes them to `MeshCryptoManager`.
3. **AES-GCM**: Each individual NAL unit is encrypted with AES-256-GCM using the symmetric session key established via ECDH.
4. **Network**: The encrypted ciphertext is routed over BLE or Wi-Fi.

## Screen Sharing Security
Screen sharing utilizes the `MediaProjection` API. 
- It mandates a **Foreground Service** notification so the user is unequivocally aware their screen is being captured.
- The `MediaProjection` token is requested via a system-level permission dialog (`startActivityForResult`), ensuring malicious background capture is impossible.
- The screen capture surface is piped directly into the hardware encoder, meaning other parts of the app cannot passively read the pixels off the heap.
