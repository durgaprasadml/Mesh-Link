import re

def add_import(filepath, import_str):
    with open(filepath, 'r') as f:
        content = f.read()
    if import_str not in content:
        # insert after package
        content = re.sub(r'(package .*?\n)', r'\1\nimport ' + import_str + '\n', content)
    with open(filepath, 'w') as f:
        f.write(content)

# Fix HybridTransportIntegrationTest imports
add_import('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'com.meshlink.domain.model.MeshPacket')
add_import('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'com.meshlink.domain.model.PacketType')

# Fix UserRepositoryImplTest import and method name
add_import('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'com.meshlink.database.data.local.UserEntity')

with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'r') as f:
    content = f.read()
content = content.replace('loginUserEntity', 'loginUser')
with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'w') as f:
    f.write(content)

print("Imports fixed")
