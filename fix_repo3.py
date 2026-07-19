import re

filepath_repo = 'app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt'
with open(filepath_repo, 'r') as f:
    content = f.read()

content = content.replace("dispatchSinglePacket", "meshMessagingManager.dispatchSinglePacket")
content = content.replace("dispatchMediaPackets", "meshMessagingManager.dispatchMediaPackets")
content = content.replace("receiveMediaMessage", "meshMessagingManager.receiveMediaMessage")

with open(filepath_repo, 'w') as f:
    f.write(content)

filepath_manager = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath_manager, 'r') as f:
    content = f.read()

content = content.replace("private fun dispatchSinglePacket", "fun dispatchSinglePacket")
content = content.replace("private fun dispatchMediaPackets", "fun dispatchMediaPackets")
content = content.replace("private suspend fun receiveMediaMessage", "suspend fun receiveMediaMessage")

with open(filepath_manager, 'w') as f:
    f.write(content)

print("Done")
