# Public Safety & First Responder Guide

Mesh Link can be deployed by First Responders (Police, Fire, EMS) to coordinate without cell towers.

## Operating Procedures

### 1. Activating Emergency Mode
When deploying into a disaster zone, commanders should toggle **Emergency Mode** on their devices.
- This instructs the local mesh routing engine to ignore power saving constraints and dedicate 100% of the radio time to routing.
- Expect increased battery drain. Pair with external power banks.

### 2. SOS Beacon
If a responder is trapped or injured, activate the **Emergency Beacon**.
- The device will emit a `CRITICAL` priority payload continuously.
- The interval automatically scales based on the remaining battery life to maximize survivability (e.g., 5 minutes between pings at 15% battery).

### 3. Medical Tagging
Medics can assign encrypted `MedicalTag` records to individuals. These tags securely replicate across the mesh, allowing other units to prepare treatments before the patient arrives at the extraction point.

### 4. Resource Allocation
Logistics officers use the `ResourceManager` to track physical supplies (Water, Medical kits) distributed across the area. Deductions happen locally and sync eventually.
