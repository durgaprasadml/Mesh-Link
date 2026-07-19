filepath = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("import com.meshlink.ble.model.MeshDevice", "import com.meshlink.domain.model.BleDevice")
content = content.replace("import com.meshlink.domain.model.MeshDevice", "import com.meshlink.domain.model.BleDevice")
content = content.replace("import com.meshlink.ble.model.MeshPacket", "import com.meshlink.domain.model.MeshPacket")
content = content.replace("import com.meshlink.ble.model.PacketType", "import com.meshlink.domain.model.PacketType")

content = content.replace("com.meshlink.ble.model.PeerConnectionState", "com.meshlink.ble.data.PeerConnectionState")

with open(filepath, 'w') as f:
    f.write(content)

print("Done")
