# Autocomplete Suggestions 功能实现方案

## 1. 需求背景

### 1.1 功能概述

Autocomplete Suggestions（自动补全建议）是一个智能代码补全功能，能够：

- 实时监听编辑器变化，预测开发者的下一个编辑意图
- 使用 AI 生成代码补全建议
- 在编辑器中以 Ghost Text（幽灵文本）的形式显示建议
- 支持通过 `Tab` 键接受建议，`Esc` 键拒绝建议
- 支持上下文感知，收集相关代码块和编辑历史

### 1.2 目标

在 `intelli-ai-engine` 中实现 Autocomplete Suggestions 功能，使其能够：

1. **作为可复用模块**：其他插件可以轻松集成和使用
2. **灵活配置**：支持启用/禁用、自定义快捷键、调整补全参数
3. **扩展性强**：支持自定义上下文收集器、建议渲染器等
4. **性能优化**：防抖机制、异步处理、请求去重

### 1.3 参考实现

参考 SweepAI 插件的实现（详见 `SweepAI-Tab补全功能实现分析.md`），核心组件包括：

- `RecentEditsTracker` - 编辑追踪和补全请求管理
- `AutocompleteSuggestion` - 补全建议数据模型
- `GhostTextRenderer` - Ghost Text 渲染器
- `EditorActionsRouterService` - 编辑器操作路由
- `AutocompleteIpResolverService` - 补全服务解析

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│              External Plugins Layer                     │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Plugin A (intelli-ai-javadoc)                  │   │
│  │  Plugin B (intelli-ai-changelog)                │   │
│  │  Plugin C (custom plugin)                       │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ▲
                        │ 使用 API / 实现扩展点
┌─────────────────────────────────────────────────────────┐
│           IntelliAI Engine Layer                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │  AutocompleteService (Extension Point)          │   │
│  │  - 统一的服务入口                                │   │
│  │  - 管理多个编辑器的补全追踪                      │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  AutocompleteTracker (Project Service)          │   │
│  │  - 监听编辑器变化                                │   │
│  │  - 收集上下文信息                                │   │
│  │  - 发起补全请求                                  │   │
│  │  - 管理补全建议生命周期                          │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  ContextCollector (Extension Point)             │   │
│  │  - 默认实现：文件内容、光标位置、编辑历史         │   │
│  │  - 插件可扩展：自定义上下文收集器                │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  SuggestionRenderer (Extension Point)           │   │
│  │  - 默认实现：GhostTextRenderer                   │   │
│  │  - 插件可扩展：自定义渲染器                      │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │  AIService (Existing)                           │   │
│  │  - 统一的 AI 服务调用                            │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 核心组件设计

#### 2.2.1 AutocompleteService

**职责**：统一的服务入口，管理项目级别的补全功能

**接口设计**：

```java
public interface AutocompleteService {
    /**
     * 获取项目级别的补全服务实例
     */
    static AutocompleteService getInstance(@NotNull Project project) {
        return project.getService(AutocompleteService.class);
    }
    
    /**
     * 启用/禁用补全功能
     */
    void setEnabled(boolean enabled);
    
    /**
     * 检查是否已启用
     */
    boolean isEnabled();
    
    /**
     * 获取编辑器的补全追踪器
     */
    @Nullable
    AutocompleteTracker getTracker(@NotNull Editor editor);
    
    /**
     * 为编辑器启用补全追踪
     */
    void enableTracking(@NotNull Editor editor);
    
    /**
     * 为编辑器禁用补全追踪
     */
    void disableTracking(@NotNull Editor editor);
}
```

#### 2.2.2 AutocompleteTracker

**职责**：追踪单个编辑器的变化，管理补全建议的生命周期

**接口设计**：

