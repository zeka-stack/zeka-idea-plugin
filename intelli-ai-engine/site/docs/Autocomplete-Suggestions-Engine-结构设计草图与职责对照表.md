# Autocomplete Suggestions Engine 侧落地结构设计草图与职责对照表

> 目标：基于现有 engine 架构，落地可复用的 Autocomplete Suggestions 能力，支持多插件接入、可扩展触发与渲染、可插拔模型调用与缓存。

## 1. 设计草图（模块拓扑）

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Plugin Consumers                            │
│  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────┐ │
│  │ intelli-ai-javadoc │  │ intelli-ai-changelog│  │   custom plugin│ │
│  └────────────────────┘  └────────────────────┘  └────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        Engine Autocomplete API                       │
│  AutocompleteFacade / AutocompleteService (Project/IDE Service)      │
└──────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           Core Orchestration                          │
│  AutocompleteCoordinator                                              │
│  - per-editor tracker registry                                        │
│  - lifecycle control (enable/disable)                                 │
│  - policy dispatch                                                    │
└──────────────────────────────────────────────────────────────────────┘
       │                 │                 │                 │
       ▼                 ▼                 ▼                 ▼
┌───────────────┐ ┌───────────────┐ ┌────────────────┐ ┌─────────────────┐
│ Trigger Engine│ │ Request Builder│ │ Suggestion Cache│ │ Renderer Engine │
│ - rules chain │ │ - context pack │ │ - local cache   │ │ - inlay renderer│
│ - entry hooks │ │ - model params │ │ - request de-dupe│ │ - ghost text    │
└───────────────┘ └───────────────┘ └────────────────┘ └─────────────────┘
       │                 │                 │                 │
       ▼                 ▼                 ▼                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Completion Pipeline                           │
│  PreCompletionDispatcher  +  CompletionDispatcher (debounced)         │
│  - async model call (AIService / LSP)                                  │
│  - timeout / cancel / metrics                                          │
└──────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          Suggestion Lifecycle                          │
│  - accept / cancel / toggle / accept-by-line                           │
│  - telemetry hooks                                                     │
│  - editor state sync                                                   │
└──────────────────────────────────────────────────────────────────────┘
```

## 2. Engine 侧模块职责对照表（与 Cosy 对齐）

| Engine 模块                                | 职责                                   | 建议位置                             | Cosy 对照实现                                                                  |
|------------------------------------------|--------------------------------------|----------------------------------|----------------------------------------------------------------------------|
| AutocompleteFacade / AutocompleteService | 对外统一入口，启用/禁用、获取 editor tracker       | `engine/autocomplete/`           | `CosyInlayManager`, `CosyCompletionService`                                |
| AutocompleteCoordinator                  | 全局调度与 registry，管理每个 Editor 的 Tracker | `engine/autocomplete/core/`      | `CosyInlayManagerImpl` 中的 editor 管理逻辑                                      |
| AutocompleteTracker                      | 监听编辑器变化，触发补全请求                       | `engine/autocomplete/tracker/`   | `CosyCommandListener`, `CosyDocumentListener`, `CosyLookupManagerListener` |
| TriggerEngine + TriggerRule              | 规则链，决定是否触发                           | `engine/autocomplete/trigger/`   | `TriggerExecutorFactory` + 各种 `TriggerChecker`                             |
| RequestBuilder                           | 组织 `CompletionContext` 与模型参数         | `engine/autocomplete/request/`   | `InlayPreviewRequest`                                                      |
| PreCompletionDispatcher                  | 预计算机制，提前 warmup                      | `engine/autocomplete/dispatch/`  | `CompletionUtil.triggerPreCompletion`                                      |
| CompletionDispatcher                     | 防抖 + 异步请求                            | `engine/autocomplete/dispatch/`  | `LanguageWebSocketService.aysncCompletionInlayWithDebouncer`               |
| SuggestionCache                          | 去重与缓存                                | `engine/autocomplete/cache/`     | `CompletionCacheManager`, `CompletionRequestManager`                       |
| SuggestionCollector                      | 响应解析、转为 SuggestionItem               | `engine/autocomplete/collector/` | `DefaultInlayCompletionCollector`                                          |
| RendererEngine                           | Suggestion 渲染（Inlay/GhostText）       | `engine/autocomplete/render/`    | `CosyInlayManagerImpl.renderInlayCompletionItem`                           |
| SuggestionLifecycle                      | 接受/取消/切换/逐行接受                        | `engine/autocomplete/lifecycle/` | `applyCompletion`, `applyCompletionByLine`, `disposeInlays`                |
| TelemetryHook                            | 记录触发、采纳、取消                           | `engine/autocomplete/metrics/`   | `TelemetryService`                                                         |

## 3. Engine 侧建议接口草图

### 3.1 外部接入层（对插件）

```java
public interface AutocompleteService {
    static AutocompleteService getInstance(Project project) {
        return project.getService(AutocompleteService.class);
    }

