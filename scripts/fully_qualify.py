with open('app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt', 'r') as f:
    content = f.read()

import re

# Remove imports so we don't have unused import errors
content = content.replace('import com.meshlink.domain.model.MeshPacket\n', '')
content = content.replace('import com.meshlink.domain.model.PacketType\n', '')

# Replace usages
content = re.sub(r'(?<!\.)\bMeshPacket\b', 'com.meshlink.domain.model.MeshPacket', content)
content = re.sub(r'(?<!\.)\bPacketType\b', 'com.meshlink.domain.model.PacketType', content)

with open('app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt', 'w') as f:
    f.write(content)
