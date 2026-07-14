# QoS Architecture

The `QoSManager` dictates packet priority. It allows mission-critical data to bypass standard restrictions.

## Priorities

1. **CRITICAL**
   - Applies to: `PacketType.SOS`
   - Behavior: Bypasses Congestion Control. Bypasses directed-routing (always broadcasted). Granted maximum possible TTL (20 hops).
2. **HIGH**
   - Applies to: `KEY_EXCHANGE`, `WIFI_NEGOTIATION`, `SESSION_REKEY`
   - Behavior: Normal routing but guaranteed immediate processing.
3. **MEDIUM**
   - Applies to: `TEXT`, `DELIVERY_ACK`, `READ_RECEIPT`
   - Behavior: Standard routing. May be delayed if network is Highly congested.
4. **LOW**
   - Applies to: `MEDIA_META`, `MEDIA_CHUNK`, `LOCATION`
   - Behavior: Background delivery. First to be dropped or queued during High/Critical congestion.
