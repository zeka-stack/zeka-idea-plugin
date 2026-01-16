# LSP 与 MCP 在 IntelliAI Engine 插件中的集成可行性分析

## 一、背景与概述

### 1.1 技术背景

- **LSP (Language Server Protocol)**：JetBrains 平台从 2025.2 版本开始，LSP API 对所有 IntelliJ IDEA 用户和插件开发者开放（包括 Community 版和
  Ultimate 版）。LSP 提供了一种标准化的方式，让 IDE 可以与语言服务器通信，实现代码补全、诊断、格式化、跳转定义等语言服务功能。

- **MCP (Model Context Protocol)**：JetBrains 从 2025.1/2025.2 版本开始，将 AI Assistant 与 MCP 客户端/服务端支持整合进所有 IntelliJ 家族
  IDE。MCP 允许 IDE 作为 MCP Server 暴露内部工具给外部客户端（如 Claude、VS Code、Cursor 等），同时 IDE 也可以作为 MCP Client 连接外部 MCP
  Servers。

### 1.2 IntelliAI Engine 定位

IntelliAI Engine 是一个面向 IntelliJ IDEA 插件开发者的 AI 服务基础引擎，提供：

- **统一的 AI Service API**：为下游插件提供简洁的 AI 调用接口
- **配置管理与凭证存储**：集中管理所有 AI 服务商的配置和 API Key
- **可复用的 UI 组件**：提供状态栏、设置页面等 UI 组件
- **扩展点机制**：支持第三方插件扩展功能

### 1.3 集成目标

在 IntelliAI Engine 中集成 LSP 和 MCP，旨在：

1. **增强 AI 上下文感知能力**：通过 LSP 获取更准确的代码语义信息
2. **扩展工具调用能力**：通过 MCP 暴露 Engine 的能力给外部 AI 工具
3. **提升跨语言支持**：通过 LSP 支持更多编程语言
4. **实现双向集成**：既作为 MCP Client 调用外部工具，也作为 MCP Server 暴露内部能力

## 二、技术可行性分析

### 2.1 LSP 集成可行性

#### 2.1.1 版本要求

- **最低版本**：IntelliJ IDEA 2025.2.1+
- **依赖模块**：`com.intellij.modules.lsp`（可选依赖）
- **当前 Engine 支持版本**：`platformSinceBuild=242.0`（2024.2），需要提升最低版本要求

#### 2.1.2 技术实现方式

**方式一：作为 LSP Client（推荐）**

Engine 可以作为 LSP Client，连接外部语言服务器，获取代码语义信息：

```java
// 伪代码示例
public class LSPCodeAnalysisService {
    // 连接语言服务器
    LSPClient client = LSPClient.connect(project, languageServerPath);

    // 获取代码诊断信息
    List<Diagnostic> diagnostics = client.getDiagnostics(file);

    // 获取代码补全建议
    List<CompletionItem> completions = client.getCompletions(position);

    // 获取符号定义
    Location definition = client.getDefinition(position);
}
```

**方式二：提供 LSP Server 包装**

Engine 可以包装现有的 AI 服务，提供 LSP 兼容的接口：

```java
// 伪代码示例
public class AILanguageServer implements LanguageServer {
    private AIService aiService;

    @Override
    public CompletableFuture<CompletionList> completion(CompletionParams params) {
        // 使用 AI 服务生成补全建议
        String code = getCodeAtPosition(params);
        String suggestion = aiService.generateCompletion(code);
        return CompletableFuture.completedFuture(convertToCompletionList(suggestion));
    }
}
```

#### 2.1.3 优势与局限

**优势**：

- ✅ 标准化协议，易于集成多种语言服务器
- ✅ 跨 IDE 兼容，支持 VS Code、Cursor 等
- ✅ 丰富的语言服务能力（诊断、补全、格式化等）
- ✅ 从 2025.2 起，Community 版也支持

**局限**：

- ❌ 需要提升最低版本要求到 2025.2.1
- ❌ LSP 的语义深度可能不如 IDE 内部 PSI API
- ❌ 需要管理语言服务器进程（启动、通信、错误处理）
- ❌ 性能开销（进程间通信）

