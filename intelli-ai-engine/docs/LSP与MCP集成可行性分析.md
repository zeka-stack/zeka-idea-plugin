# LSP 与 MCP 集成可行性分析

## 文档信息

| 项目   | 内容                                                                                                                                                                                                                                                                                                                     |
|------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 文档版本 | 1.1                                                                                                                                                                                                                                                                                                                    |
| 创建日期 | 2026-01-16                                                                                                                                                                                                                                                                                                             |
| 参考来源 | [IntelliJ LSP API 文档](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html)、[JetBrains LSP 博客](https://blog.jetbrains.com/platform/2025/09/the-lsp-api-is-now-available-to-all-intellij-idea-users-and-plugin-developers/)、[IntelliJ MCP 文档](https://www.jetbrains.com/help/idea/mcp-server.html) |

---

## 一、结论摘要

- **可行性高**：LSP 与 MCP 都是 IntelliJ 平台官方支持能力，Engine 插件可以基于现有 AI 服务能力进行集成。
- **LSP 带来 IDE 内智能增强**：用于补全、诊断、悬停文档、Code Action 等功能，适合把 Engine 的 AI 能力融入编辑体验。
- **MCP 带来外部协作能力**：允许外部 AI 客户端调用 IDE 工具集，形成“对话式开发”和自动化工作流。
- **关键约束**：LSP 仍是商业能力扩展（不在开源版与 Android Studio 中），MCP 需要用户手动开启并注意权限与安全。

---

## 二、LSP API 集成可行性分析

### 2.1 适用范围与版本要求

- **最低 IDE 版本**：2023.2+。
- **支持 IDE**：IntelliJ IDEA、WebStorm、PhpStorm、PyCharm、DataSpell、RubyMine、CLion、DataGrip、GoLand、Rider、RustRover。
- **不支持**：IntelliJ IDEA 开源版、Android Studio。
- **分发变化（博客说明）**：
    - 2025.2 起，IntelliJ IDEA Ultimate 订阅过期后仍可使用包含 LSP 的有限功能集。
    - 2025.3 起统一分发，LSP 支持对所有用户可用，但 LSP 实现仍为闭源商业组件。

### 2.2 插件依赖与构建配置

- `plugin.xml` 需要声明：
    - `com.intellij.modules.lsp`
    - `com.intellij.modules.ultimate`（面向商业 IDE 时）
- 若针对 2025.2.1+，可使用可选依赖与 since-build 限制：
    - `sinceBuild = 252.25557`
    - `<depends optional="true" config-file="lsp.xml">com.intellij.modules.lsp</depends>`

### 2.3 集成方式与关键扩展点

核心步骤来自官方最小集成方案：

1. 实现 `LspServerSupportProvider`。
2. 在 `com.intellij.platform.lsp.serverSupportProvider` 扩展点注册。
3. 在 `fileOpened()` 中启动 `LspServerDescriptor`。
4. 通过 `createCommandLine()` 提供 LSP Server 启动命令（StdIO 或 Socket）。
5. 可选：通过 `createLspServerWidgetItem()` 提供状态栏监控入口。
6. 通过 `LspCustomization` 开关/定制特性。

### 2.4 LSP 功能支持矩阵（官方文档）

| 版本     | 支持功能                                                                                                           |
|--------|----------------------------------------------------------------------------------------------------------------|
| 2025.3 | Server Initiated Progress、Highlight Usages、Go To Symbol、File Structure、Breadcrumbs、Sticky Lines、Parameter Info |
| 2025.2 | Inlay Hints、Folding Range                                                                                      |
| 2025.1 | Document Link、Pull Diagnostics                                                                                 |
| 2024.3 | Color Preview、Document Save Notification、Go To Type Declaration                                                |
| 2024.2 | Find Usages、Completion/Code Action Resolve、Semantic Highlighting                                               |
| 2024.1 | Socket 通信、Execute Command、Apply WorkspaceEdit、Show Document                                                    |
| 2023.3 | Intention Actions、Code Formatting、Request Cancellation、Quick Documentation                                     |
| 2023.2 | StdIO 通信、错误/警告高亮、Quick Fix、Completion、Go To Definition                                                         |

### 2.5 集成价值与限制

- **价值**：以较低成本提供语言能力，实现补全、诊断、导航、文档展示等功能，与 Engine 的 AI 服务形成互补。
- **限制**：LSP 不等同 PSI 深度能力，IDE 集成面更窄，性能存在进程通信开销。

---

## 三、MCP 集成可行性分析

### 3.1 平台支持与配置方式

从 IntelliJ IDEA **2025.2** 起内置 MCP Server。官方说明支持以下客户端自动配置：
Claude Code、Claude Desktop、Cursor、VS Code、Codex、Windsurf。

配置路径：

1. `Settings | Tools | MCP Server`。
2. 启用 MCP Server。
3. 通过 **Auto-Configure** 自动写入客户端 JSON 配置。
4. 如需手动配置，可复制 **SSE** 或 **Stdio** 配置。

### 3.2 安全与权限

MCP 支持 “Run shell commands or run configurations without confirmation (brave mode)”。
开启后外部客户端可以直接执行终端命令或运行配置，应在文档中明确风险与使用边界。

### 3.3 支持工具（官方文档）

工具覆盖运行、文件、搜索、重构、项目与终端等能力：

- **运行**：`execute_run_configuration`、`get_run_configurations`
- **文件**：`get_file_text_by_path`、`replace_text_in_file`、`create_new_file`、`reformat_file`
- **搜索**：`search_in_files_by_text`、`search_in_files_by_regex`、`find_files_by_glob`、`find_files_by_name_keyword`
- **代码分析**：`get_file_problems`、`get_symbol_info`
- **重构**：`rename_refactoring`
- **终端**：`execute_terminal_command`
- **项目信息**：`get_project_modules`、`get_project_dependencies`、`get_repositories`
- **编辑器/目录**：`open_file_in_editor`、`get_all_open_file_paths`、`list_directory_tree`

### 3.4 可行性结论

Engine 插件无需实现 MCP Server，本身即可受益于 IDE 的 MCP 工具集。
若需额外暴露 Engine 能力，可通过插件内 Action/Run Configuration 等方式被 MCP 调用，实现“外部 AI 驱动 Engine 能力”的闭环。

---

## 四、高价值功能特性（结合 LSP + MCP）

### 4.1 LSP 驱动的 IDE 智能能力

1. **AI 增强补全**
    - 在 `textDocument/completion` 与 `completionItem/resolve` 中注入 AI 建议。
    - 提供更强的上下文理解与代码片段生成。

2. **AI 悬停文档**
    - 在 `textDocument/hover` 返回 AI 生成的解释、使用示例与注意事项。

3. **AI 诊断与修复**
    - 结合 `textDocument/diagnostic` + `codeAction`，输出问题与修复方案。
    - 可扩展为“AI 代码审查”体验。

4. **AI 导航与结构洞察**
    - 在 `documentSymbol`、`workspace/symbol` 中提供语义增强展示。

### 4.2 MCP 驱动的外部协作能力

1. **对话式自动化开发**
    - 外部 AI 通过 MCP 工具搜索、修改、格式化、运行测试。
    - 可形成“需求 -> 变更 -> 测试 -> 报告”的自动化链路。

2. **跨应用协作**
    - 外部 AI 客户端读取工程结构与文件内容，结合 Engine 的 AI 生成能力输出高质量变更。

3. **远程与 CI 辅助**
    - SSE/StdIO 连接支持远程控制 IDE 工具链。
    - 可作为 CI/质量门禁的辅助执行器。

### 4.3 LSP + MCP 组合收益

- LSP 提供实时交互式能力（编辑器内体验）。
- MCP 提供流程化能力（跨工具自动化）。
- Engine 既能服务本地开发体验，也能作为外部 AI 的“行动载体”。

---

## 五、实施建议

### 5.1 分阶段落地

**第一阶段（可感知价值）**

- LSP：实现 Hover 文档与基础诊断
- MCP：补充文档与引导，降低用户启用成本

**第二阶段（核心能力）**

- LSP：补全 + Code Action 修复
- MCP：外部 AI 自动化修改、测试、报告

**第三阶段（生态扩展）**

- LSP：更丰富的符号/结构能力
- MCP：与 Engine 内部动作形成完整工作流

### 5.2 风险与对策

| 风险               | 级别 | 对策                     |
|------------------|----|------------------------|
| LSP 版本差异         | 中  | 基于 IDE build 进行功能开关    |
| LSP 性能开销         | 中  | 缓存 + 限流 + 异步执行         |
| MCP 安全风险         | 高  | 默认关闭 brave mode，提供清晰提示 |
| 外部 LSP Server 依赖 | 中  | 提供内置或可配置路径             |

---

## 六、总结

LSP 与 MCP 的集成对 Engine 插件具有高可行性与高收益：

- **LSP**：把 AI 能力融入 IDE 内的补全、诊断、导航、文档展示。
- **MCP**：把 IDE 工具集开放给外部 AI，形成自动化开发与协作链路。

综合来看，优先落地 LSP Hover/诊断与 MCP 自动化工作流，可快速形成用户可感知价值。
