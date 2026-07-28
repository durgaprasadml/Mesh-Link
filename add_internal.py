import os
import re

count = 0
for root, _, files in os.walk('app/src/main/java/com/meshlink'):
    for file in files:
        if file.endswith('Impl.kt') or file == 'MtuNegotiationManager.kt':
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            # find class name
            match = re.search(r'^(?!internal\s)(?:public\s)?class\s+([A-Za-z0-9_]+Impl)\b', content, re.MULTILINE)
            if match:
                new_content = content[:match.start()] + 'internal ' + content[match.start():]
                with open(path, 'w') as f:
                    f.write(new_content)
                count += 1
                print(f"Added internal to {file}")
            
print(f"Total updated: {count}")