### 2.2 MCP 集成可行性

#### 2.2.1 版本要求

- **最低版本**：IntelliJ IDEA 2025.1+（MCP Server），2025.2+（完整支持）
- **当前 Engine 支持版本**：`platformSinceBuild=242.0`（2024.2），需要提升最低版本要求

#### 2.2.2 技术实现方式

**方式一：作为 MCP Server（高价值）**

Engine 可以作为 MCP Server，暴露 AI 相关工具给外部客户端：

```java
// 伪代码示例
@MCPServerTool(
    name = "generate_javadoc",
    description = "Generate Javadoc comments for selected code"
)
public class JavadocGenerationTool {
    public String execute(String code, String language) {
        AIService aiService = AIService.getInstance();
        AIProviderConfig config = getDefaultConfig();
        AIChatRequest request = new AIChatRequest(
            "Generate Javadoc for the following code",
            code
        );
        return aiService.generateContent(project, request, config, null);
    }
}

@MCPServerTool(
    name = "generate_changelog",
    description = "Generate changelog from git commits"
)
public class ChangelogGenerationTool {
    public String execute(String gitRange) {
        // 生成 changelog 逻辑
    }
}

@MCPServerTool(
    name = "get_project_version",
    description = "Get project version from pom.xml or build.gradle"
)
public class ProjectVersionTool {
    public String execute() {
        return ProjectVersionResolver.resolveVersion(project);
    }
}
```

**方式二：作为 MCP Client**

Engine 可以作为 MCP Client，调用外部 MCP Servers 提供的工具：

```java
// 伪代码示例
public class ExternalMCPClient {
    private MCPClient client;

    public void connect(String serverUrl) {
        client = MCPClient.connect(serverUrl);
    }

    public String callExternalTool(String toolName, Map<String, Object> params) {
        return client.callTool(toolName, params);
    }
}
```

#### 2.2.3 优势与局限

**优势**：

- ✅ IDE 内置支持，无需额外依赖
- ✅ 可以暴露 Engine 的 AI 能力给外部工具（Claude、Cursor 等）
- ✅ 可以调用外部 MCP Servers 扩展功能
- ✅ 标准化的工具调用协议

**局限**：

- ❌ 需要提升最低版本要求到 2025.1+
- ❌ 安全性和权限控制需要谨慎处理
- ❌ 用户可能不了解 MCP，需要良好的文档和配置界面

### 2.3 兼容性策略

#### 2.3.1 版本兼容处理

由于 LSP 和 MCP 需要较新的 IDE 版本，建议采用以下策略：

1. **可选依赖声明**：在 `plugin.xml` 中将 LSP 和 MCP 模块声明为可选依赖
2. **运行时检测**：在运行时检测 IDE 是否支持这些功能
3. **优雅降级**：如果不支持，则禁用相关功能，不影响核心功能

```xml
<!-- plugin.xml -->
<depends optional="true">com.intellij.modules.lsp</depends>
<depends optional="true">com.intellij.modules.mcp</depends>
```

```java
// 运行时检测
public class LSPCapabilityChecker {
    public static boolean isLSPAvailable() {
        try {
            Class.forName("com.intellij.plugins.lsp.api.LSPClient");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isMCPAvailable() {
        try {
            Class.forName("com.intellij.mcp.MCPServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
```

#### 2.3.2 功能开关

在 Engine 的设置页面中添加功能开关，让用户控制是否启用 LSP 和 MCP 功能：

```java
public class EngineSettings {
    public boolean enableLSP = false;  // 默认关闭，需要用户明确启用
    public boolean enableMCPServer = false;
    public boolean enableMCPClient = false;
}
```

## 三、高价值功能特性分析

### 3.1 通过 LSP 实现的功能

#### 3.1.1 增强的代码理解能力

**功能描述**：

- 通过 LSP 获取代码的语义信息（符号定义、类型信息、调用关系等）
- 将这些信息作为上下文传递给 AI，提升生成质量

**实现方式**：

