import re

for filepath in ['app/src/test/java/com/meshlink/ble/data/BleRepositoryImplTest.kt', 'app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt']:
    with open(filepath, 'r') as f:
        content = f.read()

    # Find where BleRepositoryImpl( is called
    match = re.search(r'BleRepositoryImpl\((.*?)\)', content, flags=re.DOTALL)
    if match:
        old_args = match.group(0)
        
        # Replace the call with mockk arguments for the new constructor
        new_call = """BleRepositoryImpl(
            application = mockk(relaxed = true),
            bleDataSource = mockk(relaxed = true),
            meshRouter = mockk(relaxed = true),
            chatDao = mockk(relaxed = true),
            userRepository = mockk(relaxed = true),
            transferManager = mockk(relaxed = true),
            mediaTransferManager = mockk(relaxed = true),
            locationProvider = mockk(relaxed = true),
            cryptoManager = mockk(relaxed = true),
            wifiDirectManager = mockk(relaxed = true),
            wifiSocketTransport = mockk(relaxed = true),
            sessionManager = mockk(relaxed = true),
            rekeyManager = mockk(relaxed = true),
            trustManager = mockk(relaxed = true),
            securityMonitor = mockk(relaxed = true),
            discoveryManager = mockk(relaxed = true),
            connectionManager = mockk(relaxed = true),
            routingCoordinator = mockk(relaxed = true),
            meshMessagingManager = mockk(relaxed = true),
            voiceTransport = mockk(relaxed = true),
            videoTransport = mockk(relaxed = true),
            context = mockk(relaxed = true)
        )"""
        
        # We also need to map the variables that the test actually uses to the constructor arguments
        if "HybridTransportIntegrationTest" in filepath:
            new_call = """BleRepositoryImpl(
            application = app,
            bleDataSource = bleDataSource,
            meshRouter = meshRouter,
            chatDao = chatDao,
            userRepository = userRepository,
            transferManager = mockk(relaxed = true),
            mediaTransferManager = mediaTransferManager,
            locationProvider = locationProvider,
            cryptoManager = cryptoManager,
            wifiDirectManager = wifiDirectManager,
            wifiSocketTransport = wifiSocketTransport,
            sessionManager = sessionManager,
            rekeyManager = rekeyManager,
            trustManager = trustManager,
            securityMonitor = securityMonitor,
            discoveryManager = mockk(relaxed = true),
            connectionManager = mockk(relaxed = true),
            routingCoordinator = mockk(relaxed = true),
            meshMessagingManager = mockk(relaxed = true),
            voiceTransport = mockk(relaxed = true),
            videoTransport = mockk(relaxed = true),
            context = context
        )"""
        else:
            new_call = """BleRepositoryImpl(
            application = application,
            bleDataSource = bleDataSource,
            meshRouter = meshRouter,
            chatDao = chatDao,
            userRepository = userRepository,
            transferManager = mockk(relaxed = true),
            mediaTransferManager = mediaTransferManager,
            locationProvider = locationProvider,
            cryptoManager = cryptoManager,
            wifiDirectManager = wifiDirectManager,
            wifiSocketTransport = wifiSocketTransport,
            sessionManager = sessionManager,
            rekeyManager = rekeyManager,
            trustManager = trustManager,
            securityMonitor = securityMonitor,
            discoveryManager = mockk(relaxed = true),
            connectionManager = mockk(relaxed = true),
            routingCoordinator = mockk(relaxed = true),
            meshMessagingManager = mockk(relaxed = true),
            voiceTransport = mockk(relaxed = true),
            videoTransport = mockk(relaxed = true),
            context = context
        )"""
        
        content = content.replace(old_args, new_call)
        
    # Also replace MeshPacket and PacketType in HybridTransportIntegrationTest.kt since they might have old imports
    if "HybridTransportIntegrationTest" in filepath:
        content = content.replace("import com.meshlink.ble.model.MeshPacket", "import com.meshlink.domain.model.MeshPacket")
        content = content.replace("import com.meshlink.ble.model.PacketType", "import com.meshlink.domain.model.PacketType")

    with open(filepath, 'w') as f:
        f.write(content)

print("Done")
