# File Support Matrix

The Enterprise File Transfer Engine natively supports any binary file format via the robust `MEDIA_META` parser. 

## Supported Formats

| Category | MIME Type Prefix | Supported Formats |
| :--- | :--- | :--- |
| **Images** | `image/` | JPG, PNG, WEBP, GIF |
| **Audio** | `audio/` | M4A, MP3, AAC, WAV |
| **Video** | `video/` | MP4, MKV, AVI, MOV |
| **Documents**| `application/` | PDF, ZIP, APK, JSON |
| **Data** | `text/` | TXT, CSV, vCard |

## Limitations
- **BLE Transport**: Extremely slow for files > 1MB. The `ChunkManager` forces a 300-byte raw payload boundary due to the ~512-byte MTU limit.
- **Wi-Fi Transport**: Preferred for all media files > 500KB. 

## Compression Strategy
- **Images**: Pre-compressed heavily (JPEG 30-45, max 800px) prior to transfer initialization to keep BLE times under 15 seconds.
- **Videos / ZIP**: Left uncompressed, relying entirely on the Wi-Fi Direct data plane for high-throughput transmission.
