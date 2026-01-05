# Autocomplete Suggestions Engine 落地方案（整合版）

> 基于以下 4 篇文档汇总并落地：
> - `intelli-ai-engine/site/docs/SweepAI-Tab补全功能实现分析.md`
> - `intelli-ai-engine/site/docs/Cosy-Intellij-Autocomplete-Suggestions-实现分析.md`
> - `intelli-ai-engine/site/docs/Autocomplete-Suggestions功能实现方案.md`
> - `intelli-ai-engine/site/docs/Autocomplete-Suggestions-Engine-结构设计草图与职责对照表.md`
>
> 目标：在 engine 中实现完整、可复用、可扩展的 Autocomplete Suggestions 能力；本文档的类设计、职责与流程足以指导完整实现。

---

## 1. 统一目标与能力清单

### 1.1 核心能力

- 自动监听编辑器输入变化，预测下一编辑内容
- 以 Ghost Text / Inlay 的方式渲染建议（默认 Ghost Text）
- 支持手动触发、自动触发、Lookup 关闭后触发
- 支持 Tab 接受、Esc 拒绝、前后切换候选、逐行接受
- 支持上下文收集（文件内容、光标位置、最近编辑、相关代码块）
- 支持缓存 / 去重 / 防抖 / 预计算（preCompletion）
- 支持多插件共享的统一入口与扩展点

### 1.2 设计原则

- **Engine 层能力通用化**：暴露统一服务，插件只关注“开关 + 事件”。
- **规则与渲染可插拔**：Trigger / Context / Renderer 都可扩展。
- **避免与 IDE 原生 Completion 冲突**：Lookup 打开时抑制自动补全。
- **安全稳态**：大文件、中文输入法、多光标、Live Template、只读编辑器均禁用。

---

## 2. 总体架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Plugin Consumers                            │
│  - intelli-ai-javadoc / intelli-ai-changelog / custom plugin          │
└──────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        Engine Autocomplete API                       │
│  AutocompleteService (Project Service)                               │
└──────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           Core Orchestration                          │
│  AutocompleteCoordinator                                              │
│  - editor tracker registry                                            │
│  - lifecycle control                                                  │
│  - policy dispatch                                                    │
└──────────────────────────────────────────────────────────────────────┘
       │                 │                 │                 │
       ▼                 ▼                 ▼                 ▼
┌───────────────┐ ┌───────────────┐ ┌────────────────┐ ┌─────────────────┐
│ Trigger Engine│ │ Request Builder│ │ Suggestion Cache│ │ Renderer Engine │
│ - rules chain │ │ - context pack │ │ - local cache   │ │ - ghost/inlay   │
│ - entry hooks │ │ - model params │ │ - request de-dupe│ │ - styling       │
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

---

## 3. 模块职责与关键类设置

> 目录建议基于 `intelli-ai-engine/src/main`，如 engine 已有命名空间可按实际调整。

### 3.1 API 与协调层

#### AutocompleteService（Project Service）

- 职责：对外统一入口
- 关键方法：
    - `setEnabled(boolean)` / `isEnabled()`
    - `enableEditor(Editor)` / `disableEditor(Editor)`
    - `getTracker(Editor)`

#### AutocompleteCoordinator

- 职责：维护 editor → tracker 映射、统一生命周期
- 关键逻辑：
    - Editor 创建 / 销毁监听
    - 启停策略（设置 / 项目状态 / 认证）
    - 统一派发 Trigger

### 3.2 编辑追踪层

#### AutocompleteTracker（per-editor）

- 职责：监听 Document / Caret / Lookup
- 生命周期：Editor attach/detach
- 核心字段：
    - `currentSuggestion`
    - `lastTriggerTime`
    - `debouncer`
    - `editorState`（上一次编辑状态）

**监听器设置**：

- DocumentListener：捕获插入/删除/替换
- CaretListener：光标变化时取消/预计算
- LookupListener：Lookup 打开/关闭控制触发

### 3.3 触发规则引擎

#### TriggerEngine + TriggerRule

