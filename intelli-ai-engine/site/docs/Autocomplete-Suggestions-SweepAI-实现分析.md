# SweepAI Tab 补全功能实现分析

## 1. 概述

SweepAI 是一个专为 JetBrains IDE 设计的 AI 编码助手插件，其核心功能之一是 **Next-Edit Autocomplete（下一编辑自动补全）**
。该功能能够预测开发者的下一个编辑意图，并在编辑器中以 Ghost Text（幽灵文本）的形式显示补全建议，用户可以通过按 `Tab` 键接受建议，或按 `Esc`
键拒绝建议。

本文档基于 SweepAI 插件 v1.27.0 的反编译源代码，详细分析其 Tab 补全功能的实现机制。

## 2. 架构设计

### 2.1 核心组件

SweepAI 的 Tab 补全功能主要由以下核心组件构成：

```
┌─────────────────────────────────────────────────────────┐
│                    Editor Layer                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  GhostTextRenderer (EditorCustomElementRenderer)│   │
│  │  - 渲染 Ghost Text 建议                         │   │
│  │  - 语义高亮支持                                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ▲
                        │
┌─────────────────────────────────────────────────────────┐
│                Tracking & Request Layer                 │
│  ┌─────────────────────────────────────────────────┐   │
│  │  RecentEditsTracker (Project Service)           │   │
│  │  - 监听编辑器变化 (DocumentListener)            │   │
│  │  - 监听光标移动 (CaretListener)                 │   │
│  │  - 跟踪最近的编辑                               │   │
│  │  - 发起补全请求                                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ▲
                        │
┌─────────────────────────────────────────────────────────┐
│                    Service Layer                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │  AutocompleteIpResolverService                  │   │
│  │  - 解析补全服务端点                             │   │
│  │  - 支持云端和本地 (LlamaCpp)                    │   │
│  │  - 健康检查                                     │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  GhostTextManager                               │   │
│  │  - 管理 Ghost Text 输入历史                     │   │
│  │  - 持久化存储                                   │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ▲
                        │
┌─────────────────────────────────────────────────────────┐
│                   Action Layer                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │  AcceptEditCompletionAction (TAB)               │   │
│  │  RejectEditCompletionAction (ESC)               │   │
│  │  EditorActionsRouterService                     │   │
│  │  - 路由编辑器操作                               │   │
│  │  - 拦截 Tab/ESC 等按键                          │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 数据模型

#### 2.2.1 NextEditAutocompletion

补全建议的数据模型：

```java
public final class NextEditAutocompletion {
    private int start_index;        // 补全起始位置
    private int end_index;          // 补全结束位置
    private String completion;      // 补全内容
    private float confidence;       // 置信度
    private String autocomplete_id; // 补全ID（用于追踪）
}
```

#### 2.2.2 AutocompleteSuggestion

补全建议的抽象基类，支持多种显示方式：

```java
public abstract sealed class AutocompleteSuggestion implements Disposable {
    // 基础属性
    protected String content;           // 建议内容
    protected int startOffset;          // 起始偏移量
    protected int endOffset;            // 结束偏移量
    protected String autocomplete_id;   // 建议ID
    protected long shownTime;           // 显示时间
    protected long disposedTime;        // 销毁时间

    // 抽象方法
    public abstract void show(Editor editor, boolean postJump);
    public abstract Disposable accept(Editor editor);
    public abstract void dispose();
}
```

**子类实现**：

1. **GhostTextSuggestion** - 使用 Ghost Text 在行内显示建议
2. **PopupSuggestion** - 使用弹出窗口显示建议
3. **JumpToEditSuggestion** - 跳转到编辑位置的建议
4. **MultipleGhostTextSuggestion** - 多个 Ghost Text 建议的组合

## 3. 工作流程

### 3.1 编辑监听与追踪

`RecentEditsTracker` 作为项目级服务（`@Service({Level.PROJECT})`），负责监听编辑器变化：

```java
// 1. 监听文档变化
DocumentListener currentListener = new DocumentListener() {
    @Override
    public void documentChanged(DocumentEvent event) {
        // 检测文档变化类型（插入、删除、替换等）
        UserActionType actionType = detectDocumentChangeActionType(event);
        // 跟踪用户操作
        trackUserAction(actionType, lineNumber, offset, filePath);
        // 处理最新编辑（防抖）
        processLatestEdit();
    }
};

// 2. 监听光标移动
CaretListener currentCaretListener = new CaretListener() {
    @Override
    public void caretPositionChanged(CaretEvent e) {
        // 跟踪光标位置
        trackCursorPosition();
        // 如果光标移动距离过大，可能需要拒绝当前建议
        if (shouldRejectOnCaretMove()) {
            rejectSuggestion();
        }
    }
};

