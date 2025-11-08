# ActionUpdateThread 修复说明

## 📋 问题描述

### 警告日志

```
WARN - #c.i.o.a.i.PreCachedDataContext - 'virtualFileArray' is requested on EDT by GenerateJavaDocForSelectionAction#update@ProjectViewPopup (dev.dong4j.zeka.stack.idea.plugin.action.GenerateJavaDocForFilesAction). See ActionUpdateThread javadoc.
```

### 问题分析

从 **IntelliJ Platform 2022.3** 开始，插件的 `AnAction` 需要显式声明 `update()` 方法应该在哪个线程中运行，以优化性能和避免 EDT（Event Dispatch
Thread）阻塞。

**影响**：

- ❌ 如果不声明，会产生警告日志
- ❌ 可能影响 IDE 响应速度
- ❌ 不符合最新的平台最佳实践

---

## ✅ 解决方案

### ActionUpdateThread 机制

IntelliJ Platform 提供了两种选择：

1. **`ActionUpdateThread.EDT`**
    - 在 EDT（Event Dispatch Thread）中运行
    - 旧的默认行为
    - 适用于必须在 EDT 中执行的操作

2. **`ActionUpdateThread.BGT`** ✅ 推荐
    - 在后台线程（Background Thread）中运行
    - 更好的性能
    - 不阻塞 UI
    - 适用于大多数场景

---

## 🔧 修复实现

### 修复的文件

为所有 `AnAction` 子类添加 `getActionUpdateThread()` 方法：

1. ✅ `GenerateJavaDocForSelectionAction.java`
2. ✅ `GenerateJavaDocForFileAction.java`
3. ✅ `GenerateJavaDocGenerateAction.java`
4. ✅ `GenerateJavaDocShortcutAction.java`

**注意**：`GenerateJavaDocIntentionAction` 继承自 `IntentionAction`，不是 `AnAction`，不需要修改。

---

### 代码变更

#### 1. 添加导入

```java
import com.intellij.openapi.actionSystem.ActionUpdateThread;
```

#### 2. 实现 getActionUpdateThread() 方法

```java
@Override
public @NotNull ActionUpdateThread getActionUpdateThread() {
    // 在后台线程中执行 update，避免阻塞 EDT
    return ActionUpdateThread.BGT;
}
```

---

### 修复示例

**Before（会产生警告）**：

```java
public class GenerateJavaDocForSelectionAction extends AnAction {
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        boolean enabled = files != null && files.length > 0 && hasJavaFiles(files);
        e.getPresentation().setEnabled(enabled);
    }
}
```

**After（无警告）**：

```java
import com.intellij.openapi.actionSystem.ActionUpdateThread;

public class GenerateJavaDocForSelectionAction extends AnAction {
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 在后台线程中执行 update，避免阻塞 EDT
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        boolean enabled = files != null && files.length > 0 && hasJavaFiles(files);
        e.getPresentation().setEnabled(enabled);
    }
}
```

---

## 📊 为什么选择 BGT？

### 我们的 update() 方法特点

所有 Action 的 `update()` 方法只做简单的检查：

1. **GenerateJavaDocForSelectionAction**
   ```java
   VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
   boolean enabled = files != null && files.length > 0 && hasJavaFiles(files);
   ```
    - 只检查文件类型
    - 不访问 PSI 树
    - 不需要 write-action

2. **GenerateJavaDocForFileAction**
   ```java
   PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
   e.getPresentation().setEnabled(psiFile instanceof PsiJavaFile);
   ```
    - 只做类型检查
    - 不读取文件内容
    - 不需要 EDT

3. **GenerateJavaDocGenerateAction** / **GenerateJavaDocShortcutAction**
    - 同样只做简单检查

### BGT 的优势

| 特性   | EDT         | BGT      |
|------|-------------|----------|
| 性能   | ❌ 可能阻塞 UI   | ✅ 不阻塞 UI |
| 响应速度 | ❌ 较慢        | ✅ 更快     |
| 推荐度  | ❌ 不推荐       | ✅ 推荐     |
| 适用场景 | 必须在 EDT 的操作 | 大多数场景    |

---

## ✅ 验证结果

### 编译验证

```bash
./gradlew compileJava
```

**结果**：✅ BUILD SUCCESSFUL

### 运行验证

1. 编译插件并运行
2. 右键点击 Java 文件
3. 观察日志

**预期**：

- ✅ 不再出现 `ActionUpdateThread` 警告
- ✅ 菜单项正常显示和启用/禁用
- ✅ 插件功能正常工作

---

## 📚 相关文档

### IntelliJ Platform 官方文档

- [ActionUpdateThread](https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#action-update)
- [Threading Rules](https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html)

### 关键要点

1. **从 2022.3 开始**：所有 `AnAction` 都应该实现 `getActionUpdateThread()`
2. **默认行为**：如果不实现，会使用 EDT，但会产生警告
3. **推荐实践**：除非有特殊理由，否则使用 `BGT`
4. **线程安全**：确保 `update()` 方法中的操作是线程安全的

---

## 🎯 何时使用 EDT？

### 使用 EDT 的场景

以下情况需要返回 `ActionUpdateThread.EDT`：

1. **需要 write-action**
   ```java
   ApplicationManager.getApplication().runWriteAction(() -> {
       // 修改 PSI 等操作
   });
   ```

2. **访问 Swing 组件**
   ```java
   JComponent component = e.getPresentation().getClientProperty(...);
   component.setText(...);  // 必须在 EDT
   ```

3. **依赖 EDT 状态**
   ```java
   // 某些 API 要求在 EDT 中调用
   ```

### 我们的情况

我们的 `update()` 方法：

- ✅ 只读取数据
- ✅ 只做类型检查
- ✅ 不修改状态
- ✅ 不访问 UI 组件

**结论**：完全可以在后台线程运行，使用 `BGT` 是正确的选择。

---

## 🔍 相关警告

### 其他警告说明

用户日志中还有其他警告：

```
WARN - 'virtualFile' is requested on EDT by ShowFileInResourceManagerAction#update
```

**说明**：这是 Android Studio 插件的警告，不是我们的插件造成的，无需处理。

```
WARN - ignore deprecated groupId: language for id: preferences.language.Kotlin.scripting
```

**说明**：这是 Kotlin 插件的警告，不是我们的插件造成的，无需处理。

```
WARN - 'AbstractTreeBuilder' is going to be dropped soon and must not be used
```

**说明**：这是 IDEA 核心的警告，不是我们的插件造成的，无需处理。

