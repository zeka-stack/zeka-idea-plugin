# IntelliJ IDEA 插件模板（带 AI 能力）

这是一个预配置的 IntelliJ IDEA 插件开发模板，已集成 IntelliAI Engine，适用于需要 AI 功能的插件开发。

## 目录

- [1 🎯 适用场景](#1-🎯-适用场景)
- [2 ✨ 核心特性](#2-✨-核心特性)
- [3 🚀 快速开始](#3-🚀-快速开始)
- [4 📚 详细说明](#4-📚-详细说明)
- [5 📁 项目结构](#5-📁-项目结构)
- [6 📖 文档](#6-📖-文档)
- [7 ⚠️ 注意事项](#7-⚠️-注意事项)

## 1 🎯 适用场景

- 需要集成 OpenAI、通义千问、Ollama 等 AI 服务的插件
- 需要调用 AI API 进行代码生成、分析、翻译等功能
- 需要复用 IntelliAI Engine 提供的 UI 组件和工具类

## 2 ✨ 核心特性

### 2.1 🤖 AI 能力集成

- **IntelliAI Engine 依赖**：已配置编译时依赖和运行时依赖
- **自动构建任务**：`buildAiCommonPlugin` 和 `copyAiCommonPlugin` 自动处理 Engine 插件的构建和安装
- **本地开发支持**：开发时自动将 Engine 插件复制到沙盒环境
- **生产发布支持**：发布时通过 Marketplace 依赖声明，用户需单独安装 Engine 插件

### 2.2 🧩 通用能力示例

- **AI 服务商选择**：使用 `AIProviderSelectionPanel` 复用 Engine 下拉选择组件
- **状态栏集成**：通过 Engine 的状态栏扩展点注册快捷设置
- **AI 控制台日志**：统一输出到 Engine Console
- **AI 接口调用**：示例 Action 内置最小非流式调用
- **更新检查入口**：集成 `PluginUpdateInfoProvider`，统一插件更新信息
- **反馈面板**：集成 `FeedbackPanel`，用于收集用户反馈

### 2.3 🛠️ 开发环境

- **Gradle Kotlin DSL**：使用现代化的 Kotlin DSL 构建脚本
- **热更新支持**：配置了 `-XX:AllowEnhancedClassRedefinition` JVM 参数
- **测试框架**：预配置 JUnit 5、Mockito、AssertJ
- **代码规范**：集成 Lombok 支持

## 3 🚀 快速开始

### 3.1 复制模板

```bash
cp -r template-with-ai my-new-ai-plugin
cd my-new-ai-plugin
```

### 3.2 修改插件信息

编辑 `gradle.properties` 文件，修改以下配置：

```properties
# 插件基本信息
pluginGroup=dev.dong4j.zeka.stack
pluginName=My New AI Plugin
pluginVersion=2026.1.1000
kitVersion=2026.1.1000
# IntelliAI Engine 版本（统一管理，避免重复）
engineVersion=2026.1.1000

# 项目名称
rootProjectName=my-new-ai-plugin

# IntelliJ Platform 配置
platformType=IC
platformVersion=2025.2
platformSinceBuild=242.2
platformUntilBuild=261.*
```

### 3.3 修改包名和类名

1. **重命名包结构**：将 `dev.dong4j.zeka.stack.idea.plugin.example` 替换为你的包名
2. **重命名类名**：将所有 `Example*` 类重命名为你的类名
3. **更新 plugin.xml**：修改 `plugin.xml` 中的类引用和插件 ID

### 3.4 开始开发

```bash
# 运行插件（会自动构建和安装 Engine 插件）
./gradlew runIde
```

## 4 📚 详细说明

### 4.1 IntelliAI Engine 集成

#### 4.1.1 依赖配置

在 `build.gradle.kts` 中：

```kotlin
dependencies {
    // 编译时依赖
    compileOnly("dev.dong4j.zeka.stack:intelli-ai-engine:$engineVersion")

    // Idea Plugin Common 库依赖（本地库，打包时需要包含）
    implementation("dev.dong4j.zeka.stack:idea-plugin-kit:$kitVersion")

    // 运行时依赖通过 plugin.xml 中的 <depends> 声明
}
```

在 `plugin.xml` 中：

```xml
<depends>dev.dong4j.zeka.stack.idea.plugin.common.ai</depends>
```

#### 4.1.2 本地开发流程

1. `./gradlew runIde` 触发：
    - `buildAiCommonPlugin` → 构建 `intelli-ai-engine` 插件
    - `copyAiCommonPlugin` → 复制到沙盒环境
    - `runIde` → 启动带完整依赖的 IDE

#### 4.1.3 生产发布流程

1. 确保 `plugin.xml` 中已声明 `<depends>`
2. 执行发布流程
3. 用户在 Marketplace 安装时，系统会自动提示安装 IntelliAI Engine 依赖

### 4.2 使用 AI 服务

```java
AIService aiService = ApplicationManager.getApplication().getService(AIService.class);
AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);
String result = aiService.generateContent(project, request, providerConfig, null);
```

## 5 📁 项目结构

```
template-with-ai/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/dong4j/zeka/stack/idea/plugin/example/
│   │   │       ├── action/
│   │   │       ├── PluginContents.java
│   │   │       ├── settings/
│   │   │       ├── statusbar/
│   │   │       ├── icons/
│   │   │       └── util/
│   │   └── resources/
│   │       ├── dev/dong4j/zeka/stack/idea/plugin/example/icons/
│   │       ├── messages/
│   │       └── META-INF/
│   └── test/
├── includes/
└── site/
    └── docs/
```

## 6 📖 文档

- `site/docs/用户手册.md`
- `site/docs/插件开发指南.md`

## 7 ⚠️ 注意事项

1. **Engine 版本管理**：`engineVersion` 在 `gradle.properties` 中统一管理，避免版本不一致
2. **本地开发路径**：`buildAiCommonPlugin` 任务假设 `intelli-ai-engine` 位于 `../intelli-ai-engine`
3. **发布前检查**：确保 `plugin.xml` 中已正确声明 Engine 依赖
4. **线程安全**：遵循 IntelliJ Platform 的线程模型，UI 操作在 EDT，耗时操作在 BGT
5. **统一常量**：插件 ID/Name 统一维护在 `PluginContents`，避免散落引用
