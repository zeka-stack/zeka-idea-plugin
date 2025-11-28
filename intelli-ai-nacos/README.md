# IntelliAI Nacos

Nacos 配置管理增强插件，为 IntelliJ IDEA 提供便捷的 Nacos 配置中心连接、浏览、编辑和管理能力。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2022.3+-blue.svg)](https://www.jetbrains.com/idea/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)

## 🎯 项目概述

IntelliAI Nacos 是一个基于 IntelliAI Engine 的 IntelliJ IDEA 插件，专门用于增强 Nacos 配置中心的使用体验。插件提供了完整的可视化界面，支持配置的拉取、发布、对比、删除等核心操作，并支持内置本地
Nacos 注册中心。

## ✨ 核心特性

### 🔌 Nacos 连接管理

- **多服务器支持**: 可配置和管理多个 Nacos 服务器连接
- **安全认证**: 支持用户名密码认证和管理员权限识别
- **连接检测**: 一键测试连接状态并自动识别权限级别
- **本地注册中心**: 内置 Nacos 2.x 服务，支持一键启动/停止

### 📁 可视化配置管理

- **三级树视图**: 按 Namespace → Group → DataId 层次展示配置
- **智能搜索**: 支持按 Namespace、Group、DataId 快速搜索
- **多标签编辑**: 支持同时编辑多个配置文件
- **语法高亮**: 自动识别 YAML、JSON、XML、HTML 格式并提供高亮

### 🔄 配置操作增强

- **拉取配置**: 从 Nacos 拉取配置到编辑器
- **发布配置**: 将编辑器内容发布到 Nacos
- **配置对比**: 对比本地编辑内容和远程配置，显示差异
- **批量操作**: 支持批量删除 Group 或 Namespace 下的配置

### 🤖 AI 能力集成

- **智能配置生成**: 基于 AI 生成标准化配置内容
- **配置建议**: AI 分析项目结构提供配置优化建议
- **批量智能操作**: AI 辅助的批量配置管理

### 🎨 用户体验优化

- **右键菜单集成**: 项目文件直接右键发布到 Nacos
- **意图操作**: YAML 文件中 Alt+Enter 快速发布
- **实时状态反馈**: 操作状态实时显示
- **多格式支持**: YAML、JSON、XML、HTML、Text 格式自动识别

## 🚀 快速开始

### 系统要求

- **IntelliJ IDEA**: 2022.3 及以上版本
- **Java**: 17 及以上
- **Nacos**: 2.x 服务器（本地使用可选）
- **网络连接**: 连接远程 Nacos 需要

### 安装步骤

#### 方式一：从 JetBrains Marketplace 安装（推荐）

1. 打开 IntelliJ IDEA
2. 进入 `File` → `Settings` → `Plugins`
3. 搜索 `IntelliAI Nacos`
4. 点击 `Install` 并重启 IDE

#### 方式二：手动安装

1. 从 [Releases](https://github.com/zeka-stack/zeka-idea-plugin/releases) 下载插件包
2. 进入 `File` → `Settings` → `Plugins` → ⚙️ → `Install Plugin from Disk...`
3. 选择下载的 ZIP 文件并安装

### 初始配置

#### 1. 配置 Nacos 连接

1. 打开 `File` → `Settings` → `Tools` → `IntelliAI Nacos`
2. 配置 Nacos 服务器信息：
    - **服务器地址**: 如 `http://localhost:8848/nacos`
    - **用户名**: Nacos 用户名
    - **密码**: 对应密码
3. 点击 `测试连接` 验证配置
4. 如果是管理员，勾选 `全局管理员权限`

#### 2. 启动本地 Nacos（可选）

1. 在设置页面勾选 `使用本地 Nacos 注册中心`
2. 点击 `启动` 按钮
3. 等待插件自动下载并启动内置 Nacos
4. 启动成功后访问 `http://127.0.0.1:8848/nacos/index.html`

**本地 Nacos 特性:**

- 自动下载 Nacos 2.x 服务包
- 一键启动/停止管理
- 进程状态实时监控
- IDE 关闭时自动清理

## 💻 使用指南

### 工具窗口使用

1. **打开工具窗口**: `View` → `Tool Windows` → `Nacos`
2. **浏览配置**: 在左侧配置树中按层级浏览
3. **编辑配置**: 双击 DataId 节点加载到编辑器
4. **执行操作**: 使用工具栏按钮进行配置操作

#### 工具栏功能

- 🔄 **Refresh**: 重新加载配置树和状态
- ➕ **Add Tab**: 创建新的配置编辑标签
- ❌ **Close Tab**: 关闭当前编辑标签
- ⚙️ **Settings**: 打开插件设置
- ❓ **Help**: 打开 Nacos 官方文档

### 配置操作详解

#### 拉取配置（Pull）

1. 在配置树中选择要拉取的配置
2. 点击 `Pull` 按钮或双击节点
3. 配置内容加载到右侧编辑器
4. 状态栏显示拉取进度和结果

#### 发布配置（Push）

1. 在编辑器中修改配置内容
2. 点击 `Push` 按钮发布到 Nacos
3. 系统自动识别配置类型并设置相应语法高亮
4. 发布成功后清除修改标记

#### 对比配置（Compare）

1. 在编辑器中修改配置内容
2. 点击 `Compare` 按钮对比 Nacos 中的原配置
3. 使用 IntelliJ 的 Diff 工具显示差异
4. 支持手动编辑和保存更改

### 右键菜单集成

#### 项目文件右键

1. 在项目目录或配置文件上右键
2. 选择 `发布到 Nacos`
3. 在弹出框中填写：
    - **Namespace**: 命名空间（默认从文件路径推断）
    - **Group**: 分组（默认为 `DEFAULT_GROUP`）
    - **DataId**: 数据 ID（默认为文件名）
4. 确认发布

#### 配置树右键

1. 在配置树节点上右键
2. 选择删除操作
3. 管理员可以批量删除整个 Group 或 Namespace
4. 普通用户只能删除自己创建的配置

### 意图操作（Intention）

#### 发布配置意图

1. 在 YAML 配置文件中按 `Alt+Enter`（Mac: `Option+Enter`）
2. 选择 `发布到 Nacos`
3. 自动填充配置信息并发布

#### 对比配置意图

1. 在配置编辑器中按 `Alt+Enter`（Mac: `Option+Enter`）
2. 选择 `对比配置`
3. 输入目标配置信息进行对比

## ⚙️ 高级配置

### 插件设置

打开 `File` → `Settings` → `Tools` → `IntelliAI Nacos`

#### 连接设置

- **服务器地址**: Nacos 服务器完整地址
- **认证信息**: 用户名和密码
- **权限级别**: 自动检测或手动指定
- **连接超时**: 网络请求超时时间

#### 本地 Nacos 设置

- **启用本地服务**: 勾选启用内置 Nacos
- **端口配置**: 自定义本地服务端口
- **数据目录**: 本地 Nacos 数据存储位置
- **启动参数**: JVM 启动参数配置

#### AI 集成设置

- **AI 提供商**: 选择使用的 AI 服务
- **智能生成**: 启用 AI 辅助配置生成
- **模板配置**: 自定义 AI 生成模板

### 配置格式支持

插件自动识别并支持以下格式：

| 格式             | 文件扩展名           | 特性        |
|----------------|-----------------|-----------|
| **YAML**       | `.yml`, `.yaml` | 语法高亮、自动缩进 |
| **JSON**       | `.json`         | 格式化、错误检查  |
| **XML**        | `.xml`          | 标签补全、格式化  |
| **HTML**       | `.html`         | 标签补全、预览   |
| **Properties** | `.properties`   | 键值对编辑     |

## 🎨 使用场景和最佳实践

### 开发环境管理

**场景**: 本地开发时需要频繁修改配置

**最佳实践**:

- 使用本地 Nacos 注册中心避免网络依赖
- 配置自动同步机制
- 利用配置对比跟踪变更

**操作流程**:

1. 启动本地 Nacos 服务
2. 连接本地注册中心
3. 使用工具窗口管理配置
4. 测试完成后发布到远程

### 团队配置协作

**场景**: 多人协作管理项目配置

**最佳实践**:

- 建立统一的 Namespace 命名规范
- 使用 Group 进行环境分类（dev/test/prod）
- 利用权限管理控制访问范围

**协作流程**:

1. 定义配置管理规范
2. 分配不同环境的 Namespace
3. 使用插件进行配置变更
4. 通过版本控制跟踪配置历史

### 配置迁移和备份

**场景**: 环境间配置迁移或备份重要配置

**操作步骤**:

1. 使用批量导出功能
2. 利用配置对比验证差异
3. 分批迁移到目标环境
4. 建立定期备份机制

## 🔧 开发指南

### 项目结构

```
intelli-ai-nacos/
├── src/main/java/dev/dong4j/zeka/stack/idea/plugin/nacos/
│   ├── action/               # 动作类
│   │   ├── AbstractNacosAction.java           # 基础动作类
│   │   ├── RefreshAction.java               # 刷新动作
│   │   ├── SettingAction.java               # 设置动作
│   │   └── NacosHelpAction.java           # 帮助动作
│   ├── client/               # Nacos 客户端
│   │   ├── NacosClient.java                 # 客户端接口
│   │   ├── NacosClientUtils.java            # 客户端工具类
│   │   └── model/                          # 数据模型
│   ├── local/                # 本地 Nacos 管理
│   │   ├── LocalNacosService.java           # 本地服务管理
│   │   └── LocalNacosAppLifecycleListener.java # 生命周期监听
│   ├── service/               # 服务层
│   │   ├── CompareConfigService.java        # 配置对比服务
│   │   ├── UrlTestManager.java              # 连接测试服务
│   │   └── manager/                         # 管理器
│   ├── ui/                   # UI 组件
│   │   ├── toolwindow/                     # 工具窗口
│   │   │   ├── NacosToolWindow.java       # 主工具窗口
│   │   │   ├── TreePanel.java            # 配置树面板
│   │   │   └── ConfigOperationPanel.java # 操作面板
│   │   ├── settings/                      # 设置界面
│   │   └── components/                   # 自定义组件
│   ├── util/                 # 工具类
│   │   ├── NotificationUtil.java            # 通知工具
│   │   ├── YamlUtils.java                 # YAML 工具
│   │   └── CacheUtils.java                 # 缓存工具
│   ├── entity/               # 实体类
│   ├── settings/             # 设置相关
│   │   ├── SettingsState.java              # 持久化配置
│   │   ├── NacosSettingsConfigurable.java # 设置页面
│   │   └── ui/                            # 设置 UI
│   └── icons/                # 图标资源
├── src/main/resources/
│   ├── messages/            # 国际化资源
│   ├── icons/               # 图标文件
│   └── META-INF/
│       └── plugin.xml       # 插件配置
└── build.gradle.kts       # 构建配置
```

### 核心组件说明

#### Nacos 客户端

- **NacosClient**: 封装 Nacos Open API 调用
- **NacosClientUtils**: 提供便捷的客户端操作方法
- **模型类**: 定义请求和响应的数据结构

#### 本地 Nacos 管理

- **LocalNacosService**: 管理本地 Nacos 服务的启动/停止
- **PortManager**: 端口占用检测和管理
- **RegistryLogger**: 本地服务日志管理

#### UI 系统

- **NacosToolWindow**: 主工具窗口，集成配置树和编辑器
- **TreePanel**: 配置树展示，支持搜索和筛选
- **ConfigOperationPanel**: 配置操作面板（Pull/Push/Compare）

### 扩展点集成

插件注册到 IntelliAI Engine：

```xml
<!-- 注册到 intelli-ai-engine 的扩展点 -->
<extensions defaultExtensionNs="dev.dong4j.zeka.stack.idea.plugin.common.ai">
    <!-- AI 控制台日志提供者 -->
    <aiConsoleLoggerProvider implementation="dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosAIConsoleLoggerProvider"/>
</extensions>
```

### 本地开发

#### 环境准备

1. **克隆项目**:

```bash
git clone https://github.com/zeka-stack/zeka-idea-plugin.git
cd intelli-ai-nacos
```

2. **构建插件**:

```bash
./gradlew buildPlugin
```

3. **运行调试**:

```bash
./gradlew runIde
```

#### 依赖管理

插件依赖 IntelliAI Engine，本地开发时自动处理：

```kotlin
// 自动构建和安装 engine 插件
./gradlew runIde  // 会自动执行 buildAiCommonPlugin 和 copyAiCommonPlugin
```

#### 测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "NacosClientTest"
```

## 🔌 API 文档

### Nacos 客户端 API

插件提供了简化的 Nacos 客户端 API：

```java
// 获取客户端实例
NacosClient client = NacosClient.getInstance(serverAddr, username, password);

// 获取配置
String config = client.getConfig(dataId, group, namespace);

// 发布配置
boolean success = client.publishConfig(dataId, group, namespace, content);

// 删除配置
boolean deleted = client.removeConfig(dataId, group, namespace);

// 获取配置列表
List<ConfigInfo> configs = client.getConfigs(namespace, group);
```

### 事件监听

插件支持多种事件监听：

```java
// 配置变更监听
NacosClient.addListener(dataId, group, namespace, listener -> {
    System.out.println("Config changed: " + listener.getContent());
});
```

## 🤝 贡献指南

### 开发流程

1. **Fork 项目**到你的 GitHub 账户
2. **创建特性分支**:
   ```bash
   git checkout -b feature/new-feature
   ```
3. **编写代码**和测试用例
4. **运行测试**确保通过:
   ```bash
   ./gradlew test verifyPlugin
   ```
5. **提交代码**:
   ```bash
   git commit -m "feat: add new feature"
   ```
6. **推送分支**:
   ```bash
   git push origin feature/new-feature
   ```
7. **创建 Pull Request**

### 代码规范

- **Java 代码**: 使用 Google Java Format
- **提交信息**: 遵循 Conventional Commits 规范
- **测试覆盖**: 新功能必须包含单元测试
- **文档更新**: 重要变更需要更新相关文档

### 问题报告

报告 Bug 时请包含：

- IntelliJ IDEA 版本
- 插件版本
- Nacos 服务器版本
- 操作系统和 Java 版本
- 详细的重现步骤
- 错误日志（如果有）

## 📊 性能和限制

### 性能优化

- **配置缓存**: 本地缓存常用配置减少网络请求
- **异步加载**: 配置树采用异步加载避免 UI 阻塞
- **批量操作**: 优化批量配置操作的网络请求

### 已知限制

- 仅支持 Nacos 2.x 服务器
- 大型配置文件可能影响性能
- 本地 Nacos 仅支持单实例运行
- 网络环境要求稳定连接

### 安全考虑

- **凭据安全**: 密码使用 IntelliJ 凭据管理器加密存储
- **权限控制**: 严格区分管理员和普通用户权限
- **网络安全**: 支持 HTTPS 连接和证书验证

## 📚 相关资源

### 文档

- [用户手册](./docs/用户手册.md) - 详细的使用说明和最佳实践
- [功能需求说明](./docs/Nacos插件功能需求说明.md) - 功能需求文档
- [实现方案](./docs/Nacos插件实现方案.md) - 技术实现方案

### 技术文档

- [本地Nacos实现](./docs/本地Nacos注册中心功能实现方案.md) - 本地服务实现
- [配置对比增强](./docs/配置对比增强方案.md) - 配置对比功能
- [功能完善方案](./docs/IntelliAI%20Nacos遗留功能完善方案.md) - 功能增强计划

### 外部资源

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliAI Engine](../intelli-ai-engine/) - AI 能力基础引擎
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html) - Nacos 服务文档

## 📄 许可证

本项目基于 Apache License 2.0 开源许可证。详见 [LICENSE](../LICENSE) 文件。

## 🙏 致谢

特别感谢以下项目和贡献者：

- [Nacos](https://nacos.io/) - 优秀的配置中心服务
- [IntelliJ Platform](https://www.jetbrains.com/idea/) - 强大的开发平台
- 所有测试者和反馈用户 - 持续改进和优化

---

**JetBrains Marketplace**: [IntelliAI Nacos](https://plugins.jetbrains.com/plugin/29156)

**问题反馈**: [GitHub Issues](https://github.com/zeka-stack/zeka-idea-plugin/issues)

**联系方式**: dong4jj@gmail.com