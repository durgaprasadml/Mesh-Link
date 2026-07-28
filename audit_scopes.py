import os
import re

directory = '/Users/durgaprasadml/Documents/Mesh-Link/app/src/main/java/com/meshlink'
regex = re.compile(r'CoroutineScope\([^)]*\)')

print("Files creating their own CoroutineScope:")
for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
                matches = regex.findall(content)
                if matches:
                    rel_path = os.path.relpath(filepath, directory)
                    print(f"{rel_path}: {matches}")
