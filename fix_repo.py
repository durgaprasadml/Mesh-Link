import re

filepath = 'app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt'
with open(filepath, 'r') as f:
    content = f.read()

# Add meshMessagingManager to constructor
if "private val meshMessagingManager: MeshMessagingManager" not in content:
    content = content.replace(
        "private val routingCoordinator: RoutingCoordinator",
        "private val routingCoordinator: RoutingCoordinator,\n    private val meshMessagingManager: MeshMessagingManager"
    )

# Replace sendMessage
content = re.sub(
    r'override suspend fun sendMessage\(message: com\.meshlink\.domain\.model\.Message, chatName: String\) \{.*?(?=\n    override suspend fun sendImage|\n    override fun dispatchTextMessage)',
    r'override suspend fun sendMessage(message: com.meshlink.domain.model.Message, chatName: String) {\n        meshMessagingManager.sendMessage(message, chatName)\n    }\n',
    content, flags=re.DOTALL
)

# Replace sendImage
content = re.sub(
    r'override suspend fun sendImage\(targetMeshId: String, imageUri: Uri, chatName: String\) \{.*?(?=\n    override suspend fun sendVoiceNote|\n    override fun dispatchTextMessage)',
    r'override suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {\n        meshMessagingManager.sendImage(targetMeshId, imageUri, chatName)\n    }\n',
    content, flags=re.DOTALL
)

# Replace sendVoiceNote
content = re.sub(
    r'override suspend fun sendVoiceNote\(targetMeshId: String, filePath: String, durationMs: Long, chatName: String\) \{.*?(?=\n    override suspend fun sendLocation|\n    override fun dispatchTextMessage)',
    r'override suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {\n        meshMessagingManager.sendVoiceNote(targetMeshId, filePath, durationMs, chatName)\n    }\n',
    content, flags=re.DOTALL
)

# Replace sendLocation
content = re.sub(
    r'override suspend fun sendLocation\(targetMeshId: String, chatName: String\) \{.*?(?=\n    override suspend fun sendReadReceipts|\n    override fun dispatchTextMessage)',
    r'override suspend fun sendLocation(targetMeshId: String, chatName: String) {\n        meshMessagingManager.sendLocation(targetMeshId, chatName)\n    }\n',
    content, flags=re.DOTALL
)

# Replace sendReadReceipts
content = re.sub(
    r'override suspend fun sendReadReceipts\(chatId: String\) \{.*?(?=\n    override suspend fun sendSos|\n    override fun dispatchTextMessage)',
    r'override suspend fun sendReadReceipts(chatId: String) {\n        meshMessagingManager.sendReadReceipts(chatId)\n    }\n',
    content, flags=re.DOTALL
)

# Replace sendSos
content = re.sub(
    r'override suspend fun sendSos\(\) \{.*?(?=\n    override suspend fun broadcastMessage|\n    override fun dispatchTextMessage)',
    r'override suspend fun sendSos() {\n        meshMessagingManager.sendSos()\n    }\n',
    content, flags=re.DOTALL
)

# Replace broadcastMessage
content = re.sub(
    r'override suspend fun broadcastMessage\(messageText: String\) \{.*?(?=\n    override fun getMeshStatus|\n    override fun dispatchTextMessage)',
    r'override suspend fun broadcastMessage(messageText: String) {\n        meshMessagingManager.broadcastMessage(messageText)\n    }\n',
    content, flags=re.DOTALL
)

with open(filepath, 'w') as f:
    f.write(content)

print("Done")
