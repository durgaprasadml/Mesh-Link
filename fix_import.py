with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'r') as f:
    content = f.read()

content = content.replace('import org.junit.Before\n', 'import org.junit.Before\nimport org.junit.After\n')

with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'w') as f:
    f.write(content)
print("Added import")
