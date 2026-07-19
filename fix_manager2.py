import re
import os

filepath = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath, 'r') as f:
    content = f.read()

# Fix wrong imports
content = content.replace("import com.meshlink.domain.model.DeliveryStatus", "import com.meshlink.database.data.local.DeliveryStatus")
content = content.replace("import com.meshlink.domain.model.MessageType", "import com.meshlink.database.data.local.MessageType")

# Fix scope
if "val scope =" not in content:
    content = content.replace('private val TAG = "MeshMessagingManager"', 'private val TAG = "MeshMessagingManager"\n    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)')

# Write back
with open(filepath, 'w') as f:
    f.write(content)

print("Done")
