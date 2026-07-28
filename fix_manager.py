import re

file_path = "app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt"

with open(file_path, "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    # Delete DIAG-Stage logs
    if "DIAG-Stage" in line:
        continue
    
    # Replace RoutingCoordinator methods with canonicalize
    line = line.replace("routingCoordinator.networkId(", "com.meshlink.util.MeshIdNormalizer.canonicalize(")
    line = line.replace("routingCoordinator.outgoingChatId(", "com.meshlink.util.MeshIdNormalizer.canonicalize(")
    line = line.replace("routingCoordinator.incomingChatId(", "com.meshlink.util.MeshIdNormalizer.canonicalize(")
    line = line.replace("routingCoordinator.resolveChatId(", "com.meshlink.util.MeshIdNormalizer.canonicalize(")
    
    new_lines.append(line)

with open(file_path, "w") as f:
    f.writelines(new_lines)
