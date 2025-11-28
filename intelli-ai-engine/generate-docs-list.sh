#!/usr/bin/env bash
# 生成 docs/docs-list.json

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/" && pwd)"
DOCS_DIR="${PROJECT_DIR}/docs"
OUTPUT_FILE="${DOCS_DIR}/docs-list.json"

if [[ ! -d "${DOCS_DIR}" ]]; then
  echo "Docs directory not found: ${DOCS_DIR}" >&2
  exit 1
fi

DOC_FILES=()
while IFS= read -r -d '' file; do
  DOC_FILES+=("$file")
done < <(find "${DOCS_DIR}" -maxdepth 1 -type f -name '*.md' -print0 | sort -z)

if [[ ${#DOC_FILES[@]} -eq 0 ]]; then
  echo "No markdown files found under ${DOCS_DIR}" >&2
  exit 1
fi

DOCS_DIR="${DOCS_DIR}" OUTPUT_FILE="${OUTPUT_FILE}" \
python3 <<'PY'
import json
import os
from pathlib import Path

docs_dir = Path(os.environ["DOCS_DIR"])
output_file = Path(os.environ["OUTPUT_FILE"])

entries = []
for file_path in sorted(docs_dir.glob("*.md")):
    entries.append({
        "name": file_path.stem,
        "path": f"./docs/{file_path.name}"
    })

output_file.write_text(json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Generated {len(entries)} entries into {output_file}")
PY

