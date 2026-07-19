with open('/Users/durgaprasadml/.gemini/antigravity-ide/brain/864f2e3b-d6d0-4be0-a47f-a9a97d48b047/task.md', 'r') as f:
    content = f.read()

content = content.replace('- [ ] Replace `Icons.Filled.ArrowBack` with `Icons.AutoMirrored`', '- [x] Replace `Icons.Filled.ArrowBack` with `Icons.AutoMirrored`')
content = content.replace('- [ ] Replace `Divider` with `HorizontalDivider`', '- [x] Replace `Divider` with `HorizontalDivider`')
content = content.replace('- [ ] Fix Multiple ViewModels coordination and deprecated searchbars', '- [x] Fix Multiple ViewModels coordination and deprecated searchbars')

with open('/Users/durgaprasadml/.gemini/antigravity-ide/brain/864f2e3b-d6d0-4be0-a47f-a9a97d48b047/task.md', 'w') as f:
    f.write(content)
print("Updated task.md")
