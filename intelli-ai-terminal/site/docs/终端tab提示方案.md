# 终端 Tab 提示方案

## 1. 是否可行

就 UI 角度来看，终端其实也是一个 Swing 组件（`TerminalView`/`JBTerminalWidget`），可以拿到当前 prompt 所在的 `JComponent`，在其上叠加
`HintManager` 或 `Balloon` 弹窗完全可行；就逻辑角度来看，我们已经用 Tab 拦截并触发了 `TerminalAiGenerateAction`，只要在 Tab 事件前后「探测当前上下文是否满足
AI 生成条件」就可以将提示显/隐。因此，从技术栈和现有实现来看，加一个提示与 Commit Message 中所用的 hint 机制并不冲突，可行。

## 2. 复杂度评估

- **引入成本**：需要像 `changelog.hint.CommitMessageHintService` 一样监听终端中的 `Editor`/`TerminalView` 生命周期，扩展一个
  `TerminalHintService`，负责注册/清理 hint 渲染器，这部分代码量在几十行，难度低。
- **Tab 执行链路**：我们必须 intercept Tab 键的两次（提示先显示/隐藏，按下时触发 AI），需要在 `TerminalAiGenerateAction` 或对应的
  `TerminalAllowedActionsProvider` 中检查 `InputInfo` 是否可用，这部分工作在已有逻辑里，只需新增 `HintManager` 的 `showHint` / `hideHint`
  调用，属于中等复杂度。
- **渲染位置**：终端没有明显的编辑器上下文坐标，可能需要通过 `TerminalView.getComponent()` 得到可视区域，再依赖
  `HintManager.getInstance().showInformationHint` 或自定义提示 renderer（参考 `CommitMessageHintRenderer`），来保证提示出现在命令行附近。渲染实现微调略复杂但可控。
- **线程与生命周期**：终端属于工具窗口，hint 需要在 UI 线程更新，并在终端关闭/切换时销毁，必须使用 `Disposable` 关联 `ToolWindow` 或 `Project`：跟
  `CommitMessageHintManager` 绑定编辑器一样，我们可以在终端注册 `DBus`/`ContentManagerListener` 等，综合难度中等。

综述：实现难度中等偏低，主要挑战在 UI 渲染与生命周期管理，剩下逻辑与现有 Tab<>AI 调用链重用率高。

## 3. 技术方案概览

### 3.1 服务/管理器

1. 新增 `terminal.hint.TerminalHintService`，类似 Changelog 里的 `CommitMessageHintService`，维护 `TerminalView` → `TerminalHintManager` 的映射。
2. `TerminalHintManager` 负责：
    - 保存 `TerminalView` 对象、当前 hint 状态。
    - 使用 `HintManager.getInstance().showInformationHint(component, message)` 或 `HintManager.createInformationLabel` 生成轻量提示。
    - 提供 `showHint()`/`hideHint()`/`scheduleHint()` 等接口，便于 `TerminalAiGenerateAction` 在需要时显示（比如输入前缀存在且没有空命令）。
    - 绑定 `Disposable` 到 `TerminalView` 所在 `Content`（通过 `Disposer.register`）以自动清除。

### 3.2 提示时机

1. **启动时**：在终端工具窗口激活/创建时，通过类似 `TerminalTabTitleDecorator` 的回调获取 `TerminalView`，并调用
   `TerminalHintService.register(view)` 。
2. **输入变化**：终端每次在 `actionPerformed` 之前，可以判断是否存在有效 trigger prefix（已经由 `getInputInfo` 实现）。在 `update()` 或 Tab
   按键监听中调用 `TerminalHintManager.showHint()` 并传入提示文案，如“按 Tab 由 AI 填充命令”。
3. **Tab 触发**：`TerminalAiGenerateAction` 执行后（无论成功/失败），调用 `TerminalHintManager.hideHint()`，避免提示残留。

### 3.3 整合方式

1. 利用 `TerminalAiAllowedActionsProvider` 或 `TerminalContextService` 注册 `TerminalView` 的 `KeyListener`/`InputMap`，在 Tab
   是否可用时控制提示显示/隐藏。
2. `TerminalHintManager` 也可以监听 terminal focus（比如 `TerminalView.addFocusListener`）以确保 hint 只在终端获得焦点时出现。
3. 对齐 `changelog` 的 `HintRenderer` 结构，如果需要更灵活的样式，可以抽象 `TerminalHintRenderer`，通过 `JLabel` hypertext +
   `HintManagerImpl.createHint` 自定义颜色、图标、快捷键提示。

## 4. 参考 & 复用点

- 复用 `intelli-ai-changelog/src/main/java/dev/dong4j/zeka/stack/idea/plugin/changelog/hint/*` 中的服务/渲染设计思路（
  `CommitMessageHintManager` 的 notify、`CommitMessageHintRenderer` 的 UI）。
- UI 渲染可以继续依赖 `HintManager`，只是捕获对象改为 `TerminalView.getComponent()`。
- Tab 触发逻辑与 `TerminalAiGenerateAction` 已有部分共用：只需再增加 `HintManager` 的 API 触发即可。

## 5. 输出文档位置

- 添加在 `intelli-ai-terminal/docs/终端tab提示方案.md`，作为本模块的技术方案记录。