// 3. 监听编辑器焦点
EditorFactoryListener editorFactoryListener = new EditorFactoryListener() {
    @Override
    public void editorCreated(EditorFactoryEvent event) {
        // 为新建的编辑器附加监听器
        attachListenerToEditor(event.getEditor());
    }
};
```

### 3.2 补全请求发起

当检测到编辑变化时，`RecentEditsTracker` 会使用防抖机制（`Debouncer`）延迟发起补全请求：

```java
private void processLatestEdit() {
    // 防抖：延迟发起请求，避免频繁调用
    debouncer.schedule(() -> {
        // 获取当前编辑器状态
        EditorState editorState = getCurrentEditorState();

        // 检查是否应该发起补全请求
        if (shouldExcludeFromAutocomplete()) {
            return;
        }

        // 发起异步补全请求
        fetchAutocompleteRequest(editorState);
    });
}
```

### 3.3 补全请求处理

补全请求通过 `AutocompleteIpResolverService` 处理：

```java
// AutocompleteIpResolverService.java
public NextEditAutocompleteResponse fetchNextEditAutocomplete(
    NextEditAutocompleteRequest request
) {
    // 1. 检查是否指向云端服务
    if (isPointedToCloud()) {
        // 从云端获取补全
        return fetchFromCloud(request);
    } else {
        // 从本地 LlamaCpp 服务获取补全
        return fetchNextEditAutocompleteFromLocal(request);
    }
}
```

**请求数据包含**：

- 当前文件内容
- 光标位置
- 最近的编辑历史
- 相关的代码块（通过 `getRelevantFileChunks` 获取）
- 用户上下文（其他打开的文件）

### 3.4 Ghost Text 渲染

收到补全响应后，`RecentEditsTracker` 创建 `AutocompleteSuggestion` 并显示：

```java
private void showAutocomplete(
    NextEditAutocompleteRequest request,
    NextEditAutocompleteResponse response
) {
    // 创建建议对象
    AutocompleteSuggestion suggestion = createSuggestion(response);

    // 在编辑器中显示
    suggestion.show(editor, false);

    // 保存当前建议
    this.currentSuggestion = suggestion;
}
```

`GhostTextRenderer` 负责实际渲染：

```java
public class GhostTextRenderer implements EditorCustomElementRenderer {
    @Override
    public void paint(Inlay inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        Graphics2D g2d = (Graphics2D) g;

        // 1. 设置字体和颜色（使用灰色、半透明）
        g2d.setFont(hintFont);
        g2d.setColor(hintColor); // 通常是灰色

        // 2. 计算文本位置
        int x = targetRegion.x;
        int y = targetRegion.y + fontMetrics.getAscent();

        // 3. 绘制 Ghost Text
        g2d.drawString(text, x, y);

        // 4. 可选：绘制语义高亮
        if (showHint) {
            drawSemanticHighlights(g2d, targetRegion);
        }
    }

    @Override
    public int calcWidthInPixels(Inlay inlay) {
        // 计算 Ghost Text 宽度
        return cachedHintWidth;
    }
}
```

### 3.5 用户交互处理

#### 3.5.1 接受建议（Tab 键）

`AcceptEditCompletionAction` 绑定到 `TAB` 键：

```java
// plugin.xml
<action id="dev.sweep.assistant.autocomplete.edit.AcceptEditCompletionAction"
        class="dev.sweep.assistant.autocomplete.edit.AcceptEditCompletionAction">
    <keyboard-shortcut keymap="$default" first-keystroke="TAB"/>
</action>

