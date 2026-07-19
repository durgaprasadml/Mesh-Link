import re

filepath_manager = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath_manager, 'r') as f:
    content = f.read()

content = content.replace("private val context: Context,", "@ApplicationContext private val context: Context,")

if "import dagger.hilt.android.qualifiers.ApplicationContext" not in content:
    for i, line in enumerate(content.split('\n')):
        if line.startswith("import "):
            content = content[:content.find(line)] + "import dagger.hilt.android.qualifiers.ApplicationContext\n" + content[content.find(line):]
            break

with open(filepath_manager, 'w') as f:
    f.write(content)

print("Done")
