# IntelliAI Nacos 遗留功能完善方案

## 1. 背景与目标

- `intelli-ai-nacos` 已搭建核心结构（设置、客户端骨架、Tool Window UI、Action/Service 壳），但大量 `TODO` 尚未实现。
- `fkh-idea-nacos` 已经具备完整功能，但依赖的 IntelliJ SDK 与 Nacos API 版本较旧。
- 目标：在保持原有用户体验（设置页、Tool Window、右键/Intention、配置对比等）的基础上，参照旧项目逻辑，使用当前 IntelliJ Platform SDK & Nacos 2.x
  API 实现全部遗留功能。

## 2. 现状评估

| 模块                                                                             | 现状                       | 缺失点                                                          |
|--------------------------------------------------------------------------------|--------------------------|--------------------------------------------------------------|
| 设置页 (`NacosSettingsPanel`)                                                     | UI 已完成，连接测试按钮未实现         | 需要真实的 Nacos 连接校验、错误提示、状态展示                                   |
| Nacos 客户端 (`NacosClient`, `ConsumServerHttpAgent`, `ConsumSecurityProxy`)      | HTTP 架构已搭建，登录/CRUD 流程为占位 | 需要实现登录态缓存、Token 刷新、异常转换、批量查询等                                |
| Tool Window (`NacosToolWindow`, `TreePanel`, `TabBar`, `ConfigOperationPanel`) | UI 布局完成                  | 缺少数据加载与交互逻辑（刷新/新增/关闭 Tab、命名空间/分组/数据 ID 列表、Pull/Push/Compare） |
| Action (`RefreshAction` 等)                                                     | 类存在，逻辑为空                 | 需绑定 Tool Window 服务、提供图标、调用实际业务                               |
| Intention/右键发布                                                                 | 类存在，逻辑为空                 | 需检测文件类型、读取 PSI/VirtualFile、调用发布/对比服务                         |
| JSON 编辑器                                                                       | 语法组件搭好                   | 缺少格式化、修改状态检测、Before/After diff                               |
| Service (`CompareConfigService`)                                               | Service 空实现              | 需封装 IntelliJ Diff API，对接 Tool Window & Intention             |
| Notification/国际化                                                               | Bundle & Util 有          | 需补齐消息 key 与统一调用                                              |

## 3. 参考实现摘要（来自 `fkh-idea-nacos`）

1. Tool Window：包含 `TreePanel` 展示 namespaces/group/dataId，支持刷新、展开/收起、双击加载、右键删除。
2. Tab 管理：每次 Pull/Pull 比较都会在右侧创建带 JSON 编辑器的 Tab，支持关闭、修改状态标记。
3. 操作面板：可筛选命名空间、group、dataId，并执行 Pull/Push/Compare。
4. Actions：工具栏按钮（刷新、添加/关闭 Tab、设置、帮助）、项目视图右键、文件 Intention。
5. Diff：使用 IntelliJ DiffManager 比较本地 PSI 与远端配置。

## 4. 详细实现计划

### 4.1 设置与连接校验

1. 在 `NacosSettingsPanel` 的“测试连接”按钮中：
    - 使用后台任务 (`Task.Backgroundable`) 调用 `NacosClient.getInstance().login()`。
    - 显示 `ProgressManager` 进度，成功/失败通过 `NotificationUtil` & `JavaDocBundle`（待补齐 key）提示。
2. `SettingsState`：
    - 增加 `lastNamespaceId`、`defaultGroup` 等常用字段，保持与 Tool Window 联动。
    - 保存成功后触发 `NacosToolWindow` 刷新（用 `ApplicationManager.invokeLater`）。

### 4.2 Nacos 客户端与工具类

1. `ConsumSecurityProxy`：
    - 实现 `/v1/auth/users/login` 请求，缓存 `accessToken`，失效后自动重试。
2. `ConsumServerHttpAgent`：
    - 使用 `HttpClient`（IDE 提供或 OkHttp）封装 GET/POST/DELETE。
    - 支持 `connectTimeout/readTimeout` 配置。
3. `NacosClient`：
    - 实现真正的 `login()`，成功后 `isLoggedIn=true` 并在 `CLIENT_CACHE` 中复用。
    - `getNamespaces()/getConfigs()` 支持分页；增加 `getGroups(namespaceId)`、`getDataIds(namespaceId, group)`。
    - `publish/delete` 方法返回 `Result<Boolean>`，统一捕获 `NacosException` 转为 `Notification` 文案。
4. 辅助：
    - `NacosClientUtils` 提供懒加载与异常提示。
    - `CacheUtils` 管理 namespace/group/dataId 缓存，配合 `RefreshAction` 清理。

### 4.3 Tool Window 数据流

1. `NacosToolWindowFactory` 启动时：
    - 读取配置，若未配置则提示跳转设置；已配置则自动拉取命名空间树。
2. `TreePanel`：
    - 参照旧项目 `TreePanel`，构建 `DefaultMutableTreeNode(namespace -> group -> dataId)`。
    - 双击 dataId 时调用 `ConfigOperationPanel#pullConfig()` 并在 TabBar 中打开/更新 Tab。
    - 右键菜单支持删除/展开/收起。
