#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> 编译 intelli-ai-engine..."
cd "${ROOT_DIR}/intelli-ai-engine"
./gradlew --no-daemon compileJava

echo "==> 编译完成"

