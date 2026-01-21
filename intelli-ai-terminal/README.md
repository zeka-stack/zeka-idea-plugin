# IntelliAI Terminal

![](./assets/20260121_S1Bp2c.png)

IntelliAI Terminal 是一个 IntelliJ IDEA 插件，在 IDEA 的 Terminal 中注入 AI 功能，让你可以通过 AI 快速生成终端命令。只需在终端输入自然语言描述，按
TAB 键即可自动生成对应的命令并替换当前行，大大提高终端使用效率。

## 目录

- [1 🎯 功能特性](#1-🎯-功能特性)
- [2 🚀 快速开始](#2-🚀-快速开始)
- [3 💡 使用说明](#3-💡-使用说明)
- [4 ⚙️ 配置说明](#4-⚙️-配置说明)
- [5 🛠️ 技术实现](#5-🛠️-技术实现)
- [6 📁 项目结构](#6-📁-项目结构)
- [7 ⚠️ 注意事项](#7-⚠️-注意事项)
- [8 📖 更多文档](#8-📖-更多文档)
- [9 📝 许可证](#9-📝-许可证)

## 1 🎯 功能特性

![](./assets/20260121_RP9Tdk.gif)

### 1.1 核心功能

- **🚀 一键生成命令**：在终端输入自然语言描述，按 TAB 键即可生成对应的命令
- **📝 智能提取**：自动从终端最后一行提取输入内容，支持去除 shell 提示符（`$`、`>` 等）
- **🔄 自动替换**：生成的命令会自动替换当前终端行，可直接执行或继续编辑
- **✅ 格式验证**：自动验证生成的命令格式，确保输出是有效的 shell 命令
- **🎯 前缀触发**：支持自定义触发前缀（默认 `#`），只有以该前缀开头的行才会触发 AI 生成

### 1.2 技术特性

- **🔌 双终端支持**：同时支持 `TerminalView` 和 `JBTerminalWidget` 两种终端实现
- **⚡ 后台执行**：AI 调用在后台线程执行，不会阻塞 UI
- **📊 日志输出**：所有 AI 请求和响应都会输出到 Engine Console，便于调试
- **🎨 提示词定制**：支持自定义系统提示词和用户提示词模板，满足不同场景需求

### 1.3 集成能力

- **🤖 IntelliAI Engine 集成**：基于 IntelliAI Engine 统一管理 AI 服务商
- **⚙️ 设置页面**：集成到 IntelliAI Engine 设置页面下，统一管理配置
- **📌 状态栏菜单**：提供状态栏快捷菜单，快速切换服务商和打开设置
- **🔄 更新检查**：集成插件更新信息提供者，统一管理更新

## 2 🚀 快速开始

### 2.1 前置条件

1. **安装 IntelliAI Engine 插件**：
    - 从 JetBrains Marketplace 安装 [IntelliAI Engine](https://plugins.jetbrains.com/plugin/xxx) 插件
    - 或在本地开发时，插件会自动安装 Engine 插件

2. **配置 AI 服务商**：
    - 打开 `Settings → Tools → IntelliAI Engine`
    - 添加并验证至少一个 AI 服务商（如 OpenAI、通义千问、Ollama 等）
    - 确保至少有一个服务商验证成功

### 2.2 启用插件

1. 打开 `Settings → Tools → IntelliAI Engine → 🧩 IntelliAI Terminal`
2. 勾选"启用 Terminal AI"（默认已启用）
3. （可选）配置触发前缀（默认 `#`）
4. （可选）选择 AI 服务商（默认使用全局第一个已验证的服务商）

### 2.3 开始使用

1. 打开 Terminal（`Alt + F12` 或 `View → Tool Windows → Terminal`）
2. 输入自然语言描述，以触发前缀开头（如：`# 列出当前目录下所有 Python 文件`）
3. 按 `TAB` 键触发 AI 生成
4. AI 生成的命令会自动替换当前行，可以直接执行或继续编辑

## 3 💡 使用说明

### 3.1 基本使用

#### 3.1.1 示例 1：生成简单命令

```
# 查看当前目录下的所有文件
# 按 TAB 键 → ls -la
```

#### 3.1.2 示例 2：生成复杂命令

```
# 查找所有包含 "TODO" 的 Java 文件
# 按 TAB 键 → find . -name "*.java" -exec grep -l "TODO" {} \;
```

#### 3.1.3 示例 3：使用默认前缀

如果触发前缀为空，则当前行的所有内容都会作为 AI 输入：

```
列出所有 .git 目录
# 按 TAB 键 → find . -name ".git" -type d
```

### 3.2 高级功能

#### 3.2.1 自定义触发前缀

在设置页面可以自定义触发前缀，例如设置为 `ai:`：

```
ai: 统计代码行数
# 按 TAB 键触发 AI 生成
```

#### 3.2.2 自定义提示词模板

勾选"显示高级设置"后，可以配置：

- **系统提示词**：设定 AI 角色和行为准则
    - 默认值：`你是一位经验丰富的软件开发助手。你的目标是帮助开发者完成各种开发任务。你总是提供清晰、准确、有用的建议和解决方案。`

- **用户提示词模板**：用于生成终端命令的提示词模板
    - 使用 `{content}` 作为占位符，会被替换为实际的用户输入
    - 默认模板包含命令生成要求和格式说明

### 3.3 查看日志

所有 AI 请求和响应都会输出到 Engine Console：

1. 打开 `View → Tool Windows → IntelliAI Console`
2. 查看详细的请求和响应日志
3. 如果生成失败，可以查看错误日志定位问题

### 3.4 状态栏快捷操作

点击 IDE 底部状态栏的 IntelliAI Engine 图标，可以看到：

- **AI 服务商切换**：快速切换当前使用的 AI 服务商
- **打开设置**：快速打开插件设置页面

## 4 ⚙️ 配置说明

### 4.1 基础设置

| 配置项            | 说明                    | 默认值          |
|----------------|-----------------------|--------------|
| 启用 Terminal AI | 控制是否启用 Terminal AI 功能 | `true`       |
| 触发前缀           | 只有以该前缀开头的行才会触发 AI 生成  | `#`          |
| AI 服务商         | 选择默认使用的 AI 服务商        | 全局第一个已验证的服务商 |

### 4.2 高级设置

勾选"显示高级设置"后，可以配置：

| 配置项     | 说明                                | 默认值                 |
|---------|-----------------------------------|---------------------|
| 系统提示词   | 设定 AI 角色和行为准则                     | [查看默认值](#默认系统提示词)   |
| 用户提示词模板 | 生成终端命令的提示词模板，使用 `{content}` 作为占位符 | [查看默认值](#默认用户提示词模板) |

#### 4.2.1 默认系统提示词

```
你是一个命令行助手。

你的任务是：只生成【可以直接执行的 shell 命令】

【强制规则】
- 只输出 shell 命令本身，不允许输出任何解释、说明或提示文字
- 不允许输出注释（包括 #、//、/* */）
- 不允许输出 Markdown、代码块标记（```）
- 不允许输出多余的空行或空格
- 输出内容必须可以直接在 bash / zsh（macOS / Linux）中执行

【格式规则】
- 如果命令较长，必须使用 "\\" 进行换行
- 使用 "\\" 换行时，每一行在拼接后必须是合法的 shell 命令
- 不要为了换行而改变命令语义
- 除非 shell 语法必须，否则不要额外转义字符

【行为约束】
- 不允许向用户提问
- 不允许给出多个备选方案
- 如果需要多个步骤，使用 "&&" 串联为一条命令
- 不允许输出 <path>、<file>、<xxx> 等占位符

【兜底规则】
- 如果用户请求无法转换为明确可执行的 shell 命令，请直接输出空内容
```

#### 4.2.2 默认用户提示词模板

```
根据下面的描述，生成最合适的一条 shell 命令:
{content}
```

### 4.3 配置位置

- **设置页面**：`Settings → Tools → IntelliAI Engine → 🧩 IntelliAI Terminal`
- **配置文件**：`<IDE配置目录>/zeka.stack.terminal.plugin.xml`

## 5 🛠️ 技术实现

### 5.1 核心流程

1. **输入提取**：
    - 从终端最后一行提取非空内容
    - 去除 shell 提示符（`$ `、`$`、`> `、`>` 等）
    - 根据触发前缀提取有效输入

2. **AI 调用**：
    - 构建 `AIChatRequest`，包含系统提示词和用户提示词
    - 在后台线程调用 `AIService.generateContent()`
    - 显示进度提示

3. **结果处理**：
    - 从 AI 响应中提取第一行有效命令（跳过代码块和空行）
    - 验证命令格式（必须符合 shell 命令规范）
    - 使用 `Ctrl+U` 清除当前行，然后写入新命令

### 5.2 关键组件

- **`TerminalAiGenerateAction`**：核心 Action 类，处理 TAB 键触发和命令生成
- **`SettingsState`**：配置持久化，管理插件设置
- **`TerminalSettingsPanel`**：设置页面 UI 组件
- **`TerminalAiAllowedActionsProvider`**：注册允许的终端动作

### 5.3 技术要点

- **线程安全**：遵循 IntelliJ Platform 线程模型，UI 操作在 EDT，耗时操作在 BGT
- **终端兼容**：同时支持 `TerminalView` 和 `JBTerminalWidget` 两种实现
- **错误处理**：完善的错误处理和用户提示机制
- **日志记录**：所有操作都记录到 Engine Console

## 6 📁 项目结构

```
intelli-ai-terminal/
├── src/main/java/dev/dong4j/zeka/stack/idea/plugin/terminal/
│   ├── action/
│   │   └── TerminalAiGenerateAction.java      # 核心 Action 类
│   ├── settings/
│   │   ├── SettingsState.java                  # 配置持久化
│   │   ├── TerminalSettingsConfigurable.java   # 设置配置类
│   │   ├── PromptTemplateVersionNotifier.java  # 提示词版本通知
│   │   └── ui/
│   │       └── TerminalSettingsPanel.java      # 设置页面 UI
│   ├── statusbar/
│   │   └── TerminalStatusBarPopupProvider.java # 状态栏菜单
│   ├── terminal/
│   │   └── TerminalAiAllowedActionsProvider.java # 终端动作注册
│   ├── update/
│   │   └── TerminalPluginUpdateInfoProvider.java # 更新信息
│   ├── util/
│   │   ├── TerminalBundle.java                 # 国际化资源
│   │   └── NotificationUtil.java               # 通知工具
│   └── PluginContents.java                     # 插件信息常量
├── src/main/resources/
│   ├── icons/                                  # 图标资源
│   ├── messages/                               # 国际化资源文件
│   └── META-INF/
│       └── plugin.xml                          # 插件配置
├── site/docs/                                  # 文档目录
│   ├── 用户手册.md
│   └── 插件开发指南.md
├── build.gradle.kts                            # Gradle 构建脚本
└── README.md                                   # 本文件
```

## 7 ⚠️ 注意事项

### 7.1 依赖要求

- **IntelliJ IDEA 2025.3+**：插件需要 IntelliJ IDEA 2025.3 或更高版本
- **IntelliAI Engine 插件**：必须安装 IntelliAI Engine 插件并配置至少一个 AI 服务商
- **Java 21+**：需要 Java 21 或更高版本

### 7.2 使用限制

1. **AI 服务商配置**：使用前必须在 IntelliAI Engine 中配置并验证至少一个 AI 服务商
2. **网络连接**：AI 调用需要网络连接，确保网络畅通
3. **API 配额**：注意 AI 服务商的 API 配额限制
4. **命令准确性**：AI 生成的命令仅供参考，执行前请仔细检查，特别是涉及敏感操作的命令

### 7.3 开发注意事项

- **本地开发**：本地开发时，`buildAiCommonPlugin` 和 `copyAiCommonPlugin` 任务会自动处理 Engine 插件的构建和安装
- **发布流程**：发布到市场时，需要确保用户已安装 IntelliAI Engine 插件（通过 `plugin.xml` 中的 `<depends>` 声明）
- **线程安全**：所有耗时操作必须在后台线程执行，UI 更新必须在 EDT 线程

### 7.4 常见问题

#### 7.4.1 Q: 按 TAB 键没有反应？

A: 检查以下几点：

1. 确认已启用 Terminal AI（设置页面中勾选）
2. 确认当前行以触发前缀开头（默认 `#`）
3. 确认已配置并验证至少一个 AI 服务商
4. 查看 Engine Console 是否有错误日志

#### 7.4.2 Q: 生成的命令不正确？

A: 可以尝试：

1. 调整系统提示词，更明确地说明需求
2. 优化用户输入，使用更清晰的自然语言描述
3. 在设置页面勾选"显示高级设置"，自定义提示词模板

#### 7.4.3 Q: AI 调用失败？

A: 检查：

1. 网络连接是否正常
2. AI 服务商的 API Key 是否正确
3. API 配额是否充足
4. 查看 Engine Console 中的详细错误信息

## 8 📖 更多文档

- [用户手册](site/docs/用户手册.md)：详细的使用说明和配置指南
- [插件开发指南](site/docs/插件开发指南.md)：开发者文档和扩展指南

## 9 📝 许可证

MIT License

---

**Enjoy coding with AI! 🚀**
