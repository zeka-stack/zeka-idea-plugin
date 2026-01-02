# Zeka IDEA Plugin Suite

一套面向开发者的 IntelliJ IDEA 插件集合，致力于通过 AI 能力和实用工具提升开发效率。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2022.3+-blue.svg)](https://www.jetbrains.com/idea/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)

## 🎯 项目概述

Zeka IDEA Plugin Suite 是一个现代化的 IntelliJ IDEA 插件生态系统，包含两大类插件：

1. **AI 驱动插件** - 依赖 IntelliAI Engine 提供智能化功能
2. **工具类插件** - 独立运行，提供实用的开发工具

项目采用模块化架构，每个插件都是独立的 Gradle 项目，便于开发、测试和维护。

## 📦 包含插件

### 核心引擎

#### [IntelliAI Engine](./intelli-ai-engine)

AI 能力基础引擎,为其他插件提供统一的 AI 服务接口。

**核心能力:**

- 🤖 多模型服务商支持:OpenAI、通义千问、Ollama、LM Studio、SiliconFlow
- ⚙️ 统一配置面板:集中管理 API Key、模型选择和速率限制
- 🔒 安全凭证存储:基于系统级加密(Keychain/Keyring/Credential Manager)
- 🧩 可复用 UI 组件:提供状态栏、按钮、进度弹窗等 UI 组件
- 🔄 任务执行管线:处理请求调度、流式输出和错误恢复

**JetBrains Marketplace:** [IntelliAI Engine](https://plugins.jetbrains.com/plugin/29152)

---

### AI 驱动插件

> 以下插件均依赖 IntelliAI Engine,需要先安装 IntelliAI Engine 才能使用

#### [IntelliAI Javadoc](./intelli-ai-javadoc)

专为批量生成符合规范的 Javadoc 注释而设计,解决传统工具翻译质量差、生成速度慢的问题。

**核心功能:**

- ✍️ AI 生成有意义的注释内容,而非简单翻译或空模板
- ⚡ 快速批量处理,支持整个项目级别的注释生成
- ✅ 生成符合 Checkstyle 和公司标准的 Javadoc
- 🎯 多种触发方式:快捷键(`Cmd/Ctrl+Shift+D`)、右键菜单、Generate 菜单等
- 🧪 智能识别类、方法、测试方法等不同元素
- 📊 实时进度显示和结果统计

**JetBrains Marketplace:** [IntelliAI Javadoc](https://plugins.jetbrains.com/plugin/28835)

**[使用指南](./intelli-ai-javadoc/docs/用户手册.md)**

#### [IntelliAI Changelog](./intelli-ai-changelog)

基于 Git 提交记录自动生成变更日志、工作日报/周报和智能提交信息。

**核心功能:**

- 📝 AI 生成项目变更日志(Changelog)
- 📅 根据 Git 提交自动生成工作日报和周报
- 💬 基于代码差异(diff)智能生成提交信息
- 🎨 自定义提示词模板
- 🌍 完整的中英文国际化支持

**使用场景:**

- 在 Git Log 工具窗口中选择提交记录生成变更日志
- 快速生成周期性工作报告
- 在 Git 提交面板中自动生成规范的提交信息

**JetBrains Marketplace:** [IntelliAI Changelog](https://plugins.jetbrains.com/plugin/29154)

**[使用指南](./intelli-ai-changelog/docs/用户手册.md)**

#### [IntelliAI Tracer](./intelli-ai-tracer)

使用 AI 自动分析代码方法调用链和业务流程,帮助快速理解复杂系统。

**核心功能:**

- 🔍 自动分析方法的上下游调用链
- 📊 提取完整的方法上下文信息(类、注解、注释等)
- 🎨 AI 生成可视化调用时序图
- 💡 AI 生成业务逻辑说明
- 🎯 编辑器中选中方法调用即可分析

**使用场景:**

- 快速理解遗留代码的调用关系
- 代码审查时分析潜在问题
- 自动生成技术文档和时序图

**JetBrains Marketplace:** [IntelliAI Tracer](https://plugins.jetbrains.com/plugin/29155)

**[使用指南](./intelli-ai-tracer/docs/用户手册.md)**

#### [IntelliAI Nacos](./intelli-ai-nacos)

Nacos 配置管理增强插件,提供便捷的配置查看、对比和管理能力。

**核心功能:**

- 🔌 连接 Nacos 注册中心
- 📂 浏览和管理配置
- 🔍 配置对比和差异分析
- 🏠 本地 Nacos 注册中心支持

**JetBrains Marketplace:** [IntelliAI Nacos](https://plugins.jetbrains.com/plugin/29156)

**[使用指南](./intelli-ai-nacos/docs/用户手册.md)**

---

### 工具类插件

#### [Archiver Man](./archiver-man)

在 IDEA 中直接编辑 ZIP/JAR 压缩包内的文件,无需解压。

**核心功能:**

- 📂 在 Project View 中直接展开压缩包
- ✏️ 使用 IDE 原生编辑器修改压缩包内文件
- 💾 自动备份,支持批量保存和冲突检查
- 🔄 检测外部修改并提示合并
- ⚙️ 可配置文件大小限制、备份策略等

**使用场景:**

- 快速修补部署包中的配置文件
- 验证第三方 SDK 内的代码
- 批量更新压缩包中的配置

**JetBrains Marketplace:** [Archiver Man]()

**[使用指南](./archiver-man/docs/用户手册.md)**

#### [ZKS Dev Helper](./zks-dev-helper)

Zeka Stack 框架开发助手,专为 Zeka Stack 框架开发提供辅助功能。目前提供统一的代码样式配置功能,后续将持续添加更多开发辅助特性,如 MyBatis
自动提示、Proxyer 组件接口自动识别等。

**核心功能:**

- 📝 统一的文件头部注释模板
- ⚡ Live Template 快捷代码片段(`todo`、`fix`、`test`、日志等)
- 🎨 自动配置代码格式化规则
- 📊 使用统计分析

**计划中的功能:**

- 🔜 MyBatis 自动提示功能
- 🔜 Proxyer 组件接口自动识别
- 🔜 更多 Zeka Stack 框架开发辅助功能

**核心优势:**

- 🚀 零配置,安装即用
- 🎯 标准化代码风格
- ⚡ 提升开发效率
- 🔧 支持自定义

**JetBrains Marketplace:** [ZKS Dev Helper]()

**[使用指南](./zks-dev-helper/插件操作手册.md)**

---

## 🚀 快速开始

### 环境要求

- **IntelliJ IDEA:** 2022.3 及以上版本(支持 IC 和 IU)
- **Java:** 17 及以上
- **Gradle:** 8.x (项目自带 Gradle Wrapper)

### 安装插件

#### 从 JetBrains Marketplace 安装(推荐)

1. 打开 IntelliJ IDEA
2. 进入 `Settings/Preferences` → `Plugins`
3. 搜索插件名称(如 `IntelliAI Javadoc`)
4. 点击 `Install`

**注意:** 如果安装 AI 驱动插件,需要先安装 `IntelliAI Engine`

#### 手动安装

1. 从各插件的产品页下载插件 ZIP 包
2. 进入 `Settings/Preferences` → `Plugins` → 齿轮图标 → `Install Plugin from Disk...`
3. 选择下载的 ZIP 文件
4. 重启 IDEA

---

## 🛠️ 开发指南

### 🚀 快速上手开发

#### 前置要求

- **IntelliJ IDEA:** 2022.3 及以上版本
- **Java:** 17 及以上
- **Gradle:** 8.x (项目已包含 Gradle Wrapper)

#### 选择插件模板

项目提供两种插件模板，根据是否需要 AI 能力选择。模板位于 `idea-plugin-template` 目录。

**📖 详细说明：** 请查看 [idea-plugin-template/README.md](./idea-plugin-template/README.md)

##### 1. 带 AI 能力的插件模板

**适用场景:** 需要集成 OpenAI、通义千问等 AI 服务的插件

**快速开始:**

```bash
# 复制模板
cp -r idea-plugin-template/template-with-ai my-new-ai-plugin
cd my-new-ai-plugin

# 修改 gradle.properties 中的插件信息
vim gradle.properties

# 开始开发
./gradlew runIde
```

**详细说明：** 请查看 [template-with-ai/README.md](./idea-plugin-template/template-with-ai/README.md)

##### 2. 不带 AI 能力的插件模板

**适用场景:** 纯工具类插件，无需 AI 集成

**快速开始:**

```bash
# 复制模板
cp -r idea-plugin-template/template-without-ai my-new-plugin
cd my-new-plugin

# 修改 gradle.properties 中的插件信息
vim gradle.properties

# 开始开发
./gradlew runIde
```

**详细说明：** 请查看 [template-without-ai/README.md](./idea-plugin-template/template-without-ai/README.md)

### 🔌 AI 插件 Engine 依赖集成详解

对于需要 AI 能力的插件，必须正确集成 IntelliAI Engine。以下是完整的集成步骤：

#### 1. 依赖配置

在 `build.gradle.kts` 中配置依赖：

```kotlin
dependencies {
    // IntelliJ Platform 基础依赖
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // 本地开发时注释掉，发布时取消注释
        // plugin("dev.dong4j.zeka.stack.idea.plugin.common.ai")


        zipSigner()
        pluginVerifier()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // 编译时依赖 - 本地开发和发布都需要
    compileOnly("dev.dong4j.zeka.stack:intelli-ai-engine:1.1.0")

    // 其他依赖...
}
```

#### 2. 关键 Gradle 任务配置

**两个核心任务用于本地开发：**

##### `buildAiCommonPlugin` 任务

```kotlin
val buildAiCommonPlugin = register<Exec>("buildAiCommonPlugin") {
    description = "Build intelli-ai-engine plugin for local development"
    group = "intellij"

    val aiCommonDir = file("../intelli-ai-engine")
    workingDir = aiCommonDir
    commandLine = listOf(aiCommonDir.resolve("gradlew").absolutePath, "clean", "buildPlugin")
}
```

##### `copyAiCommonPlugin` 任务

```kotlin
val copyAiCommonPlugin = register<Copy>("copyAiCommonPlugin") {
    description = "Copy intelli-ai-engine plugin to sandbox for local development"
    group = "intellij"

    // 依赖构建任务
    dependsOn(buildAiCommonPlugin)

    // 在 prepareSandbox 之后执行
    mustRunAfter("prepareSandbox")

    // 从 intelli-ai-engine 的构建输出复制插件
    val aiCommonPluginDir = file("../intelli-ai-engine/build/distributions")
    from(aiCommonPluginDir) {
        include("*.zip")
    }

    // 复制到 sandbox 的 plugins 目录
    val sandboxProductDir = "${providers.gradleProperty("platformType").get()}-${providers.gradleProperty("platformVersion").get()}"
    val sandboxPluginsDir = layout.buildDirectory.dir("idea-sandbox/$sandboxProductDir/plugins").get().asFile
    sandboxPluginsDir.mkdirs()
    into(sandboxPluginsDir)

    // 自动解压插件 ZIP 文件
    doLast {
        val zipFiles = fileTree(sandboxPluginsDir) { include("*.zip") }
        zipFiles.forEach { zipFile ->
            val pluginDirName = zipFile.nameWithoutExtension.substringBeforeLast("-", zipFile.nameWithoutExtension)
            val targetDir = sandboxPluginsDir.resolve(pluginDirName)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }

            copy {
                from(zipTree(zipFile))
                into(sandboxPluginsDir)
            }
            zipFile.delete()
        }
    }
}
```

#### 3. 自动化集成

在 `runIde` 任务中自动执行依赖插件的安装：

```kotlin
runIde {
    dependsOn(copyAiCommonPlugin)  // 自动构建和安装 Engine 插件
    jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")  // 支持热更新
}
```

#### 4. 本地开发 vs 生产发布

**本地开发流程:**

1. `./gradlew runIde` 自动触发：
    - `buildAiCommonPlugin` → 构建引擎插件
    - `copyAiCommonPlugin` → 复制到沙盒环境
    - `runIde` → 启动带完整依赖的 IDE

**生产发布流程:**

1. 取消注释 `plugin("dev.dong4j.zeka.stack.idea.plugin.common.ai")`
2. 移除或注释 `buildAiCommonPlugin` 和 `copyAiCommonPlugin` 任务
3. 执行发布流程

#### 5. plugin.xml 配置

```xml
<!-- 依赖 IntelliAI Engine 插件 -->
<depends>dev.dong4j.zeka.stack.idea.plugin.common.ai</depends>

<!-- 注册到 Engine 的扩展点 -->
<extensions defaultExtensionNs="dev.dong4j.zeka.stack.idea.plugin.common.ai">
    <aiConsoleLoggerProvider implementation="com.your.plugin.YourAIConsoleLoggerProvider"/>
</extensions>
```

### 克隆仓库

```bash
git clone https://github.com/zeka-stack/zeka-idea-plugin.git
cd zeka-idea-plugin
```

### 项目结构

```
zeka-idea-plugin/
├── intelli-ai-engine/         # AI 引擎(其他 AI 插件的依赖)
├── intelli-ai-javadoc/        # Javadoc 生成插件
├── intelli-ai-changelog/      # Changelog 生成插件
├── intelli-ai-tracer/         # 调用链追踪插件
├── intelli-ai-nacos/          # Nacos 配置管理插件
├── archiver-man/              # 压缩包编辑插件
├── zks-dev-helper/            # Zeka 开发助手插件
└── idea-plugin-template/      # 插件开发模板集合
    ├── template-with-ai/      # 带 AI 能力的插件模板
    └── template-without-ai/   # 不带 AI 能力的插件模板
```

每个子目录都是一个独立的 Gradle 项目,包含:

- `build.gradle.kts` - Kotlin DSL 构建脚本
- `gradle.properties` - 插件配置(版本号、支持的 IDEA 版本等)
- `src/main/` - 源代码
- `includes/` - 插件描述和更新日志
- `docs/` - 详细设计文档
- `deploy.sh` - 自动化部署脚本

### 开发环境配置

#### 1. 导入项目

使用 IntelliJ IDEA 打开任一子项目目录(如 `intelli-ai-javadoc`),IDE 会自动识别 Gradle 项目。

#### 2. 构建插件

```bash
cd intelli-ai-javadoc  # 进入任一插件目录
./gradlew buildPlugin  # 构建插件
```

构建产物位于 `build/distributions/` 目录。

#### 3. 运行调试

**方法一:使用 Gradle 任务**

```bash
./gradlew runIde
```

这会启动一个带插件的 IDEA 沙盒环境,支持热更新(`-XX:AllowEnhancedClassRedefinition`)。

**方法二:使用 IDEA Run Configuration**

项目已配置 `.run/Run Plugin.run.xml`,直接点击运行按钮即可。

#### 4. 依赖 IntelliAI Engine 的插件开发

对于依赖 `intelli-ai-engine` 的插件(如 `intelli-ai-javadoc`),本地开发时需要:

1. **编译时依赖**:通过 `compileOnly("dev.dong4j.zeka.stack:intelli-ai-engine:1.1.0")` 声明
2. **运行时依赖**:通过自定义 Gradle 任务自动构建和复制 Engine 插件到沙盒

```kotlin
// 构建脚本已配置 buildAiCommonPlugin 和 copyAiCommonPlugin 任务
./gradlew runIde  // 会自动触发依赖插件的构建和安装
```

**注意:**发布到 Marketplace 后,需要:

- 取消注释 `plugin("dev.dong4j.zeka.stack.idea.plugin.common.ai")`
- 移除 `buildAiCommonPlugin` 和 `copyAiCommonPlugin` 任务

### 代码规范

项目使用 Google Java Format 进行代码格式化:

```bash
./gradlew googleJavaFormat      # 格式化代码
./gradlew verifyGoogleJavaFormat # 验证格式
```

### 测试

```bash
./gradlew test  # 运行单元测试
```

测试框架:

- JUnit 5
- Mockito
- AssertJ
- IntelliJ Platform Test Framework

### 插件验证

```bash
./gradlew verifyPlugin  # 验证插件描述符
./gradlew runPluginVerifier  # 验证插件兼容性
```

### 发布流程

#### 1. 更新版本号

编辑 `gradle.properties`:

```properties
pluginVersion=2025.3.1
```

#### 2. 更新变更日志

编辑 `includes/pluginChanges.html`,添加新版本的变更内容。

#### 3. 插件签名和发布

**环境变量配置:**

```bash
export CERTIFICATE_CHAIN="证书链内容"
export PRIVATE_KEY="私钥内容"
export PRIVATE_KEY_PASSWORD="私钥密码"
export PUBLISH_TOKEN="JetBrains Marketplace Token"
```

**执行发布:**

```bash
./gradlew signPlugin publishPlugin
```

或使用自动化部署脚本:

```bash
./deploy.sh
```

**JetBrains Marketplace 账号:** `code.dsj@gmail.com`

#### 4. 版本号规范

遵循 [JetBrains 版本号范围规范](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html):

- `platformSinceBuild`: 最低支持版本(如 `223`)
- `platformUntilBuild`: 最高支持版本(如 `252.*`)

---

## ⚠️ 注意事项

### 1. Gradle 构建脚本

- 使用 **Kotlin DSL** (`build.gradle.kts`),而非 Groovy
- 必须使用 Kotlin 语法编写配置

### 2. 热更新支持

所有插件的 `runIde` 任务已配置 JVM 参数:

```kotlin
jvmArgs = listOf("-XX:AllowEnhancedClassRedefinition")
```

开发时修改代码后,使用 `Build → Reload Changed Classes` 即可热更新。

### 3. API Key 安全存储

AI 插件使用 IntelliJ Platform 的 `PasswordSafe` 机制:

- macOS: Keychain Access
- Linux: GNOME Keyring / KWallet
- Windows: Credential Manager

**绝不**将 API Key 保存在配置文件或代码中。

### 4. 插件依赖关系

```
IntelliAI Engine (基础引擎)
    ├── IntelliAI Javadoc
    ├── IntelliAI Changelog
    ├── IntelliAI Tracer
    └── IntelliAI Nacos

Archiver Man (独立)
ZKS Dev Helper (独立)
```

### 5. 国际化支持

插件支持中英文双语,资源文件位于:

```
src/main/resources/messages/
    ├── Bundle.properties      # 默认(英文)
    └── Bundle_zh.properties   # 中文
```

### 6. 插件描述文件

插件的市场描述和更新日志通过外部 HTML 文件管理:

```
includes/
    ├── pluginDescription.html  # 插件描述
    └── pluginChanges.html      # 更新日志
```

### 7. 文件路径限制

开发时只能编辑工作区路径内的文件:

```
/Users/dong4j/Developer/0.Worker/opensource/zeka.stack/zeka-idea-plugin/
```

无法编辑此路径之外的文件。

### 8. 线程安全

插件开发必须注意:

- **EDT (Event Dispatch Thread)**: UI 操作必须在 EDT 中执行
- **BGT (Background Thread)**: 耗时操作必须在后台线程执行
- 使用 `ApplicationManager.getApplication().invokeLater()` 切换到 EDT
- 使用 `ReadAction.run()` / `WriteAction.run()` 保证读写安全

详见各插件的 `docs/EDT&BGT.md` 文档。

---

## 📚 文档资源

### 各插件详细文档

- [IntelliAI Engine - AI 工作流解释器](./intelli-ai-engine/docs/AI工作流解释器功能实现方案.md)
- [IntelliAI Engine - 扩展点实现方案](./intelli-ai-engine/docs/扩展点实现方案.md)
- [IntelliAI Javadoc - 开发指南](./intelli-ai-javadoc/docs/DEVELOPMENT_GUIDE.md)
- [IntelliAI Javadoc - 快速开始](./intelli-ai-javadoc/docs/QUICK_START.md)
- [IntelliAI Changelog - 方案设计](./intelli-ai-changelog/docs/方案设计.md)
- [IntelliAI Tracer - 功能设计](./intelli-ai-tracer/docs/)
- [Archiver Man - 功能规划](./archiver-man/docs/ArchiverMan功能规划清单.md)
- [ZKS Dev Helper - 重构总结](./zks-dev-helper/重构总结.md)

### 官方文档

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Plugin 开发基础](https://plugins.jetbrains.com/docs/intellij/getting-started.html)
- [插件发布指南](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [插件签名要求](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request!

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 许可证

MIT License

---

## 📮 联系方式

- **GitHub:** [zeka-stack/zeka-idea-plugin](https://github.com/zeka-stack/zeka-idea-plugin)
- **Issues:** [提交问题](https://github.com/zeka-stack/zeka-idea-plugin/issues)
- **Email:** dong4jj@gmail.com

---

## 🙏 致谢

感谢所有贡献者的支持!

特别感谢 JetBrains 提供的优秀开发平台和完善的插件生态系统。
