with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'r') as f:
    content = f.read()

content = content.replace('wifiSocketTransport = spyk(WifiSocketTransport())', 'wifiSocketTransport = spyk(WifiSocketTransport())\n        meshMessagingManager = mockk(relaxed = true)')

with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'w') as f:
    f.write(content)
print("Fixed initialization")
