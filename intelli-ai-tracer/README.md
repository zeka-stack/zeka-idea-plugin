# IntelliAI Tracer

基于 AI 的智能代码分析插件，自动分析 Java 方法调用链和业务流程，帮助开发者快速理解复杂系统。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2022.3+-blue.svg)](https://www.jetbrains.com/idea/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)

## 🎯 项目概述

IntelliAI Tracer 是一个基于 IntelliAI Engine 的 IntelliJ IDEA 插件，专注于 Java 代码的深度分析。插件能够自动提取方法调用关系、类依赖结构，并利用
AI 生成可视化时序图和业务逻辑说明，大幅提升代码理解和审查效率。

## ✨ 核心特性

### 🔍 智能代码分析

- **方法调用链分析**: 自动识别方法的上下游调用关系
- **类依赖分析**: 提取类之间的继承和组合关系
- **上下文提取**: 获取完整的方法上下文信息（类、注解、注释等）
- **元素类型识别**: 智能识别字段、方法、构造函数、注解等元素

### 🤖 AI 驱动的解释生成

- **业务逻辑分析**: AI 理解代码背后的业务逻辑和设计意图
- **时序图生成**: 自动生成标准化的 UML 时序图
- **技术文档生成**: 生成清晰的技术说明和架构文档
- **智能总结**: 提供代码结构的智能总结和优化建议

### 🎯 多样化触发方式

- **意图操作**: 在编辑器中 Alt+Enter 快速触发
- **菜单集成**: 集成到 IntelliJ IDEA 的菜单系统
- **快捷键支持**: 自定义快捷键快速访问
- **上下文感知**: 根据光标位置自动选择分析目标

### 🎨 可视化展示

- **调用链可视化**: 清晰展示方法调用关系
- **类关系图**: 可视化类的依赖和继承关系
- **层次化展示**: 按调用层级组织分析结果
- **交互式浏览**: 支持点击展开和详细信息查看

## 🚀 快速开始

### 系统要求

- **IntelliJ IDEA**: 2022.3 及以上版本
- **Java**: 17 及以上
- **项目类型**: Java 项目（支持 Maven、Gradle 等）
- **网络连接**: 需要连接 AI 服务提供商

### 安装步骤

#### 方式一：从 JetBrains Marketplace 安装（推荐）

1. 打开 IntelliJ IDEA
2. 进入 `File` → `Settings` → `Plugins`
3. 搜索 `IntelliAI Tracer`
4. 点击 `Install` 并重启 IDE

#### 方式二：手动安装

1. 从 [Releases](https://github.com/zeka-stack/zeka-idea-plugin/releases) 下载插件包
2. 进入 `File` → `Settings` → `Plugins` → ⚙️ → `Install Plugin from Disk...`
3. 选择下载的 ZIP 文件并安装

### 初始配置

#### 1. 安装依赖插件

确保已安装 **IntelliAI Engine** 插件：

- 插件会自动提示安装 IntelliAI Engine
- 或手动在 Marketplace 搜索并安装

#### 2. 配置 AI 服务

1. 打开 `File` → `Settings` → `Tools` → `IntelliAI Engine`
2. 点击 `+` 添加 AI 提供商
3. 推荐配置：
    - **通义千问**: 中文内容理解优秀
    - **OpenAI**: 技术文档生成质量高
    - **硅基流动**: 性价比和速度平衡

#### 3. 测试连接

配置完成后点击 `测试连接`，确保显示"连接成功"。

## 💻 使用指南

### 基本使用流程

#### 意图操作方式（推荐）

1. **定位目标代码**: 将光标放在要分析的代码元素上
2. **触发分析**: 按 `Alt+Enter`（Mac: `Option+Enter`）
3. **选择操作**: 在弹出的意图菜单中选择 `解释工作流`
4. **查看结果**: 在专用的工具窗口中查看分析结果

#### 菜单操作方式

1. **打开菜单**: `Tools` → `IntelliAI Tracer` → `解释工作流`
2. **自动识别**: 插件自动识别光标位置的有效元素
3. **执行分析**: 开始深度分析当前代码上下文

### 支持的代码元素

插件能够分析以下类型的代码元素：

#### 方法相关

- **实例方法**: 对象实例的方法调用
- **静态方法**: 类级别的静态方法调用
- **构造函数**: 对象创建时的构造调用
- **方法重载**: 自动识别重载方法的调用

#### 字段相关

- **实例字段**: 对象的成员变量访问
- **静态字段**: 类级别的静态变量
- **常量引用**: final 字段和枚举常量

#### 注解相关

- **类注解**: 类级别的注解信息
- **方法注解**: 方法上的注解信息
- **字段注解**: 字段上的注解信息
- **自定义注解**: 用户定义的注解类型

### 分析结果展示

#### 调用链分析

显示方法的完整调用关系：

```
当前方法: UserService.createUser()

调用链:
├── UserService.validateInput()        [直接调用]
├── UserMapper.insertUser()           [直接调用]
├── NotificationService.sendEmail()   [间接调用]
└── AuditLogger.logUserAction()      [间接调用]
```

#### 类关系分析

展示类之间的依赖关系：

```
类关系: UserService

继承关系:
└── AbstractUserService          [继承]

组合关系:
├── UserMapper                  [依赖注入]
├── NotificationService         [依赖注入]
└── AuditLogger               [依赖注入]

注解信息:
├── @Service                   [类注解]
├── @Transactional            [类注解]
└── @Autowired                [字段注解]
```

#### AI 生成文档

基于分析结果生成技术文档：

```
# UserService 业务逻辑分析

## 功能描述
UserService 负责用户创建、验证、通知和审计等核心业务逻辑。

## 调用流程
1. 输入验证 -> validateInput()
2. 数据持久化 -> insertUser()
3. 通知发送 -> sendEmail()
4. 审计记录 -> logUserAction()

## 设计模式
- Service Pattern: 业务逻辑封装
- Dependency Injection: 依赖注入管理
- Template Method: 标准化处理流程
```

## ⚙️ 配置选项

### 插件设置

打开 `File` → `Settings` → `Tools` → `IntelliAI Tracer`

#### 基础设置

- **默认 AI 提供商**: 选择常用的 AI 服务
- **分析深度**: 设置代码分析的深度级别
- **显示详细程度**: 控制分析结果的详细程度

#### 高级设置

##### 分析配置

**分析范围**:

- **当前类**: 仅分析当前类的关系
- **依赖包**: 扩展分析相关包中的类
- **项目全量**: 分析整个项目的类关系

**过滤设置**:

- **排除测试类**: 跳过测试代码的分析
- **排除生成类**: 跳过自动生成的代码
- **包含私有方法**: 是否分析私有方法调用

##### AI 配置

**提示词模板**:

```markdown
你是一位资深的系统架构师，擅长分析 Java 代码的业务逻辑和设计模式。

请分析以下代码信息：
- 方法调用关系
- 类依赖结构
- 注解和注释信息

要求：
1. 生成清晰的调用链描述
2. 识别使用的设计模式
3. 分析业务逻辑流程
4. 提供架构优化建议
```

**输出格式**:

- **调用链图**: 启用/禁用调用链可视化
- **时序图**: 启用/禁用时序图生成
- **技术文档**: 启用/禁用技术文档生成

### 快捷键配置

可以在 `Keymap` 设置中自定义快捷键：

- **解释工作流**: 默认 `Alt+Enter`
- **分析选中元素**: 自定义快捷键
- **显示分析结果**: 自定义快捷键

## 🎨 使用场景和最佳实践

### 代码理解场景

#### 遗留代码分析

**场景**: 接手维护遗留系统，需要快速理解代码结构

**操作步骤**:

1. 选择核心业务类中的关键方法
2. 执行工作流分析
3. 查看调用链和类关系
4. 阅读 AI 生成的业务逻辑说明

**最佳实践**:

- 从入口方法开始分析
- 逐步扩展分析范围
- 结合 AI 生成文档理解设计意图

#### 代码审查场景

**场景**: 代码审查时需要评估代码质量和设计

**操作步骤**:

1. 分析新提交代码的关键方法
2. 检查调用关系的合理性
3. 评估类依赖的耦合度
4. 参考 AI 提供的优化建议

**审查要点**:

- 调用链深度是否合理
- 是否存在循环依赖
- 设计模式使用是否恰当
- 业务逻辑是否清晰

### 架构设计场景

#### 技术文档生成

**场景**: 为项目生成技术文档和架构图

**操作步骤**:

1. 选择核心业务流程
2. 批量分析相关方法
3. 导出 AI 生成的时序图
4. 整理技术文档

**输出内容**:

- 系统架构图
- 业务流程说明
- 接口设计文档
- 数据流向分析

## 🔧 开发指南

### 项目结构

```
intelli-ai-tracer/
├── src/main/java/dev/dong4j/zeka/stack/idea/plugin/workflow/
│   ├── action/                    # 动作类
│   │   ├── ExplainWorkflowAction.java           # 主菜单动作
│   │   └── ExplainWorkflowIntentionAction.java # 意图动作
│   ├── service/                   # 服务层
│   │   ├── WorkflowExplainerService.java     # AI 解释服务
│   │   └── CallGraphBuilder.java            # 调用图构建
│   ├── model/                     # 数据模型
│   │   ├── WorkflowContext.java              # 工作流上下文
│   │   ├── MethodCallerChainContext.java   # 方法调用链上下文
│   │   ├── ClassRelationshipContext.java    # 类关系上下文
│   │   ├── MethodInfo.java                   # 方法信息
│   │   └── ClassInfo.java                    # 类信息
│   ├── ui/                        # UI 组件
│   │   ├── WorkflowResultToolWindow.java     # 结果工具窗口
│   │   └── MarkdownLinkHandler.java        # Markdown 处理器
│   ├── util/                      # 工具类
│   │   ├── PSIUtil.java                       # PSI 工具
│   │   ├── NotificationUtil.java              # 通知工具
│   │   ├── JSONSerializer.java               # JSON 序列化
│   │   └── LinkUtil.java                     # 链接工具
│   └── settings/                  # 设置相关
│       ├── SettingsState.java                  # 持久化配置
│       ├── TracerSettingsConfigurable.java  # 设置页面
│       └── ui/
│           └── TracerSettingsPanel.java      # 设置 UI 组件
├── src/main/resources/
│   ├── messages/          # 国际化资源
│   │   ├── WorkflowBundle.properties           # 英文
│   │   └── WorkflowBundle_zh_CN.properties    # 中文
│   ├── icons/            # 图标资源
│   └── intentionDescriptions/ # 意图描述
└── build.gradle.kts     # 构建配置
```

### 核心组件说明

#### Action 系统

- **ExplainWorkflowAction**: 主菜单触发的工作流分析
- **ExplainWorkflowIntentionAction**: 基于光标位置的智能意图
    - 自动检测光标位置的代码元素
    - 支持字段、方法、类、注解等多种类型
    - 异步执行避免 UI 阻塞

#### Service 层

- **WorkflowExplainerService**: 核心 AI 分析服务
    - 调用 IntelliAI Engine 进行内容生成
    - 处理不同类型工作流（单方法、调用链、类关系）
    - 流式响应处理和错误恢复

- **CallGraphBuilder**: 调用图构建工具
    - 基于 PSI 构建方法调用关系
    - 识别间接调用和递归调用
    - 生成标准化的调用链数据结构

#### 数据模型

- **WorkflowContext**: 统一的工作流上下文
- **MethodInfo**: 方法信息模型（参数、返回值、注解等）
- **ClassInfo**: 类信息模型（继承关系、字段、方法等）
- **MethodCallerChainContext**: 方法调用链上下文
- **ClassRelationshipContext**: 类关系上下文

#### UI 组件

- **WorkflowResultToolWindow**: 专用结果展示窗口
    - 支持 Markdown 格式化显示
    - 可点击的链接和交互元素
    - 刷新和导航功能

### 扩展点集成

插件注册到 IntelliAI Engine：

```xml
<!-- 注册到 intelli-ai-engine 的扩展点 -->
<extensions defaultExtensionNs="dev.dong4j.zeka.stack.idea.plugin.common.ai">
    <!-- AI 控制台日志提供者 -->
    <aiConsoleLoggerProvider implementation="dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowAIConsoleLoggerProvider"/>
</extensions>
```

### 本地开发

#### 环境准备

1. **克隆项目**:

```bash
git clone https://github.com/zeka-stack/zeka-idea-plugin.git
cd intelli-ai-tracer
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
./gradlew test --tests "WorkflowExplainerServiceTest"
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
- 项目类型和规模
- 光标位置和代码片段
- 分析期望结果 vs 实际结果
- 错误日志（如果有）

## 📊 性能和限制

### 性能考虑

- **异步处理**: AI 分析在后台线程执行，避免 UI 阻塞
- **PSI 缓存**: 智能缓存 PSI 分析结果
- **增量分析**: 支持增量分析大型项目

### 已知限制

- 仅支持 Java 代码分析
- 大型项目分析可能需要较长时间
- AI 生成质量依赖于代码的复杂度和清晰度
- 复杂的继承关系可能影响分析准确性

### 优化建议

1. **提高分析效率**:
    - 在关键业务逻辑上使用
    - 避免在过于简单的代码上分析
    - 合理设置分析深度参数

2. **获得更好的结果**:
    - 保持代码结构清晰
    - 添加有意义的注释和文档
    - 使用标准的设计模式

3. **项目结构优化**:
    - 合理的包结构划分
    - 避免过深的调用层次
    - 减少不必要的依赖关系

## 📚 相关资源

### 文档

- [功能设计](./docs/) - 插件功能设计文档
- [插件实现方案](./docs/插件实现方案.md) - 技术实现方案
- [性能问题分析](./docs/性能问题分析.md) - 性能优化相关

### 技术文档

- [设置页面实现](./docs/设置页面与提示词管理实现方案.md) - 设置系统实现
- [流式响应实现](./docs/流式响应实现方案.md) - AI 流式响应处理
- [多场景扩展方案](./docs/多场景工作流生成扩展方案.md) - 扩展场景设计

### 外部资源

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliAI Engine](../intelli-ai-engine/) - AI 能力基础引擎
- [PSI 开发指南](https://plugins.jetbrains.com/docs/intellij/psi.html) - PSI 编程指南

## 📄 许可证

本项目基于 Apache License 2.0 开源许可证。详见 [LICENSE](../LICENSE) 文件。

## 🙏 致谢

特别感谢以下项目和贡献者：

- [IntelliJ Platform](https://www.jetbrains.com/idea/) - 优秀的开发平台和 PSI 框架
- 所有 AI 服务提供商 - 提供强大的代码分析和理解能力
- 社区贡献者 - 持续改进和功能建议

---

**JetBrains Marketplace**: [IntelliAI Tracer](https://plugins.jetbrains.com/plugin/dev.dong4j.zeka.stack.idea.plugin.workflow)

**问题反馈**: [GitHub Issues](https://github.com/zeka-stack/zeka-idea-plugin/issues)

**联系方式**: dong4jj@gmail.com