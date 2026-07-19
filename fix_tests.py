import re

def replace_in_file(filepath, old, new):
    with open(filepath, 'r') as f:
        content = f.read()
    content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file('app/src/test/java/com/meshlink/database/data/local/UserDaoTest.kt', 'User(', 'UserEntity(')
replace_in_file('app/src/test/java/com/meshlink/database/data/local/UserDaoTest.kt', 'import com.meshlink.domain.model.User', 'import com.meshlink.database.data.local.UserEntity')

replace_in_file('app/src/test/java/com/meshlink/database/data/local/UserEntityTest.kt', 'User(', 'UserEntity(')
replace_in_file('app/src/test/java/com/meshlink/database/data/local/UserEntityTest.kt', 'import com.meshlink.domain.model.User', 'import com.meshlink.database.data.local.UserEntity')

replace_in_file('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'User(meshId = "mesh_1", name = "Alice", phoneNumber = "12345", pinHash = "hash")', 'User(meshId = "mesh_1", name = "Alice", phoneNumber = "12345")')
replace_in_file('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'UserEntity(meshId = "mesh_1", name = "Alice", phoneNumber = "12345", pinHash = "hash")', 'UserEntity(meshId = "mesh_1", name = "Alice", phoneNumber = "12345", pinHash = "hash")')
replace_in_file('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'expectedUser = User(', 'expectedUser = UserEntity(')
replace_in_file('app/src/test/java/com/meshlink/core/data/UserRepositoryImplTest.kt', 'assertEquals(expectedUser', 'assertEquals(com.meshlink.domain.model.User(meshId = "mesh_1", name = "Alice", phoneNumber = "12345")')

print("Fixed UserDaoTest, UserEntityTest, UserRepositoryImplTest")
