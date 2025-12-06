# IntelliJ IntelliAI JavaDoc 插件

**中文** | [English](./README.md)

> 🌟 [查看精美的推广页面](https://aij.dong4j.site) - 更直观地了解插件功能！

IntelliJ IntelliAI JavaDoc 插件是一个功能强大的工具，利用人工智能为您的 Java 代码生成 JavaDoc 注释。它支持多种 AI 服务提供商，包括通义千问和
Ollama
本地模型，提供智能且上下文感知的文档建议，适用于方法、类和接口。

## 功能特性

- **多 AI 提供商支持**：支持通义千问（阿里云）和 Ollama 本地模型，满足不同用户需求
- **自动生成 JavaDoc**：让插件为您生成 JavaDoc 注释，简化文档编写过程
- **智能元素识别**：根据光标位置智能识别要生成文档的元素（方法、字段、类）
- **上下文感知建议**：插件会分析您的代码并考虑周围的上下文，以提供更精确和有意义的 JavaDoc 建议
- **多种触发方式**：支持快捷键、右键菜单、Generate 菜单和 Intention Action
- **批量处理**：支持为整个文件或选中的多个文件批量生成文档
- **增量更新**：只为没有 JavaDoc 注释的类和方法生成文档，不会覆盖已有的注释
- **测试方法识别**：自动识别 JUnit 4 和 JUnit 5 的测试方法，使用专门的模板生成文档
- **配置验证**：内置连接测试功能，确保配置正确

## 安装

1. 启动 IntelliJ IDEA。
2. 进入 **Settings**（macOS 上为 Preferences）→ **Plugins**。
3. 点击 **Marketplace** 标签。
4. 在搜索栏中搜索"AIJavadoc"。
5. 点击 IntelliJ IntelliAI JavaDoc 插件旁边的 **Install**。
6. 重启 IntelliJ IDEA 以激活插件。

## 使用方法

插件提供多种触发方式，您可以根据需要选择：

### 快捷键方式（推荐）

1. 在 IntelliJ IDEA 中打开您的 Java 项目
2. 将光标放在要生成文档的元素上（方法、字段、类）
3. 按 **Ctrl + Shift + D**（Windows/Linux）或 **Cmd + Shift + D**（macOS）
4. 插件会根据光标位置智能识别并生成相应的文档

### 其他触发方式

- **右键菜单**：在编辑器中右键 → "Generate Javadoc with AI"
- **Generate 菜单**：按 **Alt + Insert**（Windows/Linux）或 **Cmd + N**（macOS）→ 选择 "Generate Javadoc with AI"
- **Intention Action**：按 **Alt + Enter**（Windows/Linux）或 **Option + Enter**（macOS）→ 选择 "Generate Javadoc with AI"

### 智能定位

插件会根据光标位置智能决定生成范围：

- 光标在方法上 → 只为该方法生成文档
- 光标在字段上 → 只为该字段生成文档
- 光标在类声明上 → 只为该类生成文档
- 光标在类内部（但不在特定成员上）→ 为整个类及所有成员生成文档
- 其他情况 → 为整个文件生成文档

## 配置

插件支持多种 AI 服务提供商，您可以根据需要选择：

### 通义千问（推荐）

通义千问是阿里云提供的大语言模型服务，支持中文和多语言处理，适合中文文档生成。

**配置步骤：**

1. 访问 [阿里云 DashScope 控制台](https://dashscope.console.aliyun.com/) 注册账户
2. 获取 API Key
3. 在 IntelliJ IDEA 中进入 **Settings** → **Tools** → **IntelliAI JavaDoc**
4. 选择 **AI Provider** 为 "qianwen"
5. 输入您的 **API Key**
6. **Base URL** 使用默认值：`https://dashscope.aliyuncs.com/compatible-mode/v1`
7. **Model** 选择推荐模型：
    - `qwen-max`：最强大的模型（推荐）
    - `qwen-plus`：性价比较高
    - `qwen-turbo`：速度最快
8. 点击 **Test Connection** 验证配置
9. 点击 **Apply** 保存配置

### Ollama（本地模型）

Ollama 支持在本地运行开源大语言模型，数据完全私有，无需 API Key。

**配置步骤：**

1. 安装 Ollama：访问 [https://ollama.ai](https://ollama.ai) 下载安装
2. 拉取模型：`ollama pull qwen:7b`（推荐用于中文）
3. 启动服务：`ollama serve`
4. 在插件设置中选择 **AI Provider** 为 "ollama"
5. **Base URL** 使用默认值：`http://localhost:11434/v1`
6. **API Key** 留空（本地服务不需要）
7. **Model** 输入已安装的模型名称，如：`qwen:7b`
8. 点击 **Test Connection** 验证配置
9. 点击 **Apply** 保存配置

**推荐模型：**

- `qwen:7b` / `qwen:14b`：通义千问系列，中文支持好
- `codellama:7b`：代码专用模型
- `deepseek-coder:6.7b`：深度求索代码模型
- `llama2:7b`：通用模型

### 自定义服务（OpenAI 兼容）

支持任何兼容 OpenAI API 接口的自定义服务，包括 OpenAI 官方服务、Azure OpenAI、第三方服务等。

**配置步骤：**

1. 在插件设置中选择 **AI Provider** 为 "custom"
2. **Base URL** 输入服务地址，例如：
    - OpenAI：`https://api.openai.com/v1`
    - Azure OpenAI：`https://your-resource.openai.azure.com/openai/deployments/your-deployment`
    - 自部署服务：`http://localhost:8000/v1`
3. **API Key** 输入服务的 API 密钥
4. **Model** 输入模型名称，例如：
    - `gpt-3.5-turbo`：OpenAI 的快速模型
    - `gpt-4`：OpenAI 的高质量模型
    - `gpt-4-turbo`：OpenAI 的最新模型
    - `claude-3-sonnet`：Anthropic Claude 模型
    - `gemini-pro`：Google Gemini 模型
5. 点击 **Test Connection** 验证配置
6. 点击 **Apply** 保存配置

**支持的常见服务：**

- **OpenAI**：官方 OpenAI 服务
- **Azure OpenAI**：微软 Azure 上的 OpenAI 服务
- **Anthropic Claude**：通过兼容层使用 Claude 模型
- **Google Gemini**：通过兼容层使用 Gemini 模型
- **自部署服务**：任何兼容 OpenAI API 的自部署服务

**注意事项：**

- 确保服务支持 OpenAI 兼容的 `/chat/completions` 接口
- API Key 格式通常为 "Bearer {key}" 或直接使用 key
- 模型名称必须与服务提供商支持的模型一致
- 某些服务可能有速率限制或使用配额

### 高级配置

在设置面板中，您还可以配置：

- **生成选项**：选择为类、方法、字段生成文档
- **跳过已有文档**：避免覆盖现有 JavaDoc
- **语言支持**：目前支持 Java（Kotlin 即将支持）
- **高级参数**：重试次数、超时时间、温度参数等
- **Prompt 模板**：自定义文档生成模板

## 限制

- 插件依赖于所选 AI 服务提供商的可用性和性能。网络连接和 API 速率限制可能会影响其功能
- 生成的 JavaDoc 注释的准确性和质量取决于底层 AI 模型的能力和限制
- 使用 Ollama 本地模型需要足够的系统资源（内存、CPU）来运行模型
- 通义千问服务需要有效的 API Key 和网络连接

## 贡献

欢迎为 IntelliJ IntelliAI JavaDoc 插件做出贡献！如果您遇到任何错误、有功能请求或想要贡献代码改进，请在 GitHub 仓库上提交问题或拉取请求。

## 作者

dong4j dong4j@gmail.com

---

## 相关文档

- [IDEA Plugin 开发文档](docs/IDEA_Plugin_开发文档.md) - IntelliJ IDEA 插件开发完整指南
- [English README](README.md) - English version of this document

## 快速开始

### 开发环境设置

1. 克隆项目：
   ```bash
   git clone https://github.com/zeka-stack/zeka-idea-plugin.git
   cd zeka-idea-plugin/intelli-ai-javadoc
   ```

2. 使用 Gradle 构建项目：
   ```bash
   ./gradlew build
   ```

3. 运行插件（启动带插件的 IDE）：
   ```bash
   ./gradlew runIde
   ```

4. 运行测试：
   ```bash
   ./gradlew test
   ```

### 项目结构

```
intelli-ai-javadoc/
├── src/main/java/com/github/intellijjavadocai/
│   ├── action/          # 动作处理（用户交互入口）
│   ├── config/          # 配置管理（API 配置）
│   ├── generator/       # JavaDoc 生成器
│   ├── service/         # 服务层（HTTP 请求、Prompt 管理）
│   └── ErrorHandler.java  # 错误处理
├── src/main/resources/
│   ├── META-INF/
│   │   └── plugin.xml   # 插件配置文件
│   ├── openai/
│   │   └── config.properties  # OpenAI 配置
│   ├── method-prompt-template.txt   # 普通方法的提示模板
│   └── test-prompt-template.txt     # 测试方法的提示模板
└── build.gradle.kts     # Gradle 构建脚本
```

## 技术栈

- **Java 11** - 开发语言
- **IntelliJ Platform SDK** - 插件开发框架
- **Spring Web** - HTTP 客户端
- **通义千问 API** - 阿里云 AI 文档生成服务
- **Ollama** - 本地大语言模型运行环境
- **Gradle** - 构建工具
- **Lombok** - 简化 Java 代码
- **SLF4J** - 日志框架

## 核心功能

### 1. 智能识别测试方法

插件能够自动识别 JUnit 4 和 JUnit 5 的测试方法，并使用专门的模板生成更符合测试方法特点的文档。

### 2. 增量更新

只为没有 JavaDoc 注释的类和方法生成文档，不会覆盖已有的注释。

### 3. 错误重试机制

采用指数退避策略处理 API 限流和临时故障，确保稳定性。

### 4. 完整的 IDE 集成

- 快捷键支持
- 仅在 Java 文件中可用
- 索引期间自动禁用

## 常见问题

### Q: 为什么插件没有反应？

A: 请检查以下几点：

1. 确保已正确配置 AI 提供商（通义千问或 Ollama）
2. 确认当前文件是 Java 文件
3. 检查 IDE 是否正在索引（状态栏显示索引进度）
4. 查看 IDE 日志中的错误信息
5. 确保配置已通过连接测试

### Q: 如何查看 API 调用失败的原因？

A: 查看 IDE 日志文件：

- **macOS**: `~/Library/Logs/JetBrains/IntelliJIdea<Version>/idea.log`
- **Windows**: `%USERPROFILE%\AppData\Local\JetBrains\IntelliJIdea<Version>\log\idea.log`
- **Linux**: `~/.cache/JetBrains/IntelliJIdea<Version>/log/idea.log`

### Q: 如何自定义 Prompt 模板？

A: 在设置面板的 "Prompt Template Configuration" 部分可以自定义：

- Class Prompt：类文档模板
- Method Prompt：方法文档模板
- Field Prompt：字段文档模板
- Test Method Prompt：测试方法文档模板
  使用 `%s` 作为代码占位符

### Q: 支持哪些测试框架？

A: 目前支持：

- JUnit 4 (`@org.junit.Test`)
- JUnit 5 (`@org.junit.jupiter.api.Test`)

### Q: 如何选择 AI 提供商？

A: 根据您的需求选择：

- **通义千问**：适合中文文档生成，需要 API Key，有使用成本
- **Ollama**：本地运行，数据私有，无需 API Key，需要足够的系统资源

### 外部资源

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliAI Engine](https://plugins.jetbrains.com/plugin/29152) - AI 能力基础引擎
- [Git 文档](https://git-scm.com/doc) - Git 版本控制文档

## 📄 许可证

本项目基于 MIT 开源许可证。详见 [LICENSE](https://github.com/zeka-stack/zeka-stack/blob/main/LICENSE) 文件。

## 支持

如果您遇到问题或有任何疑问，请：

1. 查看[IDEA Plugin 开发文档](docs/IDEA_Plugin_开发文档.md)
2. 在 [GitHub Issues](https://github.com/zeka-stack/zeka-idea-plugin/issues) 上提交问题
3. 发送邮件至 dong4j@gmail.com

## 致谢

感谢以下服务提供商使这个项目成为可能：

- **阿里云通义千问** - 提供强大的中文 AI 服务
- **Ollama** - 提供本地大语言模型运行环境

---

**JetBrains Marketplace**: [IntelliAI Tracer](https://plugins.jetbrains.com/plugin/28835)

**问题反馈**: [GitHub Issues](https://github.com/zeka-stack/zeka-idea-plugin/issues)

**联系方式**: dong4jj@gmail.com