# Commit Message 上下文构建方案

本文档通过对比分析主流插件的实现方式，总结出 `intelli-ai-changelog` 插件的 commit message 上下文构建最终方案。

## 一、参考实现分析

### 1.1 SweepAI 实现分析

本文基于反编译文件 `reference/sweepai/jetbrains-1.27.0/.../SweepCommitMessageService.java` 及相关 utils 的实现，梳理 commit message 的 diff
与上下文构建方式。

#### 总体流程

- 入口：`SweepCommitMessageService.generateCommitMessage(...)`
- 获取分支名、默认 changelist、选中变更
- 生成 diff 文本（完整或 partial）
- 组装 `CommitMessageRequest`：
    - `context` = diff 文本
    - `previous_commits` = 最近提交（过滤 merge PR）
    - `branch` = 当前分支名
    - `commit_template` = 可选模板
- 发送请求生成 commit message

#### PartialChangesUtilsKt（部分提交）

- `getPartialChanges(...)`
    - 依赖 `PartialLocalLineStatusTracker`
    - 读取包含部分提交信息的 `PartialCommitContent`
    - 仅在启用 partial changelists 时生效

- `formatPartialChangesForCommitMessage(...)`
    - 生成自定义格式的 diff：
        - 以 `Partial Changes (Selected Chunks)` 开头
        - 对每个 chunk 输出 `@@` 块，包含 `-` 和 `+` 行
    - 直接拼接 `vcsContent` 与 `currentContent` 的行范围

- `generateCombinedDiffString(...)`
    - 将变更拆分为：
        - `fullChanges`：未被 partial 覆盖的文件
        - `partialChanges`：仅选中的 chunk
    - full 部分使用 `DiffUtilsKt.generateDiffStringFromChanges`
    - partial 部分使用 `formatPartialChangesForCommitMessage`
    - 结果为二者拼接

#### DiffUtilsKt.generateDiffStringFromChanges

- 针对每个 `Change`：
    - 取 `beforeRevision` / `afterRevision`
    - 读取文本内容（若 size > 20MB 则跳过）
    - 使用 `java-diff-utils` 生成 unified diff：
        - `UnifiedDiffUtils.generateUnifiedDiff(...)`，上下文行数为 `2`
    - 附带变更类型描述：
        - `Added new file` / `Deleted file` / `Moved/renamed file` / `Modified file`

- 排序与截断策略：
    - 先计算每个 diff 的文本长度
    - 按长度降序排序
    - 总长度最大 `500000` 字符
    - 首条 diff 超大时，裁剪到 `250000` 字符（加入 `"... (diff truncated)"`）

#### 上下文构建要点

- 上下文的核心就是 **diff 文本**（`context` 字段）
- 额外上下文来自：
    - `branch`（当前分支）
    - `previous_commits`（最近 10 条，过滤 merge PR）
    - `commit_template`（可选模板）
- 没有做语义降噪或 AST/PSI 级上下文补齐

#### 结论

- SweepAI 的核心策略是"尽量完整 diff + 最近提交 + 分支"。
- partial changes 会被显式编码成 `@@ Chunk` 块，避免误用未选内容。
- diff 输出为标准 unified diff，但做了长度控制与大文件跳过。

#### 示例载荷（上下文 + diff）

> SweepAI 发送给服务端的 payload 核心字段为 `context`/`previous_commits`/`branch`/`commit_template`。

```json
{
  "context": "Modified file: src/main/java/com/example/AuthService.java\n--- a/src/main/java/com/example/AuthService.java\n+++ b/src/main/java/com/example/AuthService.java\n@@ -42,6 +42,8 @@\n-    if (token == null) {\n+    if (token == null) {\n+    token = token.trim();\n+    validate(token);\n",
  "previous_commits": "Recent Commit Messages:\n1. fix(auth): handle null token\n2. refactor(auth): cleanup utils",
  "branch": "main",
  "commit_template": "type(scope): subject"
}
```

> 如果存在 partial changes，`context` 中会追加：

```
Partial Changes (Selected Chunks):

Modified file (partial): src/main/java/com/example/AuthService.java
Selected chunks: 1

@@ Chunk 1/1 @@ -42,2 +42,4 @@
-    if (token == null) {
+    if (token == null) {
+    token = token.trim();
+    validate(token);
```

