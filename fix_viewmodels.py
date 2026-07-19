import re

files = [
    'app/src/test/java/com/meshlink/ui/auth/AuthViewModelTest.kt',
    'app/src/test/java/com/meshlink/ui/home/HomeViewModelTest.kt',
    'app/src/test/java/com/meshlink/ui/nearby/NearbyViewModelTest.kt',
    'app/src/test/java/com/meshlink/domain/usecase/messaging/SendMessageUseCaseTest.kt',
    'app/src/test/java/com/meshlink/ui/settings/SettingsViewModelTest.kt'
]

for filepath in files:
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Remove pinHash from User(...) calls
    content = re.sub(r'User\(([^)]*),\s*pinHash\s*=\s*"[^"]*"\)', r'User(\1)', content)
    
    with open(filepath, 'w') as f:
        f.write(content)

print("Fixed viewmodel tests")
