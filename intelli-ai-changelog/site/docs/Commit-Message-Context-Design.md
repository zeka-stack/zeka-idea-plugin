# Commit Message 上下文构建方案

本文总结 commit message 生成的最终方案，分为 diff 构建与上下文构建两部分，并附简要实现步骤。

## 一、Diff 构建方案

**固定策略：IDEA_PATCH + CodeDiffUtil 二次处理（降噪/上下文补齐）**

输出结构：

```
<IDEA 原生 patch>
=== 降噪摘要 ===
<CodeDiffUtil 输出>
```

设计要点：

- **IDEA_PATCH** 提供完整上下文（方法/类附近）与原始 patch 格式。
- **CodeDiffUtil** 做降噪（空白/注释/导入/重排过滤）并限制 hunk/行数，突出语义变更。
- 两者互补，既保留完整背景，又提供重点摘要。

## 二、上下文构建方案

目标：让 AI 生成 commit message 更准确、更稳定。

### 核心结构（JSON 结构化）

- `project`
    - `name` / `branch` / `is_git_repository`
- `statistics`
    - `files_changed` / `lines_added` / `lines_deleted` / `change_type` / `scope`
- `changes` 或 `categorized_changes`
    - `path` / `type` / `language` / `extension`
    - `summary` / `diff_summary` / `full_diff_content`
- `metadata`
    - `recent_commits`（最近 3~5 条）
    - `commit_template`（若有）
    - `preferred_language`
    - `partial_changes`（若用户只勾选部分 chunk）

### 拼接策略（建议顺序）

```
[结构化上下文 JSON]
[IDEA 原生 patch]
=== 降噪摘要 ===
[CodeDiffUtil 输出]
[最近提交记录]
[用户补充说明（可选）]
```

## 简要实现步骤

1. **Diff 生成**
    - 使用 `IdeaTextPatchBuilder` 生成完整 patch 文本。
    - 使用 `CodeDiffUtil` 生成降噪 diff。
    - 按约定拼接为 `IDEA_PATCH + === 降噪摘要 === + CodeDiffUtil`。

2. **上下文构建**
    - 统计信息：文件数、增删行、变更类型、scope。
    - 文件上下文：路径、类型、语言、摘要。
    - 可选信息：最近提交、分支、模板、用户说明、partial changes。

3. **Prompt 组装**
    - 将结构化 JSON 放在 prompt 开头。
    - 接入 diff 拼接内容。
    - 补充最近提交与用户说明。

4. **可配置项（建议）**
    - 最近提交条数
    - 是否包含 partial changes
    - 是否追加降噪摘要
    - 最大 diff 长度
