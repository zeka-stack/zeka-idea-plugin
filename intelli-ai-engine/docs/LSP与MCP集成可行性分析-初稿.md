# LSP 与 MCP 集成可行性分析

## 文档信息

| 项目   | 内容                                                                                                                                                                                                                                                                                                                     |
|------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 文档版本 | 1.0                                                                                                                                                                                                                                                                                                                    |
| 创建日期 | 2026-01-16                                                                                                                                                                                                                                                                                                             |
| 参考来源 | [IntelliJ LSP API 文档](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html)、[JetBrains LSP 博客](https://blog.jetbrains.com/platform/2025/09/the-lsp-api-is-now-available-to-all-intellij-idea-users-and-plugin-developers/)、[IntelliJ MCP 文档](https://www.jetbrains.com/help/idea/mcp-server.html) |

---

## 一、概述

本文档分析在 IntelliAI Engine 插件中集成 LSP（Language Server Protocol）和 MCP（Model Context Protocol）的可行性，并探讨集成后能够实现的高价值功能特性。

### 1.1 技术背景

- **LSP（Language Server Protocol）**：语言服务器协议，标准化了开发工具与语言服务器之间的通信，使 IDE 能够提供代码补全、跳转到定义、代码诊断等功能。
- **MCP（Model Context Protocol）**：模型上下文协议，允许外部 AI 客户端（如 Claude Desktop、Cursor）访问 IDE 提供的工具，实现跨应用控制 JetBrains
  IDE。

### 1.2 技术开放情况

| 技术         | 开放时间    | 最低 IDE 版本             | 适用用户                       |
|------------|---------|-----------------------|----------------------------|
| LSP API    | 2025年9月 | IntelliJ IDEA 2025.2+ | 所有用户（Ultimate + Community） |
| MCP Server | 2025年2月 | IntelliJ IDEA 2025.2+ | 所有用户                       |

---

## 二、LSP API 集成可行性分析

### 2.1 兼容性要求

| 要求项       | 详情                                                                                                        |
|-----------|-----------------------------------------------------------------------------------------------------------|
| 最低 IDE 版本 | IntelliJ IDEA 2023.2+（完整功能需 2025.2+）                                                                      |
| 构建版本      | `sinceBuild = "252.25557"`                                                                                |
| 依赖模块      | `com.intellij.modules.lsp`                                                                                |
| 支持的 IDE   | IntelliJ IDEA Ultimate、WebStorm、PhpStorm、PyCharm、DataSpell、RubyMine、CLion、DataGrip、GoLand、Rider、RustRover |
| **不支持**   | IntelliJ IDEA 开源版、Android Studio                                                                          |

### 2.2 集成方式

在 `plugin.xml` 中添加依赖：

```xml
<!-- 必需依赖 -->
<depends>com.intellij.modules.lsp</depends>

<!-- Ultimate 版专用功能 -->
<depends>com.intellij.modules.ultimate</depends>

<!-- 可选：自定义 LSP 配置 -->
<depends optional="true" config-file="lsp.xml">com.intellij.modules.lsp</depends>
```

### 2.3 实现步骤

1. 实现 `LspServerSupportProvider` 接口
2. 在 `com.intellij.platform.lsp.serverSupportProvider` 扩展点注册
3. 配置 `LspServerDescriptor` 自定义功能行为
4. 重写 `createLsp4jClient()` 处理自定义 LSP 通知/请求
5. 可选：通过 `createLspServerWidgetItem()` 添加状态栏监控部件

### 2.4 功能支持矩阵（按 IDE 版本）

| 版本         | 支持的功能                                                                                                          |
|------------|----------------------------------------------------------------------------------------------------------------|
| **2025.3** | Server Initiated Progress、Highlight Usages、Go To Symbol、File Structure、Breadcrumbs、Sticky Lines、Parameter Info |
| **2025.2** | Inlay Hints、Folding Range                                                                                      |
| **2025.1** | Document Link、Pull Diagnostics                                                                                 |
| **2024.3** | Color Preview、Document Save Notification、Go To Type Declaration                                                |
| **2024.2** | Find Usages、Completion/Code Action Resolve、Semantic Highlighting                                               |
| **2024.1** | Socket 通信、Execute Command、Apply WorkspaceEdit、Show Document                                                    |
| **2023.3** | Intention Actions、Code formatting、Request cancellation、Quick documentation                                     |
| **2023.2** | StdIO 通信、错误/警告高亮、快速修复、代码补全、跳转到声明                                                                               |

---

## 三、MCP 集成可行性分析

### 3.1 概述

从 **IntelliJ IDEA 2025.2** 起，IDE 内置 MCP Server，允许外部 AI 客户端通过 MCP 协议访问 IDE 工具。

### 3.2 集成方式

**自动配置**（推荐）：

1. 进入 Settings | Tools | MCP Server
2. 启用 MCP Server
3. 点击 "Auto-Configure" 自动更新客户端配置

**手动配置**：复制 SSE 或 Stdio 配置信息到客户端设置文件

### 3.3 支持的核心工具

| 类别       | 工具名称                         | 功能描述        |
|----------|------------------------------|-------------|
| **运行相关** | `execute_run_configuration`  | 执行运行配置      |
|          | `get_run_configurations`     | 获取运行配置列表    |
| **文件操作** | `get_file_text_by_path`      | 读取文件内容      |
|          | `replace_text_in_file`       | 替换文件文本      |
|          | `create_new_file`            | 创建新文件       |
|          | `reformat_file`              | 格式化文件       |
| **搜索功能** | `search_in_files_by_text`    | 文本搜索        |
|          | `search_in_files_by_regex`   | 正则搜索        |
|          | `find_files_by_glob`         | Glob 模式查找文件 |
|          | `find_files_by_name_keyword` | 按名称关键字查找    |
| **代码分析** | `get_file_problems`          | 分析文件错误和警告   |
|          | `get_symbol_info`            | 获取符号信息      |
| **重构**   | `rename_refactoring`         | 重命名重构       |
| **终端**   | `execute_terminal_command`   | 执行终端命令      |
| **项目信息** | `get_project_modules`        | 获取模块列表      |
|          | `get_project_dependencies`   | 获取依赖列表      |
|          | `get_repositories`           | 获取 VCS 仓库列表 |
| **编辑器**  | `open_file_in_editor`        | 在编辑器中打开文件   |
|          | `get_all_open_file_paths`    | 获取已打开文件路径   |
|          | `list_directory_tree`        | 列出目录树       |

### 3.4 Engine 插件作为 MCP Server 的可行性

| 维度        | 分析                                               |
|-----------|--------------------------------------------------|
| **技术可行性** | 可以基于 MCP SDK 实现自定义 MCP Server，提供插件特有功能           |
| **外部访问**  | 允许外部 AI 客户端（如 Claude Desktop）调用 Engine 的 AI 生成能力 |
| **权限控制**  | MCP 提供无确认执行模式，需注意安全风险                            |
| **配置要求**  | 需要用户在 IDE 中配置 MCP Server 连接信息                    |

---

## 四、高价值功能特性

### 4.1 LSP 集成 - AI 增强的代码智能功能

#### 4.1.1 AI 驱动的代码补全（高价值 ⭐⭐⭐⭐⭐）

**功能描述**：

- 利用 AI 模型提供上下文感知的代码补全建议
- 超越传统基于语法和词法分析的补全
- 支持根据项目代码风格、文档注释自动生成代码片段

**技术实现**：

```
1. 实现 LSP CodeCompletion 扩展点
2. 将 AI Provider 与 LSP 客户端集成
3. 过滤和排序 AI 建议与传统建议
4. 支持 resolve 机制获取详细建议信息
```

**用户价值**：

- 提升编码效率，尤其是复杂业务逻辑场景
- 减少查阅文档和示例的时间
- AI 理解项目上下文，提供更精准的建议

#### 4.1.2 AI 生成的 Hover 文档（高价值 ⭐⭐⭐⭐）

**功能描述**：

- 在鼠标悬停时调用 AI 生成详细的文档说明
- 分析方法/类的实现逻辑，自动生成使用示例
- 结合项目现有文档生成增强说明

**技术实现**：

```
1. 实现 LSP Hover 扩展点
2. 提取 PSI 元素信息
3. 构造 AI 提示词（包含上下文）
4. 返回 Markdown 格式的文档内容
```

**用户价值**：

- 快速理解陌生代码
- 无需离开 IDE 即可获得详细说明
- 动态生成的文档始终保持最新

#### 4.1.3 AI 驱动的代码诊断与修复（高价值 ⭐⭐⭐⭐⭐）

**功能描述**：

- 利用 AI 进行深度代码质量分析
- 识别潜在 bug、安全漏洞、性能问题
- 提供智能修复建议并支持一键应用

**技术实现**：

```
1. 实现 LSP Pull/Push Diagnostics 扩展点
2. 调用 AI 分析代码质量
3. 返回诊断结果和修复方案
4. 实现 Code Action 支持快速修复
```

**用户价值**：

- 在编码时实时获得 AI 代码审查
- 提前发现潜在问题
- 学习最佳实践和改进方式

#### 4.1.4 AI 增强的"跳转到定义"（高价值 ⭐⭐⭐⭐）

**功能描述**：

- 不仅跳转到定义位置，还提供 AI 生成的使用说明
- 分析调用关系，展示调用栈和调用点
- 智能推断用户意图，跳转到最相关的位置

**技术实现**：

```
1. 实现 Go To Definition / Type Definition
2. 结合 PSI 分析和 AI 上下文理解
3. 生成跳转目标的使用文档
```

**用户价值**：

- 更快理解代码结构和调用关系
- 在大型项目中导航更高效
- 学习项目架构和设计模式

#### 4.1.5 AI 代码重构建议（高价值 ⭐⭐⭐⭐⭐）

**功能描述**：

- 识别代码中的重构机会
- 提供安全的重构方案和风险评估
- 批量处理多个重构点

**技术实现**：

```
1. 实现 LSP Code Action 扩展点
2. 调用 AI 分析代码质量和技术债
3. 生成重构建议和代码转换
4. 支持预览和逐步应用
```

**用户价值**：

- 持续改进代码质量
- 降低技术债积累
- 学习重构最佳实践

### 4.2 MCP 集成 - 外部 AI 协作能力

#### 4.2.1 外部 AI 客户端控制 IDE（高价值 ⭐⭐⭐⭐⭐）

**功能描述**：

- 允许 Claude Desktop、Cursor 等外部 AI 客户端控制 IntelliJ IDEA
- 通过自然语言指令让外部 AI 操作 IDE
- 实现"对话式开发"工作流

**技术实现**：

```
1. 配置 MCP Server 连接
2. 暴露 IDE 工具给外部 AI
3. 解析和执行 AI 的 IDE 操作指令
4. 反馈执行结果给 AI
```

**用户价值**：

- 融合外部 AI 的理解能力和 IDE 的开发功能
- 实现更复杂的自动化任务
- 远程开发场景支持

#### 4.2.2 跨应用协作开发（高价值 ⭐⭐⭐⭐）

**功能描述**：

- 外部 AI 可以读取项目文件、分析代码结构
- 根据项目上下文生成代码并写入文件
- 执行测试、构建等操作

**技术实现**：

```
1. 利用 MCP 工具集：
   - get_file_text_by_path / replace_text_in_file
   - search_in_files_by_text
   - execute_run_configuration
   - execute_terminal_command
2. 实现工作流协调机制
```

**用户价值**：

- 外部 AI 能够基于完整项目上下文工作
- 实现"需求→代码"的端到端自动化
- 提升 AI 编程的准确性和效率

#### 4.2.3 远程开发与 CI/CD 集成（高价值 ⭐⭐⭐⭐）

**功能描述**：

- 远程服务器上运行 IDE，通过 MCP 控制
- CI/CD 流程中调用 IDE 工具进行分析
- 自动化代码审查和测试

**技术实现**：

```
1. MCP over SSE 实现远程连接
2. 暴露必要的 IDE 功能给 CI 系统
3. 集成报告和通知机制
```

**用户价值**：

- 开发环境与执行环境分离
- 统一的代码质量检查流程
- 自动化开发工作流

### 4.3 Engine 插件作为 MCP Server

#### 4.3.1 暴露 AI 生成能力（高价值 ⭐⭐⭐⭐⭐）

**功能描述**：

- Engine 插件可以作为 MCP Server 运行
- 外部 AI 客户端可以调用 Engine 的 AI 生成功能
- 实现能力的标准化输出

**技术实现**：

```
1. 实现自定义 MCP Server
2. 定义 AI 生成相关的工具接口
3. 集成 AI Provider 选择和配置
4. 提供流式输出支持
```

**工具接口设计**：

```json
{
  "tools": [
    {
      "name": "ai_generate_code",
      "description": "使用 AI 生成代码",
      "parameters": {
        "type": "object",
        "properties": {
          "prompt": {"type": "string", "description": "生成提示词"},
          "language": {"type": "string", "description": "编程语言"},
          "context": {"type": "string", "description": "上下文代码"}
        }
      }
    },
    {
      "name": "ai_generate_docs",
      "description": "使用 AI 生成文档",
      "parameters": {...}
    }
  ]
}
```

**用户价值**：

- 统一 AI 能力接口
- 外部 AI 可以利用内部 AI Provider
- 实现能力复用和组合

---

## 五、技术实现方案

### 5.1 LSP 集成架构

```
┌─────────────────────────────────────────────────────────────┐
│                      IntelliJ IDEA                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  PSI Layer  │  │  LSP API    │  │  Engine Plugin      │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                     │             │
│         └────────────────┼─────────────────────┘             │
│                          │                                   │
│                  ┌───────▼───────┐                          │
│                  │   LspServer   │                          │
│                  │   (Language   │                          │
│                  │   Server)     │                          │
│                  └───────┬───────┘                          │
│                          │                                   │
│                  ┌───────▼───────┐                          │
│                  │   AI Engine   │                          │
│                  │   (via LSP)   │                          │
│                  └───────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 MCP 集成架构

```
┌──────────────────────┐     MCP      ┌──────────────────────┐
│   External AI        │──────────────▶│   IntelliJ IDEA      │
│   (Claude Desktop,   │               │                      │
│    Cursor, etc.)     │               │  ┌────────────────┐ │
│                      │◀──────────────│  │  MCP Server    │ │
└──────────────────────┘               │  └───────┬────────┘ │
                                        │          │          │
                                        │  ┌───────▼────────┐│
                                        │  │  Engine Plugin ││
                                        │  │  (AI Provider) ││
                                        │  └───────┬────────┘│
                                        │          │          │
                                        │  ┌───────▼────────┐│
                                        │  │  AI Services   ││
                                        │  └────────────────┘│
                                        └────────────────────┘
```

### 5.3 核心类设计

#### LSP 集成

```java
// LSP Server 支持提供器
public class EngineLspServerSupportProvider implements LspServerSupportProvider {
    @Override
    public LspServerSupportIndicator createLspServer(
            Project project,
            LspServerDescriptorBuilder builder
    ) {
        builder.withServerId("engine-lsp")
               .withServerName("IntelliAI Engine LSP");

        // 配置支持的 LSP 功能
        builder.lspCustomization(c -> {
            c.setCompletionSupport(true);
            c.setHoverSupport(true);
            c.setDiagnosticsSupport(true);
            c.setCodeActionSupport(true);
            c.setDefinitionSupport(true);
            c.setFindUsagesSupport(true);
        });

        return builder.build();
    }
}
```

#### MCP Server 集成

```java
// Engine 作为 MCP Server
public class EngineMcpServer implements McpServer {

    @Tool
    public String generateCode(
            @Param(name = "prompt") String prompt,
            @Param(name = "language") String language,
            @Param(name = "context") String context
    ) {
        return aiService.generate(prompt, language, context);
    }

    @Tool
    public String generateDocumentation(
            @Param(name = "code") String code,
            @Param(name = "language") String language
    ) {
        return documentationService.generate(code, language);
    }
}
```

---

## 六、风险与挑战

### 6.1 技术风险

| 风险          | 级别 | 缓解措施               |
|-------------|----|--------------------|
| IDE 版本兼容性问题 | 中  | 明确最低版本要求，提供特性检测    |
| LSP 性能影响    | 中  | 异步实现，缓存机制，限流控制     |
| MCP 安全风险    | 高  | 严格权限控制，敏感操作确认      |
| 外部依赖稳定性     | 中  | 降级策略，多 Provider 切换 |

### 6.2 功能限制

| 限制项            | 说明                     |
|----------------|------------------------|
| Ultimate 专属功能  | 部分 LSP 功能仅 Ultimate 可用 |
| 平台差异           | 不同 IDE 支持的功能可能不同       |
| AI Provider 限制 | 外部 MCP 访问需要配置 AI 服务    |

---

## 七、实施建议

### 7.1 优先级排序

| 优先级 | 功能                   | 原因            |
|-----|----------------------|---------------|
| P0  | AI 驱动的代码诊断与修复        | 高频刚需，直接提升代码质量 |
| P0  | AI 生成的 Hover 文档      | 实现简单，用户感知明显   |
| P1  | AI 驱动的代码补全           | 核心功能，但实现复杂度高  |
| P1  | 外部 AI 客户端控制 IDE      | 差异化特性，竞争力强    |
| P2  | AI 代码重构建议            | 高级功能，依赖基础能力   |
| P2  | Engine 作为 MCP Server | 生态扩展，长期价值     |

### 7.2 分阶段实施

**第一阶段（基础能力）**

- 实现 LSP 基础框架
- 集成 AI Provider 到 LSP 接口
- 实现 Hover 文档功能

**第二阶段（核心功能）**

- 实现 AI 代码诊断
- 实现 AI 代码补全
- 实现 Code Action 快速修复

**第三阶段（高级特性）**

- 实现 MCP Server 功能
- 实现外部 AI 协作
- 实现 AI 重构建议

---

## 八、参考资源

### 8.1 官方文档

- [IntelliJ LSP API 文档](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html)
- [IntelliJ MCP Server 文档](https://www.jetbrains.com/help/idea/mcp-server.html)
- [JetBrains LSP API 博客发布](https://blog.jetbrains.com/platform/2025/09/the-lsp-api-is-now-available-to-all-intellij-idea-users-and-plugin-developers/)

### 8.2 参考实现

- [Prisma ORM 插件 LSP 实现](https://github.com/JetBrains/intellij-plugins/tree/idea/253.29346.240/prisma/src/org/intellij/prisma/ide/lsp)

---

## 九、总结

LSP 和 MCP 的集成为 Engine 插件带来了显著的能力提升：

| 集成方向    | 核心价值                                           |
|---------|------------------------------------------------|
| **LSP** | 将 AI 能力深度嵌入 IDE 的代码智能功能，提供 AI 驱动的补全、诊断、导航和重构能力 |
| **MCP** | 打通外部 AI 客户端与 IDE 的连接，实现"对话式开发"和跨应用协作           |

通过这两个协议的集成，Engine 插件可以从"单一 AI 生成工具"升级为"AI 驱动的智能开发平台"，显著提升用户开发效率和体验。
