import re

with open('app/src/main/java/com/meshlink/ui/home/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('    onNavigateToChats: () -> Unit,\n', '')
content = content.replace('    onNavigateToMeshDebug: () -> Unit,\n', '')
content = content.replace('    onNavigateToAnalytics: () -> Unit,\n', '')

with open('app/src/main/java/com/meshlink/ui/home/HomeScreen.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/meshlink/ui/navigation/AppNavigation.kt', 'r') as f:
    content = f.read()

content = content.replace('                    onNavigateToChats = { navController.navigate(Screen.ChatsList.route) },\n', '')
content = content.replace('                    onNavigateToMeshDebug = { navController.navigate(Screen.DebugMesh.route) },\n', '')
content = content.replace('                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },\n', '')

with open('app/src/main/java/com/meshlink/ui/navigation/AppNavigation.kt', 'w') as f:
    f.write(content)
print("Removed unused parameters")
