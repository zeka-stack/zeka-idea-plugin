# ActionUpdateThread

## 📋 分析目标

检查 ai-javadoc 项目中所有使用 `ActionUpdateThread.BGT` 或 `ActionUpdateThread.EDT` 的代码，找出本应该使用 EDT 却使用了 BGT 的情况。

## 🔍 检查结果

### 1. GenerateJavaDocForFilesAction ✅ 正确

**文件**: `src/main/java/dev/dong4j/zeka/stack/idea/plugin/action/GenerateJavaDocForFilesAction.java`

**使用情况**:

```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;  // ✅ 正确
}
```

**分析**:

- `update()` 方法中访问 `e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)`
- `VIRTUAL_FILE_ARRAY` 数据键只能在 EDT 中安全访问
- **结论**: 使用 EDT 是正确的 ✅

---

### 2. AbstractGenerateJavaDocAction ✅ 正确

**文件**: `src/main/java/dev/dong4j/zeka/stack/idea/plugin/action/AbstractGenerateJavaDocAction.java`

**使用情况**:

```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;  // ✅ 正确
}
```

**分析**:

- `update()` 方法中访问 `e.getData(CommonDataKeys.PSI_FILE)`
- `PSI_FILE` 可以在 BGT 中通过 read-action 访问
- 只做类型检查，不读取文件内容
- **结论**: 使用 BGT 是正确的 ✅

---

### 3. GenerateJavaDocForCommitAction ✅ 正确

**文件**: `src/main/java/dev/dong4j/zeka/stack/idea/plugin/action/GenerateJavaDocForCommitAction.java`

**使用情况**:

```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;  // ✅ 正确
}
```

**分析**:

- `update()` 方法中使用 `runReadAction` 访问 VCS 数据
- 设置 `e.getPresentation()` 的文本和图标（这些操作在 BGT 中是安全的）
- **结论**: 使用 BGT 是正确的 ✅

---

### 4. JavaDocSettingsPanel - 清空可用服务商 Action ⚠️ 需要改进

**文件**: `src/main/java/dev/dong4j/zeka/stack/idea/plugin/settings/ui/JavaDocSettingsPanel.java` (行 225-238)

**当前使用情况**:

```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;  // ⚠️ 可能有问题
}
```

**问题分析**:

- 该 Action **没有实现 `update()` 方法**
- 如果实现 `update()` 方法来根据表格状态启用/禁用按钮，需要访问 `availableProvidersTableModel`
- 访问 Swing 组件（表格模型）必须在 EDT 中执行
- 当前代码中，按钮总是启用的，即使表格为空也可以点击（但会在 `actionPerformed()` 中检查并直接返回）

**建议**:

1. **如果不需要动态更新按钮状态**：保持当前实现（BGT），但这不是最佳实践
2. **如果需要根据表格状态启用/禁用按钮**（推荐）：
    - 实现 `update()` 方法
    - 在 `update()` 中检查 `availableProvidersTableModel.getRowCount()`
    - 将 `getActionUpdateThread()` 改为返回 `ActionUpdateThread.EDT`

---

### 5. JavaDocSettingsPanel - 清空自定义标签 Action ⚠️ 需要改进

**文件**: `src/main/java/dev/dong4j/zeka/stack/idea/plugin/settings/ui/JavaDocSettingsPanel.java` (行 276-288)

**当前使用情况**:

```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;  // ⚠️ 可能有问题
}
```

**问题分析**:

- 该 Action **没有实现 `update()` 方法**
- 如果实现 `update()` 方法来根据表格状态启用/禁用按钮，需要访问 `customJavaDocTagsTableModel`
- 访问 Swing 组件（表格模型）必须在 EDT 中执行
- 当前代码中，按钮总是启用的，即使表格为空也可以点击（但会在 `actionPerformed()` 中检查并直接返回）

**建议**:

