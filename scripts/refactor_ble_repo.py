import re

with open('app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt', 'r') as f:
    content = f.read()

# 1. Update constructor
content = re.sub(
    r'private val bleDataSource: BleMeshDataSource,\n',
    r'private val discoveryManager: DiscoveryManager,\n    private val connectionManager: BleConnectionManager,\n',
    content
)
content = re.sub(
    r'private val discoveryEngine: com\.meshlink\.ble\.discovery\.DiscoveryEngine,\n',
    r'',
    content
)

# 2. Update peerStates property
content = re.sub(
    r'    private val peerStates = ConcurrentHashMap<String, PeerConnectionState>\(\)\n',
    r'',
    content
)

# 3. Replace updatePeerState and checkAndTriggerHandshake
# Wait, checkAndTriggerHandshake is still in BleRepositoryImpl
content = re.sub(
    r'    private fun updatePeerState\(address: String, newState: PeerConnectionState\) \{[\s\S]*?    private fun updateAnalyticsConnectionCount\(\) \{[\s\S]*?    \}',
    r'',
    content
)

# 4. Fix bleDataSource calls
content = content.replace('bleDataSource.startAdvertising', 'discoveryManager.startAdvertising')
content = content.replace('bleDataSource.stopAdvertising()', 'discoveryManager.stopAdvertising()')
content = content.replace('bleDataSource.startScanning()', 'discoveryManager.startScanning()')
content = content.replace('bleDataSource.stopScanning()', 'discoveryManager.stopScanning()')
content = content.replace('bleDataSource.isAdvertising', 'discoveryManager.isAdvertising()')
content = content.replace('bleDataSource.isScanning', 'discoveryManager.isScanning()')
content = content.replace('bleDataSource.scannedDevices', 'discoveryManager.scannedDevices')
content = content.replace('bleDataSource.startServer()', 'connectionManager.startServer()')
content = content.replace('bleDataSource.stopServer()', 'connectionManager.stopServer()')
content = content.replace('bleDataSource.connectToDevice', 'connectionManager.connectToDevice')
content = content.replace('bleDataSource.disconnectFromDevice', 'connectionManager.disconnectFromDevice')
content = content.replace('bleDataSource.connectedServers', 'connectionManager.connectedServers')
content = content.replace('bleDataSource.activeClients', 'connectionManager.activeClients')

# 5. Fix peerStates usages
content = content.replace('peerStates[address]', 'connectionManager.getPeerState(address)')
content = content.replace('peerStates[record.macAddress]', 'connectionManager.getPeerState(record.macAddress)')
content = content.replace('updatePeerState(address', 'connectionManager.updatePeerState(address')
# Wait, checkAndTriggerHandshake still does peerStates[address] = ...
content = content.replace('peerStates[address] = ', 'connectionManager.updatePeerState(address, ')
content = content.replace('PeerConnectionState.KEY_EXCHANGE_STARTED', 'PeerConnectionState.KEY_EXCHANGE_STARTED)')
content = content.replace('PeerConnectionState.SESSION_READY', 'PeerConnectionState.SESSION_READY)')

# 6. Fix discoveryEngine usages
content = content.replace('discoveryEngine.', 'discoveryManager.discoveryEngine.')

with open('app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt', 'w') as f:
    f.write(content)
