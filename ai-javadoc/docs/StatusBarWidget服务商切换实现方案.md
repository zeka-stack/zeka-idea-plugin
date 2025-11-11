# StatusBarWidget 服务商切换实现方案

## 功能概述

- 在编辑器状态栏新增快速切换默认服务商的入口，展示插件主图标与当前默认服务商名称。
- 点击控件弹出已通过验证的服务商列表（来源 `availableProviders`），并高亮当前默认服务商。
- 选择列表项后，更新 `SettingsState.defaultProviders` 中对应条目，并同步刷新全局默认服务商 `providerType`。

## 现状分析

- 目前默认服务商只能在设置页中切换，操作路径长，且难以观察当前使用的服务商。
- `SettingsState` 已维护 `availableProviders`（验证通过的配置列表）与 `defaultProviders`（每种 `AIProviderType` 的默认配置），缺少 UI 入口消费这些数据。
- 插件尚未实现任何 `StatusBarWidget`，需要从零搭建注册、UI 与交互逻辑。

## 实现方案

### 组件结构

- 新增 `dev.dong4j.zeka.stack.idea.plugin.statusbar.AIProviderStatusBarWidgetFactory` 实现 `StatusBarWidgetFactory`，用于注册控件并按项目创建实例。
- 新增 `AIProviderStatusBarWidget` 继承 `EditorBasedStatusBarPopup`（或 `StatusBarWidget.MultipleTextValuesPresentation`），负责：
    - 构建状态栏展示：`AIJicons.Plugin` + 当前默认服务商显示名（通过 `AIProviderType.getDisplayName()`）。
    - 弹出 `ListPopup` 展示候选项，内容来自 `SettingsState.getAvailableProviders()`，仅包含 `configurationVerified = true` 的配置。
    - 处理选项点击，调用 `SettingsState.updateDefaultProviderConfig()` 并更新 `providerType`。
- 新增 `AIProviderStatusBarWidgetModel`（可选），封装数据访问逻辑，便于单元测试与与 UI 解耦。

### 数据流

1. Widget 初始化时读取 `SettingsState.getInstance()`，解析当前 `providerType` 对应的默认配置。
2. 构建候选列表：
    - 直接使用 `SettingsState.getAvailableProviders()`。
    - 按 `AIProviderType` 分组，仅展示当前类型与其默认配置对应的 display name。
    - 文本展示：`AIProviderType` 的 `displayName` 或配置 remark（若需要）。
3. 用户选择服务商后：
    - 通过 `AIProviderType.fromProviderId()` 与 `SettingsState.ProviderConfig.providerType` 校验类型。
    - 更新 `SettingsState.providerType = selectedProviderType`。
    - 调用 `SettingsState.updateDefaultProviderConfig(selectedProviderType, selectedConfigCopy)`，确保 `defaultProviders` 写入。
    - `ApplicationManager.getApplication().invokeLater()` + `runWriteAction()`（如触发 PSI/配置写入）确保线程安全。
    - 使用 `NotificationUtil.showInfo()` 提示切换结果（文案走 `AIJBundle`）。
4. 通知状态栏刷新：调用 `StatusBarWidgetFactory` 的 `updateWidget()` 或直接触发 `statusBar.updateWidget(id)`。

### 国际化资源

- 在 `messages/AIJBundle_zh_CN.properties` 与 `AIJBundle_en_US.properties` 增加：
    - `statusbar.provider.title`（状态栏 tooltip）。
    - `statusbar.provider.switch.success`、`statusbar.provider.switch.failed`。
    - `statusbar.provider.current`（例如 "默认服务商: {0}"）。
    - `statusbar.provider.popup.title`（弹窗标题）。
- 所有 UI 文本使用 `AIJBundle.message()`。

### plugin.xml 变更

- 在 `META-INF/plugin.xml` 注册
  `<statusBarWidgetFactory implementation="dev.dong4j.zeka.stack.idea.plugin.statusbar.AIProviderStatusBarWidgetFactory" order="first"/>`。
- 如需通知组复用现有 `notificationGroup`，无需新增配置。

### 线程与性能

- Widget 仅在显式交互时读取配置，数据规模小，对性能影响可忽略。
- 切换默认服务商涉及配置写入，需通过 `invokeLater + runWriteAction` 确保在 EDT 上更新 UI、在写动作中持久化。
- 弹窗数据量有限，可直接在 EDT 中构建；若未来列表较大，可考虑缓存。

### 日志与异常处理

- 使用 `@Slf4j` 记录切换失败原因，`log.error` 包含详细异常。
- 切换失败时通过 `NotificationUtil.showError()` 告知用户。

### 测试计划

- **单元测试**：
    - 新增 `AIProviderStatusBarWidgetModelTest` 验证：
        - `getCurrentProviderDisplayName()` 呈现正确名称。
        - `buildAvailableProviderItems()` 仅返回验证通过的配置。
        - `switchProvider()` 能正确更新 `SettingsState.defaultProviders` 与 `providerType`。
- **集成测试**（可选）：基于现有 `SettingsStateTest` 扩展，模拟配置写入与读取。
- **手动测试**：
    - IDE 启动后状态栏展示正确。
    - 切换到不同服务商后，设置页同步展示新的默认服务商。
    - 弹窗包含 remark/模型信息（若显示）。

### 影响范围与兼容性

- 新增状态栏控件，不影响现有功能路径。
- 设置持久化逻辑复用 `SettingsState`，保持兼容。
- 需确保无 API Key 泄露：弹窗展示仅使用 display name。

### 变更文件列表

- `src/main/java/dev/dong4j/zeka/stack/idea/plugin/statusbar/AIProviderStatusBarWidgetFactory.java`
- `src/main/java/dev/dong4j/zeka/stack/idea/plugin/statusbar/AIProviderStatusBarWidget.java`
- （可选）`src/main/java/dev/dong4j/zeka/stack/idea/plugin/statusbar/AIProviderStatusBarWidgetModel.java`
- `src/main/resources/messages/AIJBundle_*.properties`
- `src/main/resources/META-INF/plugin.xml`
- 新增测试文件位于 `src/test/java/dev/dong4j/zeka/stack/idea/plugin/statusbar/`