1. **如果不需要动态更新按钮状态**：保持当前实现（BGT），但这不是最佳实践
2. **如果需要根据表格状态启用/禁用按钮**（推荐）：
    - 实现 `update()` 方法
    - 在 `update()` 中检查 `customJavaDocTagsTableModel.getRowCount()`
    - 将 `getActionUpdateThread()` 改为返回 `ActionUpdateThread.EDT`

---

## 📊 总结

### 当前状态

| Action                         | 线程类型 | 状态     | 说明                          |
|--------------------------------|------|--------|-----------------------------|
| GenerateJavaDocForFilesAction  | EDT  | ✅ 正确   | 需要访问 VIRTUAL_FILE_ARRAY     |
| AbstractGenerateJavaDocAction  | BGT  | ✅ 正确   | 只做类型检查，可在 BGT 中执行           |
| GenerateJavaDocForCommitAction | BGT  | ✅ 正确   | 使用 runReadAction，可在 BGT 中执行 |
| JavaDocSettingsPanel - 清空服务商   | BGT  | ⚠️ 可改进 | 未实现 update()，如果实现需要 EDT     |
| JavaDocSettingsPanel - 清空标签    | BGT  | ⚠️ 可改进 | 未实现 update()，如果实现需要 EDT     |

### 关键发现

1. **没有发现明显的错误**：所有当前使用 BGT 的 Action 都是合理的
2. **有两个 Action 可以改进**：`JavaDocSettingsPanel` 中的两个清空按钮如果实现 `update()` 方法，应该使用 EDT

### 建议

#### 选项 1：保持现状（不推荐）

- 保持当前实现，按钮总是启用
- 在 `actionPerformed()` 中检查并直接返回
- **缺点**：用户体验不佳，按钮在表格为空时仍然可用

#### 选项 2：实现 update() 方法（推荐）

- 为两个清空按钮实现 `update()` 方法
- 根据表格模型的行数动态启用/禁用按钮
- 将 `getActionUpdateThread()` 改为返回 `ActionUpdateThread.EDT`
- **优点**：更好的用户体验，按钮状态与数据状态同步

---

## 🔧 修复建议

如果选择选项 2，需要修改以下代码：

### 修改 1: 清空可用服务商 Action

```java
.addExtraAction(new AnAction("清空全部",
                             "清空所有可用服务商配置",
                             com.intellij.icons.AllIcons.Actions.GC) {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        clearAllAvailableProviders();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 根据表格状态启用/禁用按钮
        boolean hasData = availableProvidersTableModel.getRowCount() > 0;
        e.getPresentation().setEnabled(hasData);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 需要访问 Swing 组件（表格模型），必须在 EDT 中执行
        return ActionUpdateThread.EDT;  // ✅ 改为 EDT
    }
});
```

### 修改 2: 清空自定义标签 Action

```java
.addExtraAction(new AnAction(JavaDocBundle.message("settings.custom.javadoc.tags.clear.all"),
                             JavaDocBundle.message("settings.custom.javadoc.tags.clear.all.description"),
                             com.intellij.icons.AllIcons.Actions.GC) {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        clearAllCustomJavaDocTags();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 根据表格状态启用/禁用按钮
        boolean hasData = customJavaDocTagsTableModel.getRowCount() > 0;
        e.getPresentation().setEnabled(hasData);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 需要访问 Swing 组件（表格模型），必须在 EDT 中执行
        return ActionUpdateThread.EDT;  // ✅ 改为 EDT
    }
});
```

---

## 📚 参考文档

- [IntelliJ Platform - ActionUpdateThread](https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#action-update)
- [IntelliJ Platform - Threading Rules](https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html)
- `ai-javadoc/docs/ActionUpdateThread修复说明.md`

---

## ✅ 结论

**当前代码中没有发现明显的错误**，所有使用 BGT 的地方都是合理的。

**但是有两个 Action 可以改进**：如果实现 `update()` 方法来根据表格状态动态启用/禁用按钮，应该使用 EDT 而不是 BGT。