- 规则链输出 shouldTrigger(boolean)
- 默认规则建议：
    - `InitCaretRule`：caret PSI 获取成功
    - `CaretAroundRule`：光标周围合法
    - `CommentRule`：注释场景限制
    - `InvalidElementRule`：过滤 literal/import/keyword
    - `ValidChangeTextRule`：过滤非法字符/符号组合
    - `MultiCaretRule`：多光标直接拒绝
    - `LargeFileRule`：文件过大直接拒绝
    - `LookupActiveRule`：Lookup 开启时禁止
    - `LiveTemplateRule`：模板展开时禁止
    - `IMEChineseRule`：中文输入中间态过滤

### 3.4 请求构建与上下文

#### CompletionContext

- 内容建议：
    - `fileContent`
    - `caretOffset/line/column`
    - `filePath`
    - `recentEdits`（可选）
    - `relatedChunks`（可选）
    - `language`

#### RequestBuilder

- 从 Editor + ContextCollector 构建统一请求
- 生成 `CompletionRequest`：
    - `requestId`
    - `prompt`（上下文 + 指令）
    - `modelParams`（温度、长度、模型等级）
    - `triggerMode`（AUTO/MANUAL/LOOKUP）

#### ContextCollector（扩展点）

- 默认：当前文件上下文、光标、最近编辑
- 可扩展：额外文件、同项目相关片段

### 3.5 Completion Pipeline

#### PreCompletionDispatcher

- 触发时机：光标移动 / 补全前
- 目的：warm-up 降低首 token 延迟

#### CompletionDispatcher

- 统一异步补全请求调度
- 具备：
    - Debounce
    - Timeout
    - Cancellation
    - Dedup（基于 requestId 或内容 hash）

### 3.6 Suggestion 处理与渲染

#### SuggestionCollector

- 将服务端响应解析为 `AutocompleteSuggestion`
- 处理：
    - `content` → `inlineLines` / `blockLines`
    - `<|cursor|>` 占位处理

#### RendererEngine

- 默认：`GhostTextRenderer`
- 可扩展：`InlayRenderer`、`PopupRenderer`
- 统一接口：
    - `render(Editor, AutocompleteSuggestion)`
    - `dispose(Editor)`

### 3.7 Suggestion 生命周期

#### SuggestionLifecycle

- `accept()` / `reject()` / `toggleNext()` / `acceptByLine()`
- 插入时处理：
    - 缩进对齐
    - line-prefix/suffix
    - `<|cursor|>`

### 3.8 Cache / Telemetry

- SuggestionCache：
    - 去重（防重复触发）
    - 最近 N 条缓存
- TelemetryHook：
    - 记录触发、渲染、采纳、拒绝、耗时

---

## 4. 关键类建议定义（可直接实现）

### 4.1 AutocompleteService

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

### 4.2 Trigger 规则

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

### 4.3 Suggestion

```java
public class AutocompleteSuggestion {
    private final String content;
    private final List<String> inlineLines;
    private final List<String> blockLines;
    private final int cursorOffset;
    private final String requestId;
    private final String cacheId;
    private final boolean finished;
}
```

---

## 5. 事件链路与时序

### 5.1 自动触发（Typing）

```
DocumentListener
  -> TriggerEngine.shouldTrigger
  -> RequestBuilder.build
  -> PreCompletionDispatcher.send
  -> CompletionDispatcher.send
  -> SuggestionCollector.onCollect
  -> RendererEngine.render
```

### 5.2 Lookup 关闭触发

```
LookupListener.onLookupClosed
  -> TriggerEngine.shouldTrigger
  -> CompletionDispatcher.send
```

### 5.3 手动触发

```
Action/Shortcut
  -> AutocompleteService.enableEditor
  -> CompletionDispatcher.send (MANUAL)
```

### 5.4 接受/取消

```
Tab -> SuggestionLifecycle.accept
Esc -> SuggestionLifecycle.reject
Alt+[ / Alt+] -> toggle
```

---

## 6. 设置项设计（Settings）

建议在 engine Settings 中提供：

- enableAutocomplete (bool)
- autoTrigger (bool)
- manualTriggerLength
- autoTriggerLength
- modelLevel (fast/balanced/power)
- debounceMs
- timeoutMs
- showInlineWhenIDECompletion (bool)
- disabledLanguages / disabledExtensions

---

## 7. 扩展点（EP）建议

