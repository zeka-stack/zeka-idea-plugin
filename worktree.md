# **一、AI + worktree 一键脚本**

## **1️⃣ 脚本目标**

这个脚本解决 4 件事：

- 为 AI 创建 worktree + 分支
- 列出当前 AI worktree
- 完成后安全清理
- 防止误删 / 误用

------

## **2️⃣ 脚本：**

## **worktree.sh**

```
#!/usr/bin/env bash

set -e

BASE_BRANCH="dev"
PREFIX="ai"
WT_PREFIX="wt-ai"

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
dir="../${WT_PREFIX}-${task}"

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
```

### **使用方式**

```
chmod +x worktree.sh

./worktree.sh create javadoc
./worktree.sh create changelog
./worktree.sh list
./worktree.sh remove javadoc
```

------

## **3️⃣ 你立刻获得的能力**

- 每个 AI 都在 **独立沙盒**
- 分支 / 目录 / 语义强绑定
- 用完即删，不留“幽灵 worktree”

------

# **二、AI worktree 使用规范**

> **你可以直接放进 AI_WORKFLOW.md 或 CONTRIBUTING.md**



------

## **🚫 严禁事项**

### **❌ 1. 禁止 AI 使用 main / develop 分支**

```
AI 只能在 ai/* 分支中工作
```

------

### **❌ 2. 禁止多个 AI 共用一个 worktree**

```
1 AI = 1 worktree = 1 branch
```

------

### **❌ 3. 禁止 AI 直接 merge / rebase**

```
AI 只能：
- 修改代码
- 提交 commit

merge / rebase 只能由人完成
```

------

### **❌ 4. 禁止 AI 依赖其他 AI 的分支**

```
所有 AI 分支必须直接基于 dev
```

------

### **❌ 5. 禁止长期保留 AI worktree**

```
AI worktree 是一次性产物，用完必须删除
```

------

### **❌ 6. 禁止 AI 修改 Git 配置 / hooks / CI**

```
AI 不得修改：
- .gitconfig
- .git/hooks
- CI workflow
```

------

## **✅ 强制约定**

- 分支命名：ai/<task>
- 目录命名：wt-ai-<task>
- commit message 必须标明 AI 来源：

```
feat(javadoc): generate class-level Javadoc [AI]
```

------

# **三、AI 产出 → Review → Merge 的完整流水线**

这是**整个体系最重要的一部分**。



------

## **0️⃣ 总体结构图**

```
dev
 ├─ ai/javadoc     → wt-ai-javadoc     → commit
 ├─ ai/refactor    → wt-ai-refactor    → commit
 └─ ai/changelog   → wt-ai-changelog   → commit

          ↓
      人工 Review
          ↓
      人工 Merge 到 dev
          ↓
      删除 AI worktree
```

------

## **1️⃣ 创建任务**

```
./worktree.sh create javadoc
```

👉 把 ../wt-ai-javadoc 交给 AI



------

## **2️⃣ AI 工作阶段（AI）**

AI **只能**：

- 修改代码
- 提交 commit（可多次）

禁止：

- merge
- rebase
- pull dev

------

## **3️⃣ 冻结 AI 分支**

```
git checkout ai/javadoc
git log --oneline
```

确认 AI 工作完成



------

## **4️⃣ Review 阶段**

你只需要做 3 件事：

```
git diff dev..ai/javadoc
git log dev..ai/javadoc
git show <commit>
```

你在这一阶段是 **架构师 / 审核者**，不是执行者。



------

## **5️⃣ 合并策略（你）**

### **✅ 推荐方式一：merge**

```
git checkout dev
git merge ai/javadoc
```

适合：

- 功能型 AI
- 文档 / Javadoc / Changelog

------

### **✅ 推荐方式二：rebase**

```
git checkout ai/javadoc
git rebase dev
git checkout dev
git merge ai/javadoc
```

适合：

- 小而干净的 AI 改动

------

## **6️⃣ 清理**

```
./worktree.sh remove javadoc
```

------

# AI + Git Worktree 协作开发工作流规范

> 本文档定义了 **AI 参与代码开发时的统一工作流规范**，
> 目标是：**并行、高效、可控、可回滚、不污染主线**。

适用场景：

* 单人 / 小团队项目
* AI 作为“虚拟工程师”参与开发
* 多个 AI 同时处理不同任务（Javadoc、重构、生成代码、文档等）

---

## 一、核心设计原则（必须理解）

### 1. 人类是最终裁决者

* AI 只负责 **产出代码**
* **所有合并、发布、回滚必须由人类完成**

> AI ≠ Committer ≠ Maintainer

---

### 2. 强隔离：1 AI = 1 分支 = 1 Worktree