### 1.2 Cosy 实现分析

本文基于 `reference/cosy-intellij-0.7.0/.../CosyCommitMessageGenerationAction.java` 及相关类，梳理 Cosy 在 commit message 生成时的 diff
与上下文构建方式。

#### 总体流程

- 入口：`CosyCommitMessageGenerationAction.actionPerformed`
- 从提交窗口获取 `CommitMessage` 控件与 `CommitWorkflowHandler`
- 获取选中变更（含未版本控制文件）
- 生成 patch diff 列表（字符串集合）
- 收集最近 3 条提交信息
- 组装 `GenerateCommitMsgParam`：
    - `codeDiffs`: List<String>
    - `commitMessages`: List<String>
    - `preferredLanguage`
    - `stream = true`
- 通过 `Cosy` 语言服务请求生成（流式回写）

#### diff 构建

##### 变更来源

- `AbstractCommitWorkflowHandler.getUi().getIncludedChanges()`（选中变更）
- `getIncludedUnversionedFiles()`（未版本控制文件，转成 `CurrentContentRevision`）

##### 过滤与限制

- 二进制文件直接跳过
- 单行超长内容（长度 > 300 且无换行）会被过滤
- 单个 patch hunk 若只有 1 行且行长 > 300，直接忽略
- 总 patch 长度上限：`MAX_PATCH_LEN = 70000`
- 最大文件数：`MAX_FILE = 50`

##### diff 生成方式

- 使用 `IdeaTextPatchBuilder.buildPatch(project, changes, basePath, false, false)` 生成 `FilePatch`
- 使用 `UnifiedDiffWriter.write(...)` 输出统一 diff 文本到 `StringWriter`

当 `buildPatch` 返回空时，会回退为简单文本：`<fileName> change mod`

#### 上下文构建

- `commitMessages`：最近 3 条 Git commit message（按时间倒序）
    - 获取方式：`GitHistoryUtils.history(..., --max-count=3)`
- `preferredLanguage`：从设置读取
- `codeDiffs`：上面生成的 diff 文本列表

该实现没有语义级上下文补齐，也不做 AST/PSI 分析；上下文主要来自 patch diff 和最近提交历史。

#### 结论

- Cosy 以 IDEA 的 patch 能力为核心，直接输出统一 diff
- 强调"大小可控"和"噪音过滤"（二进制/超长/超限）
- 上下文主要是最近提交记录 + diff 列表，整体偏向"工程化稳定输出"

#### 示例载荷（上下文 + diff）

> Cosy 通过 `GenerateCommitMsgParam` 发送以下核心字段：

```json
{
  "requestId": "uuid-123",
  "codeDiffs": [
    "--- a/src/main/java/com/example/AuthService.java\n+++ b/src/main/java/com/example/AuthService.java\n@@ -42,6 +42,8 @@\n-    if (token == null) {\n+    if (token == null) {\n+    token = token.trim();\n+    validate(token);\n",
    "[ADD]: /path/to/new/File.java\n+++ /path/to/new/File.java\n+public class File {}\n"
  ],
  "commitMessages": [
    "fix(auth): handle null token",
    "refactor(auth): cleanup utils",
    "docs: update README"
  ],
  "stream": true,
  "preferredLanguage": "en"
}
```

### 1.3 AI Commits 插件实现分析

本文记录 reference/commit/ai-commits-intellij-plugin 的 diff 构建与上下文注入方式，便于后续统一规划。

#### diff 生成路径

- 使用 `IdeaTextPatchBuilder.buildPatch(...)` 直接从 `Change` 生成 patch。
- 通过 `UnifiedDiffWriter.write(...)` 输出统一 diff 文本（原生 patch 风格）。
- 支持按仓库根路径分组：每个仓库前输出 `Repository: <path>`。
- 过滤逻辑：
    - 排除用户配置的路径（全局 + 项目级）
    - 排除子模块变更

#### 上下文注入

- 支持的模板占位符：
    - `{diff}`：完整 diff 文本
    - `{branch}`：从变更所属仓库推断公共分支
    - `{taskId}`/`{taskSummary}`/`{taskDescription}`/`{taskTimeSpent}`：来自 TaskManager 的活动任务
    - `{hint}`：可选提示语，支持特殊占位符拼接语法
    - `{locale}`：当前 IDE 语言