```java
@Service(Level.PROJECT)
public class AutocompleteTracker implements Disposable {
    private final Project project;
    private final Editor editor;
    private final Debouncer debouncer;
    
    // 当前建议
    private volatile AutocompleteSuggestion currentSuggestion;
    
    // 监听器
    private DocumentListener documentListener;
    private CaretListener caretListener;
    
    /**
     * 处理文档变化
     */
    private void onDocumentChanged(DocumentEvent event) {
        // 1. 防抖：延迟处理
        debouncer.schedule(() -> {
            // 2. 收集上下文
            AutocompleteContext context = collectContext();
            
            // 3. 检查是否应该发起补全请求
            if (shouldRequestCompletion(context)) {
                // 4. 发起异步补全请求
                requestCompletion(context);
            }
        }, DEBOUNCE_DELAY_MS);
    }
    
    /**
     * 收集上下文信息
     */
    private AutocompleteContext collectContext() {
        // 使用扩展点收集上下文
        return ContextCollector.EP_NAME
            .getExtensionList()
            .stream()
            .map(collector -> collector.collect(editor, project))
            .reduce(AutocompleteContext.EMPTY, AutocompleteContext::merge);
    }
    
    /**
     * 发起补全请求
     */
    private void requestCompletion(AutocompleteContext context) {
        // 1. 构建提示词
        String prompt = buildPrompt(context);
        
        // 2. 使用 AIService 调用 AI
        AIService aiService = AIService.getInstance();
        CompletableFuture<String> future = aiService.callAsync(
            prompt,
            getSystemPrompt()
        );
        
        // 3. 处理响应
        future.thenAccept(response -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                // 4. 解析补全建议
                List<AutocompleteSuggestion> suggestions = parseSuggestions(response);
                
                // 5. 显示建议
                showSuggestions(suggestions);
            });
        });
    }
    
    /**
     * 接受当前建议
     */
    public void acceptSuggestion() {
        if (currentSuggestion != null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                currentSuggestion.accept(editor);
                currentSuggestion.dispose();
                currentSuggestion = null;
            });
        }
    }
    
    /**
     * 拒绝当前建议
     */
    public void rejectSuggestion() {
        if (currentSuggestion != null) {
            AutocompleteRejectionCache.getInstance(project)
                .addRejection(currentSuggestion.getRejectionKey());
            currentSuggestion.dispose();
            currentSuggestion = null;
        }
    }
}
```

#### 2.2.3 AutocompleteContext

**职责**：封装补全请求所需的上下文信息

**数据模型**：

```java
public class AutocompleteContext {
    // 当前文件内容
    private final String fileContent;
    
    // 光标位置
    private final int caretOffset;
    private final int caretLine;
    private final int caretColumn;
    
    // 最近编辑历史
    private final List<EditRecord> recentEdits;
    
    // 相关代码块
    private final List<CodeChunk> relevantChunks;
    
    // 其他打开的文件（上下文）
    private final List<FileContext> otherOpenFiles;
    
    // 语言类型
    private final String language;
    
    // 项目信息
    private final ProjectInfo projectInfo;
    
    // 合并多个上下文
    public AutocompleteContext merge(AutocompleteContext other) {
        // 合并逻辑
    }
}
```

#### 2.2.4 AutocompleteSuggestion

**职责**：补全建议的数据模型和显示

**接口设计**：

```java
public abstract class AutocompleteSuggestion implements Disposable {
    protected final String content;
    protected final int startOffset;
    protected final int endOffset;
    protected final String suggestionId;
    
    /**
     * 显示建议
     */
    public abstract void show(@NotNull Editor editor);
    
    /**
     * 接受建议（应用到文档）
     */
    public abstract void accept(@NotNull Editor editor);
    
    /**
     * 更新建议（当文档变化时）
     */
    @Nullable
    public abstract Integer update(@NotNull Editor editor);
    
    /**
     * 获取拒绝缓存键
     */
    public String getRejectionKey() {
        return content;
    }
}
```

**实现类**：

