# IntelliJ IDEA 插件开发模板

这是一个基于 Gradle 的 IntelliJ IDEA 插件开发模板，提供了完整的插件开发基础结构和示例代码。

## 项目结构

```
template/
├── build.gradle.kts              # Gradle 构建配置
├── settings.gradle.kts            # Gradle 设置
├── gradlew                       # Gradle Wrapper 脚本
├── gradlew.bat                   # Windows Gradle Wrapper 脚本
├── gradle/                       # Gradle Wrapper 文件
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/plugin/
│   │   │       ├── action/        # Action 类
│   │   │       │   ├── ExampleShortcutAction.java
│   │   │       │   ├── ExampleFileAction.java
│   │   │       │   ├── ExampleSelectionAction.java
│   │   │       │   └── ExampleIntentionAction.java
│   │   │       ├── service/        # 服务类
│   │   │       │   └── ExampleService.java
│   │   │       ├── settings/      # 设置相关类
│   │   │       │   ├── SettingsState.java
│   │   │       │   ├── ExampleSettingsConfigurable.java
│   │   │       │   └── ui/
│   │   │       │       └── ExampleSettingsPanel.java
│   │   │       └── util/          # 工具类
│   │   │           ├── NotificationUtil.java
│   │   │           └── ExampleBundle.java
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml     # 插件配置文件
│   │       ├── intentionDescriptions/     # Intention Action 描述文件
│   │       │   └── ExampleIntention/
│   │       │       └── description.html
│   │       ├── messages.properties        # 英文资源文件
│   │       └── messages_zh_CN.properties  # 中文资源文件
│   └── test/
│       └── java/
│           └── com/example/plugin/
│               ├── service/
│               │   └── ExampleServiceTest.java
│               └── settings/
│                   └── SettingsStateTest.java
└── README.md                     # 本文件
```

## 功能特性

### 1. Action 系统

- **快捷键 Action**: `Ctrl+Shift+E` (Windows/Linux) 或 `Cmd+Shift+E` (Mac)
- **编辑器右键菜单**: 在编辑器中右键选择 "Example File Action"
- **项目视图右键菜单**: 在项目视图中右键选择 "Example Selection Action"
- **Intention Action**: 使用 `Option+Enter` / `Alt+Enter` 触发

### 2. 设置系统

- 完整的设置面板，位于 `Settings → Tools → Example Plugin`
- 配置持久化存储
- 支持基础设置和高级设置

### 3. 服务层

- 使用 `@Service` 注解的服务类
- 项目级别的服务管理
- 示例业务逻辑处理

### 4. 工具类

- 通知工具类 (`NotificationUtil`)
- 国际化资源包工具类 (`ExampleBundle`)

### 5. 国际化支持

- 英文和中文资源文件
- 支持参数化消息

### 6. 测试支持

- JUnit 5 测试框架
- Mockito 模拟框架
- AssertJ 断言库
- HTTP Mock Server (OkHttp MockWebServer) 用于 API 测试

### 7. 开发工具

- Google Java Format 自动代码格式化
- SLF4J 日志框架
- Lombok 简化 Java 代码

## 快速开始

### 1. 复制模板

```bash
cp -r template/ your-plugin-name/
cd your-plugin-name/
```

### 2. 修改配置

1. 修改 `settings.gradle.kts` 中的项目名称
2. 修改 `build.gradle.kts` 中的 `group` 和 `version`
3. 修改 `plugin.xml` 中的插件信息
4. 修改包名 `com.example.plugin` 为你的包名

### 3. 构建插件

```bash
./gradlew build
```

### 4. 运行插件

```bash
./gradlew runIde
```

### 5. 运行测试

```bash
./gradlew test
```

## 开发指南

### 添加新的 Action

1. 在 `action/` 包下创建新的 Action 类
2. 在 `plugin.xml` 中注册 Action
3. 添加相应的资源文件条目

### 添加新的设置

1. 在 `SettingsState` 中添加新的字段
2. 在 `ExampleSettingsPanel` 中添加 UI 组件
3. 更新 `isModified()`, `apply()`, `reset()` 方法

### 添加新的服务

1. 在 `service/` 包下创建新的服务类
2. 使用 `@Service` 注解
3. 在 `plugin.xml` 中注册服务（如果需要）

### 添加国际化支持

1. 在 `messages.properties` 中添加英文条目
2. 在 `messages_zh_CN.properties` 中添加中文条目
3. 使用 `ExampleBundle.message()` 方法获取文本
4. 使用 `ExampleBundle.messagePointer()` 获取延迟加载的消息（用于 Action 等场景）

**注意**：模板使用 `DynamicBundle` 而非 `AbstractBundle`，支持运行时动态切换语言环境。

## 配置说明

### Gradle 配置

- 使用 IntelliJ Platform Gradle Plugin 2.x
- 支持 Java 17
- 包含测试框架和代码格式化工具
- 预配置常用依赖（Lombok、SLF4J、JUnit、Mockito、AssertJ、OkHttp MockWebServer）

### 插件配置

- 支持 IntelliJ IDEA 2022.3 及更高版本
- 依赖 Java 模块
- 包含通知组和设置面板

## 发布插件

### 1. 签名插件

设置环境变量：

```bash
export CERTIFICATE_CHAIN="your-certificate-chain"
export PRIVATE_KEY="your-private-key"
export PRIVATE_KEY_PASSWORD="your-password"
```

### 2. 发布到 JetBrains Marketplace

设置环境变量：

```bash
export PUBLISH_TOKEN="your-publish-token"
```

运行发布命令：

```bash
./gradlew publishPlugin
```

## 常见问题

### Q: 如何调试插件？

A: 使用 `./gradlew runIde` 启动带有插件的 IDE 实例，然后使用 IDE 的调试功能。

### Q: 如何添加依赖？

A: 在 `build.gradle.kts` 的 `dependencies` 块中添加依赖。

### Q: 如何自定义插件图标？

A: 在 `src/main/resources/` 下添加图标文件，然后在 `plugin.xml` 中引用。

### Q: 如何支持其他语言？

A: 添加对应的 `messages_xx_XX.properties` 文件。

## 参考资源

- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Gradle Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
- [Plugin Development Guide](https://plugins.jetbrains.com/docs/intellij/plugin-development.html)

## 许可证

本模板基于 MIT 许可证开源。
