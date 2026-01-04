# AIGitCommit 上下文构建分析（context 包）

本文聚焦 `reference/commit/AIGitCommit/src/main/java/com/hmydk/aigit/context` 的上下文建模与提示词构建方式，重点在“上下文层面”的结构设计。

## 上下文数据模型

- `CommitContext` 是核心聚合对象，包含：
    - `ProjectInfo`：项目名、分支、是否 Git 仓库
    - `ChangeStatistics`：文件数、增删行、变更类型、scope、复杂度、语言分布
    - `List<FileChange>`：每个文件的变更明细
    - `metadata`：扩展字段（可选）
    - `ideaProject`：直接持有 IDEA `Project` 用于分析

## 智能分析层

- `CommitContext.toAIPrompt()` 会创建 `ContextAnalyzer` 并执行 `analyze(this)`。
- 分析结果会被 `AIPromptBuilder` 注入到输出中：
    - `pattern` / `pattern_description`
    - `complexity` / `complexity_level`
    - `key_insights`
    - `categorized_changes`（按类别分组的变更）

## 提示词构建方式（结构化 JSON）

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

## 提示词策略

- 智能版与兼容版提示词都强调：
    - 使用 `full_diff_content` 理解真实代码变化
    - 结合统计信息、分类变更、关键洞察生成更准确的 commit message
- `buildSimple(...)` 提供简化输入，仅保留统计 + 摘要 + 格式要求

## 结论（上下文层面）

- 该插件最核心的价值在“结构化上下文”：
    - 以 JSON 形式输出，降低解析噪音
    - 明确区分统计、分类变更、分析洞察、完整 diff
    - 将“提示词逻辑”与“变更数据”解耦
- diff 的具体算法较简化，但上下文层面的组织对 AI 友好程度更高。

## 插件功能概览（不含 AI 集成）

> 说明：以下分析基于 `reference/commit/AIGitCommit`，不包含具体 LLM 接入细节与模型调用逻辑。

### 入口与交互

- Action 入口：`GenerateCommitMessageAction`
    - 仅在 Git 仓库项目中启用（`GitUtil.isGitRepository`）
    - 从提交窗口获取选中变更与未版本控制文件
    - 生成过程使用图标动画提示状态
    - 结果写回提交消息输入框

### 变更采集与过滤

- 变更来源：
    - `AbstractCommitWorkflowHandler.getUi().getIncludedChanges()`
    - `getIncludedUnversionedFiles()`
- 文件排除：
    - `ApiKeySettings` 提供排除模式（文件名 / 路径 / glob）
    - `GitUtil.shouldExcludeFile` 统一过滤

### diff 生成与上下文组织

- 传统 diff：
    - `GitUtil.computeDiff(...)` 使用 `IdeaTextPatchBuilder + UnifiedDiffWriter`
    - 每个 patch 前加变更类型标记（`[ADD]/[DELETE]/[MOVE]/[MODIFY]`）
    - 未版本控制文件输出全量新增内容（逐行 `+`）
- 结构化上下文：
    - `GitUtil.buildCommitContext(...)` 生成 `CommitContext`
    - `FileChange.fromGitChanges(...)` 统一处理已版本控制与未版本控制文件
    - `CommitContext.toLegacyFormat()` 输出兼容旧格式

### 项目与文件上下文收集

- `computeEnhancedDiff(...)` 会补充：
    - 文件类型/扩展名/语言（PSI + FileType）
    - 是否二进制
    - 项目名、当前分支
- 这些信息可作为“非 AI 依赖”的上下文增强来源

### 设置与提示词管理（不含 AI 调用）

- `ApiKeySettings` / `ApiKeyConfigurable`：
    - Prompt 类型选择（项目级 / 自定义）
    - 自定义 Prompt 列表管理
    - 最近一次 Prompt 展示与复制
- `LastPromptService`：
    - 记录“最近一次生成使用的 Prompt”

### 辅助 UI

- `ApiKeyConfigurableUI`：
    - Prompt 配置面板与 Recent Prompt 展示
    - Prompt 弹窗编辑（新增/编辑/删除）
- `LastPromptUIUtil`：
    - 最近 Prompt 弹窗展示

## 结论（功能层面）

- 主流程是“提交窗口选中变更 → 生成上下文 → 写回提交消息”。
- 插件提供完整的“变更采集 + 排除规则 + diff/上下文组织 + 设置面板”的闭环。
- 结构化上下文与传统 diff 共存，兼容旧格式输出与更丰富的数据组织。

## 示例载荷（上下文 + diff）

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