#### 提示词拼接策略

- 若模板包含 `{diff}`，则替换该占位符。
- 若模板未包含 `{diff}`，则在模板末尾追加 diff 文本。

#### 与当前实现的关键差异

- 该插件输出的是原生 patch diff，不做降噪/裁剪。
- 上下文主要来自任务、分支与提示语，占位符系统更丰富。
- 不做代码语义级"上下文补齐"（如方法/类级上下文），也不做空白/注释/导入/重排过滤。

#### 示例载荷（上下文 + diff）

> 以下示例用于展示该插件拼接后的 prompt 结构（模板 + diff + 上下文占位符）。

```
You are an assistant that writes commit messages.
Branch: main
Task: ABC-123 Fix login timeout

Diff:
diff --git a/src/main/java/com/example/AuthService.java b/src/main/java/com/example/AuthService.java
index 1a2b3c..4d5e6f 100644
--- a/src/main/java/com/example/AuthService.java
+++ b/src/main/java/com/example/AuthService.java
@@ -42,6 +42,8 @@ public class AuthService {
     if (token == null) {
         return false;
     }
+    token = token.trim();
+    validate(token);
     return check(token);
 }
```

### 1.4 AIGitCommit 实现分析

本文聚焦 `reference/commit/AIGitCommit/src/main/java/com/hmydk/aigit/context` 的上下文建模与提示词构建方式，重点在"上下文层面"的结构设计。

#### 上下文数据模型

- `CommitContext` 是核心聚合对象，包含：
    - `ProjectInfo`：项目名、分支、是否 Git 仓库
    - `ChangeStatistics`：文件数、增删行、变更类型、scope、复杂度、语言分布
    - `List<FileChange>`：每个文件的变更明细
    - `metadata`：扩展字段（可选）
    - `ideaProject`：直接持有 IDEA `Project` 用于分析
- scope 生成逻辑：
    - `ChangeStatistics.inferScope(...)` 基于路径启发式（如 `/service/`、`/util/`、`/config/`、`/test/`、`/ui/` 等）推断范围
    - 默认回退为 `core`，并在 `AIPromptBuilder` 中提示可结合统计或路径二次推断

#### 智能分析层

- `CommitContext.toAIPrompt()` 会创建 `ContextAnalyzer` 并执行 `analyze(this)`。
- 分析结果会被 `AIPromptBuilder` 注入到输出中：
    - `pattern` / `pattern_description`
    - `complexity` / `complexity_level`
    - `key_insights`
    - `categorized_changes`（按类别分组的变更）

#### 提示词构建方式（结构化 JSON）

- `AIPromptBuilder` 将 `CommitContext` 转为结构化 JSON，而不是拼接自然语言。
- 输出字段分层清晰：
    - `analysis`
    - `project`
    - `statistics`
    - `categorized_changes` / `changes`
    - `metadata`（若存在）
- 每个文件变更包含：
    - `path` / `type` / `language` / `extension`
    - `lines_added` / `lines_deleted`
    - `summary`
    - `diff_summary`
    - `full_diff_content`（强调必须使用该字段理解实际变更）

#### 提示词策略

- 智能版与兼容版提示词都强调：
    - 使用 `full_diff_content` 理解真实代码变化
    - 结合统计信息、分类变更、关键洞察生成更准确的 commit message
- `buildSimple(...)` 提供简化输入，仅保留统计 + 摘要 + 格式要求

#### 结论（上下文层面）

- 该插件最核心的价值在"结构化上下文"：
    - 以 JSON 形式输出，降低解析噪音
    - 明确区分统计、分类变更、分析洞察、完整 diff
    - 将"提示词逻辑"与"变更数据"解耦
- diff 的具体算法较简化，但上下文层面的组织对 AI 友好程度更高。

#### 插件功能概览（不含 AI 集成）

> 说明：以下分析基于 `reference/commit/AIGitCommit`，不包含具体 LLM 接入细节与模型调用逻辑。

##### 入口与交互

- Action 入口：`GenerateCommitMessageAction`
    - 仅在 Git 仓库项目中启用（`GitUtil.isGitRepository`）
    - 从提交窗口获取选中变更与未版本控制文件
    - 生成过程使用图标动画提示状态
    - 结果写回提交消息输入框

