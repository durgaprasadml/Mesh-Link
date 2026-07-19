# AI Privacy & Security

## 100% Offline Guarantee
The defining feature of the Phase E8 Intelligence Platform is that it is **strictly offline**.
- No cloud inference.
- No OpenAI / Gemini API calls.
- No telemetry leaves the device.

## Data Privacy
The `LearningRepository` learns metadata (packet sizes, connection times, success rates), but it **NEVER** analyzes or logs payload contents. 
- All data stored in `SharedPreferences` is highly localized and anonymous.
- The `UserBehaviorEngine` predicts transfer sizes and frequencies to optimize buffer allocations, but does not track *who* you are talking to in a personally identifiable way outside of the ephemeral Mesh ID.

## Threat Detection
The `AnomalyDetector` seamlessly integrates with the `TrustManager` to provide active defense:
- **DDoS / Flood Attacks**: Detects if a specific node is pushing 20x their normal traffic volume and flags them for potential throttling.
- **Broadcast Storms**: Detects anomalous spikes in network-wide broadcasts, allowing the local `RoutingEngine` to sever connection chains that are looping malicious traffic.
