#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "用法: $0 <模块目录>"
  echo "示例: $0 intelli-ai-engine"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

MODULE_DIR="${1:-}"
if [[ -z "${MODULE_DIR}" ]]; then
  usage
  exit 1
fi

if [[ ! -d "${MODULE_DIR}" ]]; then
  echo "错误: 目录不存在: ${MODULE_DIR}" >&2
  exit 1
fi

if [[ ! -x "${MODULE_DIR}/gradlew" ]]; then
  echo "错误: 未找到可执行的 Gradle Wrapper: ${MODULE_DIR}/gradlew" >&2
  echo "提示: 请确认模块目录正确，或先执行: chmod +x ${MODULE_DIR}/gradlew" >&2
  exit 1
fi

pick_java() {
  # 1) 已显式设置 JAVA_HOME
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    export PATH="${JAVA_HOME}/bin:${PATH}"
    return 0
  fi

  # 2) 优先使用 asdf 安装的 JDK（避免依赖系统 Java 或 Homebrew）
  if command -v asdf >/dev/null 2>&1; then
    local asdf_java_home
    asdf_java_home="$(asdf where java 2>/dev/null | tr -d '\r' | awk 'NF{print $1; exit}' || true)"
    if [[ -n "${asdf_java_home}" && -x "${asdf_java_home}/bin/java" ]]; then
      export JAVA_HOME="${asdf_java_home}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      return 0
    fi
  fi

  # 3) 回退：系统已有 java（如果存在）
  if command -v java >/dev/null 2>&1; then
    return 0
  fi

  return 1
}

if ! pick_java; then
  echo "错误: 未找到可用的 Java Runtime。" >&2
  echo "建议: 使用 asdf 安装并启用 JDK，例如:" >&2
  echo "  asdf plugin add java && asdf install java temurin-21.0.9+10.0.LTS && asdf local java temurin-21.0.9+10.0.LTS" >&2
  exit 1
fi

echo "Java 信息:"
java -version

echo
echo "开始编译: ${MODULE_DIR}"
(cd "${MODULE_DIR}" && ./gradlew compileJava)

