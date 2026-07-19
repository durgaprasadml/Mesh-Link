import re
import os

filepath = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath, 'r') as f:
    content = f.read()

# 1. Update imports
imports = """import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.annotation.VisibleForTesting
import com.meshlink.media.data.ImageCompressor
import java.io.File
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.flow.first
import android.net.Uri
import com.meshlink.util.NotificationHelper
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.MessageType
import com.meshlink.data.local.entity.MessageEntity
import com.meshlink.ble.model.MeshDevice
import com.meshlink.ble.model.PeerConnectionState
import com.meshlink.ble.model.MeshPacket
import com.meshlink.ble.model.PacketType
"""

# Replace package declaration and imports block if needed, but let's just insert after package.
content = re.sub(r'package com\.meshlink\.ble\.data\n', r'package com.meshlink.ble.data\n\n' + imports + '\n', content)

# 2. Fix constructor
old_constructor = r"private val transferManager: MediaTransferManager,"
new_constructor = r"""private val transferManager: com.meshlink.transfer.TransferManager,
    private val mediaTransferManager: com.meshlink.media.data.MediaTransferManager,
    private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,"""
content = content.replace(old_constructor, new_constructor)

# 3. Add scope
old_scope = r"private val TAG = \"MeshMessagingManager\""
new_scope = r"private val TAG = \"MeshMessagingManager\"\n    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)"
content = content.replace(old_scope, new_scope)

# 4. Replace routingCoordinator methods
content = content.replace("resolvePeerAddress(", "routingCoordinator.resolvePeerAddress(")
content = content.replace("networkId(", "routingCoordinator.networkId(")
content = content.replace("outgoingChatId(", "routingCoordinator.outgoingChatId(")
content = content.replace("incomingChatId(", "routingCoordinator.incomingChatId(")

# 5. Replace updatePeerState
content = content.replace("updatePeerState(", "connectionManager.updatePeerState(")

# 6. Fix scannedDevices
content = content.replace("scannedDevices.value.values.forEach", "discoveryManager.scannedDevices.value.values.forEach")

with open(filepath, 'w') as f:
    f.write(content)

print("Done")
