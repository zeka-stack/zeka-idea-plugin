# Cosy IntelliJ 0.7.0 Autocomplete Suggestions 实现分析

> 参考源码目录：`reference/cosy-intellij-0.7.0/`

## 1. 总览

Cosy 插件的“Autocomplete Suggestions”由两条链路组成：

1. **Inline/Inlay Suggestion（云端）**：在编辑器内以 inline 或 block inlay 形式展示整段建议，使用远端模型推理，支持自动触发与手动触发。
2. **Completion Contributor（本地/列表型补全）**：基于 IntelliJ CompletionContributor 机制，生成常规补全列表（Lookup），使用本地模型推理。

> 注意：在 `reference/cosy-intellij-0.7.0/META-INF/cosy-withJava.xml` 和 `reference/cosy-intellij-0.7.0/META-INF/cosy-withPython.xml` 中，
`completion.contributor` 的注册被注释掉了，说明该版本可能仅保留 Inline/Inlay 的补全链路，或在其他发行版中启用。

## 2. 关键模块与入口

### 2.1 触发入口

- **命令级触发（Typing）**：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/listener/CosyCommandListener.java`
    - 在 `commandFinished` 中判断输入变化与触发规则，最终调用 `CosyInlayManager.editorChanged(...)`。
- **文档变化触发**：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/listener/CosyDocumentListener.java`
    - 监听 `DocumentEvent`，在特定场景下触发补全（自动模式）。
- **IDE Lookup 结束触发**：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/listener/CosyLookupManagerListener.java`
    - Lookup 关闭后，满足条件则触发 inlay completion；同时避免与 IDE 自带补全冲突。
- **手动触发 Action**：
  `reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/completion/action/CosyTriggerInlayCompletionAction.java`
    - 通过快捷键/菜单主动触发 `MANUAL` 模式补全。

### 2.2 核心服务

- `CosyInlayManager` / `CosyInlayManagerImpl`
    - inlay 提示的生命周期管理与渲染入口。
    - 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/editor/CosyInlayManagerImpl.java`
- `CosyCompletionService` / `CosyCompletionServiceImpl`
    - 统一补全服务封装，转发到 LanguageWebSocketService。
    - 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/completion/CosyCompletionServiceImpl.java`
- `LanguageWebSocketService`
    - 与 LSP 服务端通信，提供 completion / preCompletion 接口，并使用 Debouncer。
    - 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/core/lsp/LanguageWebSocketService.java`

## 3. Inline/Autocomplete Suggestions（Inlay）工作流

### 3.1 触发与过滤规则

**触发器链路**：`CosyCommandListener` → `TriggerExecutorFactory` → 一组 TriggerChecker

- 触发器配置：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/completion/trigger/TriggerExecutorFactory.java`
- 关键检查器：
    - `InitTriggerChecker`：获取 caret 的 PSI 元素
    - `CaretAroundTriggerChecker`：检查光标周围合法性
    - `CommentTriggerChecker`：注释位置限制、频控（Caffeine 缓存）
    - `JavaMethodNewLineTriggerChecker`：避免 Java 方法换行触发
    - `InvalidElementTriggerChecker`：过滤 literal/import/keyword 等
    - `ValidChangeTextTriggerChecker`：过滤非法输入（如括号/符号）

这些规则综合决定“是否触发”。如果命中阻止条件，就不会发起补全请求。

### 3.2 构建补全请求

入口：`InlayPreviewRequest.generate(...)`

- 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/editor/request/InlayPreviewRequest.java`
- 主要流程：
    1. **读取设置**：`CosySetting` / `CosyPersistentSetting`，判断是否启用云端补全、触发模式（自动/手动）、模型强度、生成长度等。
    2. **构建 CompletionParams**：
        - `fileContent`（全文）、`position`（line/column）、`textDocument`（文件 URI）
        - `remoteModelParams`（triggerMode / modelLevel / generateLength / tabWidth 等）
    3. **触发 preCompletion**：`CompletionUtil.triggerPreCompletion(request, delay, triggerMode)`
    4. **异步发起补全**：`CosyCompletionService.asyncCompletionInlay(...)`
    5. **更新请求缓存**：`CosyCacheKeys.KEY_COMPLETION_LATEST_REQUEST` 等

### 3.3 防抖与超时

- `LanguageWebSocketService` 内部维护 `Debouncer`：
    - `completionDebouncer`
    - `inlayCompletionDebouncer`
    - `preCompletionDebouncer`
- inlay 补全调用 `aysncCompletionInlayWithDebouncer(...)`：
    - 在 delay 后发起远端请求
    - 支持超时处理与取消

### 3.4 结果收集与渲染

**收集器**：`DefaultInlayCompletionCollector`

