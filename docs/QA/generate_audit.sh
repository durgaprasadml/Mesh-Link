#!/bin/bash
echo "# UI Hardcoded Values Audit" > docs/QA/HARDCODED_DP_REPORT.md
echo "Files containing hardcoded .dp values outside of theme package:" >> docs/QA/HARDCODED_DP_REPORT.md
echo "\`\`\`" >> docs/QA/HARDCODED_DP_REPORT.md
grep -rnw "app/src/main/java/com/meshlink/ui" -e "\.dp" | grep -v "designsystem/theme" | awk -F: '{print $1 ": " $2}' >> docs/QA/HARDCODED_DP_REPORT.md
echo "\`\`\`" >> docs/QA/HARDCODED_DP_REPORT.md
echo "Audit complete."
