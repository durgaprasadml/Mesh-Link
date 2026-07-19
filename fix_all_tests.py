import re

# 1. Fix UserDaoTest
with open('app/src/test/java/com/meshlink/database/data/local/UserDaoTest.kt', 'r') as f:
    content = f.read()
content = content.replace('insertUserEntity', 'insertUser')
content = content.replace('getUserEntity', 'getUser')
content = content.replace('getLocalUserEntity', 'getLocalUser')
with open('app/src/test/java/com/meshlink/database/data/local/UserDaoTest.kt', 'w') as f:
    f.write(content)

# 2. Fix UserRepositoryImplTest
with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'r') as f:
    content = f.read()
# Replace User(..., "hash") in mockk returns with UserEntity(..., "hash")
content = re.sub(r'User\(([^)]*),\s*"[^"]*"\)', r'UserEntity(\1, "dummyHash")', content)
content = re.sub(r'User\(([^)]*),\s*pinHash\s*=\s*"[^"]*"\)', r'UserEntity(\1, "dummyHash")', content)
# Also fix any remaining 4-arg User calls to UserEntity
content = re.sub(r'User\(\s*(".*?")\s*,\s*(".*?")\s*,\s*(".*?")\s*,\s*(".*?")\s*\)', r'UserEntity(\1, \2, \3, \4)', content)
with open('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'w') as f:
    f.write(content)

# 3. Fix HybridTransportIntegrationTest
with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'r') as f:
    content = f.read()
# Fix User(4 args) to User(3 args)
content = re.sub(r'User\(\s*(".*?")\s*,\s*(".*?")\s*,\s*(".*?")\s*,\s*(".*?")\s*\)', r'User(\1, \2, \3)', content)
with open('app/src/test/java/com/meshlink/ble/data/HybridTransportIntegrationTest.kt', 'w') as f:
    f.write(content)

# 4. Fix SendMessageUseCaseTest, AuthViewModelTest, HomeViewModelTest, NearbyViewModelTest, SettingsViewModelTest
for path in [
    'app/src/test/java/com/meshlink/domain/usecase/messaging/SendMessageUseCaseTest.kt',
    'app/src/test/java/com/meshlink/ui/auth/AuthViewModelTest.kt',
    'app/src/test/java/com/meshlink/ui/home/HomeViewModelTest.kt',
    'app/src/test/java/com/meshlink/ui/nearby/NearbyViewModelTest.kt',
    'app/src/test/java/com/meshlink/ui/settings/SettingsViewModelTest.kt'
]:
    with open(path, 'r') as f:
        content = f.read()
    # Fix User(4 args) or missing pinHash named param
    content = re.sub(r'User\(\s*(".*?")\s*,\s*(".*?")\s*,\s*(".*?")\s*,\s*(".*?")\s*\)', r'User(\1, \2, \3)', content)
    content = re.sub(r',\s*pinHash\s*=\s*".*?"', '', content)
    with open(path, 'w') as f:
        f.write(content)
