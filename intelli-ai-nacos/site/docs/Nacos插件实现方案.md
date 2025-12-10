# Nacos 插件实现方案

## 1. 实现概述

基于 `template` 模板，在 `intelli-ai-nacos` 中实现完整的 Nacos 配置管理插件功能。

## 2. 实现步骤

### 2.1 项目初始化

1. 从 `template` 复制基础项目结构
2. 修改项目配置（gradle.properties, settings.gradle.kts, build.gradle.kts）
3. 修改包名从 `com.example.plugin` 到 `com.dong4j.zeka.stack.idea.plugin.nacos`
4. 配置 Nacos Client 依赖

### 2.2 核心模块实现顺序

#### 阶段一：基础配置和设置页面

1. **配置持久化** (`SettingsState`, `NacosSettingsConfigurable`)
    - 实现配置状态管理
    - 实现设置页面 UI
    - 实现连接测试功能

2. **工具类** (`NacosBundle`, `NotificationUtil`)
    - 国际化资源管理
    - 通知工具类

#### 阶段二：Nacos 客户端

3. **Nacos 客户端** (`NacosClient`, `NacosClientUtils`)
    - 实现 HTTP Agent
    - 实现认证代理
    - 实现配置 CRUD 操作
    - 实现客户端缓存管理

4. **数据模型** (`ConfigInfo`, `ConfigInfoWrapper`, `Namespace`)
    - 配置信息模型
    - Namespace 模型

#### 阶段三：UI 组件

5. **Tool Window 基础结构**
    - `NacosToolWindowFactory`
    - `ToolBarPanel`
    - `TabBar` 和 `Tab`

6. **配置编辑器** (`JsonEditor`)
    - 支持多种文件类型
    - 语法高亮和格式化

7. **配置树** (`TreePanel`)
    - 树形结构展示
    - 节点操作（双击、右键）

8. **配置编辑面板** (`NacosToolWindow`)
    - Namespace/Group/DataId 输入框
    - Pull/Push/Compare 按钮
    - 自动完成功能

#### 阶段四：Action 和功能

9. **工具栏 Action**
    - `RefreshAction`
    - `AddTabAction`
    - `CloseTabAction`
    - `SettingAction`
    - `NacosHelpAction`

10. **右键菜单 Action** (`PublishConfigByMenuAction`)
    - 配置文件识别
    - 批量发布功能

11. **Intention Action**
    - `PublishConfigIntentionAction`
    - `CompareConfigIntentionAction`

12. **配置对比服务** (`CompareConfigService`)
    - 使用 IntelliJ Diff 工具

#### 阶段五：工具类和辅助功能

13. **工具类**
    - `YamlUtils` (YAML 解析)
    - `ConfigFile` (配置文件实体)

14. **国际化资源**
    - 中英文资源文件

15. **图标和资源**
    - 插件图标
    - 其他资源文件

### 2.3 关键技术点

#### 2.3.1 Nacos Client 适配

- 原插件使用 Nacos 1.4.1，需要适配到 Nacos 2.x
- 注意 API 变化：
    - HTTP 接口可能有所变化
    - 认证机制可能有所变化
    - 需要测试兼容性

#### 2.3.2 UI 组件更新

- 使用最新的 IntelliJ Platform UI 组件
- 注意废弃 API 的替换
- 使用 `JBTable`, `JBCheckBox`, `JBTextField` 等

#### 2.3.3 线程安全

- 所有 UI 操作在 EDT 线程
- 网络请求在后台线程
- 使用 `invokeLater` 和 `runReadAction`

#### 2.3.4 配置持久化

- 使用 `PersistentStateComponent`
- 密码使用 `CredentialStore`
- 注意配置迁移（如果有旧版本）

### 2.4 测试计划

1. 单元测试：核心业务逻辑
2. 集成测试：Nacos 客户端操作
3. UI 测试：主要功能流程
4. 兼容性测试：不同 IntelliJ 版本

## 3. 文件清单

### 3.1 配置文件

- `build.gradle.kts` - Gradle 构建配置
- `settings.gradle.kts` - Gradle 设置
- `gradle.properties` - 项目属性
- `plugin.xml` - 插件配置
- `messages.properties` / `messages_zh_CN.properties` - 国际化资源

### 3.2 Java 源文件（约 30+ 个文件）

- Action 类：7 个
- Client 类：5 个
- Configuration 类：2 个
- Service 类：1 个
- UI 类：10+ 个
- Util 类：3 个
- Entity 类：1 个
- Model 类：4 个

### 3.3 资源文件

- 图标文件
- 帮助文档
- 其他资源

## 4. 注意事项

### 4.1 API 兼容性

- 注意 Nacos Client API 版本差异
- 注意 IntelliJ Platform API 版本差异
- 做好异常处理和降级方案

### 4.2 代码质量

- 遵循 IntelliJ Platform 开发规范
- 完整的 Javadoc 注释
- 使用 Lombok 简化代码
- 适当的异常处理

### 4.3 用户体验

- 友好的错误提示
- 操作状态实时反馈
- 支持撤销操作（如果可能）
- 支持快捷键

## 5. 预计工作量

- 项目初始化：0.5 天
- 基础配置和设置页面：1 天
- Nacos 客户端：1.5 天
- UI 组件：2 天
- Action 和功能：1.5 天
- 测试和优化：1 天

**总计：约 7.5 个工作日**

## 6. 风险点

1. **Nacos API 兼容性**：需要验证 Nacos 2.x API 是否完全兼容
2. **IntelliJ Platform API 变化**：某些 API 可能已废弃
3. **性能问题**：大量配置加载可能影响性能
4. **认证机制**：Nacos 2.x 认证机制可能有变化

## 7. 后续优化

1. 配置历史版本管理
2. 配置变更通知
3. 配置模板功能
4. 批量导入/导出
5. 性能优化（分页、缓存等）