1. **GhostTextSuggestion** - 使用 Ghost Text 显示（默认）
2. **PopupSuggestion** - 使用弹出窗口显示（可选）

#### 2.2.5 ContextCollector（扩展点）

**职责**：允许插件自定义上下文收集逻辑

**接口设计**：

```java
public interface ContextCollector {
    ExtensionPointName<ContextCollector> EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.autocomplete.contextCollector");
    
    /**
     * 收集上下文信息
     */
    @NotNull
    AutocompleteContext collect(@NotNull Editor editor, @NotNull Project project);
    
    /**
     * 获取优先级（数字越大优先级越高）
     */
    default int getPriority() {
        return 0;
    }
}
```

**默认实现**：

```java
public class DefaultContextCollector implements ContextCollector {
    @Override
    public AutocompleteContext collect(Editor editor, Project project) {
        // 1. 获取文档内容
        String content = editor.getDocument().getText();
        
        // 2. 获取光标位置
        int offset = editor.getCaretModel().getOffset();
        
        // 3. 获取语言类型
        PsiFile psiFile = PsiDocumentManager.getInstance(project)
            .getPsiFile(editor.getDocument());
        String language = psiFile != null ? psiFile.getLanguage().getID() : "unknown";
        
        // 4. 构建基础上下文
        return AutocompleteContext.builder()
            .fileContent(content)
            .caretOffset(offset)
            .language(language)
            .build();
    }
}
```

#### 2.2.6 SuggestionRenderer（扩展点）

**职责**：允许插件自定义建议渲染方式

**接口设计**：

```java
public interface SuggestionRenderer {
    ExtensionPointName<SuggestionRenderer> EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.autocomplete.suggestionRenderer");
    
    /**
     * 渲染建议
     */
    void render(@NotNull AutocompleteSuggestion suggestion, @NotNull Editor editor);
    
    /**
     * 获取优先级（数字越大优先级越高）
     */
    default int getPriority() {
        return 0;
    }
}
```

**默认实现**：

```java
public class GhostTextSuggestionRenderer implements SuggestionRenderer, EditorCustomElementRenderer {
    @Override
    public void render(AutocompleteSuggestion suggestion, Editor editor) {
        // 使用 Inlay API 渲染 Ghost Text
        InlayProperties properties = InlayProperties()
            .disableSoftWrapping(false)
            .priority(100);
        
        Inlay<GhostTextSuggestionRenderer> inlay = InlayModelUtil.addInlineElement(
            editor,
            suggestion.getStartOffset(),
            properties,
            this
        );
    }
    
    @Override
    public void paint(Inlay inlay, Graphics g, Rectangle targetRegion, TextAttributes textAttributes) {
        // 绘制 Ghost Text
    }
}
```

### 2.3 扩展点定义

在 `plugin.xml` 中定义扩展点：

```xml
<extensionPoints>
    <!-- 上下文收集器扩展点 -->
    <extensionPoint name="contextCollector"
                    interface="dev.dong4j.zeka.stack.idea.plugin.common.autocomplete.ContextCollector"/>
    
    <!-- 建议渲染器扩展点 -->
    <extensionPoint name="suggestionRenderer"
                    interface="dev.dong4j.zeka.stack.idea.plugin.common.autocomplete.SuggestionRenderer"/>
</extensionPoints>
```

## 3. 实现细节

### 3.1 编辑器操作拦截

使用 `EditorActionsRouterService` 拦截 Tab/ESC 等按键：

