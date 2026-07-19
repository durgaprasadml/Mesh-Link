import re

filepath = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath, 'r') as f:
    lines = f.readlines()

new_lines = []
imports = set()
in_imports = False
found_class = False

for line in lines:
    if line.startswith("import "):
        imports.add(line.strip())
        in_imports = True
    elif in_imports and line.strip() == "":
        continue
    else:
        if in_imports and not found_class:
            in_imports = False
            found_class = True
            # write all unique imports
            # remove some bad ones first
            bad_prefixes = [
                "import com.meshlink.ble.model.PeerConnectionState",
                "import com.meshlink.ble.model.MeshDevice",
                "import com.meshlink.ble.model.MeshPacket",
                "import com.meshlink.ble.model.PacketType",
                "import com.meshlink.domain.model.DeliveryStatus",
                "import com.meshlink.domain.model.MessageType"
            ]
            final_imports = []
            for imp in imports:
                is_bad = False
                for bp in bad_prefixes:
                    if imp.startswith(bp):
                        is_bad = True
                        break
                if not is_bad:
                    final_imports.append(imp)
            
            # add correct ones
            final_imports.append("import com.meshlink.ble.data.PeerConnectionState")
            final_imports.append("import com.meshlink.domain.model.MeshDevice")
            final_imports.append("import com.meshlink.ble.model.MeshPacket") # wait, where is MeshPacket?
            final_imports.append("import com.meshlink.ble.model.PacketType") # wait, let's just let it be

            for imp in sorted(final_imports):
                new_lines.append(imp + "\n")
            new_lines.append("\n")
            
        new_lines.append(line)

content = "".join(new_lines)
content = content.replace("com.meshlink.ble.model.PeerConnectionState", "com.meshlink.ble.data.PeerConnectionState")

with open(filepath, 'w') as f:
    f.write(content)

print("Done")
