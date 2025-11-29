# IntelliJ IDEA 插件 Console 集成指南

## 📋 目录

- [概述](#概述)
- [快速集成](#快速集成)
- [核心组件说明](#核心组件说明)
- [功能特性](#功能特性)
- [使用示例](#使用示例)
- [高级特性](#高级特性)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

---

## 概述

Console 是 IntelliJ IDEA 插件的标准日志输出组件，用于在 IDE 底部的 Tool Window 中显示插件运行时的各种信息。本文档基于 MIK 项目的实现，提供完整的集成指南。

### 主要优势

- ✅ 用户友好：在 IDE 中直接查看日志，无需打开额外的日志文件
- ✅ 实时输出：即时显示任务处理进度和结果
- ✅ 彩色分类：支持普通、成功、警告、错误四种类型，自动着色
- ✅ 自动展示：输出日志时自动显示工具窗口
- ✅ 项目隔离：每个项目独立的 Console 实例

---

## 快速集成

### 步骤 1：创建核心类

创建两个核心类文件：

#### 1.1 MikConsoleView.java

```java
package info.dong4j.idea.plugin.console;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service(Service.Level.PROJECT)
public final class MikConsoleView {
    public static final String TOOL_WINDOW_ID = "MIK Console";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    private ConsoleView consoleView;
    private final Project project;
    
    public MikConsoleView(@NotNull Project project) {
        this.project = project;
    }
    
    public ConsoleView initConsole() {
        if (consoleView == null) {
            consoleView = TextConsoleBuilderFactory.getInstance()
                .createBuilder(project)
                .getConsole();
        }
        return consoleView;
    }
    
    // 其他方法见完整实现
    
    @NotNull
    public static MikConsoleView getInstance(@NotNull Project project) {
        return project.getService(MikConsoleView.class);
    }
}
```

#### 1.2 MikConsoleToolWindowFactory.java

```java
package info.dong4j.idea.plugin.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class MikConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MikConsoleView mikConsoleView = MikConsoleView.getInstance(project);
        ConsoleView consoleView = mikConsoleView.initConsole();
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(consoleView.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
        
        // 输出欢迎信息
        mikConsoleView.print("Welcome to Your Plugin!");
    }
}
```

### 步骤 2：注册到 plugin.xml

在 `plugin.xml` 中注册 ToolWindow：

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Console 工具窗口 -->
    <toolWindow id="MIK Console"
                factoryClass="info.dong4j.idea.plugin.console.MikConsoleToolWindowFactory"
                anchor="bottom"
                icon="icons.AIJicons.MIK"/>
</extensions>
```

**配置说明：**

- `id`: 工具窗口唯一标识符，需要与 `MikConsoleView.TOOL_WINDOW_ID` 一致
- `factoryClass`: 工具窗口工厂类的全限定名
- `anchor`: 工具窗口位置，可选值：`bottom`、`left`、`right`、`top`
- `icon`: 工具窗口图标（可选）

### 步骤 3：使用 Console

在代码中使用：

```java
// 方式 1：静态方法（推荐）
MikConsoleView.printMessage(project, "处理中...");
MikConsoleView.printSuccessMessage(project, "✓ 上传成功");
MikConsoleView.printErrorMessage(project, "✗ 上传失败: " + error);
MikConsoleView.printWarningMessage(project, "⚠ 警告信息");

// 方式 2：获取实例
MikConsoleView console = MikConsoleView.getInstance(project);
console.print("普通信息");
console.printSuccess("成功信息");
console.printError("错误信息");
console.printWarning("警告信息");
```

---

## 核心组件说明

### 1. Service 架构

```java
@Service(Service.Level.PROJECT)
public final class MikConsoleView {
    // 项目级别的服务，每个项目独立的实例
}
```

**关键点：**

- `@Service(Service.Level.PROJECT)`: 项目级别服务，确保每个项目有独立的 Console
- `final` 类：防止被继承
- 通过 `project.getService(MikConsoleView.class)` 获取实例

### 2. ConsoleView 核心 API

#### 创建 Console

```java
consoleView = TextConsoleBuilderFactory.getInstance()
    .createBuilder(project)
    .getConsole();
```

#### 输出内容

```java
console.print(message, ConsoleViewContentType);
```

#### 内容类型（ConsoleViewContentType）

- `NORMAL_OUTPUT`: 普通输出（黑色）
- `LOG_INFO_OUTPUT`: 信息输出（绿色）
- `LOG_WARNING_OUTPUT`: 警告输出（黄色）
- `ERROR_OUTPUT`: 错误输出（红色）
- `SYSTEM_OUTPUT`: 系统输出（蓝色）
- `USER_INPUT`: 用户输入（紫色）

### 3. ToolWindowFactory

实现 `ToolWindowFactory` 接口创建工具窗口内容：

```java
public class MikConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, 
                                       @NotNull ToolWindow toolWindow) {
        // 创建并添加内容
    }
}
```

**DumbAware 接口：**

- 实现此接口表示组件在索引构建期间也可以工作
- 对于 Console 这种基础组件，建议实现

---

## 功能特性

### 1. 自动时间戳

每条日志自动添加时间戳：

```java
String timestamp = TIME_FORMAT.format(new Date());
console.print("[" + timestamp + "] " + message + "\n", contentType);
```

**输出示例：**

```
[14:23:45] 开始处理图片...
[14:23:46] ✓ 图片上传成功
```

### 2. 自动显示工具窗口

输出日志时自动展示 Console 窗口：

```java
private void showToolWindow() {
    ApplicationManager.getApplication().invokeLater(() -> {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null && !toolWindow.isVisible()) {
            toolWindow.show(null);
        }
    });
}
```

### 3. 彩色分类输出

支持四种类型的输出：

```java
// 普通信息（黑色）
public void print(String message) {
    print(message, ConsoleViewContentType.NORMAL_OUTPUT);
}

// 成功信息（绿色）
public void printSuccess(String message) {
    print(message, ConsoleViewContentType.LOG_INFO_OUTPUT);
}

// 错误信息（红色）
public void printError(String message) {
    print(message, ConsoleViewContentType.ERROR_OUTPUT);
}

// 警告信息（黄色）
public void printWarning(String message) {
    print(message, ConsoleViewContentType.LOG_WARNING_OUTPUT);
}
```

### 4. 智能输出

根据消息内容自动选择输出类型：

```java
public static void printSmart(Project project, @NotNull String message) {
    MikConsoleView consoleView = getInstance(project);
    if (message.contains("✗") || message.contains("失败") || message.contains("错误")) {
        consoleView.printError(message);
    } else if (message.contains("✓") || message.contains("完成") || message.contains("成功")) {
        consoleView.printSuccess(message);
    } else if (message.contains("警告")) {
        consoleView.printWarning(message);
    } else {
        consoleView.print(message);
    }
}
```

**使用示例：**

```java
MikConsoleView.printSmart(project, "✓ 操作成功");  // 自动使用绿色
MikConsoleView.printSmart(project, "✗ 操作失败");  // 自动使用红色
MikConsoleView.printSmart(project, "警告: 文件已存在"); // 自动使用黄色
```

### 5. 清空控制台

```java
public void clear() {
    ApplicationManager.getApplication().invokeLater(() -> {
        if (consoleView != null) {
            consoleView.clear();
        }
    });
}
```

### 6. 资源释放

```java
public void dispose() {
    if (consoleView != null) {
        consoleView.dispose();
        consoleView = null;
    }
}
```

### 7. 控制台开关

支持通过配置控制是否启用控制台日志：

```java
private static boolean isConsoleLogEnabled() {
    try {
        return MikPersistenComponent.getInstance()
            .getState()
            .isEnableConsoleLog();
    } catch (Exception e) {
        return true; // 默认启用
    }
}

public static void printMessage(Project project, @NotNull String message) {
    if (project == null || !isConsoleLogEnabled()) {
        return;
    }
    getInstance(project).print(message);
}
```

---

## 使用示例

### 示例 1：简单日志输出

```java
public void uploadImage(Project project, File imageFile) {
    MikConsoleView.printMessage(project, "开始上传图片: " + imageFile.getName());
    
    try {
        // 上传逻辑
        String url = upload(imageFile);
        MikConsoleView.printSuccessMessage(project, "✓ 上传成功: " + url);
    } catch (Exception e) {
        MikConsoleView.printErrorMessage(project, "✗ 上传失败: " + e.getMessage());
    }
}
```

### 示例 2：进度跟踪

```java
public void batchProcess(Project project, List<File> files) {
    MikConsoleView console = MikConsoleView.getInstance(project);
    
    console.print("======================================");
    console.print("开始批量处理，共 " + files.size() + " 个文件");
    console.print("======================================");
    
    int success = 0;
    int failed = 0;
    
    for (int i = 0; i < files.size(); i++) {
        File file = files.get(i);
        console.print(String.format("[%d/%d] 处理: %s", 
            i + 1, files.size(), file.getName()));
        
        try {
            process(file);
            console.printSuccess("  ✓ 处理成功");
            success++;
        } catch (Exception e) {
            console.printError("  ✗ 处理失败: " + e.getMessage());
            failed++;
        }
    }
    
    console.print("======================================");
    console.print(String.format("处理完成: 成功 %d，失败 %d", success, failed));
    console.print("======================================");
}
```

### 示例 3：详细日志

```java
public void complexTask(Project project) {
    MikConsoleView console = MikConsoleView.getInstance(project);
    
    console.print("========== 开始复杂任务 ==========");
    
    // 步骤 1
    console.print("步骤 1/3: 验证环境");
    if (validateEnvironment()) {
        console.printSuccess("  ✓ 环境验证通过");
    } else {
        console.printError("  ✗ 环境验证失败");
        return;
    }
    
    // 步骤 2
    console.print("步骤 2/3: 处理数据");
    try {
        processData();
        console.printSuccess("  ✓ 数据处理完成");
    } catch (Exception e) {
        console.printError("  ✗ 数据处理失败: " + e.getMessage());
        return;
    }
    
    // 步骤 3
    console.print("步骤 3/3: 保存结果");
    try {
        saveResults();
        console.printSuccess("  ✓ 结果保存成功");
    } catch (Exception e) {
        console.printError("  ✗ 结果保存失败: " + e.getMessage());
        return;
    }
    
    console.print("========== 任务完成 ==========");
}
```

### 示例 4：实时流式输出

```java
public void streamProcess(Project project) {
    MikConsoleView console = MikConsoleView.getInstance(project);
    
    console.print("开始流式处理...");
    
    // 模拟实时输出
    for (int i = 0; i < 10; i++) {
        console.print("正在处理第 " + (i + 1) + " 项...");
        Thread.sleep(500); // 模拟耗时操作
    }
    
    console.printSuccess("流式处理完成");
}
```

---

## 高级特性

### 1. 自定义过滤器（Filter）

添加文件路径、URL 等可点击的链接：

```java
public ConsoleView initConsole() {
    if (consoleView == null) {
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project);
        
        // 添加 URL 过滤器，使 URL 可点击
        builder.addFilter(new UrlFilter());
        
        // 添加文件路径过滤器，使文件路径可点击
        builder.addFilter(new RegexpFilter(project, 
            "\\b[A-Za-z]:[/\\\\][^:*?\"<>|\\r\\n]+"));
        
        consoleView = builder.getConsole();
    }
    return consoleView;
}
```

### 2. 添加工具栏动作

在 Console 上方添加自定义按钮：

```java
@Override
public void createToolWindowContent(@NotNull Project project, 
                                   @NotNull ToolWindow toolWindow) {
    MikConsoleView mikConsoleView = MikConsoleView.getInstance(project);
    ConsoleView consoleView = mikConsoleView.initConsole();
    
    // 创建动作组
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new ClearConsoleAction(mikConsoleView));
    actionGroup.add(new ExportConsoleAction(mikConsoleView));
    
    // 创建工具栏
    ActionToolbar toolbar = ActionManager.getInstance()
        .createActionToolbar("MikConsole", actionGroup, false);
    toolbar.setTargetComponent(consoleView.getComponent());
    
    // 组合布局
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(toolbar.getComponent(), BorderLayout.WEST);
    panel.add(consoleView.getComponent(), BorderLayout.CENTER);
    
    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(panel, "", false);
    toolWindow.getContentManager().addContent(content);
}
```

### 3. 自定义清空动作

```java
public class ClearConsoleAction extends AnAction {
    private final MikConsoleView consoleView;
    
    public ClearConsoleAction(MikConsoleView consoleView) {
        super("Clear", "Clear console", AllIcons.Actions.GC);
        this.consoleView = consoleView;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        consoleView.clear();
    }
}
```

### 4. 导出日志

```java
public class ExportConsoleAction extends AnAction {
    private final MikConsoleView consoleView;
    
    public ExportConsoleAction(MikConsoleView consoleView) {
        super("Export", "Export console content", AllIcons.Actions.Export);
        this.consoleView = consoleView;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取控制台文本
        ConsoleView console = consoleView.getConsoleView();
        Editor editor = console.getEditor();
        String text = editor.getDocument().getText();
        
        // 保存到文件
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
            false, true, false, false, false, false);
        VirtualFile file = FileChooser.chooseFile(descriptor, 
            e.getProject(), null);
        
        if (file != null) {
            try {
                File outputFile = new File(file.getPath(), "console.log");
                Files.writeString(outputFile.toPath(), text);
                consoleView.printSuccess("日志已导出到: " + outputFile.getPath());
            } catch (IOException ex) {
                consoleView.printError("导出失败: " + ex.getMessage());
            }
        }
    }
}
```

### 5. 限制日志大小

防止日志过多导致内存问题：

```java
public ConsoleView initConsole() {
    if (consoleView == null) {
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project);
        
        consoleView = builder.getConsole();
        
        // 设置缓冲区大小（字符数）
        consoleView.getComponent().putClientProperty(
            ConsoleView.KEY_CYCLIC_BUFFER_SIZE, 1024 * 1024); // 1MB
    }
    return consoleView;
}
```

### 6. 分组输出

使用可折叠的分组：

```java
public void printGroup(String title, Runnable content) {
    print("▼ " + title);
    try {
        content.run();
    } finally {
        print("▲ " + title + " 结束");
    }
}

