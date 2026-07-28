import os
import re

count = 0
for root, _, files in os.walk('app/src/main/java/com/meshlink'):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
                
            # If it has dispatcher and applicationScope.launch {
            if "dispatcher" in content and "applicationScope.launch {" in content:
                # Replace with applicationScope.launch(dispatcher) {
                new_content = content.replace("applicationScope.launch {", "applicationScope.launch(dispatcher) {")
                with open(path, 'w') as f:
                    f.write(new_content)
                count += 1
                print(f"Fixed {file}")
            # Same for defaultDispatcher
            elif "defaultDispatcher" in content and "applicationScope.launch {" in content:
                new_content = content.replace("applicationScope.launch {", "applicationScope.launch(defaultDispatcher) {")
                with open(path, 'w') as f:
                    f.write(new_content)
                count += 1
                print(f"Fixed {file}")

print(f"Total fixed: {count}")
