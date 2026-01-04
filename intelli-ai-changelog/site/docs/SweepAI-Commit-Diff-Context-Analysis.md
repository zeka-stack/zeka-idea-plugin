# SweepAI Commit Message diff/上下文构建分析

本文基于反编译文件 `reference/sweepai/jetbrains-1.27.0/.../SweepCommitMessageService.java` 及相关 utils 的实现，梳理 commit message 的 diff
与上下文构建方式。

## 总体流程

- 入口：`SweepCommitMessageService.generateCommitMessage(...)`
- 获取分支名、默认 changelist、选中变更
- 生成 diff 文本（完整或 partial）
- 组装 `CommitMessageRequest`：
    - `context` = diff 文本
    - `previous_commits` = 最近提交（过滤 merge PR）
    - `branch` = 当前分支名
    - `commit_template` = 可选模板
- 发送请求生成 commit message

## PartialChangesUtilsKt（部分提交）

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

## DiffUtilsKt.generateDiffStringFromChanges

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

## 上下文构建要点

- 上下文的核心就是 **diff 文本**（`context` 字段）
- 额外上下文来自：
    - `branch`（当前分支）
    - `previous_commits`（最近 10 条，过滤 merge PR）
    - `commit_template`（可选模板）
- 没有做语义降噪或 AST/PSI 级上下文补齐

## 结论

- SweepAI 的核心策略是“尽量完整 diff + 最近提交 + 分支”。
- partial changes 会被显式编码成 `@@ Chunk` 块，避免误用未选内容。
- diff 输出为标准 unified diff，但做了长度控制与大文件跳过。

## 示例载荷（上下文 + diff）

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
