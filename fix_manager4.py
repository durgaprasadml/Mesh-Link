filepath = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("private val wifiDirectManager: com.meshlink.wifi.WifiDirectManager,", "private val wifiDirectManager: com.meshlink.wifi.data.WifiDirectManager,")

with open(filepath, 'w') as f:
    f.write(content)

print("Fixed")
