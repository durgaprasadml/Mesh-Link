import os
import re

ui_dir = "app/src/main/java/com/meshlink/ui"

spacing_map = {
    "2.dp": "MeshTheme.spacing.extraSmall",
    "4.dp": "MeshTheme.spacing.small",
    "8.dp": "MeshTheme.spacing.mediumSmall",
    "12.dp": "MeshTheme.spacing.medium",
    "16.dp": "MeshTheme.spacing.mediumLarge",
    "20.dp": "MeshTheme.spacing.large",
    "24.dp": "MeshTheme.spacing.extraLarge",
    "32.dp": "MeshTheme.spacing.huge",
    "40.dp": "MeshTheme.spacing.extraHuge",
    "48.dp": "MeshTheme.spacing.giant",
    "64.dp": "MeshTheme.spacing.extraGiant",
}

shapes_map = {
    "RoundedCornerShape(4.dp)": "MeshTheme.shapes.extraSmall",
    "RoundedCornerShape(8.dp)": "MeshTheme.shapes.small",
    "RoundedCornerShape(12.dp)": "MeshTheme.shapes.medium",
    "RoundedCornerShape(16.dp)": "MeshTheme.shapes.large",
    "RoundedCornerShape(24.dp)": "MeshTheme.shapes.extraLarge",
}

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    original_content = content

    # 1. Replace shapes
    for old, new in shapes_map.items():
        content = content.replace(old, new)

    # 2. Replace spacing (be careful to only match exact numbers)
    # Using regex to ensure we don't replace "124.dp" when searching for "24.dp"
    for old, new in spacing_map.items():
        # Match boundaries like non-digit before the number
        pattern = r'(?<!\d)' + re.escape(old)
        content = re.sub(pattern, new, content)

    # Add MeshTheme import if we added MeshTheme usages and it's not imported
    if "MeshTheme." in content and "import com.meshlink.ui.designsystem.theme.MeshTheme" not in content:
        # Find the last import statement
        import_index = content.rfind("import ")
        if import_index != -1:
            end_of_line = content.find("\n", import_index)
            content = content[:end_of_line] + "\nimport com.meshlink.ui.designsystem.theme.MeshTheme" + content[end_of_line:]

    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

def main():
    for root, dirs, files in os.walk(ui_dir):
        for file in files:
            if file.endswith(".kt"):
                process_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