##### 变更采集与过滤

- 变更来源：
    - `AbstractCommitWorkflowHandler.getUi().getIncludedChanges()`
    - `getIncludedUnversionedFiles()`
- 文件排除：
    - `ApiKeySettings` 提供排除模式（文件名 / 路径 / glob）
    - `GitUtil.shouldExcludeFile` 统一过滤
    - 内置默认忽略规则 `Constants.DEFAULT_EXCLUDE_PATTERNS`：
        - 常见生成物（`*.pb.*`、`*.generated.*`、`*_gen.*`）
        - 依赖锁文件（`package-lock.json`、`yarn.lock`、`go.sum`、`Cargo.lock` 等）
        - 构建产物目录（`dist/`、`build/`、`target/`、`node_modules/` 等）
        - 系统/临时文件（`.DS_Store`、`*.log`、`*.tmp`、`*.swp` 等）
    - 规则可在设置界面编辑/覆盖，适合作为基础忽略模板借鉴

##### diff 生成与上下文组织

- 传统 diff：
    - `GitUtil.computeDiff(...)` 使用 `IdeaTextPatchBuilder + UnifiedDiffWriter`
    - 每个 patch 前加变更类型标记（`[ADD]/[DELETE]/[MOVE]/[MODIFY]`）
    - 未版本控制文件输出全量新增内容（逐行 `+`）
- 结构化上下文：
    - `GitUtil.buildCommitContext(...)` 生成 `CommitContext`
    - `FileChange.fromGitChanges(...)` 统一处理已版本控制与未版本控制文件
    - `CommitContext.toLegacyFormat()` 输出兼容旧格式

##### 项目与文件上下文收集

- `computeEnhancedDiff(...)` 会补充：
    - 文件类型/扩展名/语言（PSI + FileType）
    - 是否二进制
    - 项目名、当前分支
- 这些信息可作为"非 AI 依赖"的上下文增强来源

##### 设置与提示词管理（不含 AI 调用）

- `ApiKeySettings` / `ApiKeyConfigurable`：
    - Prompt 类型选择（项目级 / 自定义）
    - 自定义 Prompt 列表管理
    - 最近一次 Prompt 展示与复制
- `LastPromptService`：
    - 记录"最近一次生成使用的 Prompt"

##### 辅助 UI

- `ApiKeyConfigurableUI`：
    - Prompt 配置面板与 Recent Prompt 展示
    - Prompt 弹窗编辑（新增/编辑/删除）
- `LastPromptUIUtil`：
    - 最近 Prompt 弹窗展示

#### 结论（功能层面）

- 主流程是"提交窗口选中变更 → 生成上下文 → 写回提交消息"。
- 插件提供完整的"变更采集 + 排除规则 + diff/上下文组织 + 设置面板"的闭环。
- 结构化上下文与传统 diff 共存，兼容旧格式输出与更丰富的数据组织。

#### 示例载荷（上下文 + diff）

> 以下为示意性 JSON 结构，用于展示该插件提交给 AI 的上下文组织形式。`full_diff_content` 即完整 diff 内容字段。

```json
{
  "analysis": {
    "pattern": "doc_update",
    "pattern_description": "Documentation updates",
    "complexity": 4,
    "complexity_level": "简单",
    "key_insights": [
      "Comment/docstring expanded for clarity"
    ]
  },
  "project": {
    "name": "demo-project",
    "branch": "main",
    "is_git_repository": true
  },
  "statistics": {
    "files_changed": 1,
    "lines_added": 6,
    "lines_deleted": 2,
    "total_lines": 8,
    "change_type": "docs",
    "scope": "core",
    "complexity": 4,
    "language_distribution": {
      "Java": 1
    }
  },
  "categorized_changes": {
    "docs": [
      {
        "path": "src/main/java/com/example/Foo.java",
        "type": "MODIFIED",
        "language": "Java",
        "extension": "java",
        "lines_added": 6,
        "lines_deleted": 2,
        "summary": "Update Foo (+6/-2 lines)",
        "diff_summary": "[MODIFY]: src/main/java/com/example/Foo.java",
        "full_diff_content": "--- a/src/main/java/com/example/Foo.java\n+++ b/src/main/java/com/example/Foo.java\n@@ -10,7 +10,11 @@\n- * old comment\n+ * new comment\n+ * more detail\n"
      }
    ]
  },
  "metadata": {
    "custom": "value"
  }
}
```