// AcceptEditCompletionAction.java
protected void handleCompletion(Project project, Editor editor) {
    RecentEditsTracker.getInstance(project).acceptSuggestion();
}
```

接受建议的流程：

```java
public void acceptSuggestion() {
    if (currentSuggestion == null) {
        return;
    }

    // 1. 使用 WriteCommandAction 写入文档
    WriteCommandAction.runWriteCommandAction(project, () -> {
        // 2. 应用补全内容
        Disposable disposable = currentSuggestion.accept(editor);

        // 3. 追踪接受事件
        trackAcceptance();

        // 4. 清理当前建议
        currentSuggestion.dispose();
        currentSuggestion = null;
    });
}
```

#### 3.5.2 拒绝建议（Esc 键）

`RejectEditCompletionAction` 绑定到 `ESCAPE` 键：

```java
public void rejectSuggestion() {
    if (currentSuggestion == null) {
        return;
    }

    // 1. 将建议添加到拒绝缓存
    AutocompleteRejectionCache.getInstance(project)
        .addRejection(currentSuggestion.rejectionCacheKey());

    // 2. 追踪拒绝事件
    trackRejection();

    // 3. 清理当前建议
    currentSuggestion.dispose();
    currentSuggestion = null;
}
```

#### 3.5.3 编辑器操作路由

`EditorActionsRouterService` 负责拦截编辑器操作，在用户按下 Tab/ESC 等按键时，优先处理补全建议：

```java
private void installHandlers() {
    EditorActionManager eam = EditorActionManager.getInstance();

    // 拦截多个编辑器操作
    String[] actions = {
        "EditorTab", "EditorEnter", "EditorEscape",
        "EditorRight", "EditorLeft", "EditorUp", "EditorDown",
        "EditorDelete", "EditorBackSpace", ...
    };

    for (String actionId : actions) {
        // 保存原始处理器
        EditorActionHandler original = eam.getActionHandler(actionId);
        originals.put(actionId, original);

        // 安装自定义处理器
        eam.setActionHandler(actionId, new EditorActionHandler() {
            @Override
            public void doExecute(Editor editor, Caret caret, DataContext dataContext) {
                // 1. 检查是否有活动的补全建议
                RecentEditsTracker tracker = trackerFor(editor);
                if (tracker != null && tracker.isCompletionShown()) {
                    // 2. 如果是接受操作（Tab），先接受建议
                    if (activeAcceptActions.contains(actionId)) {
                        tracker.acceptSuggestion();
                        return;
                    }
                    // 3. 如果是拒绝操作（Esc），先拒绝建议
                    if (activeRejectActions.contains(actionId)) {
                        tracker.rejectSuggestion();
                        return;
                    }
                }

                // 4. 否则执行原始操作
                original.doExecute(editor, caret, dataContext);
            }
        });
    }
}
```

## 4. 关键技术细节

### 4.1 Ghost Text 实现

SweepAI 使用 IntelliJ Platform 的 `Inlay` API 来实现 Ghost Text：

```java
// 创建 Inlay（行内元素）
InlayProperties properties = InlayProperties()
    .disableSoftWrapping(false)
    .priority(100); // 较高的优先级

Inlay<GhostTextRenderer> inlay = InlayModelUtil.addInlineElement(
    editor,
    offset,  // 插入位置
    properties,
    new GhostTextRenderer(editor, text, attributes, ...)
);
```

**特点**：

- `Inlay` 是编辑器中的行内元素，不会影响文档内容
- 可以自定义渲染器（`EditorCustomElementRenderer`）
- 支持语义高亮（通过 `HighlightInfo` 获取）

### 4.2 防抖机制

使用 `Debouncer` 来减少不必要的补全请求：

```java
public class Debouncer {
    private final AtomicReference<Future<?>> lastTask = new AtomicReference<>();
    private final ScheduledExecutorService executor;

    public void schedule(Runnable task, long delayMs) {
        // 取消之前的任务
        Future<?> previous = lastTask.getAndSet(null);
        if (previous != null) {
            previous.cancel(false);
        }

        // 调度新任务
        Future<?> newTask = executor.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        lastTask.set(newTask);
    }
}
```

**优点**：

- 避免用户快速输入时频繁发起请求
- 降低服务器负载
- 提高响应性能

### 4.3 上下文收集

补全请求需要收集丰富的上下文信息：

```java
private NextEditAutocompleteRequest buildRequest(EditorState editorState) {
    return new NextEditAutocompleteRequest(
        // 1. 当前文件内容
        fileContents: editorState.documentText,

        // 2. 光标位置
        caretPosition: editorState.caretOffset,

        // 3. 最近的编辑历史
        recentEdits: getRecentEdits(),

        // 4. 相关的代码块（通过实体搜索）
        relevantChunks: getRelevantFileChunks(),

        // 5. 其他打开的文件
        otherOpenedFiles: getOtherOpenedFileChunks(),

        // 6. 最近的提交信息（如果可用）
        recentCommits: getRecentCommits(),

        // 7. 剪贴板内容（如果相关）
        clipboardEntry: getClipboardEntry()
    );
}
```

### 4.4 拒绝缓存

`AutocompleteRejectionCache` 用于记住用户拒绝的建议，避免重复显示：

```java
public class AutocompleteRejectionCache {
    private final Set<String> rejectedSuggestions = ConcurrentHashMap.newKeySet();

    public void addRejection(String suggestionKey) {
        rejectedSuggestions.add(suggestionKey);

        // 限制缓存大小
        if (rejectedSuggestions.size() > MAX_CACHE_SIZE) {
            // 移除最旧的条目
            evictOldest();
        }
    }

