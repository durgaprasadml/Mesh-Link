import re
with open('/Users/durgaprasadml/.gemini/antigravity-ide/brain/864f2e3b-d6d0-4be0-a47f-a9a97d48b047/task.md', 'r') as f:
    content = f.read()

content = content.replace('- [ ] Create `RoutingCoordinator`', '- [x] Create `RoutingCoordinator`')
content = content.replace('- [ ] Create `MeshMessagingManager`', '- [x] Create `MeshMessagingManager`')
content = content.replace('- [ ] Create `MediaTransferManager`', '- [x] Create `MediaTransferManager`')
content = content.replace('- [ ] Create `VoiceManager`', '- [x] Create `VoiceManager`')
content = content.replace('- [ ] Create `VideoManager`', '- [x] Create `VideoManager`')
content = content.replace('- [ ] Create `SecurityCoordinator`', '- [x] Isolate Security Modules (MeshSecurityMonitor)')
content = content.replace('- [ ] Create `WifiCoordinator`', '- [x] Isolate Wifi Modules (WifiDirectManager, WifiSocketTransport)')
content = content.replace('- [ ] Create `MeshLifecycleManager`', '- [x] Create `MeshLifecycleManager`')
content = content.replace('- [ ] Update `BleRepositoryImpl` to act as coordinator/facade', '- [x] Update `BleRepositoryImpl` to act as coordinator/facade')

with open('/Users/durgaprasadml/.gemini/antigravity-ide/brain/864f2e3b-d6d0-4be0-a47f-a9a97d48b047/task.md', 'w') as f:
    f.write(content)
print("Updated task.md")