```java
public class EnhancedAIContextBuilder {
    private LSPClient lspClient;

    public String buildContextWithLSP(Project project, PsiFile file) {
        // 1. 获取当前符号的定义
        Location definition = lspClient.getDefinition(file, cursorPosition);

        // 2. 获取符号的引用
        List<Location> references = lspClient.getReferences(file, cursorPosition);

        // 3. 获取类型层次结构
        TypeHierarchy hierarchy = lspClient.getTypeHierarchy(file, cursorPosition);

        // 4. 构建增强的上下文
        StringBuilder context = new StringBuilder();
        context.append("Definition: ").append(definition).append("\n");
        context.append("References: ").append(references.size()).append("\n");
        context.append("Type Hierarchy: ").append(hierarchy).append("\n");

        return context.toString();
    }
}
```

**价值**：

- ✅ 更准确的代码理解，提升 AI 生成质量
- ✅ 支持多语言（通过不同的语言服务器）
- ✅ 减少对 PSI API 的依赖，降低实现复杂度

#### 3.1.2 跨语言代码补全

**功能描述**：

- 通过 LSP 支持多种编程语言的代码补全
- 结合 AI 生成更智能的补全建议

**实现方式**：

```java
public class AILSPCompletionProvider {
    public List<CompletionItem> getCompletions(
        Project project,
        PsiFile file,
        int offset
    ) {
        // 1. 从 LSP 获取基础补全
        List<CompletionItem> lspCompletions = lspClient.getCompletions(file, offset);

        // 2. 使用 AI 生成智能补全
        String context = getCodeContext(file, offset);
        String aiSuggestion = aiService.generateCompletion(context);

        // 3. 合并结果
        return mergeCompletions(lspCompletions, aiSuggestion);
    }
}
```

**价值**：

- ✅ 支持更多编程语言
- ✅ 结合 AI 的智能补全，超越传统补全能力
- ✅ 统一的补全接口，简化实现

#### 3.1.3 代码诊断与修复建议

**功能描述**：

- 通过 LSP 获取代码诊断信息（错误、警告、提示）
- 使用 AI 生成修复建议

**实现方式**：

```java
public class AIDiagnosticProvider {
    public List<Diagnostic> getDiagnosticsWithAIFix(PsiFile file) {
        // 1. 从 LSP 获取诊断
        List<Diagnostic> diagnostics = lspClient.getDiagnostics(file);

        // 2. 对每个诊断，使用 AI 生成修复建议
        for (Diagnostic diagnostic : diagnostics) {
            String fixSuggestion = aiService.generateFix(
                diagnostic.getMessage(),
                getCodeAroundDiagnostic(file, diagnostic)
            );
            diagnostic.setCodeAction(new CodeAction(fixSuggestion));
        }

        return diagnostics;
    }
}
```

**价值**：

- ✅ 更智能的错误修复建议
- ✅ 支持多语言的诊断
- ✅ 统一的诊断接口

### 3.2 通过 MCP Server 实现的功能

#### 3.2.1 暴露 AI 能力给外部工具

**功能描述**：

- 将 Engine 的 AI 能力（Javadoc 生成、Changelog 生成等）暴露为 MCP Tools
- 外部 AI 工具（Claude、Cursor 等）可以调用这些工具

**实现方式**：

```java
@MCPServerTool(
    name = "intelliai_generate_javadoc",
    description = "Generate Javadoc comments for Java/Kotlin code using AI"
)
public class MCPJavadocTool {
    public String execute(
        @MCPParameter(name = "code", description = "Code to document") String code,
        @MCPParameter(name = "language", description = "Programming language") String language
    ) {
        AIService aiService = AIService.getInstance();
        AIProviderConfig config = getDefaultConfig();
        AIChatRequest request = new AIChatRequest(
            "Generate Javadoc for the following " + language + " code",
            code
        );
        return aiService.generateContent(project, request, config, null);
    }
}

@MCPServerTool(
    name = "intelliai_generate_changelog",
    description = "Generate changelog from git commits using AI"
)
public class MCPChangelogTool {
    public String execute(
        @MCPParameter(name = "git_range", description = "Git commit range") String gitRange
    ) {
        // 生成 changelog 逻辑
    }
}

@MCPServerTool(
    name = "intelliai_get_project_info",
    description = "Get project information (version, dependencies, etc.)"
)
public class MCPProjectInfoTool {
    public Map<String, Object> execute() {
        Map<String, Object> info = new HashMap<>();
        info.put("version", ProjectVersionResolver.resolveVersion(project));
        info.put("type", detectProjectType(project));
        info.put("dependencies", getDependencies(project));
        return info;
    }
}
```

