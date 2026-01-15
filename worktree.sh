#!/usr/bin/env bash

set -e

BASE_BRANCH="main"
PREFIX="ai"
WT_PREFIX="wt-ai"
BASE_DIR="../zeka-idea-plugin-worktree"

usage() {
  cat <<EOF
Usage:
  $0 create <task>
  $0 list
  $0 remove <task>

Examples:
  $0 create javadoc
  $0 create refactor
  $0 list
  $0 remove javadoc
EOF
}

cmd="$1"
task="$2"

branch="${PREFIX}/${task}"
dir="${BASE_DIR}/${WT_PREFIX}-${task}"

case "$cmd" in
  create)
    if [[ -z "$task" ]]; then
      echo "❌ task is required"
      usage
      exit 1
    fi

    if git show-ref --verify --quiet "refs/heads/${branch}"; then
      echo "❌ branch ${branch} already exists"
      exit 1
    fi

    echo "🚀 Creating AI worktree:"
    echo "  branch: ${branch}"
    echo "  dir:    ${dir}"
    echo "  base:   ${BASE_BRANCH}"

    git worktree add "${dir}" -b "${branch}" "${BASE_BRANCH}"
    echo "✅ Done"
    ;;

  list)
    git worktree list
    ;;

  remove)
    if [[ -z "$task" ]]; then
      echo "❌ task is required"
      usage
      exit 1
    fi

    echo "🧹 Removing AI worktree:"
    echo "  branch: ${branch}"
    echo "  dir:    ${dir}"

    git worktree remove "${dir}"
    git branch -d "${branch}"
    echo "✅ Cleaned"
    ;;

  *)
    usage
    ;;
esac
