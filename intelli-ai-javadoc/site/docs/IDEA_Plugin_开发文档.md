# IntelliJ IDEA Plugin 开发完整指南

本文档基于 IntelliJ IntelliAI Javadoc 项目，详细介绍 IntelliJ IDEA 插件的开发流程、核心概念、最佳实践和常见问题。

## 目录

1. [开发环境准备](#1-开发环境准备)
2. [项目结构](#2-项目结构)
3. [核心概念](#3-核心概念)
4. [插件开发基础](#4-插件开发基础)
5. [高级特性](#5-高级特性)
6. [测试与调试](#6-测试与调试)
7. [发布与分发](#7-发布与分发)
8. [最佳实践](#8-最佳实践)

---

## 1. 开发环境准备

### 1.1 必需工具

| 工具            | 版本要求    | 说明                            |
|---------------|---------|-------------------------------|
| IntelliJ IDEA | 2021.3+ | 推荐使用 Ultimate 版，Community 版也可 |
| JDK           | 11+     | 插件开发推荐使用 JDK 11               |
| Gradle        | 7.0+    | 构建工具                          |
| Git           | 最新版     | 版本控制                          |

### 1.2 创建新项目

#### 方式一：使用 IntelliJ IDEA 向导

1. 打开 IntelliJ IDEA
2. 选择 **File → New → Project**
3. 选择 **Gradle** → **IntelliJ Platform Plugin**
4. 配置项目信息：
    - Name: `my-plugin`
    - Location: 项目路径
    - Language: Java
    - Build system: Gradle
    - JDK: 11+
5. 点击 **Create**

#### 方式二：手动配置 Gradle 项目

创建 `build.gradle.kts`：

```kotlin
plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.6.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

intellij {
    version.set("2021.3")
    type.set("IC") // IC = Community, IU = Ultimate
    plugins.set(listOf("java"))
}

dependencies {
    // 添加依赖
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("223.*")
    }
}
```

### 1.3 项目配置说明

#### IntelliJ Platform Gradle Plugin 配置

```kotlin
intellij {
    // IDE 版本
    version.set("2021.3")

    // IDE 类型：IC (Community) 或 IU (Ultimate)
    type.set("IC")

    // 依赖的插件
    plugins.set(listOf("java", "kotlin"))

    // 下载源码（用于调试）
    downloadSources.set(true)
}
```

#### 版本兼容性配置

```kotlin
patchPluginXml {
    // 最低支持版本（213 = 2021.3）
    sinceBuild.set("213")

    // 最高支持版本（223.* = 2022.3.x）
    untilBuild.set("223.*")
}
```

**版本号规则**：

- 格式：`BRANCH.BUILD.FIX`
- 示例：`213.1234.56`
- `213` = 2021.3
- `221` = 2022.1
- `223` = 2022.3

---

## 2. 项目结构

### 2.1 标准项目结构

```
my-plugin/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── plugin/
│   │   │               ├── action/        # 动作类
│   │   │               ├── service/       # 服务类
│   │   │               ├── config/        # 配置类
│   │   │               └── ui/            # UI 组件
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   ├── plugin.xml             # 插件描述文件
│   │       │   └── pluginIcon.svg         # 插件图标
│   │       └── messages/
│   │           └── MyBundle.properties    # 国际化资源
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── plugin/
├── build.gradle.kts                        # 构建配置
├── settings.gradle.kts                     # 项目设置
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                                 # Gradle 包装脚本（Unix）
├── gradlew.bat                             # Gradle 包装脚本（Windows）
└── README.md
```

### 2.2 plugin.xml 详解

`plugin.xml` 是插件的核心配置文件：

```xml
<idea-plugin>
    <!-- 插件唯一标识符 -->
    <id>com.example.my-plugin</id>

    <!-- 插件名称 -->
    <name>My Awesome Plugin</name>

    <!-- 开发者信息 -->
    <vendor email="support@example.com" url="https://example.com">
        Example Corp
    </vendor>

    <!-- 插件描述 -->
    <description><![CDATA[
        This plugin does amazing things.
        <ul>
            <li>Feature 1</li>
            <li>Feature 2</li>
        </ul>
    ]]></description>

    <!-- 变更日志 -->
    <change-notes><![CDATA[
        <h3>Version 1.0</h3>
        <ul>
            <li>Initial release</li>
        </ul>
    ]]></change-notes>

    <!-- 依赖的模块 -->
    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.java</depends>

    <!-- 扩展点 -->
    <extensions defaultExtensionNs="com.intellij">
        <!-- 项目服务 -->
        <projectService
            serviceImplementation="com.example.plugin.MyService"/>

        <!-- 应用级服务 -->
        <applicationService
            serviceImplementation="com.example.plugin.MyAppService"/>

        <!-- 配置面板 -->
        <projectConfigurable
            instance="com.example.plugin.MyConfigurable"/>
    </extensions>

    <!-- 动作 -->
    <actions>
        <action
            id="com.example.MyAction"
            class="com.example.plugin.action.MyAction"
            text="My Action"
            description="Does something cool">
            <!-- 快捷键 -->
            <keyboard-shortcut
                keymap="$default"
                first-keystroke="shift ctrl A"/>
            <!-- 添加到菜单 -->
            <add-to-group
                group-id="ToolsMenu"
                anchor="first"/>
        </action>
    </actions>
</idea-plugin>
```

---

## 3. 核心概念

### 3.1 PSI (Program Structure Interface)

PSI 是 IntelliJ Platform 用于表示代码结构的核心 API。

#### 3.1.1 PSI 元素层次结构

```
PsiElement (根接口)
├── PsiFile                      # 文件
│   ├── PsiJavaFile             # Java 文件
│   ├── PsiXmlFile              # XML 文件
│   └── ...
├── PsiClass                     # 类/接口
├── PsiMethod                    # 方法
├── PsiField                     # 字段
├── PsiParameter                 # 参数
├── PsiStatement                 # 语句
├── PsiExpression                # 表达式
└── PsiComment                   # 注释
```

#### 3.1.2 获取 PSI 元素

**从编辑器获取**：

```java
Editor editor = e.getData(CommonDataKeys.EDITOR);
Project project = e.getProject();
PsiFile psiFile = PsiDocumentManager.getInstance(project)
    .getPsiFile(editor.getDocument());
```

**从虚拟文件获取**：

```java
VirtualFile virtualFile = ...;
PsiFile psiFile = PsiManager.getInstance(project)
    .findFile(virtualFile);
```

**查找特定元素**：

```java
// 查找光标位置的元素
int offset = editor.getCaretModel().getOffset();
PsiElement element = psiFile.findElementAt(offset);

// 查找类
PsiClass psiClass = PsiTreeUtil.getParentOfType(
    element, PsiClass.class);

// 查找方法
PsiMethod psiMethod = PsiTreeUtil.getParentOfType(
    element, PsiMethod.class);
```

#### 3.1.3 遍历 PSI 树

**使用访问者模式**：

```java
psiFile.accept(new JavaRecursiveElementVisitor() {
    @Override
    public void visitClass(PsiClass aClass) {
        super.visitClass(aClass);
        System.out.println("Found class: " + aClass.getName());
    }

    @Override
    public void visitMethod(PsiMethod method) {
        super.visitMethod(method);
        System.out.println("Found method: " + method.getName());
    }
});
```

**使用 PsiTreeUtil**：

```java
// 查找所有子类
Collection<PsiClass> classes = PsiTreeUtil.findChildrenOfType(
    psiFile, PsiClass.class);

// 查找所有方法
Collection<PsiMethod> methods = PsiTreeUtil.findChildrenOfType(
    psiClass, PsiMethod.class);
```

### 3.2 Virtual File System (VFS)

VFS 是 IntelliJ Platform 的虚拟文件系统。

#### 3.2.1 VirtualFile vs PsiFile

| 特性 | VirtualFile | PsiFile  |
|----|-------------|----------|
| 层级 | 文件系统级别      | 代码结构级别   |
| 内容 | 原始文件内容      | 解析后的代码结构 |
| 缓存 | 文件系统缓存      | PSI 缓存   |
| 用途 | 文件操作        | 代码分析和修改  |

#### 3.2.2 获取 VirtualFile

```java
// 从 PSI 获取
VirtualFile vf = psiFile.getVirtualFile();

// 从路径获取
VirtualFile vf = LocalFileSystem.getInstance()
    .findFileByPath("/path/to/file");

// 从编辑器获取
VirtualFile vf = FileDocumentManager.getInstance()
    .getFile(editor.getDocument());
```

### 3.3 Document API

Document 表示可编辑的文本文档。

#### 3.3.1 Document 操作

```java
// 获取 Document
Document document = FileDocumentManager.getInstance()
    .getDocument(virtualFile);

// 读取内容
String text = document.getText();
String lineText = document.getText(
    new TextRange(startOffset, endOffset));

// 修改内容（必须在 WriteAction 中）
ApplicationManager.getApplication().runWriteAction(() -> {
    document.insertString(offset, "text");
    document.deleteString(startOffset, endOffset);
    document.replaceString(startOffset, endOffset, "newText");
});

// 提交修改（同步到 PSI）
PsiDocumentManager.getInstance(project).commitDocument(document);
```

#### 3.3.2 Document 与 PSI 同步

```java
// Document 变更 → PSI
PsiDocumentManager psiDocManager =
    PsiDocumentManager.getInstance(project);

// 立即提交
psiDocManager.commitDocument(document);

// 等待所有操作完成
psiDocManager.doPostponedOperationsAndUnblockDocument(document);

// 检查是否有未提交的更改
boolean hasUncommitted = psiDocManager.isUncommited(document);
```

### 3.4 Thread Model（线程模型）

IntelliJ Platform 有严格的线程模型。

#### 3.4.1 线程类型

| 线程类型                        | 说明    | 用途              |
|-----------------------------|-------|-----------------|
| Event Dispatch Thread (EDT) | UI 线程 | UI 操作           |
| Read Thread                 | 读线程   | 读取 PSI          |
| Write Thread                | 写线程   | 修改 PSI/Document |
| Background Thread           | 后台线程  | 耗时操作            |

#### 3.4.2 线程操作 API

**读操作**：

```java
ApplicationManager.getApplication().runReadAction(() -> {
    // 读取 PSI
    String name = psiClass.getName();
});
```

**写操作**：

```java
ApplicationManager.getApplication().runWriteAction(() -> {
    // 修改 Document 或 PSI
    document.insertString(offset, text);
});
```

**后台任务**：

```java
ProgressManager.getInstance().run(
    new Task.Backgroundable(project, "Task Title") {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            indicator.setText("Processing...");
            // 耗时操作
        }
    }
);
```

**Command（支持撤销）**：

```java
CommandProcessor.getInstance().executeCommand(
    project,
    () -> ApplicationManager.getApplication().runWriteAction(() -> {
        // 可撤销的写操作
    }),
    "Command Name",
    "Group ID"
);
```

---

## 4. 插件开发基础

### 4.1 Actions（动作）

Actions 是插件与用户交互的主要方式。

#### 4.1.1 创建 Action

```java
package com.example.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class MyAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取上下文
        Project project = e.getProject();
        if (project == null) return;

        // 执行操作
        Messages.showMessageDialog(
            project,
            "Hello from my plugin!",
            "Information",
            Messages.getInformationIcon()
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 更新动作状态
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}
```

#### 4.1.2 注册 Action

在 `plugin.xml` 中注册：

```xml
<actions>
    <action
        id="MyPlugin.MyAction"
        class="com.example.plugin.action.MyAction"
        text="My Action"
        description="My action description"
        icon="AllIcons.Actions.Execute">

        <!-- 添加到工具菜单 -->
        <add-to-group group-id="ToolsMenu" anchor="first"/>

        <!-- 快捷键 -->
        <keyboard-shortcut
            keymap="$default"
            first-keystroke="shift ctrl M"/>
    </action>

    <!-- 动作组 -->
    <group id="MyPlugin.ActionGroup" text="My Plugin">
        <action id="MyPlugin.Action1"
                class="com.example.plugin.action.Action1"/>
        <action id="MyPlugin.Action2"
                class="com.example.plugin.action.Action2"/>
        <add-to-group group-id="ToolsMenu" anchor="last"/>
    </group>
</actions>
```

#### 4.1.3 动作放置位置

常用的动作组 ID：

| Group ID                | 位置         |
|-------------------------|------------|
| `EditorPopupMenu`       | 编辑器右键菜单    |
| `ProjectViewPopupMenu`  | 项目视图右键菜单   |
| `MainMenu`              | 主菜单栏       |
| `ToolsMenu`             | 工具菜单       |
| `MainToolBar`           | 主工具栏       |
| `EditorGutterPopupMenu` | 编辑器左侧栏右键菜单 |

#### 4.1.4 获取上下文数据

```java
@Override
public void actionPerformed(@NotNull AnActionEvent e) {
    // 项目
    Project project = e.getProject();

    // 编辑器
    Editor editor = e.getData(CommonDataKeys.EDITOR);

    // PSI 文件
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

    // 虚拟文件
    VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

    // 选中的文件数组
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
}
```

### 4.2 Services（服务）

Services 用于共享应用或项目级别的功能。

#### 4.2.1 服务类型

| 类型                  | 生命周期 | 用途      |
|---------------------|------|---------|
| Application Service | 整个应用 | 全局配置、缓存 |
| Project Service     | 单个项目 | 项目相关功能  |
| Module Service      | 单个模块 | 模块相关功能  |

#### 4.2.2 创建服务

**项目服务**：

```java
package com.example.plugin.service;

import com.intellij.openapi.project.Project;

public class MyProjectService {
    private final Project project;

    public MyProjectService(Project project) {
        this.project = project;
    }

    public void doSomething() {
        // 项目相关操作
    }

    // 获取服务实例的静态方法
    public static MyProjectService getInstance(Project project) {
        return project.getService(MyProjectService.class);
    }
}
```

**应用服务**：

```java
package com.example.plugin.service;

import com.intellij.openapi.application.ApplicationManager;

public class MyApplicationService {

    public void doSomething() {
        // 应用级操作
    }

    // 获取服务实例
    public static MyApplicationService getInstance() {
        return ApplicationManager.getApplication()
            .getService(MyApplicationService.class);
    }
}
```

#### 4.2.3 注册服务

在 `plugin.xml` 中注册：

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 项目服务 -->
    <projectService
        serviceImplementation="com.example.plugin.service.MyProjectService"/>

    <!-- 应用服务 -->
    <applicationService
        serviceImplementation="com.example.plugin.service.MyApplicationService"/>
</extensions>
```

#### 4.2.4 使用服务

```java
// 获取项目服务
MyProjectService service = project.getService(MyProjectService.class);
// 或
MyProjectService service = MyProjectService.getInstance(project);

// 获取应用服务
MyApplicationService appService =
    ApplicationManager.getApplication()
        .getService(MyApplicationService.class);
// 或
MyApplicationService appService = MyApplicationService.getInstance();
```

### 4.3 Extensions（扩展点）

Extensions 允许插件扩展 IDE 的功能。

#### 4.3.1 常用扩展点

**配置面板**：

```xml
<extensions defaultExtensionNs="com.intellij">
    <projectConfigurable
        instance="com.example.plugin.MyConfigurable"
        displayName="My Plugin Settings"/>
</extensions>
```

```java
public class MyConfigurable implements Configurable {
    @Override
    public String getDisplayName() {
        return "My Plugin";
    }

    @Override
    public JComponent createComponent() {
        // 创建 UI 组件
        return new JPanel();
    }

    @Override
    public boolean isModified() {
        // 检查是否有修改
        return false;
    }

    @Override
    public void apply() {
        // 应用设置
    }
}
```

**文件类型**：

```xml
<extensions defaultExtensionNs="com.intellij">
    <fileType
        name="My File Type"
        implementationClass="com.example.plugin.MyFileType"
        fieldName="INSTANCE"
        language="MyLanguage"
        extensions="myext"/>
</extensions>
```

**语言支持**：

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 语法高亮 -->
    <lang.syntaxHighlighterFactory
        language="MyLanguage"
        implementationClass="com.example.plugin.MySyntaxHighlighter"/>

    <!-- 代码补全 -->
    <completion.contributor
        language="MyLanguage"
        implementationClass="com.example.plugin.MyCompletionContributor"/>
</extensions>
```

### 4.4 UI Components（UI 组件）

#### 4.4.1 对话框

**简单消息框**：

```java
Messages.showMessageDialog(
    project,
    "Message content",
    "Title",
    Messages.getInformationIcon()
);
```

**确认对话框**：

```java
int result = Messages.showYesNoDialog(
    project,
    "Are you sure?",
    "Confirmation",
    Messages.getQuestionIcon()
);

if (result == Messages.YES) {
    // 用户点击了 Yes
}
```

**自定义对话框**：

```java
public class MyDialog extends DialogWrapper {

    public MyDialog(Project project) {
        super(project);
        init();
        setTitle("My Dialog");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Content"), BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void doOKAction() {
        // OK 按钮处理
        super.doOKAction();
    }
}

// 使用
MyDialog dialog = new MyDialog(project);
if (dialog.showAndGet()) {
    // 用户点击了 OK
}
```

#### 4.4.2 通知

```java
// 信息通知
Notifications.Bus.notify(
    new Notification(
        "MyPlugin",
        "Title",
        "Content",
        NotificationType.INFORMATION
    ),
    project
);

// 错误通知
Notifications.Bus.notify(
    new Notification(
        "MyPlugin",
        "Error",
        "Something went wrong",
        NotificationType.ERROR
    ),
    project
);

// 带动作的通知
Notification notification = new Notification(
    "MyPlugin",
    "Title",
    "Content",
    NotificationType.INFORMATION
);
notification.addAction(new AnAction("Action") {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 处理点击
    }
});
Notifications.Bus.notify(notification, project);
```

#### 4.4.3 工具窗口

```xml
<extensions defaultExtensionNs="com.intellij">
    <toolWindow
        id="MyToolWindow"
        factoryClass="com.example.plugin.MyToolWindowFactory"
        anchor="right"
        icon="AllIcons.Toolwindows.Documentation"/>
</extensions>
```

```java
public class MyToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(
            @NotNull Project project,
            @NotNull ToolWindow toolWindow) {

        MyToolWindow myToolWindow = new MyToolWindow(project);
        ContentFactory contentFactory =
            ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(
            myToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

public class MyToolWindow {
    private JPanel contentPanel;

    public MyToolWindow(Project project) {
        contentPanel = new JPanel();
        // 初始化 UI
    }

    public JComponent getContent() {
        return contentPanel;
    }
}
```

---

## 5. 高级特性

### 5.1 代码检查（Inspection）

创建自定义代码检查：

```java
public class MyInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                // 检查逻辑
                if (method.getName().startsWith("bad")) {
                    holder.registerProblem(
                        method.getNameIdentifier(),
                        "Method name should not start with 'bad'",
                        new MyQuickFix()
                    );
                }
            }
        };
    }
}

// 快速修复
public class MyQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getFamilyName() {
        return "Fix method name";
    }

    @Override
    public void applyFix(
            @NotNull Project project,
            @NotNull ProblemDescriptor descriptor) {

        // 修复逻辑
        PsiElement element = descriptor.getPsiElement();
        // ...
    }
}
```

注册：

```xml
<extensions defaultExtensionNs="com.intellij">
    <localInspection
        language="JAVA"
        displayName="My Inspection"
        groupName="My Plugin"
        enabledByDefault="true"
        level="WARNING"
        implementationClass="com.example.plugin.MyInspection"/>
</extensions>
```

### 5.2 代码意图（Intention）

创建代码意图：

```java
public class MyIntentionAction extends PsiElementBaseIntentionAction {

    @NotNull
    @Override
    public String getText() {
        return "Do something";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "My Plugin";
    }

    @Override
    public boolean isAvailable(
            @NotNull Project project,
            Editor editor,
            @NotNull PsiElement element) {

        // 判断是否可用
        return element instanceof PsiMethod;
    }

    @Override
    public void invoke(
            @NotNull Project project,
            Editor editor,
            @NotNull PsiElement element) {

        // 执行意图
        PsiMethod method = (PsiMethod) element;
        // ...
    }
}
```

注册：

```xml
<extensions defaultExtensionNs="com.intellij">
    <intentionAction>
        <className>com.example.plugin.MyIntentionAction</className>
    </intentionAction>
</extensions>
```

### 5.3 代码生成

```java
public class MyGenerator {

    public void generateMethod(PsiClass psiClass, Project project) {
        // 创建方法
        PsiElementFactory factory =
            JavaPsiFacade.getInstance(project).getElementFactory();

        PsiMethod method = factory.createMethodFromText(
            "public void myMethod() { }",
            psiClass
        );

        // 添加到类中
        ApplicationManager.getApplication().runWriteAction(() -> {
            psiClass.add(method);
        });
    }

    public void generateClass(PsiDirectory directory, Project project) {
        PsiElementFactory factory =
            JavaPsiFacade.getInstance(project).getElementFactory();

        PsiClass psiClass = factory.createClass("MyClass");

        ApplicationManager.getApplication().runWriteAction(() -> {
            directory.add(psiClass);
        });
    }
}
```

### 5.4 重构

支持重构操作：

```java
public class MyRefactoringProcessor extends BaseRefactoringProcessor {

    public MyRefactoringProcessor(Project project) {
        super(project);
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(
            @NotNull UsageInfo[] usages) {
        return new MyUsageViewDescriptor();
    }

    @NotNull
    @Override
    protected UsageInfo[] findUsages() {
        // 查找需要修改的位置
        return new UsageInfo[0];
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo[] usages) {
        // 执行重构
    }

    @NotNull
    @Override
    protected String getCommandName() {
        return "My Refactoring";
    }
}
```

---

## 6. 测试与调试

### 6.1 单元测试

创建测试类：

```java
public class MyPluginTest extends BasePlatformTestCase {

    @Test
    public void testMyAction() {
        // 准备测试数据
        myFixture.configureByText(
            JavaFileType.INSTANCE,
            "public class Test { <caret> }"
        );

        // 执行动作
        myFixture.testAction(new MyAction());

        // 验证结果
        PsiFile file = myFixture.getFile();
        assertNotNull(file);
    }

    @Test
    public void testCodeGeneration() {
        PsiClass psiClass = myFixture.addClass(
            "public class TestClass {}"
        );

        MyGenerator generator = new MyGenerator();
        generator.generateMethod(psiClass, getProject());

        assertEquals(1, psiClass.getMethods().length);
    }
}
```

### 6.2 运行插件

#### 使用 Gradle

```bash
# 运行插件（启动带插件的 IDE）
./gradlew runIde

# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew test
```

#### 在 IDE 中运行

1. 打开 **Run → Edit Configurations**
2. 点击 **+** → **Gradle**
3. 配置：
    - Name: `Run Plugin`
    - Gradle project: 选择项目
    - Tasks: `runIde`
4. 点击 **OK**
5. 点击运行按钮

### 6.3 调试

#### 调试插件代码

1. 在代码中设置断点
2. 以调试模式运行 `runIde`
3. 在启动的 IDE 中触发你的插件功能
4. 调试器会在断点处暂停

#### 查看 IDE 日志

```java
// 使用 Logger
import com.intellij.openapi.diagnostic.Logger;

public class MyClass {
    private static final Logger LOG = Logger.getInstance(MyClass.class);

    public void myMethod() {
        LOG.info("Info message");
        LOG.warn("Warning message");
        LOG.info("Error message", new Exception());
    }
}
```

日志位置：

- **macOS**: `~/Library/Logs/JetBrains/<Product><Version>/idea.log`
- **Windows**: `%USERPROFILE%\AppData\Local\JetBrains\<Product><Version>\log\idea.log`
- **Linux**: `~/.cache/JetBrains/<Product><Version>/log/idea.log`

### 6.4 性能分析

```java
// 测量执行时间
import com.intellij.util.TimeoutUtil;

long startTime = System.currentTimeMillis();
// 执行操作
long duration = System.currentTimeMillis() - startTime;
LOG.info("Operation took " + duration + "ms");

// 使用 PerformanceWatcher
import com.intellij.diagnostic.PerformanceWatcher;

PerformanceWatcher.Snapshot snapshot =
    PerformanceWatcher.takeSnapshot();
// 执行操作
snapshot.logResponsivenessSinceCreation("My operation");
```

---

## 7. 发布与分发

### 7.1 准备发布

#### 更新版本号

`build.gradle.kts`:

```kotlin
version = "1.0.0"
```

#### 更新 plugin.xml

```xml
<change-notes><![CDATA[
    <h3>Version 1.0.0</h3>
    <ul>
        <li>Initial release</li>
        <li>Feature A</li>
        <li>Feature B</li>
    </ul>
]]></change-notes>
```

### 7.2 构建插件

```bash
./gradlew buildPlugin
```

输出位置：`build/distributions/plugin-name-version.zip`

### 7.3 发布到 JetBrains Marketplace

#### 方式一：Web 界面上传

1. 访问 [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. 登录账号
3. 点击 **Upload Plugin**
4. 上传生成的 ZIP 文件
5. 填写插件信息
6. 提交审核

#### 方式二：使用 Gradle 发布

```kotlin
// build.gradle.kts
tasks {
    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
```

获取 Token：

1. 登录 [JetBrains Account](https://account.jetbrains.com/profile)
2. 在 **Tokens** 页面创建新 Token
3. 设置环境变量：
   ```bash
   export PUBLISH_TOKEN=your_token_here
   ```

发布：

```bash
./gradlew publishPlugin
```

### 7.4 签名插件

```kotlin
// build.gradle.kts
tasks {
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
}
```

### 7.5 版本管理

**语义化版本**：

- MAJOR.MINOR.PATCH
- 例：1.0.0, 1.1.0, 1.1.1

**版本发布流程**：

1. 开发新功能
2. 更新版本号
3. 更新变更日志
4. 构建插件
5. 测试插件
6. 发布到 Marketplace
7. 打 Git 标签

---

## 8. 最佳实践

### 8.1 代码规范

#### 使用 IntelliJ Platform API

```java
// ✅ 正确
PsiElement element = psiFile.findElementAt(offset);

// ❌ 错误（自己解析代码）
String code = file.getText();
int classIndex = code.indexOf("class");
```

#### 遵循线程模型

```java
// ✅ 正确
ApplicationManager.getApplication().runReadAction(() -> {
    String name = psiClass.getName();
});

// ❌ 错误（在错误的线程访问 PSI）
String name = psiClass.getName();
```

#### 正确处理 null

```java
// ✅ 正确
Project project = e.getProject();
if (project == null) {
    return;
}

// ❌ 错误（可能 NPE）
String projectName = e.getProject().getName();
```

### 8.2 性能优化

#### 避免频繁的 PSI 操作

```java
// ✅ 正确（一次遍历）
Collection<PsiMethod> methods =
    PsiTreeUtil.findChildrenOfType(psiClass, PsiMethod.class);

// ❌ 错误（多次遍历）
for (PsiElement child : psiClass.getChildren()) {
    if (child instanceof PsiMethod) {
        // ...
    }
}
```

#### 使用缓存

```java
@Service
public final class MyCacheService {
    private final Map<String, CachedValue<String>> cache =
        new ConcurrentHashMap<>();

    public String getCachedValue(Project project, String key) {
        return CachedValuesManager.getManager(project)
            .getCachedValue(
                project,
                () -> CachedValueProvider.Result.create(
                    computeValue(key),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            );
    }
}
```

#### 后台任务

```java
// ✅ 正确（耗时操作在后台）
ProgressManager.getInstance().run(
    new Task.Backgroundable(project, "Processing") {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            // 耗时操作
        }
    }
);

// ❌ 错误（阻塞 UI 线程）
doHeavyWork();
```

### 8.3 错误处理

```java
// ✅ 正确
try {
    riskyOperation();
} catch (Exception e) {
    LOG.info("Failed to perform operation", e);
    Notifications.Bus.notify(
        new Notification(
            "MyPlugin",
            "Error",
            "Operation failed: " + e.getMessage(),
            NotificationType.ERROR
        ),
        project
    );
}

// ❌ 错误（吞掉异常）
try {
    riskyOperation();
} catch (Exception e) {
    // 什么都不做
}
```

### 8.4 用户体验

#### 提供反馈

```java
// 操作前
ProgressManager.getInstance().runProcessWithProgressSynchronously(
    () -> {
        // 长时间操作
    },
    "Processing...",
    true,
    project
);

// 操作后
Notifications.Bus.notify(
    new Notification(
        "MyPlugin",
        "Success",
        "Operation completed successfully",
        NotificationType.INFORMATION
    ),
    project
);
```

#### 可撤销操作

```java
CommandProcessor.getInstance().executeCommand(
    project,
    () -> {
        // 可撤销的操作
    },
    "Command Name",
    null
);
```

#### 键盘快捷键

```xml
<!-- 使用标准修饰键 -->
<keyboard-shortcut
    keymap="$default"
    first-keystroke="shift ctrl A"/>

<!-- 避免与 IDE 默认快捷键冲突 -->
```

### 8.5 兼容性

#### 支持多个 IDE 版本

```kotlin
patchPluginXml {
    sinceBuild.set("213")
    untilBuild.set("233.*")  // 支持更宽的版本范围
}
```

#### 使用 API 版本检查

```java
if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 213) {
    // 使用新 API
} else {
    // 使用旧 API
}
```

### 8.6 文档与帮助

#### 提供完整的文档

- README.md
- CHANGELOG.md
- 用户指南
- API 文档

#### 插件描述

```xml
<description><![CDATA[
    <h1>Plugin Name</h1>
    <p>Brief description of what the plugin does.</p>

    <h2>Features</h2>
    <ul>
        <li>Feature 1</li>
        <li>Feature 2</li>
    </ul>

    <h2>Usage</h2>
    <p>How to use the plugin...</p>

    <h2>Support</h2>
    <p>Report issues at: <a href="...">GitHub Issues</a></p>
]]></description>
```

---

## 9. 常见问题

### 9.1 插件无法加载

**问题**：插件无法在 IDE 中加载

**解决方案**：

1. 检查 `plugin.xml` 语法
2. 检查依赖的模块是否正确
3. 查看 IDE 日志中的错误信息
4. 确认版本兼容性

### 9.2 PSI 访问异常

**问题**：`java.lang.Throwable: Read access is allowed from event dispatch thread or inside read-action only`

**解决方案**：

```java
ApplicationManager.getApplication().runReadAction(() -> {
    // 访问 PSI
});
```

### 9.3 文档修改失败

**问题**：`java.lang.Throwable: Write access is allowed inside write-action only`

**解决方案**：

```java
ApplicationManager.getApplication().runWriteAction(() -> {
    // 修改文档
});
```

### 9.4 类加载冲突

**问题**：插件依赖与 IDE 内置库冲突

**解决方案**：

```kotlin
// build.gradle.kts
intellij {
    // 使用 IDE 内置的库
    plugins.set(listOf("java"))
}

dependencies {
    // 或使用 compileOnly
    compileOnly("some:library:1.0")
}
```

---

## 10. 学习资源

### 10.1 官方文档

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Explorer](https://plugins.jetbrains.com/intellij-platform-explorer/)
- [IntelliJ Platform UI Guidelines](https://jetbrains.design/intellij/)

### 10.2 示例项目

- [intellij-sdk-code-samples](https://github.com/JetBrains/intellij-sdk-code-samples)
- [intellij-sdk-docs](https://github.com/JetBrains/intellij-sdk-docs)
- [intellij-community](https://github.com/JetBrains/intellij-community)（IDE 源码）

### 10.3 社区资源

- [JetBrains Platform Slack](https://plugins.jetbrains.com/slack/)
- [Plugin Development Forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/intellij-plugin)

