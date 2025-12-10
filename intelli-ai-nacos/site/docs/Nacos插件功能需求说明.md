# Nacos 插件功能需求说明

## 1. 项目概述

### 1.1 项目背景

这是一个 IntelliJ IDEA 插件，用于在 IDE 中直接管理和操作 Nacos 配置中心。插件提供了可视化的配置管理界面，支持配置的拉取、发布、对比、删除等操作，极大提升了开发效率。

### 1.2 技术栈

- **开发框架**: IntelliJ Platform SDK
- **构建工具**: Gradle (Kotlin DSL)
- **Nacos 客户端**: Nacos Client API
- **UI 框架**: IntelliJ Platform UI Components
- **语言**: Java 17+

## 2. 功能需求

### 2.1 设置页面 (Settings)

#### 2.1.1 配置项

- **Nacos 服务器地址** (serverAddr)
    - 必填项
    - 格式：`http://host:port` 或 `https://host:port`
    - 支持输入验证

- **用户名** (username)
    - 必填项
    - Nacos 认证用户名

- **密码** (password)
    - 必填项
    - 使用 IntelliJ 密码管理器存储（CredentialStore）
    - 不在配置文件中明文存储

#### 2.1.2 功能

- **测试连接按钮**
    - 验证 Nacos 服务器连接
    - 验证用户名密码是否正确
    - 检测用户权限（是否为全局管理员）
    - 连接成功后自动获取所有 Namespace 列表
    - 显示连接结果（成功/失败）和权限信息

- **配置持久化**
    - 使用 `PersistentStateComponent` 存储配置
    - 服务器地址和用户名存储在 XML 配置文件中
    - 密码存储在 IntelliJ 密码管理器中

- **认证状态管理**
    - `isAuthed`: 是否已认证
    - `globalAdmin`: 是否为全局管理员
    - 影响删除配置的权限控制

#### 2.1.3 UI 布局

- 使用 `FormBuilder` 构建表单布局
- 服务器地址输入框
- 用户名输入框
- 密码输入框（密码类型）
- 测试连接按钮
- 帮助链接（可选）

### 2.2 Tool Window (工具窗口)

#### 2.2.1 窗口布局

- **左侧面板**: 配置树形结构
    - 按 `Namespace` → `Group` → `DataId` 三级分组
    - 支持展开/收拢所有节点
    - 支持搜索过滤
    - 双击 DataId 节点可加载配置到右侧编辑器

- **右侧面板**: 配置编辑区域
    - 支持多 Tab 编辑
    - 每个 Tab 独立编辑一个配置
    - Tab 可拖拽排序
    - Tab 可关闭

#### 2.2.2 工具栏功能

- **刷新按钮**: 刷新左侧配置树和所有 Tab
- **添加 Tab 按钮**: 创建新的配置编辑 Tab
- **删除 Tab 按钮**: 关闭当前 Tab（每个 Tab 上有关闭按钮）
- **打开设置按钮**: 打开插件设置页面
- **展开全部按钮**: 展开左侧树的所有节点
- **收拢全部按钮**: 收拢左侧树的所有节点
- **帮助按钮**: 打开帮助文档

#### 2.2.3 配置编辑器

- **编辑器组件**: 使用 `EditorTextField` 实现
- **支持的文件类型**:
    - YAML (默认)
    - JSON
    - XML
    - HTML
    - Text
- **编辑器功能**:
    - 语法高亮
    - 代码折叠
    - 行号显示
    - 缩进指南
    - 自动格式化

#### 2.2.4 配置操作区域

每个 Tab 包含以下组件：

- **Namespace 选择框**
    - 支持输入和自动完成
    - 从 Nacos 获取所有 Namespace 列表
    - 支持 "public" 命名空间（空字符串）

- **Group 输入框**
    - 支持输入和自动完成
    - 根据选中的 Namespace 自动加载该 Namespace 下的所有 Group
    - 支持输入验证

- **DataId 输入框**
    - 支持输入和自动完成
    - 根据选中的 Namespace 和 Group 自动加载该 Group 下的所有 DataId
    - 支持输入验证

- **Pull 按钮** (拉取配置)
    - 从 Nacos 拉取指定配置
    - 显示加载状态
    - 拉取成功后填充到编辑器
    - 显示操作结果（成功/失败）和耗时

- **Push 按钮** (发布配置)
    - 将编辑器中的配置发布到 Nacos
    - 根据 DataId 后缀自动识别配置类型（yml/yaml/json/xml/html/txt）
    - 显示加载状态
    - 显示操作结果（成功/失败）和耗时

- **Compare 按钮** (对比配置)
    - 对比编辑器中的配置和 Nacos 中的配置
    - 使用 IntelliJ 的 Diff 工具显示差异
    - 左侧显示 Nacos 配置（只读）
    - 右侧显示本地配置（可编辑）

- **状态提示标签**
    - 显示操作状态（Requesting... / Success / Error）
    - 成功时显示绿色，失败时显示红色
    - 显示操作耗时

#### 2.2.5 配置树功能

