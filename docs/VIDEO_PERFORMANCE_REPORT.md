# Video Performance Report

## Zero-Copy Architecture
The most significant performance optimization in Phase E6 is the complete elimination of CPU-bound byte array copying for video frames.
1. `CameraX` outputs directly to an Android `Surface`.
2. `MediaCodec` encodes that Surface in hardware.
3. The resulting NAL units are transmitted.
4. The receiver's `MediaCodec` decodes the NAL units directly onto a UI `SurfaceView`.

This means the uncompressed YUV video data (which is hundreds of megabytes per second at 720p 30FPS) never touches the Kotlin heap or the CPU, resulting in a **dramatic reduction in battery consumption and thermal throttling**.

## Adaptive Bitrate & Resolution
- **Wi-Fi Direct**: Comfortably supports 720p at 30 FPS using ~1.5 Mbps bitrate.
- **BLE Fallback**: If Wi-Fi Direct drops, the system must immediately scale the encoder down to 144p or 240p at 5 FPS to survive the BLE bandwidth bottleneck.

## Memory Profiling
Because frames are rendered directly to hardware surfaces, memory footprint remains flat. The only heap allocations during a video call are the encrypted NAL byte arrays, which are rapidly garbage collected.
