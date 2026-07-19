import re

for filepath in ['app/src/test/java/com/meshlink/ble/data/BleRepositoryImplTest.kt', 'app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt']:
    with open(filepath, 'r') as f:
        content = f.read()

    # Find where BleRepositoryImpl is assigned
    # It starts with "repository = BleRepositoryImpl("
    # We can replace everything from "repository = BleRepositoryImpl(" to the next "    }" (which closes the @Before block)
    
    match = re.search(r'repository = BleRepositoryImpl\(.*?\)\s*\}', content, flags=re.DOTALL)
    if match:
        old_block = match.group(0)
        
        if "HybridTransportIntegrationTest" in filepath:
            new_call = """repository = BleRepositoryImpl(
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
        )
    }"""
        else:
            new_call = """repository = BleRepositoryImpl(
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
        )
    }"""
        
        content = content.replace(old_block, new_call)
        
    with open(filepath, 'w') as f:
        f.write(content)

print("Done")