**价值**：

- ✅ 外部 AI 工具可以直接调用 Engine 的能力
- ✅ 实现跨工具的 AI 工作流
- ✅ 提升 Engine 的可见度和使用场景

#### 3.2.2 项目上下文获取工具

**功能描述**：

- 提供工具让外部 AI 获取项目上下文信息
- 包括项目结构、依赖、配置等

**实现方式**：

```java
@MCPServerTool(
    name = "intelliai_get_project_structure",
    description = "Get project module structure"
)
public class MCPProjectStructureTool {
    public List<Map<String, Object>> execute() {
        List<Map<String, Object>> modules = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            Map<String, Object> moduleInfo = new HashMap<>();
            moduleInfo.put("name", module.getName());
            moduleInfo.put("path", module.getModuleFilePath());
            modules.add(moduleInfo);
        }
        return modules;
    }
}

@MCPServerTool(
    name = "intelliai_get_dependencies",
    description = "Get project dependencies (Maven/Gradle)"
)
public class MCPDependenciesTool {
    public List<Map<String, String>> execute() {
        // 从 pom.xml 或 build.gradle 读取依赖
    }
}
```

**价值**：

- ✅ 外部 AI 工具可以更好地理解项目结构
- ✅ 支持更智能的代码生成和建议
- ✅ 统一的上下文获取接口

### 3.3 通过 MCP Client 实现的功能

#### 3.3.1 调用外部 MCP Servers

**功能描述**：

- Engine 可以连接外部 MCP Servers，调用外部工具
- 扩展 Engine 的能力边界

**实现方式**：

```java
public class ExternalMCPIntegration {
    private MCPClient client;

    public void connectToExternalServer(String serverUrl) {
        client = MCPClient.connect(serverUrl);
    }

    public String enhanceContextWithExternalTool(String context) {
        // 调用外部 MCP Server 的工具
        return client.callTool("enhance_context", Map.of("context", context));
    }
}
```

**价值**：

- ✅ 扩展 Engine 的能力，不限于 IDE 内部
- ✅ 可以集成第三方工具和服务
- ✅ 实现更复杂的 AI 工作流

## 四、实施建议

### 4.1 实施优先级

#### 阶段一：基础能力（高优先级）

1. **MCP Server 基础能力**
    - 实现 `get_project_version` 工具
    - 实现 `get_project_info` 工具
    - 在设置页面添加 MCP Server 开关

2. **版本兼容处理**
    - 添加运行时检测机制
    - 实现优雅降级
    - 更新文档说明版本要求

#### 阶段二：AI 能力暴露（中优先级）

1. **暴露核心 AI 工具**
    - `intelliai_generate_javadoc`
    - `intelliai_generate_changelog`
    - `intelliai_analyze_code`

2. **项目上下文工具**
    - `intelliai_get_project_structure`
    - `intelliai_get_dependencies`
    - `intelliai_get_code_context`

#### 阶段三：LSP 集成（低优先级）

1. **LSP Client 基础能力**
    - 连接语言服务器
    - 获取代码诊断信息
    - 获取代码补全建议

2. **AI + LSP 增强**
    - 使用 LSP 信息增强 AI 上下文
    - AI 驱动的代码补全
    - AI 驱动的错误修复

### 4.2 技术架构设计

#### 4.2.1 模块划分

