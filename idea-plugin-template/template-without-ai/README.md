# IntelliJ IDEA 插件模板（不带 AI 能力）

这是一个轻量级的 IntelliJ IDEA 插件开发模板，适用于纯工具类插件开发，无需 AI 集成。

## 🎯 适用场景

- 代码格式化、代码检查等工具类插件
- 文件操作、项目管理等实用工具
- 框架开发助手、代码生成器等
- 任何不需要 AI 能力的插件

## ✨ 核心特性

### ⚡ 轻量级设计

- **无额外依赖**：不依赖 IntelliAI Engine，插件体积小
- **快速启动**：开发环境启动速度快
- **简单配置**：配置项少，易于理解

### 🛠️ 开发环境

- **Gradle Kotlin DSL**：使用现代化的 Kotlin DSL 构建脚本
- **热更新支持**：配置了 `-XX:AllowEnhancedClassRedefinition` JVM 参数
- **测试框架**：预配置 JUnit 5、Mockito、AssertJ
- **代码规范**：集成 Lombok 支持

### 📦 预配置内容

- **示例 Action**：`ExampleAction` 展示如何创建插件动作
- **国际化支持**：`ExampleBundle` 展示国际化资源使用
- **通知工具**：`NotificationUtil` 提供便捷的通知显示方法
- **图标资源**：示例 SVG 图标文件

## 🚀 快速开始

### 1. 复制模板

```bash
cp -r template-without-ai my-new-plugin
cd my-new-plugin
```

### 2. 修改插件信息

编辑 `gradle.properties` 文件，修改以下配置：

```properties
# 插件基本信息
pluginGroup=dev.dong4j.zeka.stack
pluginName=My New Plugin
pluginVersion=1.0.0

# 项目名称
rootProjectName=my-new-plugin

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
# 运行插件
./gradlew runIde
```

## 📚 详细说明

### 项目结构

```
template-without-ai/
├── build.gradle.kts              # Gradle 构建脚本
├── gradle.properties             # 插件配置
├── settings.gradle.kts            # Gradle 设置
├── src/
│   ├── main/
│   │   ├── java/                 # Java 源代码
│   │   │   └── dev/dong4j/zeka/stack/idea/plugin/example/
│   │   │       ├── action/       # 动作类
│   │   │       ├── icons/        # 图标常量
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

### 添加设置页面（可选）

如果需要设置页面，可以参考 `template-with-ai` 模板中的实现：

1. 创建 `SettingsState` 类（实现 `PersistentStateComponent`）
2. 创建 `SettingsConfigurable` 类（实现 `SearchableConfigurable`）
3. 创建 `SettingsPanel` 类（UI 面板）
4. 在 `plugin.xml` 中注册 `projectConfigurable`

### 依赖管理

如果需要添加其他依赖，在 `build.gradle.kts` 的 `dependencies` 块中添加：

```kotlin
dependencies {
    // 添加你的依赖
    implementation("com.example:library:1.0.0")
}
```

## 🔧 配置说明

### gradle.properties

| 配置项                  | 说明            | 示例                                          |
|----------------------|---------------|---------------------------------------------|
| `pluginGroup`        | 插件组 ID        | `dev.dong4j.zeka.stack.idea.plugin.example` |
| `pluginName`         | 插件显示名称        | `My New Plugin`                             |
| `pluginVersion`      | 插件版本号         | `1.0.0`                                     |
| `rootProjectName`    | 项目根目录名称       | `my-new-plugin`                             |
| `platformType`       | IntelliJ 平台类型 | `IC` (Community) 或 `IU` (Ultimate)          |
| `platformVersion`    | IntelliJ 版本   | `2025.2`                                    |
| `platformSinceBuild` | 最低支持版本        | `242.2`                                     |
| `platformUntilBuild` | 最高支持版本        | `999.*`                                     |

### plugin.xml

关键配置项：

- `<id>`：插件唯一标识符
- `<name>`：插件显示名称
- `<description>`：插件描述
- `<extensions>`：扩展点注册（如需要）

## 📖 更多资源

- [主项目 README](../../README.md) - 完整的插件开发指南
- [IntelliJ Platform SDK 文档](https://plugins.jetbrains.com/docs/intellij/welcome.html) - 官方开发文档
- [插件开发基础](https://plugins.jetbrains.com/docs/intellij/getting-started.html) - 入门教程

## ⚠️ 注意事项

1. **线程安全**：遵循 IntelliJ Platform 的线程模型，UI 操作在 EDT，耗时操作在 BGT
2. **资源管理**：图标、国际化资源等放在 `src/main/resources` 对应目录
3. **测试覆盖**：建议为关键功能编写单元测试
4. **代码规范**：使用 Google Java Format 格式化代码

## 🔄 与带 AI 模板的区别

| 特性                  | 不带 AI 模板 | 带 AI 模板 |
|---------------------|----------|---------|
| IntelliAI Engine 依赖 | ❌        | ✅       |
| 自动构建 Engine 任务      | ❌        | ✅       |
| 设置页面示例              | ❌        | ✅       |
| 插件体积                | 小        | 较大      |
| 启动速度                | 快        | 较慢      |
| 适用场景                | 工具类插件    | AI 功能插件 |

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

