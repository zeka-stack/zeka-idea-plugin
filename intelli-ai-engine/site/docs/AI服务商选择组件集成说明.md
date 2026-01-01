# AI 服务商选择组件集成说明

本文档说明 IntelliAI Engine 提供的通用组件 `AIProviderSelectionPanel` 的作用、使用方式与注意事项，供子插件在设置页面中复用。

## 组件位置

- 类：`dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderSelectionPanel`
- 作用：统一显示“AI 服务商选择”UI，读取 Engine 的已验证服务商列表，并提供无服务商时的引导入口。

## 组件能力概览

`AIProviderSelectionPanel` 主要做了以下事情：

1. 从 Engine 全局配置读取已验证服务商列表（`AIProviderSettings#getVerifiedProviders()`）
2. 生成服务商下拉框，显示服务商图标、名称、模型
3. 无服务商时，展示提示文案 + 跳转到 Engine 设置的链接
4. 监听 Engine 配置变更，动态刷新面板
5. 提供获取/设置选中项的 API

## 适用场景

- 子插件设置页面中需要选择 AI 服务商
- 子插件只关心“可用服务商列表”和“选中项”，不重复实现 UI

## 使用方式

### 1. 创建面板

```java
AIProviderSelectionPanel panel = new AIProviderSelectionPanel(
    MyBundle::message,
    () -> {
        // 可选：面板刷新后的回调
    }
);
JPanel ui = panel.getPanel();
```

`MessageProvider` 用于读取子插件自己的 i18n 文案，保持命名与表情统一。

### 2. 设置/读取选中项

```java
// 设置默认选中项
panel.setSelectedProvider(settings.providerConfig);

// 读取选中项
AIProviderConfig selected = panel.getSelectedProvider();
```

### 3. 释放资源

面板会注册 `AIProviderSettingsListener`，必须在 UI 销毁时调用 `dispose()`：

```java
@Override
public void disposeUIResources() {
    panel.dispose();
}
```

## 集成示例（Javadoc）

`intelli-ai-javadoc` 在设置面板中直接复用该组件，并在面板刷新时恢复选中项：

```java
// JavadocSettingsPanel#createUI
aiProviderSelectionPanel = new AIProviderSelectionPanel(
    JavadocBundle::message,
    () -> {
        // 面板刷新后的回调：恢复选中的服务商
        SettingsState settings = SettingsState.getInstance();
        loadSettings(settings);
    }
);
```

在保存/加载时读取选中项：

```java
AIProviderConfig selected = aiProviderSelectionPanel.getSelectedProvider();
settings.providerConfig = selected;
```

## 关键接口说明

### MessageProvider

用于让子插件传入自己的国际化文案：

```java
@FunctionalInterface
public interface MessageProvider {
    @NotNull String message(@NotNull String key);
}
```

组件内部会使用以下 key：

- `settings.ai.provider.selection`
- `settings.ai.provider`
- `settings.ai.provider.hint`
- `settings.ai.provider.no.available.warning`
- `settings.ai.provider.open.ai.common.settings`

请确保子插件的 i18n 中存在这些键，并保持表情与命名规范一致。

## 注意事项

1. 该组件只读取“已验证服务商”列表
    - 未通过验证的服务商不会出现在下拉框
2. 无服务商时提供跳转链接
    - 跳转目标为 Engine 设置页面（应用级）
3. 面板会在服务商列表变化时“重建”
    - 如果你在父容器里缓存了子组件引用，请处理刷新回调
4. UI 释放
    - 忘记调用 `dispose()` 会导致监听器泄漏

## 最小集成清单

- [ ] 使用 `AIProviderSelectionPanel` 构建服务商选择 UI
- [ ] 通过 `MessageProvider` 传入子插件 i18n
- [ ] 在 `disposeUIResources()` 中调用 `dispose()`
- [ ] 读取/保存选中项到子插件 SettingsState