- `AutocompleteContextCollector`：上下文扩展
- `AutocompleteRenderer`：渲染扩展
- `AutocompleteTriggerRule`：触发规则扩展
- `AutocompleteCompletionProvider`：模型调用扩展（AI / LSP）

---

## 7.1 如何与 AI 集成（多服务商场景）

### 7.1.1 总体原则

- **Autocomplete 不直接绑定单一模型**，只依赖 engine 统一的 AI 调度能力
- **通过 Provider 抽象屏蔽差异**：同一套 request/response 结构，交给不同 Provider 适配
- **支持动态路由**：根据用户设置、语言、模型等级或网络状态切换 Provider

### 7.1.2 建议集成方式

1. **复用 engine 现有 AIService/Provider 体系**
    - Autocomplete 模块只负责构建 `AutocompleteCompletionRequest`
    - 交给 engine 的 `AIService` 或 `ProviderManager` 去选择具体 Provider
2. **增加 Autocomplete 维度的 Provider 能力标识**
    - 例如 `capabilities = [autocomplete, chat, completion]`
    - 在 Provider 选择时过滤不支持 Autocomplete 的服务商
3. **统一请求/响应模型**
    - `AutocompleteCompletionRequest`：上下文 + prompt + modelParams + triggerMode
    - `AutocompleteCompletionResponse`：content + finished + cacheId + extraMeta
4. **多 Provider 适配策略**
    - **云端 Provider**：直接调用远端接口
    - **本地 Provider**：对接 Ollama / LM Studio / local LLM
    - **LSP Provider**：复用语言服务端的 completion 接口

### 7.1.3 参考接口草图

```java
public interface AutocompleteCompletionProvider {
    boolean isSupported(@NotNull AutocompleteCompletionRequest request);

    CompletableFuture<AutocompleteCompletionResponse> complete(
        @NotNull AutocompleteCompletionRequest request
    );
}
```

```java
public final class AutocompleteCompletionRouter {
    public CompletableFuture<AutocompleteCompletionResponse> route(
        AutocompleteCompletionRequest request
    ) {
        AutocompleteCompletionProvider provider = providerManager.select(request);
        return provider.complete(request);
    }
}
```

### 7.1.4 Provider 选择建议

- **优先级策略**：用户手动选择 > 语言匹配 > 默认 Provider
- **故障切换**：超时或失败时降级到备选 Provider（可配置）
- **能力灰度**：不同 Provider 支持不同长度与速度策略

### 7.1.5 与 Settings 对接

增加 Autocomplete 维度的 Provider 设置项：\n

- `autocomplete.providerId`：绑定某一 Provider
- `autocomplete.fallbackProviderId`：故障切换\n
- `autocomplete.model`：特定模型名称\n
- `autocomplete.useLocalOnly` / `autocomplete.useCloudOnly`\n

---

## 8. 风险控制与兼容

- 大文件阈值：超过上限跳过
- 多光标：禁用补全
- Live Template：跳过
- 中文输入法：过滤中间态
- Lookup 打开：抑制自动补全
- 只读 / viewer：禁用

---

## 9. 实现阶段建议

1. **Phase 1**：基本链路 + GhostTextRenderer
2. **Phase 2**：PreCompletion + Cache + accept-by-line
3. **Phase 3**：扩展点开放 + 多渲染器支持

---

## 10. 可直接落地的对照清单

- 触发机制：参考 Cosy 的 TriggerExecutor + SweepAI 的 RecentEditsTracker
- 渲染机制：参考 SweepAI GhostTextRenderer / Cosy InlayRenderer
- 请求构建：参考 Cosy InlayPreviewRequest
- 防抖与超时：参考 Cosy LanguageWebSocketService
- 接受/取消：参考 Cosy InlayManager 的 apply/escape 机制

---

## 11. 最终落地路径（结论）

engine 中完整实现 Autocomplete Suggestions 需要具备：

1. 编辑器事件监听 + TriggerRule 链
2. CompletionContext 构建与 RequestBuilder
3. Debounced CompletionDispatcher + PreCompletionDispatcher
4. SuggestionCollector + RendererEngine
5. SuggestionLifecycle + Cache + Telemetry
6. Settings 与 EP 支持

按本方案实现，即可完整达到 Cosy + SweepAI 的核心体验，同时保留 engine 级复用能力。