```
intelli-ai-engine/
├── src/main/java/
│   ├── ... (现有代码)
│   ├── lsp/                          # LSP 集成模块
│   │   ├── LSPClientManager.java     # LSP 客户端管理
│   │   ├── LSPCodeAnalysisService.java  # 代码分析服务
│   │   └── AILSPCompletionProvider.java # AI 补全提供者
│   ├── mcp/                          # MCP 集成模块
│   │   ├── server/                   # MCP Server
│   │   │   ├── MCPServerManager.java
│   │   │   ├── tools/                # MCP Tools
│   │   │   │   ├── JavadocTool.java
│   │   │   │   ├── ChangelogTool.java
│   │   │   │   └── ProjectInfoTool.java
│   │   └── client/                   # MCP Client
│   │       └── MCPClientManager.java
│   └── capability/                   # 能力检测模块
│       ├── CapabilityChecker.java    # 检测 LSP/MCP 可用性
│       └── FeatureToggle.java        # 功能开关
```

#### 4.2.2 配置管理

在 `AIProviderSettings` 或新增 `EngineSettings` 中添加配置：

```java
public class EngineSettings implements PersistentStateComponent<EngineSettings> {
    // LSP 配置
    public boolean enableLSP = false;
    public String lspServerPath = "";
    public List<String> enabledLanguages = new ArrayList<>();

    // MCP Server 配置
    public boolean enableMCPServer = false;
    public int mcpServerPort = 8080;
    public List<String> exposedTools = new ArrayList<>();

    // MCP Client 配置
    public boolean enableMCPClient = false;
    public List<String> externalMCPUrls = new ArrayList<>();
}
```

### 4.3 风险与缓解措施

#### 4.3.1 版本兼容风险

**风险**：LSP 和 MCP 需要较新的 IDE 版本，可能影响现有用户

**缓解措施**：

- 使用可选依赖，确保在不支持的版本上也能正常运行
- 提供清晰的版本要求说明
- 实现优雅降级，核心功能不受影响

#### 4.3.2 安全性风险

**风险**：MCP Server 暴露工具给外部，可能存在安全风险

**缓解措施**：

- 默认关闭 MCP Server，需要用户明确启用
- 提供权限控制机制
- 限制可执行的操作范围
- 添加安全警告和确认对话框

#### 4.3.3 性能风险

**风险**：LSP 和 MCP 可能带来性能开销

**缓解措施**：

- 异步处理所有 LSP/MCP 调用
- 实现缓存机制
- 提供性能监控和日志

## 五、总结

### 5.1 可行性结论

**LSP 集成**：✅ **可行**

- 技术成熟，JetBrains 官方支持
- 需要提升最低版本要求到 2025.2.1
- 可以显著增强代码理解能力

**MCP 集成**：✅ **高度可行**

- IDE 内置支持，无需额外依赖
- 可以暴露 Engine 的能力给外部工具
- 需要提升最低版本要求到 2025.1+

### 5.2 高价值功能特性

1. **通过 LSP**：
    - 增强的代码理解能力
    - 跨语言代码补全
    - 智能代码诊断与修复

2. **通过 MCP Server**：
    - 暴露 AI 能力给外部工具（Claude、Cursor 等）
    - 项目上下文获取工具
    - 实现跨工具的 AI 工作流

3. **通过 MCP Client**：
    - 调用外部 MCP Servers
    - 扩展 Engine 的能力边界

### 5.3 建议

1. **优先实施 MCP Server**：价值最高，可以立即让外部 AI 工具使用 Engine 的能力
2. **谨慎处理版本兼容**：使用可选依赖和优雅降级，确保不影响现有用户
3. **分阶段实施**：先实现基础能力，再逐步增强功能
4. **完善文档**：提供详细的使用文档和示例

### 5.4 预期收益

- ✅ **提升 Engine 的可见度**：通过 MCP Server 暴露能力，让更多工具可以使用
- ✅ **增强 AI 能力**：通过 LSP 获取更准确的代码语义信息
- ✅ **扩展使用场景**：支持跨工具的 AI 工作流
- ✅ **提升用户体验**：更智能的代码生成和建议

---

**文档版本**：v1.0
**创建日期**：2025-01-XX
**作者**：IntelliAI Engine Team
