# AI Commits 插件 diff/上下文构建分析

本文记录 reference/commit/ai-commits-intellij-plugin 的 diff 构建与上下文注入方式，便于后续统一规划。

## diff 生成路径

- 使用 `IdeaTextPatchBuilder.buildPatch(...)` 直接从 `Change` 生成 patch。
- 通过 `UnifiedDiffWriter.write(...)` 输出统一 diff 文本（原生 patch 风格）。
- 支持按仓库根路径分组：每个仓库前输出 `Repository: <path>`。
- 过滤逻辑：
    - 排除用户配置的路径（全局 + 项目级）
    - 排除子模块变更

## 上下文注入

- 支持的模板占位符：
    - `{diff}`：完整 diff 文本
    - `{branch}`：从变更所属仓库推断公共分支
    - `{taskId}`/`{taskSummary}`/`{taskDescription}`/`{taskTimeSpent}`：来自 TaskManager 的活动任务
    - `{hint}`：可选提示语，支持特殊占位符拼接语法
    - `{locale}`：当前 IDE 语言

## 提示词拼接策略

- 若模板包含 `{diff}`，则替换该占位符。
- 若模板未包含 `{diff}`，则在模板末尾追加 diff 文本。

## 与当前实现的关键差异

- 该插件输出的是原生 patch diff，不做降噪/裁剪。
- 上下文主要来自任务、分支与提示语，占位符系统更丰富。
- 不做代码语义级“上下文补齐”（如方法/类级上下文），也不做空白/注释/导入/重排过滤。

## 示例载荷（上下文 + diff）

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
