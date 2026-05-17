# IDEA 2026 Problems View 兼容说明

## 现象

升级到 IDEA 2026.1 后，IntelliAI Engine 的日志面板不再显示。

## 原因

Engine 以前是通过 `ToolWindowManager.getToolWindow("Problems")` 找到 Problems 工具窗口，再调用
`toolWindow.getContentManager().addContent(...)` 把日志控制台作为普通 `Content` 挂进去。

在 IDEA 2026.1 之后，IDE 侧的 Problems 工具窗口切换到了新的 `Problems View` 生命周期：

- 工具窗口 ID 从旧的 `Problems` 变成了 `Problems View`
- Problems View 更倾向通过 `ProblemsViewPanelProvider` 注册子 tab
- 直接向 `ContentManager` 追加普通 `Content` 的方式不再稳定

因此问题不在日志输出逻辑本身，而在于日志面板挂载到 Problems View 的方式已经不适配新版本。

## 处理方式

Engine 侧改为新旧两套路径兼容：

1. 新版 IDEA 优先走 Problems View tab 模型
    - 新增 `AIConsoleProblemsViewPanelProvider`
    - 新增 `AIConsoleProblemsViewPanel`
    - 通过 `ProblemsViewToolWindowUtils` 注册并选中 `IntelliAI Engine` 日志 tab

2. 旧版 IDEA 保留原来的 `ContentManager` 兜底逻辑
    - 优先查找 `Problems View`
    - 找不到时回退到旧的 `Problems`
    - 新版 API 不可用时仍可尝试旧版 content 挂载方式

## 关键实现

- `AIConsoleView`
    - 定义 `PROBLEMS_VIEW_TOOL_WINDOW_ID = "Problems View"`
    - 定义 `PROBLEMS_TOOL_WINDOW_ID = "Problems"`
    - 定义 `PROBLEMS_VIEW_TAB_ID = "IntelliAI.Engine.Console"`
    - 优先使用 `ProblemsViewToolWindowUtils` 添加并选中日志 tab
    - 保留旧版 `ContentManager` 挂载逻辑作为 fallback

- `AIConsoleProblemsViewPanelProvider`
    - 实现 `ProblemsViewPanelProvider`
    - 为 Problems View 创建日志 tab

- `AIConsoleProblemsViewPanel`
    - 直接实现 `ProblemsViewTab`
    - 继承 `JBPanel`
    - 复用现有 Console 根面板作为 tab 内容
    - 不继承 `ProblemsViewPanel`，因为后者是问题树面板基类，不适合直接承载通用日志 UI

## 交互修复

迁移到 Problems View tab 后，日志面板外层组件从原来的 `rootPanel` 变成了 `AIConsoleProblemsViewPanel`。

原有工具栏按钮会通过 `AIConsoleView.isConsoleTabSelected()` 判断当前 tab 是否为日志面板。如果仍只判断 `rootPanel`，换行、滚动到底部、关闭日志、清空日志等按钮会被隐藏。

因此选中判断同时兼容：

- 旧版 `consoleContent`
- 旧版直挂的 `rootPanel`
- 新版 `AIConsoleProblemsViewPanel`

## 验证

-
`JAVA_HOME=/Users/dong4j/.gradle/jdks/jetbrains_s_r_o_-21-aarch64-os_x.2/jbrsdk_jcef-21.0.10-osx-aarch64-b1163.108/Contents/Home ./gradlew compileJava`

本次只要求编译通过，不要求执行 `runIde`。

建议后续在 IDEA 2026.1 环境里确认：

1. 插件启动后 Problems View 中可以看到 `IntelliAI Engine` tab
2. 日志面板内容可见
3. 左侧工具栏按钮可见，包括换行、滚动到底部、关闭日志、清空日志等操作
4. 旧版本 IDEA 仍能通过旧逻辑显示日志面板