// 使用
console.printGroup("上传任务", () -> {
    console.print("  准备文件...");
    console.print("  连接服务器...");
    console.print("  上传中...");
    console.printSuccess("  上传完成");
});
```

### 7. 进度条输出

```java
public void printProgress(int current, int total, String message) {
    int percent = (int) ((current * 100.0) / total);
    int barLength = 20;
    int filled = (int) ((current * barLength) / total);
    
    StringBuilder bar = new StringBuilder("[");
    for (int i = 0; i < barLength; i++) {
        bar.append(i < filled ? "=" : " ");
    }
    bar.append("] ").append(percent).append("% - ").append(message);
    
    print(bar.toString());
}

// 使用
for (int i = 0; i <= 100; i += 10) {
    console.printProgress(i, 100, "处理中...");
    Thread.sleep(100);
}
```

---

## 最佳实践

### 1. 线程安全

所有 Console 操作都应在 EDT 线程执行：

```java
private void print(String message, ConsoleViewContentType contentType) {
    ApplicationManager.getApplication().invokeLater(() -> {
        ConsoleView console = getConsoleView();
        if (console != null) {
            console.print(message, contentType);
        }
    });
}
```

### 2. 异常处理

始终捕获异常，避免 Console 错误影响主功能：

```java
public static void printMessage(Project project, @NotNull String message) {
    if (project == null) {
        return;
    }
    try {
        getInstance(project).print(message);
    } catch (Exception e) {
        // 记录到日志，但不中断程序
        log.trace("输出到控制台失败", e);
    }
}
```

### 3. 空值检查

```java
public static void printMessage(Project project, @NotNull String message) {
    if (project == null || !isConsoleLogEnabled()) {
        return;
    }
    // ...
}
```

### 4. 延迟初始化

Console 只在需要时创建：

```java
public ConsoleView getConsoleView() {
    if (consoleView == null) {
        initConsole();
    }
    return consoleView;
}
```

### 5. 资源清理

在项目关闭时清理资源：

```java
public void dispose() {
    if (consoleView != null) {
        consoleView.dispose();
        consoleView = null;
    }
}
```

### 6. 日志格式化

使用统一的日志格式：

```java
// ✅ 好的做法
console.print("[步骤 1/3] 验证环境");
console.printSuccess("  ✓ 验证通过");
console.printError("  ✗ 验证失败: 缺少配置");