- **树形结构**
    - 根节点：Namespace
    - 二级节点：Group（按字母排序）
    - 三级节点：DataId（按字母排序）
    - 支持多级展开/收拢

- **节点操作**
    - **双击 DataId 节点**: 加载配置到当前 Tab（如果没有 Tab 则创建新 Tab）
    - **右键菜单**:
        - 删除配置（单个或批量）
        - 管理员可以批量删除 Group 或 Namespace 下的所有配置
        - 非管理员只能删除当前用户创建的配置

- **节点标识**
    - 如果 Namespace 不存在但配置存在，显示 `[可删除]` 标识

- **搜索功能**
    - 支持在配置树中搜索
    - 搜索关键词匹配 Namespace、Group、DataId

### 2.3 右键菜单功能

#### 2.3.1 项目视图右键菜单

- **菜单项**: "发布到 Nacos"
- **触发条件**:
    - 已配置 Nacos 连接信息
    - 在项目目录或配置文件上右键
- **功能**:
    - 弹出对话框选择要发布到的 Namespace（支持多选）
    - 自动识别项目中的配置文件
    - 从 `bootstrap.yml` 读取 `fkh.app.config-group` 配置作为 Group
    - 从 Maven 项目 `pom.xml` 读取 `finalName` 作为包名
    - 支持批量发布多个配置文件
    - 显示发布结果（成功/失败列表）

#### 2.3.2 配置文件识别规则

- **目录模式**: 在 `src/main/resources` 目录上右键
    - 查找该目录下的 `bootstrap.yml` 文件
    - 查找所有以 `application` 开头的 YAML 文件
    - 从 `bootstrap.yml` 读取 Group 配置

- **文件模式**: 在 `application*.yml` 文件上右键
    - 查找同级目录下的 `bootstrap.yml` 文件
    - 从 `bootstrap.yml` 读取 Group 配置

#### 2.3.3 配置文件命名规则

- **主配置**: `application.yml` → DataId: `{packageName}.yml`
- **环境配置**: `application-{env}.yml` → DataId: `{packageName}-{env}.yml`
    - 环境名从 Namespace 中提取（去除 `fkh-` 和 `baoli-` 前缀）

#### 2.3.4 发布对话框

- **Namespace 选择**: 多选下拉框或复选框列表
- **显示**: 所有可用的 Namespace
- **确认按钮**: 执行发布操作
- **取消按钮**: 取消操作

### 2.4 Intention Action (意图操作)

#### 2.4.1 发布配置 Intention

- **触发方式**: `Ctrl+Enter` / `Alt+Enter` (Mac: `Option+Enter`)
- **触发条件**:
    - 在 YAML 文件中
    - 文件名以 `application` 开头
    - 已配置 Nacos 连接信息
- **功能**:
    - 弹出发布对话框
    - 选择要发布到的 Namespace
    - 执行发布操作

#### 2.4.2 对比配置 Intention

- **触发方式**: `Ctrl+Enter` / `Alt+Enter`
- **触发条件**:
    - 在配置编辑器中
    - 已填写 Namespace、Group、DataId
- **功能**:
    - 拉取 Nacos 中的配置
    - 对比编辑器中的配置和 Nacos 配置
    - 显示差异

### 2.5 配置对比功能

#### 2.5.1 对比界面

- 使用 IntelliJ 的 Diff 工具
- **左侧面板**: Nacos 配置（只读）
- **右侧面板**: 本地配置（可编辑）
- 支持差异高亮显示
- 支持同步滚动

#### 2.5.2 对比触发方式

- 在 Tool Window 中点击 "Compare" 按钮
- 使用 Intention Action

### 2.6 Nacos 客户端功能

#### 2.6.1 客户端管理

- **客户端缓存**: 按服务器地址缓存客户端实例
- **连接管理**: 支持多个 Nacos 服务器连接
- **认证管理**: 每个客户端独立认证

#### 2.6.2 API 操作

- **获取 Namespace 列表**: `GET /v1/console/namespaces`
- **获取所有配置**: `GET /v1/cs/configs?show=all&from=idea&namespace={namespace}`
- **拉取配置**: `GET /v1/cs/configs?tenant={namespace}&dataId={dataId}&group={group}`
- **发布配置**: `POST /v1/cs/configs`
    - 参数: tenant, dataId, group, content, type, appName, desc
- **删除配置**: `DELETE /v1/cs/configs?dataId={dataId}&group={group}&tenant={namespace}`

#### 2.6.3 错误处理

- 网络错误处理
- 认证失败处理
- 配置不存在处理
- 权限不足处理
- 友好的错误提示

### 2.7 数据模型

#### 2.7.1 ConfigInfoWrapper

- `tenant`: Namespace
- `dataId`: 配置 ID
- `group`: 配置组
- `content`: 配置内容
- `lastModified`: 最后修改时间

#### 2.7.2 Namespace

- `namespace`: 命名空间 ID
- `namespaceShowName`: 显示名称
- `namespaceDesc`: 描述

#### 2.7.3 PluginState

- `serverAddr`: 服务器地址
- `username`: 用户名
- `type`: 配置类型（YAML/JSON）
- `globalAdmin`: 是否为全局管理员
- `isAuthed`: 是否已认证