- 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/editor/DefaultInlayCompletionCollector.java`
- 流程：
    1. `onCollect` 收到 `CompletionItem`
    2. `CosyCompletionService.convertInlayItem(...)` 转换为 `CosyEditorInlayItem`
    3. 合并到 `CosyEditorInlayList`，持久在 `CosyCacheKeys.KEY_COMPLETION_INLAY_ITEMS`
    4. 调用 `CosyInlayManager.renderInlayCompletionItem(...)` 进行渲染
    5. 判断 `finished` 标志，结束状态栏“生成中”提示

**渲染器**：`CosyInlayManagerImpl.renderInlayCompletionItem(...)`

- 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/editor/CosyInlayManagerImpl.java`
- 关键点：
    - 解析 `CosyEditorInlayItem` 的 chunks（Inline / Block / AfterLineEnd）
    - 使用 `InlayModel.addInlineElement` / `addBlockElement` 渲染

### 3.5 接受/取消/切换

- **接受**：`CosyApplyInlayAction` → `CosyInlayManager.applyCompletion(...)`
- **逐行接受**：`CosyApplyInlayByLineAction` → `applyCompletionByLine(...)`
- **取消**：`CosyEscapeInlaysAction` → `disposeInlays(...)`
- **切换候选**：`CosyNextInlayCompletionAction` / `CosyPrevInlayCompletionAction`

关键逻辑在：`CosyInlayManagerImpl.applyCompletion(...)` / `applyCompletionByLine(...)`

- 支持 `<|cursor|>` 占位符并移动光标
- 插入前处理换行/缩进

### 3.6 预计算（PreCompletion）

- 预计算触发点：
    - 光标移动：`CompletionUtil.triggerCursorPreCompletion(...)`
    - 正常补全前：`CompletionUtil.triggerPreCompletion(...)`
- 目的：提前向服务端发送上下文，降低实际补全延迟。
- 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/util/CompletionUtil.java`

## 4. Completion Contributor（列表型补全）工作流

入口：`CosyCompletionContributor.fillCompletionVariants(...)`

- 文件：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/completion/CosyCompletionContributor.java`
- 主要流程：
    1. 组装 `CompletionParams`（`useLocalModel=true`，本地模型补全）
    2. 通过 `LanguageWebSocketService.completionWithDebouncer(...)` 发起请求
    3. 转换为 `LookupElement` 加入结果集
    4. 插入时处理文本替换、`$0` 占位符移动光标
    5. 与 IDE 原生补全去重（`duplicateOtherItems`）

> 但由于注册被注释，该路径可能在该版本未生效。

## 5. LSP 通信与服务端接口

- `textDocument/completion`：通用补全请求
- `textDocument/collectCompletionResult`：收集用户选择与统计（未在本次链路中直接调用）
- 相关定义：`reference/cosy-intellij-0.7.0/com/alibabacloud/intellij/qoder/core/lsp/model/LanguageClient.java`

## 6. 关键状态与缓存

- `CosyCacheKeys.KEY_COMPLETION_LATEST_REQUEST`：最近一次补全请求
- `CosyCacheKeys.KEY_COMPLETION_INLAY_ITEMS`：当前 inlay 建议列表
- `CompletionCacheManager`：缓存补全结果，减少重复请求
- `CompletionRequestManager`：管理请求生命周期

## 7. 适配到 intelli-ai-engine 的实现建议

基于 Cosy 的实现，可以将 Autocomplete Suggestions 拆成以下模块，便于在 `engine` 中复用：

1. **Trigger 管理**
    - 参考 `TriggerExecutorFactory` 结构，封装一组可配置规则
    - 在 `CommandListener` / `DocumentListener` / `LookupListener` 三类入口中调用
2. **Request Builder**
    - 参考 `InlayPreviewRequest.generate(...)` 的参数构建逻辑
    - 抽象出 `CompletionContext` 和 `ModelParams`
3. **Debouncer + Async Pipeline**
    - 参考 `LanguageWebSocketService` 的三类 Debouncer
    - 建议 engine 内提供统一 `DebouncedCompletionDispatcher`
4. **Inlay 渲染层**
    - 参考 `CosyInlayManagerImpl.renderInlayCompletionItem(...)`
    - 设计成 engine 级别的渲染接口，并允许插件自定义渲染策略
5. **Suggestion Lifecycle**
    - 接受/取消/切换/逐行接受的统一入口
    - 数据结构参考 `CosyEditorInlayItem` + `CosyEditorInlayList`
6. **预计算与性能优化**
    - `preCompletion` 可以作为可选能力提升首个 token 的体验

## 8. 简化时序图（Inlay）

```
User Typing
  -> CosyCommandListener.commandFinished
  -> TriggerExecutorFactory.check(...)
  -> CosyInlayManager.editorChanged
  -> InlayPreviewRequest.generate
  -> CosyCompletionService.asyncCompletionInlay
  -> LanguageWebSocketService.aysncCompletionInlayWithDebouncer
  -> LSP textDocument/completion
  -> DefaultInlayCompletionCollector.onCollect
  -> CosyInlayManager.renderInlayCompletionItem
  -> User accept/cancel/toggle
```

## 9. 可复用要点清单

- 多入口触发（Typing / Lookup / 手动）
- 规则化 Trigger Check（可扩展）
- 统一的 CompletionParams 构建
- Debounce + PreCompletion
- Cache + Request Manager
- Inlay 渲染与生命周期管理
- Telemetry hooks（非必需，但便于优化）

