# 可行性分析：Intelli-AI Engine 的 LSP 与 MCP 集成

**日期：** 2026-01-16
**状态：** 草稿

## 1. 简介 (Introduction)

本文档分析了在 `intelli-ai-engine` 插件中集成 **语言服务器协议 (LSP)** 和 **模型上下文协议 (MCP)** 的可行性及其潜在价值。目标是确定这些技术如何增强插件在代码理解、操作以及与
AI 智能体 (Agent) 交互方面的能力。

## 2. 语言服务器协议 (LSP) 集成

### 2.1 概述

LSP 允许 IDE 与外部语言服务器通信，以提供代码补全、导航和诊断等功能。IntelliJ IDEA 现已为插件开发者引入了原生的 LSP 支持。

### 2.2 集成机制

* **依赖要求：** 插件必须适配 IntelliJ Platform 2023.2+ 版本（为了更广泛的访问权限，推荐 2025.2.1+）。
* **核心类：**
    * `LspServerSupportProvider`：注册 LSP 服务器的入口点。
    * `LspServerDescriptor`：定义如何启动服务器以及它支持哪些文件。
* **流程：**
    1. 插件检测到受支持的文件类型。
    2. 启动 LSP 服务器进程（标准输入输出 stdio 或 socket）。
    3. IDE 自动处理 JSON-RPC 通信。

### 2.3 价值分析

* **语言无关的智能支持：** 集成现有的高质量语言服务器（例如针对小众语言或特定的内部 DSL），无需重新编写 PSI 解析器。
* **自定义 AI 服务器：** 我们可以将 AI 模型封装为 LSP 服务器。AI 可以通过标准的 LSP 协议提供“补全”或“诊断”（代码审查意见），并由 IntelliJ 原生渲染。

## 3. 模型上下文协议 (MCP) 集成

### 3.1 概述

MCP 允许 IDE 充当 **服务器 (Server)**，将其工具和上下文暴露给外部 AI **客户端 (Client)**（如 Claude Desktop、Cursor 或自定义 AI Agent）。

### 3.2 能力（IDE 作为工具提供者）

通过实现/启用 MCP，`intelli-ai-engine` 或 IDE 本身可以暴露以下工具：

* `read_file` / `replace_text_in_file`：直接的文件读写操作。
* `get_file_problems`：获取编译器错误/警告。
* `execute_run_configuration`：运行测试或应用程序。
* `get_symbol_info` / `search_in_files`：语义化代码搜索。

### 3.3 集成机制

* **原生支持：** IntelliJ IDEA 2025.2+ 具有内置的 MCP 服务器功能。
* **插件角色：** `intelli-ai-engine` 可以：
    1. **增强 MCP：** 注册标准 MCP 未提供的 *自定义* 工具（例如 "explain_codebase_architecture"（解释代码库架构）、"fetch_jira_ticket"（获取 Jira
       工单））。
    2. **充当客户端（混合模式）：** 插件可以作为其他 MCP 服务器（例如数据库 MCP）的本地客户端，将外部上下文引入 IDE。

## 4. 协同效应带来的高价值特性

集成这两种协议可以实现智能的双向流动：

### 4.1 AI 驱动的自主重构

* **工作流：**
    1. **MCP** Agent 接收任务：“将此类重构为单例模式”。
    2. **LSP** 为 Agent 提供精确的符号用法和定义位置（通过 `get_symbol_info` 或标准 LSP 查找）。
    3. **MCP** Agent 执行 `replace_text_in_file`。
    4. **LSP** 立即报告变更引入的新诊断信息（错误）。
    5. **MCP** Agent 迭代修复错误，直到 LSP 报告状态正常。

### 4.2 语义化“智能”聊天

* AI Agent（通过 MCP 连接）可以直接“看到”项目，而不是将代码粘贴到聊天窗口中。
* 在建议更改之前，它可以查询 LSP 以了解变量的类型层次结构，确保建议的代码是有效的。

### 4.3 自动化调试循环

* **MCP** 触发 `execute_run_configuration`。
* 如果运行失败，**MCP** 捕获堆栈跟踪。
* **LSP** 将堆栈跟踪映射到具体的代码位置。
* **AI** 生成修复方案并通过 **MCP** 应用。

## 5. 可行性与路线图

### 5.1 可行性

* **高可行性：** API 现已稳定（基于 2025/2026 年的背景）。
* **限制：** 需要用户使用较新的 IDE 版本（推荐 2025.2+）。

### 5.2 下一步计划

1. **LSP 原型验证：** 在 `intelli-ai-engine` 中创建一个简单的 `LspServerSupportProvider`，连接到一个模拟服务器以测试链路。
2. **MCP 工具原型：** 在 IDE 的 MCP 注册表中注册一个自定义工具（如果 API 允许扩展）或验证与外部 MCP 客户端（如 Claude Desktop）的连接。
3. **架构定义：** 决定 `intelli-ai-engine` 是否托管其自己的本地“推理引擎”，并通过这些协议与 IDE 通信。

## 6. 结论

采用 LSP 和 MCP 是一项战略性举措。它将 `intelli-ai-engine` 从一个被动的辅助工具转变为一个 **主动的代理平台 (Agentic Platform)**。它允许 IDE
与现代 AI Agent 进行原生通信，提供自主编码所需的“事实真相”（LSP）和“执行能力”（MCP）。