// ❌ 不好的做法
console.print("验证环境");
console.print("验证通过");
console.print("验证失败缺少配置");
```

### 7. 使用符号增强可读性

```java
// 成功：✓ ✔ ☑
console.printSuccess("✓ 操作成功");

// 失败：✗ ✘ ☒
console.printError("✗ 操作失败");

// 警告：⚠ ⚡ ⚑
console.printWarning("⚠ 注意事项");

// 信息：ℹ ➤ ▶
console.print("ℹ 提示信息");

// 分隔：═ ─ ━
console.print("════════════════════");
```

### 8. 结构化输出

```java
// ✅ 好的做法：清晰的层次结构
console.print("========== 开始处理 ==========");
console.print("步骤 1: 验证");
console.print("  → 检查文件");
console.printSuccess("    ✓ 文件有效");
console.print("  → 检查网络");
console.printSuccess("    ✓ 网络正常");
console.print("步骤 2: 上传");
console.print("  → 连接服务器");
console.printSuccess("    ✓ 连接成功");
console.print("========== 处理完成 ==========");
```

---

## 常见问题

### Q1: Console 不显示内容？

**原因：**

- ConsoleView 未正确初始化
- 没有在 EDT 线程执行
- ToolWindow 未注册

**解决：**

```java
// 确保在 EDT 线程
ApplicationManager.getApplication().invokeLater(() -> {
    console.print(message);
});

