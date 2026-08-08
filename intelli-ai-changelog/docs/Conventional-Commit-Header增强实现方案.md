# Conventional Commit Header 增强实现方案

## 1. 背景与目标

在 IntelliJ 的 Git Commit Message 输入框中，提供接近 [lppedd/idea-conventional-commit](https://github.com/lppedd/idea-conventional-commit) 的 **Header 体验**，但以轻量方式内嵌到 `intelli-ai-changelog`，不引入完整自定义 Language。

### 目标（范围 C）

- 高亮首行 Header 分段：`type` / `scope` / `!` / `:` / `subject`
- 自动补全：`type`、轻量 `scope`、可选 `!`
- 与现有 AI 生成 Commit Message、Inlay Hint 共存

### 非目标（本版本不做）

- `subject` / `body` / `footer` 补全
- Template 逐步引导（Tab 跳字段）
- Inspection / QuickFix / 一键 Reformat
- `conventionalcommit.json` 自定义 tokens
- 自定义 Language / Lexer / PSI
- 劫持 `EditorTextFieldProvider` 或反射替换语言

---

## 2. 方案选型

对比后采用 **方案 2：轻量正则解析 + MarkupModel 高亮 + CompletionContributor**。

| 方案 | 结论 |
|------|------|
| 1. 完整自定义 Language（接近开源插件） | 过重，反射/内部 API 易碎，不采用 |
| 2. 正则解析 + MarkupModel + CompletionContributor | **采用** |
| 3. 可选依赖开源插件 | 体验与发布耦合，不采用 |

参考开源插件的能力边界与 token 语义，但实现上刻意简化，避免复制其 Language/反射体系。

---

## 3. 架构与组件

新增包：

```text
dev.dong4j.zeka.stack.idea.plugin.changelog.conventional
```

与现有 `hint/` 并列，不改 AI 生成主链路。

```text
Commit Message Editor
        │
        ├─ hint/          （已有）Inlay「按 Tab 生成」
        └─ conventional/  （新增）
              ├─ ConventionalCommitHeaderParser
              ├─ ConventionalCommitHighlighter
              ├─ ConventionalCommitCompletionContributor
              ├─ ConventionalCommitTypes
              ├─ ConventionalCommitScopeProvider   （轻量 scope 候选）
              └─ ConventionalCommitEditorSupport
```

### 职责

| 组件 | 职责 | 不做 |
|------|------|------|
| `ConventionalCommitHeaderParser` | 解析第一行 Header，输出带 `TextRange` 的 token | 不解析 body/footer |
| `ConventionalCommitHighlighter` | 用 `MarkupModel`/`RangeHighlighter` 分段着色 | 不做 Inspection |
| `ConventionalCommitCompletionContributor` | type / scope / `!` 补全 | 不补 subject/body/footer |
| `ConventionalCommitTypes` | 与 prompt 一致的 type 白名单 | 不做用户自定义 JSON |
| `ConventionalCommitScopeProvider` | 缓存近期提交中的 scope | 不做复杂 path 推断 |
| `ConventionalCommitEditorSupport` | 识别 Commit 编辑器并挂载高亮监听 | 不换 Language |

### 与现有能力关系

- **Inlay Hint**：继续使用 `CommitMessageHintService.isCommitMessageEditor`；高亮与 Hint 并列
- **AI 生成**：写入文档后高亮自动重算首行；不改 `CommitMessageGenerator` 主流程
- **`CommitMessageFormatter`**：仍只负责 subject/body 空行约束，与补全解耦

---

## 4. Header 解析规则

只解析文档**第一行**，语义对齐 Conventional Commits：

```text
<type>[optional scope][optional !]: <subject>
```

| 字段 | 示例 | 说明 |
|------|------|------|
| type | `feat` | 到 `(` / `!` / `:` 之前 |
| scope | `(changelog)` | 可选；括号内文本（不含括号） |
| breaking | `!` | 可选；紧挨 `:` 前 |
| separator | `:` | type/scope 与 subject 分隔 |
| subject | `add highlight` | `:` 后到行尾 |

要求：

- 每个有效 token 带 `TextRange`，供高亮与补全共用
- 半输入也要能判断当前段（例如仅输入 `fe` 时仍处于 type 段）
- 第二行及以后一律忽略

---

## 5. 高亮行为

| Token | 样式意图 |
|-------|----------|
| type（白名单内） | 关键词强调色 |
| type（未知） | 弱样式（与标准 type 区分） |
| scope | 标识符/参数色 |
| `!` | 警告/强调色 |
| `:` | 普通标点 |
| subject | 普通文本或略弱强调 |

约束：

- 仅高亮第一行
- Document 变更后防抖刷新（约 50–100ms）
- AI 流式写入期间允许刷新，不做特殊屏蔽
- 使用 `TextAttributesKey` + 默认配色；本版本不做独立 Color Settings 页

---

## 6. 补全行为

触发条件：

1. 当前编辑器是 Commit Message Editor
2. BASIC 补全
3. 光标位于第一行相关段

| 光标上下文 | 补全内容 | 插入行为 |
|------------|----------|----------|
| type 段 | 标准 types | 插入 type；若其后没有 `:`，自动补 `: ` |
| scope 段（`(` 内） | 近期 git scopes（有则给，无则空） | 插入 scope 文本 |
| type 后、`:` 前可补 breaking | `!` | 插入 `!` |
| subject / body / footer | **不提供补全** | — |

### Type 白名单

与现有 Commit Message prompt 保持一致：

```text
feat, fix, refactor, perf, docs, test, build, chore, style, revert
```

本版本不加 `ci`（若后续需要再扩展白名单）。

### Scope 候选（轻量）

1. 优先：从近期 Git 提交 message 解析出的 scope（后台缓存）
2. 失败或为空：返回空列表，不阻塞补全弹层
3. 不做基于 path/module 的复杂推断（那是 AI 生成链路的职责）

### 与 Tab / Inlay 的关系

- 现有 Tab 绑定「从 Hint 生成 Commit Message」保持不变
- 补全不抢占 Tab 语义；用户通过 Ctrl+Space 或 IDE 自动弹出触发补全

---

## 7. 数据流

```text
Editor 创建
  → isCommitMessageEditor?
  → ConventionalCommitEditorSupport 挂 DocumentListener
  → parse 首行 → MarkupModel 上色

用户输入 / AI 写入
  → 防抖 → 重新 parse → 更新 Highlighter

用户触发补全
  → CompletionContributor
  → 确认 Commit Message + 首行上下文
  → type / scope / ! 分支补全
  → subject 及以下：直接跳过
```

---

## 8. 配置

在 `SettingsState` 增加总开关（默认 `true`）：

- 字段：`enableConventionalCommitAssist`
- 设置页文案：启用 Commit Message 规范高亮与 type/scope 补全
- 关闭后：不高亮、不提供本功能补全

本版本不做「只高亮不补全」细粒度拆分。

---

## 9. plugin.xml 注册

采用**独立** `editorFactoryListener`，与 Hint 监听器分离，避免互相耦合：

```xml
<editorFactoryListener
    implementation="...conventional.ConventionalCommitEditorFactoryListener"/>

<completion.contributor
    language="TEXT"
    implementationClass="...conventional.ConventionalCommitCompletionContributor"/>
```

识别 Commit Message 时优先复用 / 增强：

- `CommitMessageHintService.isCommitMessageEditor`
- 文档上的 `CommitMessage.DATA_KEY`（若可得）

---

## 10. 线程模型

- UI 更新（Highlighter、补全展示）：EDT
- Git scope 扫描 / 缓存构建：BGT（`executeOnPooledThread` 或 `ReadAction.nonBlocking`）
- 禁止在 EDT 做 Git 历史遍历

---

## 11. 测试计划

### 单测

- Parser：完整 Header、无 scope、带 `!`、半输入 `fe`、未知 type `foo:`、空行、多行只取首行
- Types：白名单内容与排序稳定
- Completion 过滤逻辑（可纯单测）：subject 段无建议；非 commit 上下文不触发

### 手测

- Modal Commit / Non-modal Commit 工具窗口
- 输入 `f` → 提示 `feat` → 插入 `feat: `
- `feat(|)` 内补近期 scope（有历史时）
- AI 生成后首行正确着色
- 与 Inlay Hint、Tab 生成共存
- 设置关闭后功能全部停用

---

## 12. 风险与对策

| 风险 | 对策 |
|------|------|
| Commit 框识别不准 | 复用并必要时增强现有识别逻辑；同时检查 `CommitMessage.DATA_KEY` |
| 与拼写检查/其他 Markup 冲突 | 仅用 `RangeHighlighter`，不改 Document/PSI |
| scope 拉取 Git 偏慢 | 后台缓存；补全只读缓存；失败返回空 |
| 与第三方 Conventional Commit 插件重叠 | 可接受；设置可关；手册说明 |
| 高亮刷新过于频繁 | 防抖 50–100ms |

---

## 13. 文档与发布后续（实现完成后）

按插件开发规范，编码完成后再更新：

1. `includes/pluginChanges.html`（中英文更新记录）
2. 用户手册中 Commit Message 相关章节（高亮/补全/开关说明）

---

## 14. 实现顺序建议

1. `ConventionalCommitHeaderParser` + 单测
2. `ConventionalCommitTypes` + Highlighter + EditorSupport
3. `ConventionalCommitCompletionContributor`（type / `!`）
4. 轻量 `ConventionalCommitScopeProvider` + scope 补全
5. 设置开关 + i18n
6. 手测与更新记录 / 手册

---

## 15. 确认记录

- 范围：C（Header 分段高亮 + 分段补全）
- 实现路径：方案 2（轻量，无自定义 Language）
- subject 段：不补全
- scope：轻量（近期 git scopes）
- 不做 Inspection / Template / JSON 自定义
