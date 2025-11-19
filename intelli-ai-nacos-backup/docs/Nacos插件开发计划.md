# intelli-ai-nacos 插件开发计划

## 项目概述

基于 `template-with-ai` 模板，使用最新的 IntelliJ Platform API 重新实现 Nacos 配置管理插件。

## 开发计划（分为 7 个阶段）

### 阶段一：项目基础搭建（预计 1 天）

#### 1.1 项目初始化

- [ ] 从 template 复制基础项目结构到 intelli-ai-nacos
- [ ] 修改包名从 `com.example.plugin` 到 `dev.dong4j.zeka.stack.idea.plugin.nacos`
- [ ] 更新 gradle.properties 中的项目信息（插件 ID、名称、版本）
- [ ] 配置 Nacos Client 依赖（版本 2.x.x）

#### 1.2 基础配置

- [ ] 更新 plugin.xml 插件配置
- [ ] 实现基础的 `NacosBundle` 国际化资源管理
- [ ] 实现 `NotificationUtil` 通知工具类
- [ ] 配置构建脚本，确保能正常构建和运行

#### 1.3 开发环境验证

- [ ] 验证插件能在 IntelliJ 中正常加载
- [ ] 确保依赖配置正确
- [ ] 测试基础功能是否正常

### 阶段二：配置管理和设置页面（预计 1.5 天）

#### 2.1 配置状态管理

- [ ] 实现 `SettingsState` 配置持久化
- [ ] 包含：serverAddr、username、type、globalAdmin、isAuthed 字段
- [ ] 使用 `CredentialStore` 存储密码
- [ ] 实现配置加载和保存逻辑

#### 2.2 设置页面 UI

- [ ] 实现 `NacosSettingsConfigurable` 配置页面
- [ ] 设计 `NacosSettingsPanel` UI 界面
- [ ] 包含：服务器地址、用户名、密码输入框，测试连接按钮
- [ ] 实现输入验证逻辑

#### 2.3 连接测试功能

- [ ] 实现 Nacos 连接测试逻辑
- [ ] 验证用户名密码和权限
- [ ] 自动获取 Namespace 列表
- [ ] 显示连接结果和权限信息

### 阶段三：Nacos 客户端和数据模型（预计 2 天）

#### 3.1 Nacos 客户端实现

- [ ] 实现 `NacosClient` 核心客户端类
- [ ] 实现 `NacosClientUtils` 工具类
- [ ] 支持多种 Nacos 服务器连接管理
- [ ] 实现客户端缓存机制

#### 3.2 HTTP 代理和认证

- [ ] 实现 `ConsumServerHttpAgent` HTTP 代理
- [ ] 实现 `ConsumSecurityProxy` 认证代理
- [ ] 处理 HTTP 请求和响应
- [ ] 实现 Nacos 2.x 认证机制

#### 3.3 数据模型

- [ ] 实现 `ConfigInfo` 配置信息模型
- [ ] 实现 `ConfigInfoWrapper` 配置包装器
- [ ] 实现 `Namespace` 命名空间模型
- [ ] 实现其他必要的数据结构

#### 3.4 API 操作实现

- [ ] 获取 Namespace 列表：`GET /v1/console/namespaces`
- [ ] 获取所有配置：`GET /v1/cs/configs?show=all`
- [ ] 拉取配置：`GET /v1/cs/configs`
- [ ] 发布配置：`POST /v1/cs/configs`
- [ ] 删除配置：`DELETE /v1/cs/configs`

### 阶段四：UI 组件和工具窗口（预计 3 天）

#### 4.1 Tool Window 基础结构

- [ ] 实现 `NacosToolWindowFactory` 工具窗口工厂
- [ ] 实现 `ToolBarPanel` 工具栏面板
- [ ] 实现 `NacosToolWindow` 主工具窗口

#### 4.2 配置树组件

- [ ] 实现 `TreePanel` 配置树面板
- [ ] 实现 Namespace → Group → DataId 三级树形结构
- [ ] 支持节点展开/收拢、双击加载配置
- [ ] 实现树节点的右键菜单功能