### 2.8 其他功能

#### 2.8.1 自动完成

- **Namespace 自动完成**: 从 Nacos 获取所有 Namespace
- **Group 自动完成**: 根据 Namespace 获取该 Namespace 下的所有 Group
- **DataId 自动完成**: 根据 Namespace 和 Group 获取该 Group 下的所有 DataId

#### 2.8.2 配置缓存

- 缓存已获取的配置列表
- 减少重复请求
- 支持手动刷新

#### 2.8.3 线程安全

- UI 操作在 EDT 线程执行
- 网络请求在后台线程执行
- 使用 `ApplicationManager.getApplication().invokeLater()` 更新 UI
- 使用 `ApplicationManager.getApplication().runReadAction()` 执行读操作

#### 2.8.4 通知系统

- 使用 IntelliJ 通知系统显示操作结果
- 成功通知（绿色）
- 错误通知（红色）
- 警告通知（黄色）

## 3. 技术实现要点

### 3.1 项目结构

```
intelli-ai-nacos/
├── src/main/java/com/dong4j/zeka/stack/idea/plugin/nacos/
│   ├── action/              # Action 类
│   │   ├── PublishConfigIntentionAction.java
│   │   ├── CompareConfigIntentionAction.java
│   │   ├── PublishConfigByMenuAction.java
│   │   ├── RefreshAction.java
│   │   ├── AddTabAction.java
│   │   ├── CloseTabAction.java
│   │   ├── SettingAction.java
│   │   └── NacosHelpAction.java
│   ├── client/              # Nacos 客户端
│   │   ├── NacosClient.java
│   │   ├── NacosClientUtils.java
│   │   ├── ConsumServerHttpAgent.java
│   │   ├── ConsumSecurityProxy.java
│   │   └── model/
│   │       ├── ConfigInfo.java
│   │       ├── ConfigInfoBase.java
│   │       ├── ConfigInfoWrapper.java
│   │       └── Namespace.java
│   ├── configuration/       # 配置管理
│   │   ├── SettingsState.java
│   │   └── NacosSettingsConfigurable.java
│   ├── service/             # 服务类
│   │   └── CompareConfigService.java
│   ├── ui/                  # UI 组件
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
│   ├── util/                # 工具类
│   │   ├── YamlUtils.java
│   │   └── NotificationUtil.java
│   ├── entity/              # 实体类
│   │   └── ConfigFile.java
│   └── util/                # 工具类
│       └── NacosBundle.java
└── src/main/resources/
    ├── META-INF/
    │   └── plugin.xml
    ├── messages.properties
    ├── messages_zh_CN.properties
    └── icons/
        └── nacos.svg
```

### 3.2 依赖管理

- **Nacos Client**: `com.alibaba.nacos:nacos-client:2.x.x` (使用最新稳定版)
- **IntelliJ Platform**: 通过 Gradle Plugin 管理
- **其他依赖**: Lombok, SLF4J, Gson 等

### 3.3 配置持久化

- 使用 `PersistentStateComponent` 存储配置
- 密码使用 `CredentialStore` 存储
- 配置文件位置: `{config}/options/nacos-settings.xml`

### 3.4 国际化支持

- 支持中文和英文
- 使用 `DynamicBundle` 管理资源文件
- 所有用户可见文本使用国际化资源

## 4. 非功能需求

### 4.1 性能要求

- 配置列表加载时间 < 3 秒
- 配置拉取/发布操作响应时间 < 2 秒
- UI 操作响应时间 < 100ms

### 4.2 兼容性要求

- 支持 IntelliJ IDEA 2022.3 及更高版本
- 支持 Nacos 2.x 版本
- 支持 Java 17+

### 4.3 安全性要求

- 密码不在配置文件中明文存储
- 使用 HTTPS 连接 Nacos 服务器（如果配置）
- 支持 Nacos 认证机制

### 4.4 可用性要求

- 友好的错误提示
- 操作状态实时反馈
- 支持撤销操作（如果可能）

## 5. 开发规范

### 5.1 代码规范

- 遵循 IntelliJ Platform 开发规范
- 使用 Lombok 简化代码
- 完整的 Javadoc 注释
- 使用 `@NotNull` 和 `@Nullable` 注解

### 5.2 UI 规范

- 使用 IntelliJ UI 组件（JBTable, JBCheckBox, JBTextField 等）
- 遵循 IntelliJ UI 设计规范
- 支持深色主题

### 5.3 测试要求

- 单元测试覆盖率 > 60%
- 集成测试覆盖主要功能
- 使用 Mockito 进行 Mock 测试

## 6. 后续优化方向

### 6.1 功能增强

- 配置历史版本管理
- 配置变更通知
- 配置模板功能
- 批量导入/导出配置
- 配置权限管理

### 6.2 性能优化

- 配置列表分页加载
- 配置内容增量更新
- 本地配置缓存机制

### 6.3 用户体验优化

- 配置搜索优化
- 快捷键支持
- 配置收藏功能
- 最近使用的配置记录