```java
@Service(Level.APP)
public class EditorActionsRouterService implements Disposable {
    private final Map<String, EditorActionHandler> originalHandlers = new ConcurrentHashMap<>();
    
    public void install() {
        EditorActionManager eam = EditorActionManager.getInstance();
        
        // 拦截 Tab 键
        String tabActionId = "EditorTab";
        EditorActionHandler original = eam.getActionHandler(tabActionId);
        originalHandlers.put(tabActionId, original);
        
        eam.setActionHandler(tabActionId, new EditorActionHandler() {
            @Override
            public void doExecute(Editor editor, Caret caret, DataContext dataContext) {
                // 1. 检查是否有活动的补全建议
                Project project = editor.getProject();
                if (project != null) {
                    AutocompleteService service = AutocompleteService.getInstance(project);
                    AutocompleteTracker tracker = service.getTracker(editor);
                    
                    if (tracker != null && tracker.hasActiveSuggestion()) {
                        // 2. 接受建议
                        tracker.acceptSuggestion();
                        return;
                    }
                }
                
                // 3. 执行原始操作
                original.doExecute(editor, caret, dataContext);
            }
        });
        
        // 类似地拦截 ESC 键
        // ...
    }
}
```

### 3.2 防抖机制

使用 `Debouncer` 减少频繁请求：

```java
public class Debouncer {
    private final AtomicReference<Future<?>> lastTask = new AtomicReference<>();
    private final ScheduledExecutorService executor;
    
    public Debouncer(ScheduledExecutorService executor) {
        this.executor = executor;
    }
    
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

### 3.3 拒绝缓存

使用 `AutocompleteRejectionCache` 记住被拒绝的建议：

```java
@Service(Level.PROJECT)
@State(name = "AutocompleteRejectionCache", storages = @Storage("autocomplete-rejections.xml"))
public class AutocompleteRejectionCache implements PersistentStateComponent<AutocompleteRejectionCache.State> {
    
    public static class State {
        public Set<String> rejectedKeys = new HashSet<>();
    }
    
    private State state = new State();
    
    public void addRejection(String key) {
        state.rejectedKeys.add(key);
        
        // 限制缓存大小
        if (state.rejectedKeys.size() > MAX_CACHE_SIZE) {
            // 移除最旧的条目（简化实现：随机移除）
            Iterator<String> it = state.rejectedKeys.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }
    
    public boolean isRejected(String key) {
        return state.rejectedKeys.contains(key);
    }
}
```

### 3.4 提示词构建

构建补全请求的提示词：

```java
private String buildPrompt(AutocompleteContext context) {
    StringBuilder sb = new StringBuilder();
    
    // 1. 系统提示词
    sb.append("You are a code completion assistant. ");
    sb.append("Predict the next edit the developer wants to make. ");
    sb.append("Return only the code to complete, without explanation.\n\n");
    
    // 2. 文件内容（截取相关部分）
    String relevantCode = extractRelevantCode(context.getFileContent(), context.getCaretOffset());
    sb.append("Current file content:\n```").append(context.getLanguage()).append("\n");
    sb.append(relevantCode);
    sb.append("\n```\n\n");
    
    // 3. 光标位置
    sb.append("Caret position: line ").append(context.getCaretLine())
      .append(", column ").append(context.getCaretColumn()).append("\n\n");
    
    // 4. 相关代码块（如果存在）
    if (!context.getRelevantChunks().isEmpty()) {
        sb.append("Relevant code chunks:\n");
        for (CodeChunk chunk : context.getRelevantChunks()) {
            sb.append("```").append(chunk.getLanguage()).append("\n");
            sb.append(chunk.getContent());
            sb.append("\n```\n");
        }
    }
    
    // 5. 最近编辑历史（如果存在）
    if (!context.getRecentEdits().isEmpty()) {
        sb.append("Recent edits:\n");
        for (EditRecord edit : context.getRecentEdits()) {
            sb.append("- ").append(edit.getDescription()).append("\n");
        }
    }
    
    // 6. 请求补全
    sb.append("\nPlease provide the code completion suggestion:");
    
    return sb.toString();
}
```

## 4. 配置管理

### 4.1 设置项

在 `AICommonSettings` 中添加补全相关设置：

```java
public class AICommonSettings {
    // 启用/禁用自动补全
    public boolean autocompleteEnabled = false;
    
