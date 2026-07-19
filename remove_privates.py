import re

filepath = 'app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt'
with open(filepath, 'r') as f:
    content = f.read()

# Define the methods to remove
methods_to_remove = [
    r'private suspend fun handleIncomingPacket\(packet: MeshPacket\) \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private fun dispatchMediaPackets\(targetPeerId: String, packets: List<MeshPacket>\): Boolean \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private fun dispatchSinglePacket\(targetPeerId: String, packet: MeshPacket\): Boolean \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private suspend fun receiveMessage\(packet: MeshPacket\) \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private suspend fun insertPlaceholderIncomingMedia\(packet: MeshPacket\) \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private suspend fun receiveLocationMessage\(packet: MeshPacket\) \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private suspend fun receiveBroadcastTextMessage\(packet: MeshPacket\) \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private suspend fun receiveSosMessage\(packet: MeshPacket\) \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
    r'private suspend fun handleWifiNegotiation\(packet: MeshPacket\) \{.*?(?=\n    private fun |\n    private suspend fun |\n\})',
]

for method_regex in methods_to_remove:
    content = re.sub(method_regex, '', content, flags=re.DOTALL)

with open(filepath, 'w') as f:
    f.write(content)

print("Removed methods")