## 二、最终方案设计

基于以上参考实现的分析，结合各插件的优秀实践，本文档为 `intelli-ai-changelog` 插件设计最终的 commit message 上下文构建方案。

### 2.1 Diff 构建方案

**核心策略：IDEA_PATCH + CodeDiffUtil 二次处理（降噪/上下文补齐）**

#### 设计思路

参考各插件的实现：

- **SweepAI** 使用 `java-diff-utils` 生成 unified diff，但未做降噪处理
- **Cosy** 使用 IDEA 原生 `IdeaTextPatchBuilder`，强调大小可控和噪音过滤
- **AIGitCommit** 使用 IDEA patch，但重点在结构化上下文组织

本方案采用**双重 diff 策略**，既保留完整上下文，又突出语义变更：

1. **IDEA_PATCH**：提供完整上下文（方法/类附近）与原始 patch 格式，确保 AI 理解代码变更的完整背景
2. **CodeDiffUtil**：做降噪处理（空白/注释/导入/重排过滤）并限制 hunk/行数，突出语义变更，减少噪音干扰

#### 输出结构

```
<IDEA 原生 patch>
=== 降噪摘要 ===
<CodeDiffUtil 输出>
```

#### 设计要点

- **IDEA_PATCH** 提供完整上下文（方法/类附近）与原始 patch 格式。
- **CodeDiffUtil** 做降噪（空白/注释/导入/重排过滤）并限制 hunk/行数，突出语义变更。
- 两者互补，既保留完整背景，又提供重点摘要。

#### 过滤与限制策略

参考 Cosy 和 SweepAI 的实践：

- 二进制文件直接跳过
- 超大文件（>20MB）跳过（参考 SweepAI）
- 超长单行（>300 字符）过滤（参考 Cosy）
- 总长度限制：500KB（参考 SweepAI）
- 最大文件数限制：50 个（参考 Cosy）

### 2.2 上下文构建方案

目标：让 AI 生成 commit message 更准确、更稳定。

#### 核心设计原则

参考 AIGitCommit 的结构化设计：

1. **结构化上下文**：采用 JSON 格式组织上下文数据，降低解析噪音
2. **分类组织**：按类型/语言/模块分组变更，便于 AI 理解
3. **元数据补充**：包含最近提交、分支、模板等额外信息

#### 核心结构（JSON 结构化）

- `project`
    - `name` / `branch` / `is_git_repository`
- `statistics`
    - `files_changed` / `lines_added` / `lines_deleted` / `change_type` / `scope`
- `changes` 或 `categorized_changes`
    - `path` / `type` / `language` / `extension`
    - `summary` / `diff_summary` / `full_diff_content`
- `metadata`
    - `recent_commits`（最近 3~5 条，参考 Cosy 的 3 条和 SweepAI 的 10 条，折中选择）
    - `commit_template`（若有，参考 SweepAI）
    - `preferred_language`（参考 Cosy）
    - `partial_changes`（若用户只勾选部分 chunk，参考 SweepAI）

#### 拼接策略（建议顺序）

```
[结构化上下文 JSON]
[IDEA 原生 patch]
=== 降噪摘要 ===
[CodeDiffUtil 输出]
[最近提交记录（3-5 条）]
[用户补充说明（可选）]
```

### 2.3 Partial Changes 支持

参考 SweepAI 的实现，支持用户只选中部分 chunk 的场景：

- 检测用户是否只选中了部分 chunk
- 使用 `PartialLocalLineStatusTracker` 获取部分变更
- 在 diff 中明确标记 `Partial Changes (Selected Chunks)`
- 格式化为 `@@ Chunk` 块，避免误用未选内容

## 三、实现步骤

### 3.1 Diff 生成

1. **IDEA Patch 生成**
    - 使用 `IdeaTextPatchBuilder.buildPatch(project, changes, basePath, false, false)` 生成 `FilePatch`
    - 使用 `UnifiedDiffWriter.write(...)` 输出完整 patch 文本

