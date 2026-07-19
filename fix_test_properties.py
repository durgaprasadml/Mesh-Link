import re

# 1. BleRepositoryImplTest
with open('app/src/test/java/com/meshlink/ble/data/BleRepositoryImplTest.kt', 'r') as f:
    content = f.read()

# Add properties
props = """    private lateinit var wifiSocketTransport: WifiSocketTransport
    private lateinit var discoveryManager: com.meshlink.ble.data.DiscoveryManager
    private lateinit var connectionManager: com.meshlink.ble.data.BleConnectionManager"""

content = content.replace('private lateinit var wifiSocketTransport: WifiSocketTransport', props)

# Change mockk(relaxed=true) to use the properties
new_setup = """            trustManager = trustManager,
            securityMonitor = securityMonitor,
            discoveryManager = discoveryManager,
            connectionManager = connectionManager,
            routingCoordinator = mockk(relaxed = true),
            meshMessagingManager = mockk(relaxed = true),
            voiceTransport = mockk(relaxed = true),
            videoTransport = mockk(relaxed = true),
            context = context
        )"""
content = re.sub(r'trustManager = trustManager,\s*securityMonitor = securityMonitor,\s*discoveryManager = mockk\(relaxed = true\),\s*connectionManager = mockk\(relaxed = true\),\s*routingCoordinator = mockk\(relaxed = true\),\s*meshMessagingManager = mockk\(relaxed = true\),\s*voiceTransport = mockk\(relaxed = true\),\s*videoTransport = mockk\(relaxed = true\),\s*context = context\s*\)', new_setup, content)

# Also need to init discoveryManager and connectionManager
init_block = """        wifiSocketTransport = mockk(relaxed = true)
        discoveryManager = mockk(relaxed = true)
        connectionManager = mockk(relaxed = true)"""
content = content.replace('wifiSocketTransport = mockk(relaxed = true)', init_block)

with open('app/src/test/java/com/meshlink/ble/data/BleRepositoryImplTest.kt', 'w') as f:
    f.write(content)

# 2. HybridTransportIntegrationTest
with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'r') as f:
    content = f.read()

# Add property
props = """    private lateinit var wifiSocketTransport: WifiSocketTransport
    private lateinit var meshMessagingManager: com.meshlink.ble.data.MeshMessagingManager"""
content = content.replace('private lateinit var wifiSocketTransport: WifiSocketTransport', props)

new_setup2 = """            trustManager = trustManager,
            securityMonitor = securityMonitor,
            discoveryManager = mockk(relaxed = true),
            connectionManager = mockk(relaxed = true),
            routingCoordinator = mockk(relaxed = true),
            meshMessagingManager = meshMessagingManager,
            voiceTransport = mockk(relaxed = true),
            videoTransport = mockk(relaxed = true),
            context = context
        )"""
content = re.sub(r'trustManager = trustManager,\s*securityMonitor = securityMonitor,\s*discoveryManager = mockk\(relaxed = true\),\s*connectionManager = mockk\(relaxed = true\),\s*routingCoordinator = mockk\(relaxed = true\),\s*meshMessagingManager = mockk\(relaxed = true\),\s*voiceTransport = mockk\(relaxed = true\),\s*videoTransport = mockk\(relaxed = true\),\s*context = context\s*\)', new_setup2, content)

init_block2 = """        wifiSocketTransport = mockk(relaxed = true)
        meshMessagingManager = mockk(relaxed = true)"""
content = content.replace('wifiSocketTransport = mockk(relaxed = true)', init_block2)

with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'w') as f:
    f.write(content)

# 3. UserRepositoryImplTest
with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'r') as f:
    content = f.read()
# Clean up duplicate/conflicting imports
lines = content.split('\n')
unique_imports = set()
new_lines = []
for line in lines:
    if line.startswith('import '):
        if line in unique_imports:
            continue
        unique_imports.add(line)
    new_lines.append(line)
with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'w') as f:
    f.write('\n'.join(new_lines))

print("Fixed test properties")
