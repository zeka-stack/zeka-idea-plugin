# IntelliJ IDEA 插件模板（带 AI 能力）

这是一个预配置的 IntelliJ IDEA 插件开发模板，已集成 IntelliAI Engine，适用于需要 AI 功能的插件开发。

## 🎯 适用场景

- 需要集成 OpenAI、通义千问、Ollama 等 AI 服务的插件
- 需要调用 AI API 进行代码生成、分析、翻译等功能
- 需要复用 IntelliAI Engine 提供的 UI 组件和工具类

## ✨ 核心特性

### 🤖 AI 能力集成

- **IntelliAI Engine 依赖**：已配置编译时依赖和运行时依赖
- **自动构建任务**：`buildAiCommonPlugin` 和 `copyAiCommonPlugin` 自动处理 Engine 插件的构建和安装
- **本地开发支持**：开发时自动将 Engine 插件复制到沙盒环境
- **生产发布支持**：发布时通过 Marketplace 依赖声明，用户需单独安装 Engine 插件

### 🛠️ 开发环境

- **Gradle Kotlin DSL**：使用现代化的 Kotlin DSL 构建脚本
- **热更新支持**：配置了 `-XX:AllowEnhancedClassRedefinition` JVM 参数
- **测试框架**：预配置 JUnit 5、Mockito、AssertJ
- **代码规范**：集成 Lombok 支持

### 📦 预配置内容

- **示例 Action**：`ExampleAction` 展示如何创建插件动作
- **设置页面**：`ExampleSettingsConfigurable` 和 `ExampleSettingsPanel` 展示设置页面实现
- **国际化支持**：`ExampleBundle` 展示国际化资源使用
- **通知工具**：`NotificationUtil` 提供便捷的通知显示方法
- **图标资源**：示例 SVG 图标文件

## 🚀 快速开始

### 1. 复制模板

```bash
cp -r template-with-ai my-new-ai-plugin
cd my-new-ai-plugin
```

### 2. 修改插件信息

编辑 `gradle.properties` 文件，修改以下配置：

```properties
# 插件基本信息
pluginGroup=dev.dong4j.zeka.stack.idea.plugin.example
pluginName=My New AI Plugin
pluginVersion=1.0.0

# IntelliAI Engine 版本（统一管理，避免重复）
aiEngineVersion=1.7.0

# 项目名称
rootProjectName=my-new-ai-plugin

# IntelliJ Platform 配置
platformType=IC
platformVersion=2025.2
platformSinceBuild=242.2
platformUntilBuild=999.*
```

### 3. 修改包名和类名

1. **重命名包结构**：将 `dev.dong4j.zeka.stack.idea.plugin.example` 替换为你的包名
2. **重命名类名**：将所有 `Example*` 类重命名为你的类名
3. **更新 plugin.xml**：修改 `plugin.xml` 中的类引用和插件 ID

### 4. 开始开发

```bash
# 运行插件（会自动构建和安装 Engine 插件）
./gradlew runIde
```

## 📚 详细说明

### IntelliAI Engine 集成

#### 依赖配置

在 `build.gradle.kts` 中：

```kotlin
dependencies {
    // 编译时依赖
    compileOnly("dev.dong4j:intelli-ai-engine:$aiEngineVersion")

    // 运行时依赖通过 plugin.xml 中的 <depends> 声明
}
```

在 `plugin.xml` 中：

```xml
<depends>dev.dong4j.zeka.stack.idea.plugin.common.ai</depends>
```

#### 本地开发流程

1. `./gradlew runIde` 触发：
    - `buildAiCommonPlugin` → 构建 `intelli-ai-engine` 插件
    - `copyAiCommonPlugin` → 复制到沙盒环境
    - `runIde` → 启动带完整依赖的 IDE

#### 生产发布流程

1. 确保 `plugin.xml` 中已声明 `<depends>`
2. 执行发布流程
3. 用户在 Marketplace 安装时，系统会自动提示安装 IntelliAI Engine 依赖

### 使用 AI 服务

参考 IntelliAI Engine 的文档，使用以下方式调用 AI 服务：

```java
// 获取 AI 服务实例
AIService aiService = AIServiceManager.getInstance().getService();

// 调用 AI API
aiService.chat(/* ... */);
```

### 项目结构

```
template-with-ai/
├── build.gradle.kts              # Gradle 构建脚本
├── gradle.properties             # 插件配置
├── settings.gradle.kts            # Gradle 设置
├── src/
│   ├── main/
│   │   ├── java/                 # Java 源代码
│   │   │   └── dev/dong4j/zeka/stack/idea/plugin/example/
│   │   │       ├── action/       # 动作类
│   │   │       ├── icons/        # 图标常量
│   │   │       ├── settings/     # 设置相关
│   │   │       └── util/         # 工具类
│   │   └── resources/
│   │       ├── icons/            # 图标资源
│   │       ├── messages/         # 国际化资源
│   │       └── META-INF/
│   │           └── plugin.xml    # 插件配置
│   └── test/                     # 测试代码
├── includes/
│   ├── pluginDescription.html    # 插件描述
│   └── pluginChanges.html        # 更新日志
└── docs/                         # 文档
```

## 🔧 配置说明

### gradle.properties

| 配置项                  | 说明                  | 示例                                          |
|----------------------|---------------------|---------------------------------------------|
| `pluginGroup`        | 插件组 ID              | `dev.dong4j.zeka.stack.idea.plugin.example` |
| `pluginName`         | 插件显示名称              | `My New AI Plugin`                          |
| `pluginVersion`      | 插件版本号               | `1.0.0`                                     |
| `aiEngineVersion`    | IntelliAI Engine 版本 | `1.7.0`                                     |
| `rootProjectName`    | 项目根目录名称             | `my-new-ai-plugin`                          |
| `platformType`       | IntelliJ 平台类型       | `IC` (Community) 或 `IU` (Ultimate)          |
| `platformVersion`    | IntelliJ 版本         | `2025.2`                                    |
| `platformSinceBuild` | 最低支持版本              | `242.2`                                     |
| `platformUntilBuild` | 最高支持版本              | `999.*`                                     |

### plugin.xml

关键配置项：

- `<id>`：插件唯一标识符
- `<name>`：插件显示名称
- `<depends>`：依赖 IntelliAI Engine
- `<extensions>`：注册到 Engine 的扩展点（如需要）

## 📖 更多资源

- [主项目 README](../../README.md) - 完整的插件开发指南
- [IntelliAI Engine 集成详解](../../README.md#-ai-插件-engine-依赖集成详解) - AI 插件开发详细说明
- [IntelliJ Platform SDK 文档](https://plugins.jetbrains.com/docs/intellij/welcome.html) - 官方开发文档

## ⚠️ 注意事项

1. **Engine 版本管理**：`aiEngineVersion` 在 `gradle.properties` 中统一管理，避免版本不一致
2. **本地开发路径**：`buildAiCommonPlugin` 任务假设 `intelli-ai-engine` 位于 `../intelli-ai-engine`，如路径不同需修改
3. **发布前检查**：确保 `plugin.xml` 中已正确声明 Engine 依赖
4. **线程安全**：遵循 IntelliJ Platform 的线程模型，UI 操作在 EDT，耗时操作在 BGT

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