    // 补全延迟时间（毫秒）
    public int autocompleteDebounceDelay = 500;
    
    // 最大上下文行数
    public int autocompleteMaxContextLines = 100;
    
    // 最大建议长度
    public int autocompleteMaxSuggestionLength = 200;
    
    // 支持的文档类型（语言ID列表）
    public Set<String> autocompleteSupportedLanguages = Set.of(
        "JAVA", "KOTLIN", "PYTHON", "JAVASCRIPT", "TYPESCRIPT"
    );
}
```

### 4.2 设置页面

在 `AICommonSettingsPanel` 中添加补全配置：

```java
private JBCheckBox autocompleteEnabledCheckBox;
private JBTextField debounceDelayTextField;
private JBTextField maxContextLinesTextField;

private JComponent createAutocompletePanel() {
    FormBuilder builder = FormBuilder.createFormBuilder();
    
    // 启用/禁用
    autocompleteEnabledCheckBox = new JBCheckBox("Enable Autocomplete Suggestions");
    builder.addComponent(autocompleteEnabledCheckBox);
    
    // 延迟时间
    builder.addLabeledComponent("Debounce Delay (ms):", debounceDelayTextField);
    
    // 最大上下文行数
    builder.addLabeledComponent("Max Context Lines:", maxContextLinesTextField);
    
    return builder.getPanel();
}
```

## 5. 使用示例

### 5.1 基本使用（自动启用）

插件只需要依赖 `intelli-ai-engine`，补全功能会自动启用（如果用户已启用）：

```java
// 无需额外代码，功能自动工作
// EditorActionsRouterService 会自动拦截 Tab/ESC 键
// AutocompleteTracker 会自动监听编辑器变化
```

### 5.2 自定义上下文收集器

插件可以实现 `ContextCollector` 扩展点，添加自定义上下文：

```java
public class CustomContextCollector implements ContextCollector {
    @Override
    public AutocompleteContext collect(Editor editor, Project project) {
        AutocompleteContext base = DefaultContextCollector.INSTANCE.collect(editor, project);
        
        // 添加自定义上下文
        // 例如：添加项目特定的配置信息
        Map<String, String> customContext = new HashMap<>();
        customContext.put("projectType", detectProjectType(project));
        customContext.put("framework", detectFramework(project));
        
        return base.withCustomContext(customContext);
    }
    
    @Override
    public int getPriority() {
        return 100; // 高于默认实现
    }
}
```

在 `plugin.xml` 中注册：

```xml
<extensions defaultExtensionNs="dev.dong4j.zeka.stack.idea.plugin.common.ai">
    <contextCollector implementation="com.example.CustomContextCollector"/>
</extensions>
```

### 5.3 自定义建议渲染器

插件可以实现 `SuggestionRenderer` 扩展点，自定义渲染方式：

```java
public class CustomSuggestionRenderer implements SuggestionRenderer {
    @Override
    public void render(AutocompleteSuggestion suggestion, Editor editor) {
        // 自定义渲染逻辑
        // 例如：使用弹出窗口而非 Ghost Text
    }
    
