import sys

def extract_methods(filepath, methods_to_extract):
    with open(filepath, 'r') as f:
        lines = f.readlines()

    extracted = {}
    remaining_lines = []
    
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Check if line matches a method signature
        matched_method = None
        for m in methods_to_extract:
            if m in line and ("fun " in line or "val " in line):
                matched_method = m
                break
                
        if matched_method:
            # Found a method, collect it until braces match
            method_lines = []
            brace_count = 0
            started = False
            
            # If there's an annotation on the previous lines, we might miss it, but we'll assume no annotations for these private methods.
            # Or we can check previous lines.
            
            while i < len(lines):
                curr_line = lines[i]
                method_lines.append(curr_line)
                brace_count += curr_line.count('{')
                brace_count -= curr_line.count('}')
                if '{' in curr_line:
                    started = True
                
                if started and brace_count == 0:
                    break
                i += 1
                
            extracted[matched_method] = "".join(method_lines)
        else:
            remaining_lines.append(line)
        i += 1
        
    return extracted, "".join(remaining_lines)

if __name__ == '__main__':
    methods = [
        "fun networkId",
        "fun normalizePeerId",
        "fun resolveChatId",
        "fun outgoingChatId",
        "fun incomingChatId",
        "fun resolvePeerAddress",
        "fun hasDeliveryPath",
        "fun dispatchTextMessage",
        "fun dispatchMediaPackets",
        "fun dispatchSinglePacket",
        "fun encryptAndWrapPayload",
        "fun checkAndTriggerHandshake",
        "fun handleKeyExchange",
        "fun generateSignedKeyExchange",
        "fun handleIncomingPacket",
    ]
    extracted, remaining = extract_methods("app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl.kt", methods)
    
    # Save remaining
    with open("app/src/main/java/com/meshlink/ble/data/BleRepositoryImpl_Remaining.kt", "w") as f:
        f.write(remaining)
        
    # Save extracted
    with open("app/src/main/java/com/meshlink/ble/data/RoutingCoordinator_Extracted.kt", "w") as f:
        for m, code in extracted.items():
            f.write(code + "\n")
            
    print(f"Extracted {len(extracted)} methods.")
