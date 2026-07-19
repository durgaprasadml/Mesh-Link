import re

# 1. BleRepositoryImplTest
with open('app/src/test/java/com/meshlink/ble/data/BleRepositoryImplTest.kt', 'r') as f:
    content = f.read()

# Replace bleDataSource verifications with discoveryManager/connectionManager
content = content.replace('bleDataSource.startAdvertising', 'discoveryManager.startAdvertising')
content = content.replace('bleDataSource.stopAdvertising()', 'discoveryManager.stopAdvertising()')
content = content.replace('bleDataSource.startScanning()', 'discoveryManager.startScanning()')
content = content.replace('bleDataSource.stopScanning()', 'discoveryManager.stopScanning()')

content = content.replace('bleDataSource.startServer()', 'connectionManager.startServer()')
content = content.replace('bleDataSource.stopServer()', 'connectionManager.stopServer()')
content = content.replace('bleDataSource.connectToDevice', 'connectionManager.connectToDevice')

with open('app/src/test/java/com/meshlink/ble/data/BleRepositoryImplTest.kt', 'w') as f:
    f.write(content)

# 2. UserRepositoryImplTest
with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'r') as f:
    content = f.read()

content = content.replace('assertEquals(mockUser, result.getOrNull())', 'assertEquals(User(mockUser.meshId, mockUser.name, mockUser.phoneNumber), result.getOrNull())')
content = content.replace('assertEquals(mockNewUser, result.getOrNull())', 'assertEquals(User(mockNewUser.meshId, mockNewUser.name, mockNewUser.phoneNumber), result.getOrNull())')
content = content.replace('import com.meshlink.database.data.local.UserEntity', 'import com.meshlink.database.data.local.UserEntity\nimport com.meshlink.domain.model.User')

with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'w') as f:
    f.write(content)

# 3. HybridTransportIntegrationTest
with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'r') as f:
    content = f.read()

# Instead of verifying chatDao, verify meshMessagingManager.handleIncomingPacket
content = re.sub(
    r'io\.mockk\.coVerify\(timeout = 1000\) \{ \s*chatDao\.insertMessageAndUpdateChat\(.*?\)\s*\}',
    r'''io.mockk.coVerify(timeout = 1000) { 
            meshMessagingManager.handleIncomingPacket(
                match { it.packetId == "test-packet" && it.payload == "hello" }
            )
        }''',
    content,
    flags=re.DOTALL
)

with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'w') as f:
    f.write(content)

print("Fixed remaining failing tests")