#### 4.3 Tab 和编辑器组件

- [ ] 实现 `TabBar` 和 `Tab` 组件
- [ ] 实现多 Tab 编辑管理
- [ ] 实现 `JsonEditor` 配置编辑器
- [ ] 支持 YAML、JSON、XML、HTML、Text 格式

#### 4.4 配置操作面板

- [ ] 在每个 Tab 中实现配置操作区域
- [ ] 实现 Namespace/Group/DataId 输入框（支持自动完成）
- [ ] 实现 Pull/Push/Compare 操作按钮
- [ ] 实现状态提示标签

### 阶段五：Action 和功能集成（预计 2.5 天）

#### 5.1 工具栏 Action

- [ ] 实现 `RefreshAction` 刷新功能
- [ ] 实现 `AddTabAction` 添加 Tab 功能
- [ ] 实现 `CloseTabAction` 关闭 Tab 功能
- [ ] 实现 `SettingAction` 打开设置功能
- [ ] 实现 `NacosHelpAction` 帮助功能

#### 5.2 右键菜单 Action

- [ ] 实现 `PublishConfigByMenuAction` 发布配置功能
- [ ] 识别项目中的配置文件（application*.yml）
- [ ] 实现批量发布到多个 Namespace
- [ ] 从 bootstrap.yml 读取 Group 配置

#### 5.3 Intention Action

- [ ] 实现 `PublishConfigIntentionAction` 发布配置意图
- [ ] 实现 `CompareConfigIntentionAction` 对比配置意图
- [ ] 在 YAML 文件中触发快速操作
- [ ] 集成到 IntelliJ 的意图系统

#### 5.4 配置对比服务

- [ ] 实现 `CompareConfigService` 对比服务
- [ ] 使用 IntelliJ Diff 工具显示差异
- [ ] 支持双向对比和编辑
- [ ] 实现配置差异高亮

### 阶段六：工具类和优化（预计 1 天）

#### 6.1 工具类实现

- [ ] 实现 `YamlUtils` YAML 解析工具
- [ ] 实现 `ConfigFile` 配置文件实体
- [ ] 实现其他必要的工具类

#### 6.2 国际化和资源

- [ ] 完善中英文资源文件
- [ ] 所有用户可见文本使用国际化
- [ ] 实现多语言切换支持

#### 6.3 性能优化和错误处理

- [ ] 实现配置缓存机制
- [ ] 优化网络请求性能
- [ ] 完善错误处理和用户提示
- [ ] 实现线程安全（UI 在 EDT，网络在后台）

### 阶段七：测试和文档（预计 1 天）

#### 7.1 单元测试

- [ ] 编写核心业务逻辑单元测试
- [ ] 测试 Nacos 客户端操作
- [ ] 测试配置管理功能
- [ ] 确保 60% 以上测试覆盖率

#### 7.2 集成测试

- [ ] 测试主要功能流程
- [ ] 测试与 Nacos 服务器的集成
- [ ] 测试在不同 IntelliJ 版本下的兼容性

#### 7.3 文档完善

- [ ] 完善插件使用文档
- [ ] 更新开发文档
- [ ] 创建示例和教程

## 总体时间安排

- **总计预计时间**：约 12 个工作日
- **关键里程碑**：
    - 阶段二完成：基础配置和设置页面可用
    - 阶段三完成：Nacos 客户端功能可用
    - 阶段四完成：UI 界面基本可用
    - 阶段五完成：所有主要功能实现

## 技术栈和依赖

- **开发框架**：IntelliJ Platform SDK（最新版本）
- **构建工具**：Gradle Kotlin DSL
- **Nacos 客户端**：com.alibaba.nacos:nacos-client:2.x.x
- **UI 组件**：IntelliJ Platform UI Components
- **开发语言**：Java 17+

## 风险评估和应对

### 主要风险

1. **API 兼容性**：新版本 IntelliJ API 可能有重大变化
2. **Nacos 2.x 兼容性**：与旧版本 API 的差异
3. **性能问题**：大量配置加载可能影响 IDE 性能

