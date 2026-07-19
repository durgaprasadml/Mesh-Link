filepath = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath, 'r') as f:
    lines = f.readlines()

new_lines = []
imports = set()
in_imports = False

for line in lines:
    if line.startswith("import "):
        # fix wrong ones
        line = line.replace("com.meshlink.data.local.entity.MessageEntity", "com.meshlink.database.data.local.MessageEntity")
        line = line.replace("com.meshlink.domain.model.DeliveryStatus", "com.meshlink.database.data.local.DeliveryStatus")
        line = line.replace("com.meshlink.domain.model.MessageType", "com.meshlink.database.data.local.MessageType")
        line = line.replace("com.meshlink.ble.model.MeshDevice", "com.meshlink.domain.model.BleDevice")
        line = line.replace("com.meshlink.domain.model.MeshDevice", "com.meshlink.domain.model.BleDevice")
        line = line.replace("com.meshlink.ble.model.MeshPacket", "com.meshlink.domain.model.MeshPacket")
        line = line.replace("com.meshlink.ble.model.PacketType", "com.meshlink.domain.model.PacketType")
        line = line.replace("com.meshlink.ble.model.PeerConnectionState", "com.meshlink.ble.data.PeerConnectionState")
        
        imports.add(line.strip())
    else:
        new_lines.append(line)

# find package
for i, line in enumerate(new_lines):
    if line.startswith("package "):
        insert_idx = i + 1
        break

sorted_imports = sorted(list(imports))
for imp in reversed(sorted_imports):
    new_lines.insert(insert_idx, imp + "\n")
new_lines.insert(insert_idx, "\n")

with open(filepath, 'w') as f:
    f.writelines(new_lines)

print("Done")