    void setEnabled(boolean enabled);
    boolean isEnabled();

    void enableEditor(Editor editor);
    void disableEditor(Editor editor);

    @Nullable
    AutocompleteTracker getTracker(Editor editor);
}
```

### 3.2 Trigger 规则接口

```java
public interface TriggerRule {
    boolean check(TriggerContext context);

    default boolean ignoreWhenNotPass() { return false; }
    default boolean passAllWhenPassOne() { return false; }
}

public final class TriggerEngine {
    private final List<TriggerRule> rules;

    public boolean shouldTrigger(TriggerContext context) {
        for (TriggerRule rule : rules) {
            if (!rule.check(context)) {
                if (!rule.ignoreWhenNotPass()) return false;
            } else if (rule.passAllWhenPassOne()) {
                return true;
            }
        }
        return true;
    }
}
```

### 3.3 Suggestion 数据结构

```java
public class AutocompleteSuggestion {
    private final String content;
    private final List<String> inlineLines;
    private final List<String> blockLines;
    private final int cursorOffset;
    private final boolean finished;
    private final String requestId;
    private final String cacheId;
}
```

## 4. 关键流程建议

### 4.1 自动触发（Typing）

1. `CommandListener` 收集输入变更
2. `TriggerEngine.shouldTrigger(...)`
3. `RequestBuilder.build(context)`
4. `PreCompletionDispatcher.send(...)`
5. `CompletionDispatcher.send(...)`
6. `SuggestionCollector.onCollect(...)`
7. `RendererEngine.render(...)`

### 4.2 手动触发

1. Action 调用 `AutocompleteService.enableEditor(editor)`
2. 构建 `CompletionTriggerMode=MANUAL`
3. 跟随自动流程

### 4.3 接受/取消

- `Tab`: accept
- `Esc`: cancel
- `Alt+[` / `Alt+]`: toggle

由 `SuggestionLifecycle` 统一处理事件，更新缓存与 renderer。

## 5. 与现有 engine 组件的对接建议

- **AI 服务调用**：复用 engine 内已有的 AI Provider 体系（或 LSP 渠道）
- **设置项**：挂到 engine Settings，支持禁用、延迟、模型级别、生成长度
- **扩展点**：允许插件注入自定义 TriggerRule / ContextCollector / Renderer
- **测试策略**：
    - Trigger 规则单测
    - RequestBuilder 生成参数单测
    - Renderer 渲染边界测试（空文本/多行/缩进）

## 6. 风险点与落地注意事项

1. **和 IDE 自带 Completion 冲突**：需要监听 Lookup 打开/关闭时机，避免双重提示。
2. **输入法场景**：中文输入期间可能触发无意义补全，建议过滤（Cosy 使用“上个字符为中文”策略）。
3. **大文件性能**：超过阈值直接跳过补全请求。
4. **多光标**：多 caret 时禁用补全，避免插入混乱。
5. **模板/Live Template**：模板展开时跳过触发，避免污染。

## 7. 建议落地路径（可迭代）

1. **Phase 1**：完成 TriggerEngine + RequestBuilder + CompletionDispatcher + RendererEngine（仅 Inline 渲染）
2. **Phase 2**：加入 SuggestionCache + PreCompletion + accept-by-line
3. **Phase 3**：扩展点开放给插件（ContextCollector / Renderer / TriggerRule）

