# Cosy Commit Message diff/上下文构建分析

本文基于 `reference/cosy-intellij-0.7.0/.../CosyCommitMessageGenerationAction.java` 及相关类，梳理 Cosy 在 commit message 生成时的 diff
与上下文构建方式。

## 总体流程

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

## diff 构建

### 变更来源

- `AbstractCommitWorkflowHandler.getUi().getIncludedChanges()`（选中变更）
- `getIncludedUnversionedFiles()`（未版本控制文件，转成 `CurrentContentRevision`）

### 过滤与限制

- 二进制文件直接跳过
- 单行超长内容（长度 > 300 且无换行）会被过滤
- 单个 patch hunk 若只有 1 行且行长 > 300，直接忽略
- 总 patch 长度上限：`MAX_PATCH_LEN = 70000`
- 最大文件数：`MAX_FILE = 50`

### diff 生成方式

- 使用 `IdeaTextPatchBuilder.buildPatch(project, changes, basePath, false, false)`
  生成 `FilePatch`
- 使用 `UnifiedDiffWriter.write(...)`
  输出统一 diff 文本到 `StringWriter`

当 `buildPatch` 返回空时，会回退为简单文本：`<fileName> change mod`

## 上下文构建

- `commitMessages`：最近 3 条 Git commit message（按时间倒序）
    - 获取方式：`GitHistoryUtils.history(..., --max-count=3)`
- `preferredLanguage`：从设置读取
- `codeDiffs`：上面生成的 diff 文本列表

该实现没有语义级上下文补齐，也不做 AST/PSI 分析；上下文主要来自 patch diff 和最近提交历史。

## 结论

- Cosy 以 IDEA 的 patch 能力为核心，直接输出统一 diff
- 强调“大小可控”和“噪音过滤”（二进制/超长/超限）
- 上下文主要是最近提交记录 + diff 列表，整体偏向“工程化稳定输出”

## 示例载荷（上下文 + diff）

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
