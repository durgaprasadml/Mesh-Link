with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'r') as f:
    content = f.read()

teardown = """    @After
    fun teardown() {
        repository.cancelScope()
    }

    @Test"""
content = content.replace('    @Test', teardown, 1)

with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'w') as f:
    f.write(content)
print("Fixed leak")
