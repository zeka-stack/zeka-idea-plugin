# GenerateJavaDocForFilesAction 线程安全问题修复

## 问题描述

在打开项目并点击项目视图右键菜单时，出现以下错误：

```
java.lang.Throwable: 'virtualFileArray' is requested on EDT by AnalyzeByteCodeAction#presentation@ProjectViewPopup
```

虽然错误信息中没有明确提到 ai-javadoc 插件，但该错误确实是由 `GenerateJavaDocForFilesAction` 引起的。

## 问题原因

### 错误代码

```java
// ❌ 错误的实现
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    // 在后台线程中执行 update，避免阻塞 EDT
    return ActionUpdateThread.BGT;  // ❌ 错误
}

@Override
public void update(@NotNull AnActionEvent e) {
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);  // ❌ 在 BGT 中访问会报错
    boolean enabled = files != null && files.length > 0 && hasJavaFiles(files);
    e.getPresentation().setEnabled(enabled);
    // ...
}
```

### 根本原因

1. **`GenerateJavaDocForFilesAction` 注册在 `ProjectViewPopupMenu`**（项目视图右键菜单）
2. **`update()` 方法中访问了 `CommonDataKeys.VIRTUAL_FILE_ARRAY`**
3. **`getActionUpdateThread()` 返回了 `ActionUpdateThread.BGT`**（后台线程）
4. **`VIRTUAL_FILE_ARRAY` 数据键只能在 EDT（Event Dispatch Thread）中访问**

根据 IntelliJ Platform 的规范：

- `VIRTUAL_FILE_ARRAY` 只能在 EDT 中访问
- 在 BGT 中访问 `VIRTUAL_FILE_ARRAY` 会触发 `Throwable` 异常

## 修复方案

### 修复后的代码

```java
// ✅ 正确的实现
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    // 必须在 EDT 中执行，因为 VIRTUAL_FILE_ARRAY 只能在 EDT 中访问
    return ActionUpdateThread.EDT;  // ✅ 正确
}

@Override
public void update(@NotNull AnActionEvent e) {
    // VIRTUAL_FILE_ARRAY 只能在 EDT 中访问，所以 getActionUpdateThread() 必须返回 EDT
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);  // ✅ 安全
    boolean enabled = files != null && files.length > 0 && hasJavaFiles(files);
    e.getPresentation().setEnabled(enabled);
    e.getPresentation().setText(JavaDocBundle.message("action.generate.javadoc"));
    e.getPresentation().setDescription(JavaDocBundle.message("action.generate.javadoc.selection.description"));
}
```

### 修改内容

1. **将 `getActionUpdateThread()` 的返回值从 `BGT` 改为 `EDT`**
2. **更新了方法注释，说明为什么必须在 EDT 中执行**
3. **在 `update()` 方法中添加了注释，说明访问 `VIRTUAL_FILE_ARRAY` 的要求**

## EDT 和 BGT 的区别

### EDT (Event Dispatch Thread) - 事件调度线程

**特点**：

- 单线程：所有 UI 操作都在这个线程中执行
- 同步：所有 UI 更新、事件处理都是同步的
- 阻塞敏感：长时间操作会冻结整个 UI

**使用场景**：

- ✅ UI 更新：修改组件状态、显示/隐藏、设置文本
- ✅ 访问某些数据键：`VIRTUAL_FILE_ARRAY`、`NAVIGATABLE_ARRAY` 等
- ✅ 用户交互：按钮点击、菜单选择等事件处理

**示例**：

```java
// ✅ 正确：在 EDT 中更新 UI
e.getPresentation().setEnabled(true);
e.getPresentation().setText("文本");

// ✅ 正确：在 EDT 中访问 VIRTUAL_FILE_ARRAY
VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
```

### BGT (Background Thread) - 后台线程

**特点**：

- 多线程：可以并行执行多个后台任务
- 非阻塞：不会冻结 UI
- 适合耗时操作：文件 I/O、网络请求、复杂计算

**使用场景**：

- ✅ 耗时操作：文件读取、网络请求、大量数据处理
- ✅ 访问 PSI：通过 `runReadAction` 访问 PSI 元素
- ✅ 数据计算：统计分析、代码分析等

