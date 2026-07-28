import os
import sys

def check_imports(root_dir, packages_to_check):
    usage = {p: [] for p in packages_to_check}
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.kt'):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        content = f.read()
                        for p in packages_to_check:
                            if f"import com.meshlink.{p}" in content and f"package com.meshlink.{p}" not in content:
                                usage[p].append(path)
                except Exception:
                    pass
    
    for p, files in usage.items():
        if not files:
            print(f"Package {p} is UNUSED outside itself.")
        else:
            print(f"Package {p} is USED in: {len(files)} files")
            for f in files[:3]:
                print(f"  - {f}")

check_imports('app/src/main/java/com/meshlink', ['ai', 'analytics', 'enterprise', 'wifi', 'video', 'emergency', 'recovery', 'routing', 'scalability', 'security', 'transfer'])
