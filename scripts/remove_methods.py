import re

with open("app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt", "r") as f:
    content = f.read()

# I will replace the methods with empty strings.
# networkId
content = re.sub(r'private fun networkId\(peerId: String\): String = BleConstants.toNetworkId\(peerId\)', '', content)
# normalizePeerId
content = re.sub(r'private fun normalizePeerId\(peerIdOrAddress: String\): String \{.*?\}', '', content, flags=re.DOTALL)
# resolveChatId
content = re.sub(r'override fun resolveChatId\(peerIdOrAddress: String\): String = normalizePeerId\(peerIdOrAddress\)', 'override fun resolveChatId(peerIdOrAddress: String): String = routingCoordinator.resolveChatId(peerIdOrAddress)', content)
# outgoingChatId
content = re.sub(r'private fun outgoingChatId\(targetMeshId: String\): String = normalizePeerId\(targetMeshId\)', '', content)
# incomingChatId
content = re.sub(r'private fun incomingChatId\(senderMeshId: String\): String = normalizePeerId\(senderMeshId\)', '', content)

with open("app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt", "w") as f:
    f.write(content)
