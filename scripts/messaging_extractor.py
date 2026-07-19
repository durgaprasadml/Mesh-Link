import re

impl_file = "app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt"
msg_file = "app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt"

with open(impl_file, 'r') as f:
    lines = f.readlines()

start_idx = -1
end_idx = -1
for i, line in enumerate(lines):
    if "private suspend fun handleIncomingPacket" in line:
        start_idx = i
    elif "private suspend fun receiveBroadcastTextMessage" in line:
        # find the end of this function
        brace_count = 0
        in_func = False
        for j in range(i, len(lines)):
            if "{" in lines[j]:
                brace_count += lines[j].count("{")
                in_func = True
            if "}" in lines[j]:
                brace_count -= lines[j].count("}")
            if in_func and brace_count == 0:
                end_idx = j
                break

if start_idx != -1 and end_idx != -1:
    extracted_lines = lines[start_idx:end_idx+1]
    
    # Write to msg_file
    with open(msg_file, 'r') as f:
        msg_lines = f.readlines()
    
    for i, line in enumerate(msg_lines):
        if "    // ... we will copy the methods here ..." in line:
            # We insert here
            # But wait, some methods have `override ` which we might need to keep if MeshMessagingManager implements an interface, but it doesn't.
            # So we remove `override `
            clean_lines = [l.replace("override ", "") for l in extracted_lines]
            msg_lines[i:i+1] = clean_lines
            break
            
    with open(msg_file, 'w') as f:
        f.writelines(msg_lines)
        
    print("Extracted to MeshMessagingManager!")
    
    # We leave the original file alone for now, but we'll remove it in a separate step or just do it here