* 每个 AI 拥有：

    * 一个独立分支（`ai/<task>`）
    * 一个独立工作区（`wt-ai-<task>`）
* AI 之间 **绝不共享目录或分支**

---

### 3. Dev 是开发主分支

* `dev` 是开发主分支，保持：

    * 可构建
  * 可测试
* **所有 AI 分支都直接基于 `dev` 创建**
* `main` 是发布分支，仅用于发布稳定版本

---

## 二、分支与目录规范（强制）

### 1. 分支命名规范

```text
ai/<task>
```

示例：

* `ai/javadoc`
* `ai/refactor-psi`
* `ai/changelog`

---

### 2. Worktree 目录命名规范

```text
wt-ai-<task>
```

示例：

* `wt-ai-javadoc`
* `wt-ai-refactor-psi`

> ⚠️ 所有 worktree 目录 **必须位于主仓库同级目录**，不得嵌套在仓库内部

---

## 三、标准工作流（全流程）

### Step 0：前置条件

* 本地仓库 clean
* `dev` 为最新状态

```bash
git checkout dev
git pull --rebase
```

---

### Step 1：创建 AI Worktree（人类）

```bash
./worktree.sh create javadoc
```

效果：

* 创建分支：`ai/javadoc`
* 创建目录：`../wt-ai-javadoc`
* 分支基线：`dev`

---

### Step 2：AI 开始工作（AI）

AI **只允许**：

* 修改代码
* 新增 / 删除文件
* 本地提交 commit

AI **严禁**：

* `merge`
* `rebase`
* `pull dev`
* 修改 CI / Git 配置

---

### Step 3：冻结 AI 产出（人类）

确认 AI 已完成任务：

```bash
git checkout ai/javadoc
git log --oneline
```

---

### Step 4：Review 阶段（人类，关键）

必须完成以下检查：

```bash
git diff dev..ai/javadoc
git log dev..ai/javadoc
```

Review 重点：

* 是否符合设计意图
* 是否引入不必要的复杂度
* 是否影响公共 API / 行为

---

### Step 5：合并策略（人类）

#### 方案 A：Merge（推荐，保留 AI 来源）

```bash
git checkout dev
git merge ai/javadoc
```

适用：

* 文档
* Javadoc
* 生成代码

---

#### 方案 B：Rebase + Merge（线性历史）

```bash
git checkout ai/javadoc
git rebase dev
git checkout dev
git merge ai/javadoc
```

适用：

* 小而干净的 AI 改动

---

### Step 6：清理（必须）

```bash
./worktree.sh remove javadoc
```

效果：

* 删除 worktree 目录
* 删除 `ai/javadoc` 分支

---

## 四、禁止事项清单（🚫 必须遵守）

### 🚫 1. 禁止 AI 操作 main / dev / release 分支

```text
AI 只能在 ai/* 分支中工作
```

---

### 🚫 2. 禁止多个 AI 共用一个 worktree

```text
1 AI = 1 worktree = 1 branch
```

---

### 🚫 3. 禁止 AI 之间互相依赖

```text
AI 分支不得基于其他 ai/* 分支
```

---

### 🚫 4. 禁止 AI 直接参与合并

```text
merge / rebase 只能由人类完成
```

---

### 🚫 5. 禁止长期保留 AI worktree

```text
AI worktree 为一次性产物，用完即删
```

---

### 🚫 6. 禁止 AI 修改以下内容

* CI / Workflow
* Git hooks
* 版本号
* 发布脚本

---

## 五、Commit Message 规范（推荐）

所有 AI 提交必须显式标注来源：

```text
feat(javadoc): generate class-level Javadoc [AI]
refactor(psi): simplify visitor logic [AI]
```

---

## 六、推荐目录结构（示意）

```text
parent/
├─ project/                 ← 主仓库（dev 分支）
├─ wt-ai-javadoc/            ← AI #1
├─ wt-ai-refactor/           ← AI #2
└─ wt-ai-changelog/          ← AI #3
```

---

## 七、常见问题（FAQ）

### Q1：AI 能不能直接 push 到远端？

* 可以 push 到 **ai/* 分支**
* 不得 push 到 main 或 dev

---

### Q2：AI 产出不满意怎么办？

* 直接丢弃分支
* 删除 worktree
* 不影响主线

---

## 八、最终总结

> * **Worktree 是 AI 的执行空间**
> * **分支是 AI 的责任边界**
> * **Dev 是开发主分支**
> * **人类拥有最终合并权**

这套流程的目标不是“让 AI 更自由”，
而是：

> **让 AI 可控、可并行、可审计、可回滚**

---

*This workflow is intentionally conservative by design.*