3. `TabBar` & `Tab`：
    - Tab 保存 `ConfigFile`（包含 namespace/group/dataId/type/content/modified 标记）。
    - 监听 `JsonEditor` 文档变化，显示 `*` 标记；关闭前询问是否保存。
4. `ConfigOperationPanel`：
    - 名称空间、Group、DataId 下拉框动态填充（通过 `NacosClient`）。
    - `Pull`：加载远端配置，打开 Tab，并把内容同步到 `JsonEditor`。
    - `Push`：读取当前 Tab/文本内容，调用 `publishConfig`。
    - `Compare`：调用 `CompareConfigService`。
    - `Load Groups/DataIds` TODO 全部实现，支持 Loading 态。

### 4.4 工具栏 Actions

- `RefreshAction`：刷新 Tree + 当前 Tab（若 namespace/group/dataId 已选中）。
- `AddTabAction`：创建空白配置编辑 Tab（用于新建配置）。
- `CloseTabAction`：关闭当前 Tab，若有未保存内容提示。
- `SettingAction`：打开设置 Configurable。
- `NacosHelpAction`：打开 `landing.html` 或在线文档 URL。
- `PublishConfigByMenuAction`：项目视图右键，读取所选文件内容，自动推送。
- `PublishConfigIntentionAction`/`CompareConfigIntentionAction`：识别 YAML/Properties/JSON，在编辑器中触发。
- 全部 Action 需要：图标（遵循 SVG 规范）、国际化文案、在 `plugin.xml` 中注册快捷键/菜单位置。

### 4.5 JSON 编辑器与 Diff

1. `JsonEditor`：
    - 整合 `LanguageFileType` 决定语法高亮（YAML/JSON/Properties）。
    - `formatContent()`：调用 `CodeStyleManager.reformatText` 或 `Jackson`（JSON）/`SnakeYAML`（YAML）美化。
    - `isModified()`：比较初始内容与当前 `Document`。
2. `CompareConfigService`：
    - 使用 `DiffContentFactory` 创建左右内容。
    - 在 Tool Window/Intention 中调用，支持多语言标题。

### 4.6 注册与配置

- `plugin.xml`：
    - 注册 Tool Window (`toolWindow`)、`postStartupActivity`（可选，用于首次提示配置）。
    - 注册所有 Actions、Intention、NotificationGroup（已有）、应用级服务。
- `messages*.properties`：补全所有用户文案，避免硬编码。
- `includes/pluginChanges.html`：新增版本条目记录本次功能。
- `docs/用户手册.md`：新增 Tool Window、右键发布、Intention 使用说明。

## 5. 技术细节与注意事项

1. **IntelliJ API 兼容**：使用 `com.intellij.openapi.ui`、`ToolbarDecorator`、`JBTable` 等现代组件；弃用旧 `AnActionEvent#getProject()` Null 安全
   API。
2. **线程模型**：网络请求在后台 (`Task.Backgroundable` / `CompletableFuture`)，UI 更新包裹 `invokeLater`；写 PSI 时 `runWriteAction`。
3. **异常处理**：统一通过 `NotificationUtil`（INFO/WARNING/ERROR）+ `log.error`，不可阻塞 UI。
4. **安全**：密码仍通过 `PasswordSafe`；请求时避免在日志中输出敏感信息。
5. **Nacos 2.x 差异**：登陆接口从 `/nacos/v1/auth/users/login` 迁移；`accessToken` 放在 `Authorization` header；命名空间分页字段变化（需要根据
   2.x 文档调整）。

## 6. 风险与缓解

| 风险           | 说明               | 缓解措施                               |
|--------------|------------------|------------------------------------|
| Nacos API 变化 | 2.x 认证与响应字段与旧版不同 | 先对接测试 Nacos 实例，封装转换层               |
| 网络/线程阻塞      | 大量配置加载可能阻塞 UI    | 所有网络请求放后台，提供取消/超时                  |
| 数据丢失         | Push 前未确认        | Push/Close Tab 前提示确认，支持 Compare    |
| 国际化缺失        | 旧代码可能硬编码         | 开发同步补全 `messages.properties` 与中文文件 |

## 7. 测试计划

1. **单元测试**：`NacosClient`（登录、CRUD 请求参数组装）、`YamlUtils`、`CacheUtils`。
2. **集成测试**：使用 MockServer/Nacos Docker 实例验证 Tool Window 操作链（可通过手动自测 + 文档）。
3. **UI 自测**：Tool Window 加载、Tab 操作、Action/Intention、通知提示。
4. **回归**：设置页保存/连接测试、多项目环境、IDE 版本（223/241/251）兼容。

## 8. 交付物

1. 功能代码：完成所有 `TODO`，新增必要类/资源。
2. 文档：更新 `docs/用户手册.md`、`includes/pluginChanges.html`。
3. 测试：`./gradlew test` 通过，关键功能提供截图或描述。

## 9. 后续可选优化

- 批量导入/导出配置。
- 历史版本回溯。
- 与 IntelliAI 引擎联动，自动生成配置注释或校验。


