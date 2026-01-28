# 调试 Action 注册表开关方案

## 背景
在插件中经常需要临时加入调试入口（Action），但不希望在对外发布版本中暴露。本文提供一种可复用的方式：使用 IntelliJ Registry 控制 Action 的显示与隐藏。

## 方案概述
通过 `Registry` 注册一个开关键（key），在 Action 的 `update()` 中读取该开关，控制菜单项的可见性与可用性。**推荐使用 `plugin.xml` 的 `com.intellij.registryKey` 扩展点注册**，确保 Registry UI 中可见。

## 实现步骤

### 1. 约定 Registry Key
选择一个清晰的内部 key，例如：

- `intelliai.engine.feedback.test`

该 key 默认关闭（false），仅在内部调试时手动开启。

### 2. Action 中读取 Registry
在 Action 的 `update()` 方法中读取该 key，并根据结果设置显示状态：

- 关闭时：`setEnabledAndVisible(false)`
- 开启时：正常显示

伪代码示例：

```java
@Override
public void update(@NotNull AnActionEvent event) {
    boolean visible = Registry.is("intelliai.engine.feedback.test");
    event.getPresentation().setEnabledAndVisible(visible);
}
```

### 3. 在 plugin.xml 注册 Registry Key（推荐）
使用 `com.intellij` 扩展点注册 key，保证 Registry 面板可检索：

```xml
<extensions defaultExtensionNs="com.intellij">
    <registryKey key="intelliai.engine.feedback.test"
                 defaultValue="false"
                 description="Show Feedback Test action in IntelliAI Engine menu"/>
</extensions>
```

### 4. IDE 内开启/关闭
在 IDE 中通过 Registry 面板开启：

- `Help` → `Find Action` → 输入 `Registry...`
- 搜索 key：`intelliai.engine.feedback.test`
- 勾选为 `true` 即可显示调试 Action

## 适用场景
- 内部测试入口
- 试验性功能
- 开发期临时工具

## 注意事项
- 仅在 `plugin.xml` 中注册的 key 才能确保在 Registry UI 中可见。
- Registry 配置只在启动时加载，修改后需重启 IDE。

## 可复用规范建议
- 统一使用前缀：`intelliai.engine.*`
- 文档中记录 key、默认值、用途
- 如果 Action 存在多个调试入口，可对应多个 key