    public boolean isRejected(String suggestionKey) {
        return rejectedSuggestions.contains(suggestionKey);
    }
}
```

### 4.5 导入检测

`AutocompleteImportDetector` 可以检测代码中缺失的导入，并提供修复建议：

```java
public class AutocompleteImportDetector {
    public void detectAndShowImportFixes(PsiFile psiFile, Editor editor) {
        // 1. 分析 PSI 树，查找未解析的引用
        List<UnresolvedReference> unresolved = findUnresolvedReferences(psiFile);

        // 2. 为每个未解析的引用查找可能的导入
        for (UnresolvedReference ref : unresolved) {
            List<ImportFixInfo> fixes = findImportFixes(ref);

            // 3. 创建导入修复建议
            AutocompleteSuggestion.PopupSuggestion suggestion =
                createImportFixSuggestion(ref, fixes);

            // 4. 显示建议
            showSuggestion(suggestion);
        }
    }
}
```

## 5. 配置与扩展

### 5.1 插件配置

在 `plugin.xml` 中注册相关组件：

```xml
<!-- 状态栏组件 -->
<statusBarWidgetFactory id="SweepAutocompleteStatus"
                        implementation="dev.sweep.assistant.statusbar.AutocompleteStatusBarWidgetFactory"/>

<!-- 动作注册 -->
<action id="dev.sweep.assistant.autocomplete.edit.AcceptEditCompletionAction"
        class="dev.sweep.assistant.autocomplete.edit.AcceptEditCompletionAction">
    <keyboard-shortcut keymap="$default" first-keystroke="TAB"/>
</action>

<action id="dev.sweep.assistant.autocomplete.edit.RejectEditCompletionAction"
        class="dev.sweep.assistant.autocomplete.edit.RejectEditCompletionAction">
    <keyboard-shortcut keymap="$default" first-keystroke="ESCAPE"/>
</action>
```

### 5.2 设置选项

用户可以在设置页面配置：

- 启用/禁用 Tab 补全
- 调整补全延迟时间
- 选择补全服务（云端/本地）
- 自定义快捷键

## 6. 性能优化

### 6.1 异步处理

所有网络请求和耗时操作都使用协程异步处理：

```kotlin
// 使用 Kotlin 协程
private suspend fun fetchNextEditAutocomplete(
    request: NextEditAutocompleteRequest
): NextEditAutocompleteResponse = withContext(Dispatchers.IO) {
    // 网络请求
    val response = httpClient.send(request)
    response
}
```

### 6.2 请求去重

使用 `ConcurrentHashMap` 和 `CompletableDeferred` 来去重并发请求：

```java
private final ConcurrentHashMap<Long, CompletableDeferred<Pair<...>>> fetchJobs;

private CompletableDeferred<...> fetchAutocompleteRequest(EditorState state) {
    long requestId = generateRequestId(state);

    // 检查是否已有相同请求
    CompletableDeferred<...> existing = fetchJobs.get(requestId);
    if (existing != null) {
        return existing;
    }

    // 创建新请求
    CompletableDeferred<...> deferred = new CompletableDeferred<>();
    fetchJobs.put(requestId, deferred);

    // 异步执行
    launch {
        val response = fetchNextEditAutocomplete(request);
        deferred.complete(response);
        fetchJobs.remove(requestId);
    }

    return deferred;
}
```

### 6.3 内存管理

- 使用 `EvictingQueue` 限制历史记录大小
- 及时清理不需要的建议对象
- 使用 `Disposer` 管理资源生命周期

## 7. 总结

SweepAI 的 Tab 补全功能通过以下关键技术实现：

1. **编辑监听** - 使用 `DocumentListener` 和 `CaretListener` 实时跟踪用户输入
2. **防抖机制** - 使用 `Debouncer` 减少不必要的请求
3. **Ghost Text 渲染** - 使用 IntelliJ Platform 的 `Inlay` API 在编辑器中显示建议
4. **操作路由** - 使用 `EditorActionsRouterService` 拦截 Tab/ESC 等按键
5. **异步处理** - 使用 Kotlin 协程处理网络请求和耗时操作
6. **上下文收集** - 收集丰富的代码上下文信息，提高补全质量
7. **拒绝缓存** - 记住用户拒绝的建议，避免重复显示

这些技术的组合使得 SweepAI 能够提供流畅、智能的代码补全体验。

## 8. 参考资源

- [IntelliJ Platform SDK - Inlay API](https://plugins.jetbrains.com/docs/intellij/inlays.html)
- [IntelliJ Platform SDK - Editor Actions](https://plugins.jetbrains.com/docs/intellij/editor-actions.html)
- [SweepAI 官方文档](https://docs.sweep.dev/)
- [Kotlin Coroutines 文档](https://kotlinlang.org/docs/coroutines-overview.html)