// 确保 plugin.xml 注册正确
<toolWindow id="MIK Console" ... />
```

### Q2: 如何自定义颜色？

```java
// 创建自定义颜色类型
ConsoleViewContentType CUSTOM_TYPE = new ConsoleViewContentType(
    "CUSTOM", 
    new TextAttributes(
        JBColor.BLUE,        // 前景色
        null,                // 背景色
        null,                // 效果颜色
        null,                // 效果类型
        Font.PLAIN           // 字体样式
    )
);

// 使用
console.print(message, CUSTOM_TYPE);
```

### Q3: 如何实现可点击的链接？

```java
public ConsoleView initConsole() {
    TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance()
        .createBuilder(project);
    
    // 添加 URL 过滤器
    builder.addFilter(new UrlFilter());
    
    // 添加自定义过滤器
    builder.addFilter(new Filter() {
        @Override
        public Result applyFilter(String line, int entireLength) {
            // 自定义匹配逻辑
            return null;
        }
    });
    
    consoleView = builder.getConsole();
    return consoleView;
}
```

### Q4: 如何实现多标签页？

```java
@Override
public void createToolWindowContent(@NotNull Project project, 
                                   @NotNull ToolWindow toolWindow) {
    ContentFactory contentFactory = ContentFactory.getInstance();
    
    // 创建多个 Console
    ConsoleView console1 = createConsole(project);
    Content content1 = contentFactory.createContent(
        console1.getComponent(), "任务日志", false);
    
    ConsoleView console2 = createConsole(project);
    Content content2 = contentFactory.createContent(
        console2.getComponent(), "错误日志", false);
    
    toolWindow.getContentManager().addContent(content1);
    toolWindow.getContentManager().addContent(content2);
}
```

### Q5: 如何持久化日志？

```java
public void exportToFile(String filePath) {
    ApplicationManager.getApplication().runReadAction(() -> {
        Editor editor = consoleView.getEditor();
        String content = editor.getDocument().getText();
        
        try {
            Files.writeString(Path.of(filePath), content);
        } catch (IOException e) {
            printError("导出失败: " + e.getMessage());
        }
    });
}
```

### Q6: 如何限制日志条数？

```java
private final int MAX_LINES = 1000;
private int lineCount = 0;

