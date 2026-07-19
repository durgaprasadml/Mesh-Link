def add_import(filepath, new_import):
    with open(filepath, 'r') as f:
        content = f.read()
    if new_import not in content:
        content = content.replace('import androidx.compose.material.icons.Icons', f'import androidx.compose.material.icons.Icons\n{new_import}')
        with open(filepath, 'w') as f:
            f.write(content)

add_import('app/src/main/java/com/meshlink/ui/analytics/AnalyticsScreen.kt', 'import androidx.compose.material.icons.automirrored.filled.Send')
add_import('app/src/main/java/com/meshlink/ui/settings/SettingsScreen.kt', 'import androidx.compose.material.icons.automirrored.filled.ExitToApp')
print("Fixed imports")