    @Override
    public int getPriority() {
        return 100; // 高于默认实现
    }
}
```

## 6. 文件结构

```
intelli-ai-engine/
├── src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/
│   └── autocomplete/
│       ├── AutocompleteService.java           # 服务接口
│       ├── AutocompleteServiceImpl.java       # 服务实现
│       ├── AutocompleteTracker.java           # 编辑器追踪器
│       ├── AutocompleteContext.java           # 上下文数据模型
│       ├── AutocompleteSuggestion.java        # 建议抽象类
│       ├── GhostTextSuggestion.java           # Ghost Text 建议
│       ├── PopupSuggestion.java               # 弹出窗口建议
│       ├── ContextCollector.java              # 上下文收集器接口
│       ├── DefaultContextCollector.java       # 默认上下文收集器
│       ├── SuggestionRenderer.java            # 渲染器接口
│       ├── GhostTextSuggestionRenderer.java   # Ghost Text 渲染器
│       ├── EditorActionsRouterService.java    # 编辑器操作路由
│       ├── AutocompleteRejectionCache.java    # 拒绝缓存
│       ├── Debouncer.java                     # 防抖工具类
│       └── util/
│           ├── ContextUtils.java              # 上下文工具类
│           └── PromptBuilder.java             # 提示词构建器
└── src/main/resources/
    └── META-INF/
        └── plugin.xml                         # 扩展点定义
```

## 7. 实现步骤

### 阶段一：核心功能（MVP）

1. ✅ 实现 `AutocompleteService` 和 `AutocompleteTracker`
2. ✅ 实现 `AutocompleteContext` 和 `AutocompleteSuggestion`
3. ✅ 实现 `DefaultContextCollector` 和基础上下文收集
4. ✅ 实现 `GhostTextSuggestionRenderer` 和 Ghost Text 显示
5. ✅ 实现 `EditorActionsRouterService` 和按键拦截
6. ✅ 集成 `AIService` 发起补全请求
7. ✅ 实现防抖机制
8. ✅ 实现拒绝缓存

### 阶段二：扩展和优化

1. ✅ 实现扩展点（`ContextCollector`、`SuggestionRenderer`）
2. ✅ 添加设置页面配置
3. ✅ 性能优化（请求去重、异步处理）
4. ✅ 支持多语言
5. ✅ 添加日志和错误处理

### 阶段三：高级功能

1. ✅ 支持多建议（`MultipleGhostTextSuggestion`）
2. ✅ 支持导入修复（`ImportFixSuggestion`）
3. ✅ 支持跳转建议（`JumpToEditSuggestion`）
4. ✅ 添加指标追踪
5. ✅ 添加用户反馈机制

## 8. 注意事项

### 8.1 线程安全

- 所有 UI 操作必须在 EDT（Event Dispatch Thread）中执行
- PSI 操作必须在 `ReadAction` 中执行
- 文档修改必须在 `WriteCommandAction` 中执行
- 网络请求在后台线程执行，结果通过 `invokeLater` 更新 UI

### 8.2 性能考虑

- 使用防抖机制减少请求频率
- 限制上下文大小（最大行数、最大字符数）
- 使用缓存减少重复请求
- 及时清理不需要的建议对象

### 8.3 兼容性

- 确保与 IntelliJ Platform 的兼容性（最低版本要求）
- 确保与现有插件的兼容性（不破坏现有功能）
- 确保与不同语言的兼容性（Java、Kotlin、Python 等）

### 8.4 用户体验

- 提供清晰的设置选项
- 提供启用/禁用的快捷方式
- 提供错误提示和日志
- 提供性能指标和统计信息

## 9. 测试计划

### 9.1 单元测试

- `AutocompleteTracker` 的编辑监听逻辑
- `ContextCollector` 的上下文收集逻辑
- `SuggestionRenderer` 的渲染逻辑
- `Debouncer` 的防抖逻辑

### 9.2 集成测试

- 完整的补全流程（编辑 → 请求 → 显示 → 接受）
- 扩展点的集成
- 多编辑器场景
- 多语言场景

### 9.3 性能测试

- 响应时间（从编辑到显示建议的时间）
- 内存使用（上下文缓存、建议对象）
- CPU 使用（防抖、渲染）

## 10. 参考资源

- [IntelliJ Platform SDK - Inlay API](https://plugins.jetbrains.com/docs/intellij/inlays.html)
- [IntelliJ Platform SDK - Editor Actions](https://plugins.jetbrains.com/docs/intellij/editor-actions.html)
- [SweepAI Tab 补全功能实现分析](./SweepAI-Tab补全功能实现分析.md)
- [Kotlin Coroutines 文档](https://kotlinlang.org/docs/coroutines-overview.html)