**示例**：

```java
// ✅ 正确：在 BGT 中执行耗时操作
ApplicationManager.getApplication().executeOnPooledThread(() -> {
    // 耗时操作
    processLargeFile();
});

// ✅ 正确：在 BGT 中访问 PSI（需要 read-action）
ApplicationManager.getApplication().runReadAction(() -> {
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    // 处理 PSI
});
```

### 关键区别总结

| 特性        | EDT                    | BGT                                  |
|-----------|------------------------|--------------------------------------|
| **线程类型**  | 单线程（主 UI 线程）           | 多线程（线程池）                             |
| **UI 操作** | ✅ 必须                   | ❌ 禁止                                 |
| **阻塞影响**  | 会冻结整个 UI               | 不会冻结 UI                              |
| **数据键访问** | `VIRTUAL_FILE_ARRAY` 等 | `PSI_FILE`、`EDITOR` 等（需 read-action） |
| **使用场景**  | UI 更新、用户交互             | 耗时操作、数据处理                            |

## 数据键访问规则

### 必须在 EDT 中访问的数据键

- `CommonDataKeys.VIRTUAL_FILE_ARRAY` - 虚拟文件数组
- `CommonDataKeys.NAVIGATABLE_ARRAY` - 可导航元素数组
- 其他与文件系统直接相关的数据键

### 可以在 BGT 中访问的数据键（需要 read-action）

- `CommonDataKeys.PSI_FILE` - PSI 文件
- `CommonDataKeys.EDITOR` - 编辑器
- `CommonDataKeys.PROJECT` - 项目对象

**访问方式**：

```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;  // ✅ 可以在 BGT 中
}

@Override
public void update(@NotNull AnActionEvent e) {
    // 需要在 read-action 中访问 PSI
    ApplicationManager.getApplication().runReadAction(() -> {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);  // ✅ 安全
    });
}
```

## 常见错误和最佳实践

### ❌ 错误 1：在 BGT 中访问 VIRTUAL_FILE_ARRAY

```java
// ❌ 错误
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;  // ❌ 错误
}

@Override
public void update(@NotNull AnActionEvent e) {
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);  // ❌ 会报错
}
```

**错误信息**：

```
java.lang.Throwable: 'virtualFileArray' is requested on EDT by ...
```

### ❌ 错误 2：在 EDT 中执行耗时操作

```java
// ❌ 错误
@Override
public void actionPerformed(@NotNull AnActionEvent e) {
    // 在 EDT 中执行耗时操作会冻结 UI
    processLargeFile();  // ❌ 会冻结 UI
}
```

### ✅ 正确做法

```java
// ✅ 正确
@Override
public void actionPerformed(@NotNull AnActionEvent e) {
    // 在后台线程中执行耗时操作
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
        processLargeFile();  // ✅ 不会冻结 UI
        
        // 更新 UI 需要在 EDT 中
        ApplicationManager.getApplication().invokeLater(() -> {
            updateUI();  // ✅ 在 EDT 中更新 UI
        });
    });
}
```

## 修复效果

修复后的效果：

1. ✅ **解决了线程安全错误**：不再出现 `'virtualFileArray' is requested on EDT` 错误
2. ✅ **正确的线程使用**：`update()` 方法在 EDT 中执行，可以安全访问 `VIRTUAL_FILE_ARRAY`
3. ✅ **避免插件冲突**：不会与其他插件（如 `AnalyzeByteCodeAction`）产生冲突
4. ✅ **符合 IntelliJ Platform 规范**：遵循平台的最佳实践

## 相关文件

- **修复的文件**：`GenerateJavaDocForFilesAction.java`
- **注册位置**：`plugin.xml` - `ProjectViewPopupMenu`
- **相关类**：`AbstractGenerateJavaDocAction`（使用 `PSI_FILE`，可以在 BGT 中）

## 参考文档

- [IntelliJ Platform SDK - ActionUpdateThread](https://plugins.jetbrains.com/docs/intellij/action-update-thread.html)
- [IntelliJ Platform SDK - Threading Model](https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html)
- [IntelliJ Platform SDK - DataContext](https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#data-context)

## 修复日期

2025-01-XX

## 修复版本

1.0.0