2. **CodeDiffUtil 降噪摘要**
    - 使用 `CodeDiffUtil` 生成降噪 diff（过滤空白/注释/导入/重排）
    - 限制 hunk/行数，突出语义变更

3. **拼接输出**
    - 按约定拼接为 `IDEA_PATCH + === 降噪摘要 === + CodeDiffUtil`

### 3.2 上下文构建

1. **统计信息收集**
    - 文件数、增删行、变更类型、scope

2. **文件上下文收集**
    - 路径、类型、语言、摘要
    - 每个文件的 `full_diff_content`（包含 IDEA patch + 降噪摘要）

3. **可选信息收集**
    - 最近提交（3-5 条，过滤 merge commit）
    - 分支信息
    - 模板（若有）
    - 用户说明（可选）
    - Partial changes（若存在）

### 3.3 Prompt 组装

1. **结构化 JSON 放在 prompt 开头**
    - 包含项目信息、统计信息、分类变更、元数据

2. **接入 diff 拼接内容**
    - IDEA 原生 patch
    - 降噪摘要

3. **补充最近提交与用户说明**
    - 最近 3-5 条提交记录
    - 用户补充说明（可选）

### 3.4 数据结构设计

```java
public class CommitContext {
    // 项目信息
    private ProjectInfo project;

    // 统计信息
    private ChangeStatistics statistics;

    // 分类变更
    private Map<String, List<FileChange>> categorizedChanges;

    // 元数据
    private CommitMetadata metadata;
}

public class FileChange {
    private String path;
    private ChangeType type; // ADD/DELETE/MODIFY/MOVE
    private String language;
    private String extension;
    private int linesAdded;
    private int linesDeleted;
    private String summary;
    private String diffSummary;
    private String fullDiffContent; // IDEA patch + 降噪摘要
}
```

### 3.5 可配置项（建议）

- 最近提交条数（默认 3-5 条）
- 是否包含 partial changes（默认开启）
- 是否追加降噪摘要（默认开启）
- 最大 diff 长度（默认 500KB）
- 最大文件数（默认 50 个）
- 是否过滤 merge commit（默认开启）

### 3.6 优化后的提示词模板

> 以下模板与 AIGitCommit 风格的结构化 JSON 对齐，强调 `full_diff_content` 为真实变更来源。

#### System Prompt（commit message）

```
你是一位经验丰富的代码审查专家和技术文档编写者。
你的任务是基于结构化上下文与代码 diff，生成高质量的 Git 提交记录。

输出必须严格遵循 Conventional Commits 规范：
<type>(<scope>): <subject>

<body（可选）>

强制规则：
1. 只依据 JSON 上下文与 full_diff_content 判断改动，不凭空猜测
2. 优先使用 full_diff_content 理解真实代码变化
3. 结合 statistics、categorized_changes/changes、recent_commits 辅助判断类型与 scope
4. 如果是重构，必须说明“为何需要重构”
5. 忽略无语义改动（格式化/空白/等价重排）

正文（body）规则（如需）：
- 必须使用 Markdown 无序列表
- 每行以 `- ` 开头
- 每条只表达一个清晰观点
- 建议 2~4 条，最多不超过 5 条
- 仅聚焦：变更动机 / 行为变化 / 影响范围 / 风险注意

其他强制要求：
- 提交消息内容必须使用 ${language}
- 仅输出最终提交记录，不要解释或附加说明
- subject 使用祈使语气，不要句号
- type/scope 使用通用英文约定（feat/fix/refactor/perf/test/docs/build/chore 等）
```

#### User Prompt 模板

```
请根据以下结构化上下文生成本次提交的 Git commit message。

【结构化上下文（JSON）】
{codeDiffs}

注意：
- JSON 中的 full_diff_content 为真实 diff，请优先参考
- statistics 提供整体变化规模与 scope 提示
- recent_commits 仅用于风格/语境参考
- 如果 extra_context 存在，请谨慎参考

仅输出最终提交记录。
```

