import re

filepath_repo = 'app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt'
with open(filepath_repo, 'r') as f:
    content = f.read()

content = content.replace("handleIncomingPacket(packet)", "meshMessagingManager.handleIncomingPacket(packet)")

with open(filepath_repo, 'w') as f:
    f.write(content)

filepath_manager = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath_manager, 'r') as f:
    content = f.read()

content = content.replace("private suspend fun handleIncomingPacket(packet: MeshPacket)", "suspend fun handleIncomingPacket(packet: MeshPacket)")

with open(filepath_manager, 'w') as f:
    f.write(content)

print("Done")
