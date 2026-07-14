# Support Runbook (GA Update)

## Issue Triage Protocol (V2.0)

1. **Bluetooth Failure (GATT Error 133)**
   - *Symptom:* Device refuses to connect to nearby peers.
   - *Resolution:* The Android BLE stack has crashed. Instruct the user to toggle Bluetooth OFF and ON from the system quick settings, which forces a hardware reset of the BLE radio. The app will auto-recover.
2. **Wi-Fi Direct Failure (Group Owner Conflicts)**
   - *Symptom:* Large file transfers fail instantly.
   - *Resolution:* Two devices are fighting to be the Group Owner (GO). Instruct one user to disconnect from their current Wi-Fi network (if applicable), which drastically improves Wi-Fi Direct GO negotiation.
3. **Database Corruption**
   - *Symptom:* App crashes immediately on launch.
   - *Resolution:* Extreme physical power loss corrupted the SQLite WAL file. Instruct user to clear App Data. *Warning: This permanently deletes all local encryption keys and chat history.*