```java
// 文件忽略相关常量
public static final String[] DEFAULT_EXCLUDE_PATTERNS = {
        "*.pb.go",           // Protocol Buffer生成文件
        "*.pb.cc",           // Protocol Buffer C++生成文件
        "*.pb.h",            // Protocol Buffer头文件
        "go.sum",            // Go依赖锁定文件
        "go.mod",            // Go模块文件（可选）
        "package-lock.json", // Node.js依赖锁定文件
        "yarn.lock",         // Yarn依赖锁定文件
        "pnpm-lock.yaml",    // PNPM依赖锁定文件
        "Cargo.lock",        // Rust依赖锁定文件
        "Pipfile.lock",      // Python依赖锁定文件
        "poetry.lock",       // Poetry依赖锁定文件
        "*.generated.*",     // 通用生成文件
        "*.gen.*",           // 生成文件简写
        "*_generated.*",     // 下划线生成文件
        "*_gen.*",           // 下划线生成文件简写
        "vendor/**",         // Go vendor目录
        "node_modules/**",   // Node.js依赖目录
        ".next/**",          // Next.js构建目录
        "dist/**",           // 构建输出目录
        "build/**",          // 构建目录
        "target/**",         // Maven/Rust构建目录
        "*.min.js",          // 压缩的JS文件
        "*.min.css",         // 压缩的CSS文件
        "*.bundle.*",        // 打包文件
        "*.chunk.*",         // 代码分块文件
        "coverage/**",       // 测试覆盖率目录
        ".nyc_output/**",    // NYC覆盖率输出
        "*.lcov",            // 覆盖率报告文件
        "*.log",             // 日志文件
        "*.tmp",             // 临时文件
        "*.temp",            // 临时文件
        ".DS_Store",         // macOS系统文件
        "Thumbs.db",         // Windows系统文件
        "*.swp",             // Vim交换文件
        "*.swo",             // Vim交换文件
        "*~"                 // 备份文件
};
```

## 四、实现总结

### 4.1 与参考实现的对比

| 特性              | SweepAI         | Cosy       | AI Commits | AIGitCommit | 本方案                       |
|-----------------|-----------------|------------|------------|-------------|---------------------------|
| diff 生成         | java-diff-utils | IDEA patch | IDEA patch | IDEA patch  | IDEA patch + CodeDiffUtil |
| 降噪处理            | ❌               | ❌          | ❌          | ❌           | ✅                         |
| 结构化上下文          | ❌               | ❌          | ❌          | ✅           | ✅                         |
| 最近提交            | ✅ (10条)         | ✅ (3条)     | ❌          | ❌           | ✅ (3-5条)                  |
| Partial changes | ✅               | ❌          | ❌          | ❌           | ✅                         |
| 长度限制            | ✅ (500KB)       | ✅ (70KB)   | ❌          | ❌           | ✅ (500KB)                 |
| 文件数限制           | ❌               | ✅ (50)     | ❌          | ❌           | ✅ (50)                    |

### 4.2 实现优先级

1. **Phase 1：基础实现**
    - IDEA patch 生成
    - 基础上下文统计（文件数、增删行）
    - 最近提交记录收集
    - 基础 prompt 组装

2. **Phase 2：增强功能**
    - CodeDiffUtil 降噪摘要
    - 结构化 JSON 上下文
    - 分类变更组织
    - Partial changes 支持

3. **Phase 3：优化与配置**
    - 长度/文件数限制
    - 过滤规则配置
    - 模板系统
    - 用户自定义提示词

### 4.3 注意事项

1. **线程安全**：所有 PSI 操作必须在 ReadAction 中执行，UI 更新必须在 EDT 中执行
2. **性能优化**：大文件/大变更集需要及时跳过或截断，避免阻塞 UI
3. **错误处理**：diff 生成失败时应有降级方案（如简单文本描述）
4. **用户体验**：提供进度提示，支持流式输出（如果 AI 服务支持）
5. **兼容性**：考虑不同 IntelliJ 版本的 API 差异

### 4.4 总结

`intelli-ai-changelog` 插件的 commit message 上下文构建应该：

1. **采用双重 diff 策略**，既保留完整上下文，又突出语义变更
2. **使用结构化 JSON 组织上下文**，提高 AI 理解效率
3. **参考各插件的优秀实践**，如 SweepAI 的 partial changes 处理、Cosy 的过滤策略、AIGitCommit 的结构化设计
4. **提供灵活的配置选项**，满足不同用户需求
5. **注重性能和用户体验**，避免阻塞 UI，提供清晰的反馈

通过以上设计，可以生成更准确、更稳定的 commit message，提升开发效率。