private void print(String message, ConsoleViewContentType contentType) {
    ApplicationManager.getApplication().invokeLater(() -> {
        if (lineCount >= MAX_LINES) {
            // 清空一半的日志
            clearHalfLogs();
            lineCount = MAX_LINES / 2;
        }
        
        console.print(message + "\n", contentType);
        lineCount++;
    });
}

private void clearHalfLogs() {
    Editor editor = consoleView.getEditor();
    Document doc = editor.getDocument();
    int halfPos = doc.getLineEndOffset(doc.getLineCount() / 2);
    WriteCommandAction.runWriteCommandAction(project, () -> {
        doc.deleteString(0, halfPos);
    });
}
```

---

## 总结

通过本指南，你应该能够：

1. ✅ 在 IDEA 插件中集成 Console
2. ✅ 使用各种输出类型（普通、成功、警告、错误）
3. ✅ 实现自动显示、清空、导出等功能
4. ✅ 添加自定义过滤器和工具栏
5. ✅ 遵循最佳实践避免常见问题

Console 是提升插件用户体验的重要工具，合理使用能让用户清楚了解插件的运行状态。

---

## 参考资源

- [IntelliJ Platform SDK - Tool Windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)
- [IntelliJ Platform SDK - Console](https://plugins.jetbrains.com/docs/intellij/execution.html)
- [MIK Console 实现源码](../src/main/java/info/dong4j/idea/plugin/console/)