### 应对措施

1. **API 兼容性**：参考最新 IntelliJ Platform 文档，使用兼容性注解
2. **Nacos 兼容性**：充分测试 Nacos 2.x API，做好降级处理
3. **性能问题**：实现分页加载、缓存机制、异步处理

## 项目结构

```
intelli-ai-nacos/
├── src/main/java/dev/dong4j/zeka/stack/idea/plugin/nacos/
│   ├── action/              # Action 类（7个）
│   │   ├── PublishConfigIntentionAction.java
│   │   ├── CompareConfigIntentionAction.java
│   │   ├── PublishConfigByMenuAction.java
│   │   ├── RefreshAction.java
│   │   ├── AddTabAction.java
│   │   ├── CloseTabAction.java
│   │   ├── SettingAction.java
│   │   └── NacosHelpAction.java
│   ├── client/              # Nacos 客户端（5个）
│   │   ├── NacosClient.java
│   │   ├── NacosClientUtils.java
│   │   ├── ConsumServerHttpAgent.java
│   │   ├── ConsumSecurityProxy.java
│   │   └── model/
│   │       ├── ConfigInfo.java
│   │       ├── ConfigInfoBase.java
│   │       ├── ConfigInfoWrapper.java
│   │       └── Namespace.java
│   ├── configuration/       # 配置管理（2个）
│   │   ├── SettingsState.java
│   │   └── NacosSettingsConfigurable.java
│   ├── service/             # 服务类（1个）
│   │   └── CompareConfigService.java
│   ├── ui/                  # UI 组件（10+个）
│   │   ├── toolwindow/
│   │   │   ├── NacosToolWindowFactory.java
│   │   │   ├── ToolBarPanel.java
│   │   │   ├── TreePanel.java
│   │   │   ├── TabBar.java
│   │   │   ├── Tab.java
│   │   │   └── NacosToolWindow.java
│   │   ├── settings/
│   │   │   └── NacosSettingsPanel.java
│   │   └── components/
│   │       └── JsonEditor.java
│   ├── util/                # 工具类（3个）
│   │   ├── YamlUtils.java
│   │   ├── NotificationUtil.java
│   │   └── NacosBundle.java
│   └── entity/              # 实体类（1个）
│       └── ConfigFile.java
├── src/main/resources/
│   ├── META-INF/
│   │   └── plugin.xml
│   ├── messages.properties
│   ├── messages_zh_CN.properties
│   └── icons/
│       └── nacos.svg
└── docs/                    # 文档目录
    ├── Nacos插件功能需求说明.md
    ├── Nacos插件实现方案.md
    └── Nacos插件开发计划.md
```

## 功能特性

### 核心功能

1. **Nacos 配置管理**：在 IDE 中直接管理 Nacos 配置中心
2. **多入口点**：工具窗口、右键菜单、意图操作
3. **可视化界面**：配置树、编辑器、对比工具
4. **智能识别**：自动识别项目配置文件

### 技术特点

1. **现代化架构**：基于最新 IntelliJ Platform API
2. **高性能**：异步处理、缓存机制
3. **用户友好**：直观的界面、实时反馈
4. **可扩展**：模块化设计、易于维护

## 开发注意事项

1. **线程安全**：UI 操作在 EDT 线程，网络请求在后台线程
2. **错误处理**：友好的错误提示，完善的异常处理
3. **性能优化**：避免阻塞 UI，使用异步加载
4. **兼容性**：确保在不同 IntelliJ 版本下的兼容性
5. **安全性**：密码使用 CredentialStore 存储，不在配置文件中明文存储

## 版本规划

### v1.0.0（基础版）

- 基础配置管理
- Nacos 客户端功能
- 简单的配置操作

### v1.1.0（完整版）

- 完整的 UI 界面
- 所有 Action 功能
- 配置对比功能

### v1.2.0（优化版）

- 性能优化
- 错误处理完善
- 用户体验改进

### v2.0.0（增强版）

- 配置历史版本管理
- 配置变更通知
- 批量导入/导出功能