with open('app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt', 'r') as f:
    content = f.read()

# Replace discoveryEngine usages to go through discoveryManager
# Wait, discoveryManager doesn't expose discoveryEngine directly yet, but I can add it, or I can just keep discoveryEngine in BleRepositoryImpl constructor!
# Let's just keep discoveryEngine in BleRepositoryImpl constructor for now and only replace bleDataSource.

# Only replace bleDataSource usages that belong to DiscoveryManager
content = content.replace('bleDataSource.startAdvertising', 'discoveryManager.startAdvertising')
content = content.replace('bleDataSource.stopAdvertising()', 'discoveryManager.stopAdvertising()')
content = content.replace('bleDataSource.startScanning()', 'discoveryManager.startScanning()')
content = content.replace('bleDataSource.stopScanning()', 'discoveryManager.stopScanning()')
content = content.replace('bleDataSource.isAdvertising', 'discoveryManager.isAdvertising')
content = content.replace('bleDataSource.isScanning', 'discoveryManager.isScanning')
content = content.replace('bleDataSource.scannedDevices', 'discoveryManager.scannedDevices')

# Replace bleDataSource usages that belong to BleConnectionManager
content = content.replace('bleDataSource.startServer()', 'connectionManager.startServer()')
content = content.replace('bleDataSource.stopServer()', 'connectionManager.stopServer()')
content = content.replace('bleDataSource.connectToDevice', 'connectionManager.connectToDevice')
content = content.replace('bleDataSource.disconnectFromDevice', 'connectionManager.disconnectFromDevice')
content = content.replace('bleDataSource.connectedServers', 'connectionManager.connectedServers')
content = content.replace('bleDataSource.activeClients', 'connectionManager.activeClients')

with open('app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt', 'w') as f:
    f.write(content)
