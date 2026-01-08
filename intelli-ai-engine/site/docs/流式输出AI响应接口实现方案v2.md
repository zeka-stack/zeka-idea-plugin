# 流式输出 AI 响应接口实现方案 v2

## 📋 目录

- [背景](#背景)
- [问题分析](#问题分析)
- [架构设计](#架构设计)
- [核心组件](#核心组件)
- [实现细节](#实现细节)
- [使用示例](#使用示例)
- [优势对比](#优势对比)
- [注意事项](#注意事项)

---

## 背景

### 需求背景

在 AI 服务集成中，流式输出是提升用户体验的关键特性。不同 AI 厂商和模型对流式响应的格式定义存在显著差异，这给统一解析带来了巨大挑战。

### v1 方案的问题

v1 方案采用 **厂商 + 模型** 的静态映射方式选择解析器：

```java
(vendor, model) -> Parser
```

这种方案存在以下问题：

1. **新模型无法匹配**：新增模型无法自动匹配到解析器，导致解析失败
2. **同模型版本差异**：同一模型的不同版本可能使用不同的输出格式
3. **代理网关干扰**：通过代理或网关时，字段可能被重写
4. **私有部署不规范**：私有部署的模型返回字段可能不规范
5. **思考模式切换**：模型开启/关闭思考模式时，结构会发生变化

### 核心问题

**流式输出格式 ≠ 模型本身定义的，而是由「厂商的推理服务实现」决定的。**

同一个模型 C，被不同厂商 A / B 部署，流式输出结构很可能不一样。即使同一个厂商，不同模型、不同版本、是否开启思考模式、是否走代理，输出结构都可能变化。

---

## 问题分析

### 为什么是「厂商绑定」而不是「模型绑定」

#### 1. 模型本身只定义"能力"，不定义"协议"

从模型视角：

- 模型内部只有：token、hidden state、是否开启 chain-of-thought（思考）
- 模型根本不知道：SSE、JSON、reasoning_content、`<think>` 标签、delta / chunk / event

这些都是推理服务（Serving Layer）的事情。

#### 2. 流式输出发生在「模型之外」

真实链路：

```
Client
↓
HTTP / WebSocket / SSE
↓
厂商推理服务（Adapter / Gateway）
↓
Tokenizer / Decoder
↓
Model
```

你看到的这些字段：

```json
{
  "delta": {
    "content": "...",
    "reasoning_content": "..."
  }
}
```

或者：

```
"content": "<think>xxx</think>yyy"
```

全部是厂商在「模型输出 token 后二次包装的结果」。

#### 3. 现实世界的差异

不同厂商对思考内容的表示方式：

| 厂商         | 思考内容形式                           |
|------------|----------------------------------|
| OpenAI     | `reasoning_content`（部分模型）        |
| Qwen / Ali | `reasoning` / `thinking`         |
| DeepSeek   | `content` 中 `<think>...</think>` |
| 一些私有部署     | 完全不暴露思考，只给结果                     |
| 某些代理网关     | 把 `reasoning` 合并进 `content`      |

**模型一样，字段完全不一样。**

---

## 架构设计

### 核心思想

**从「是谁」→「它怎么说话」**

根据"流式行为特征"来判断解析策略，而不是根据厂商和模型名称。

### 架构总览

```
Raw Stream → Strategy Chain → Semantic Chunks → UI / Engine
```

```
┌──────────────┐
│ LLM Client   │ ← SSE / WS / HTTP Chunk
└──────┬───────┘
       ↓ RawChunk
┌─────────────────────────────┐
│ StreamParseEngine           │
│  (Strategy Chain Executor)  │
└──────┬──────────────────────┘
       ↓ StreamChunk（语义）
┌─────────────────────────────┐
│ UI / IDE / Logger / Stats   │
└─────────────────────────────┘
```

### 设计原则

1. **协议无关**：不依赖具体的字段名
2. **语义归一化**：统一输出语义层模型
3. **动态识别**：运行时特征探测，而非静态绑定
4. **容错解析**：多策略兜底，确保永远能输出结果

---

## 核心组件

### 1. StreamParseEngine（解析引擎）

负责执行策略链，按优先级顺序尝试匹配策略。

```java
public final class StreamParseEngine {
    private final List<StreamParseStrategy> strategies;

    public void parse(ParseContext context, RawStreamChunk chunk, StreamChunkEmitter emitter) {
        for (StreamParseStrategy strategy : strategies) {
            if (strategy.supports(context, chunk)) {
                strategy.parse(context, chunk, emitter);
                break; // 只允许一个策略消费
            }
        }
    }

    public static StreamParseEngine createDefault() {
        List<StreamParseStrategy> strategies = List.of(
            new ReasoningFieldStrategy(),    // 优先级 100
            new ThinkTagStrategy(),           // 优先级 90
            new MessageContentStrategy(),     // 优先级 50
            new FallbackTextStrategy()        // 优先级 0
        );
        return new StreamParseEngine(strategies);
    }
}
```

### 2. StreamParseStrategy（解析策略接口）

定义了解析策略的通用接口：

```java
public interface StreamParseStrategy {
    /**
     * 获取优先级，数值越大优先级越高
     */
    int priority();

    /**
     * 判断是否支持当前上下文和数据块
     */
    boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk);

    /**
     * 解析数据块并发射结果
     */
    void parse(@NotNull ParseContext context,
               @NotNull RawStreamChunk chunk,
               @NotNull StreamChunkEmitter emitter);
}
```

### 3. ParseContext（解析上下文）

维护跨 chunk 的状态，用于处理流式 `<think>` 标签：

```java
public class ParseContext {
    private boolean inThinking = false;
    private final StringBuilder tagBuffer = new StringBuilder();
    private boolean fallbackWarningEmitted = false;

    public void enterThinking() { inThinking = true; }
    public void exitThinking() { inThinking = false; }
    // ... 标签缓冲区管理方法
}
```

### 4. RawStreamChunk（原始数据块）

封装从 JSON 解析出的原始字段：

```java
public final class RawStreamChunk {
    private final @Nullable String content;
    private final @Nullable String reasoning;
    private final @Nullable String reasoningContent;
    private final @Nullable String role;
    private final @Nullable String finishReason;
    private final @Nullable String rawJson;

    public static @NotNull RawStreamChunk fromJson(@NotNull JsonObject json) {
        // 从 JSON 中提取各字段
    }
}
```

### 5. StreamChunk（语义数据块）

统一的语义层输出：

```java
public record StreamChunk(@NotNull StreamChunkType type, @NotNull String text) {
}

public enum StreamChunkType {
    THINKING,    // 思考内容
    CONTENT,     // 正文内容
    TOOL_CALL,   // 工具调用
    META,        // 元数据
    END          // 结束标记
}
```

### 6. StreamChunkEmitter（数据发射器）

用于策略将解析结果发送到下游：

```java
public interface StreamChunkEmitter {
    void emit(StreamChunk chunk);
}
```

---

## 实现细节

### 策略实现

#### 1. ReasoningFieldStrategy（推理字段策略）

**优先级：100（最高）**

处理包含 `reasoning` 或 `reasoningContent` 字段的情况：

```java
public class ReasoningFieldStrategy implements StreamParseStrategy {
    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        return hasText(chunk.reasoning()) || hasText(chunk.reasoningContent());
    }

    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        // 发射思考内容
        emitIfPresent(emitter, StreamChunkType.THINKING, chunk.reasoningContent());
        emitIfPresent(emitter, StreamChunkType.THINKING, chunk.reasoning());
        // 发射正文内容
        emitIfPresent(emitter, StreamChunkType.CONTENT, chunk.content());
    }
}
```

**覆盖场景**：OpenAI、Qwen、Ali、部分私有模型

#### 2. ThinkTagStrategy（标签策略）

**优先级：90**

处理 `content` 中包含 `<think>...</think>` 标签的情况：

```java
public class ThinkTagStrategy implements StreamParseStrategy {
    @Override
    public int priority() {
        return 90;
    }

    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        String content = chunk.content();
        if (content == null || content.isEmpty()) {
            return context.hasTagBuffer();
        }
        return context.hasTagBuffer()
               || context.isInThinking()
               || content.contains("<")
               || content.contains(">");
    }

    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        String content = chunk.content();
        StringBuilder textBuffer = new StringBuilder();

        // 字符级状态机处理 <think> 标签
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (context.hasTagBuffer() || ch == '<') {
                context.appendTagChar(ch);
                if (context.isOpenTagComplete()) {
                    flushText(context, textBuffer, emitter);
                    context.clearTagBuffer();
                    context.enterThinking();
                    continue;
                }
                if (context.isCloseTagComplete()) {
                    flushText(context, textBuffer, emitter);
                    context.clearTagBuffer();
                    context.exitThinking();
                    continue;
                }
                if (!context.isTagPrefix()) {
                    textBuffer.append(context.consumeTagBuffer());
                }
                continue;
            }
            textBuffer.append(ch);
        }
        flushText(context, textBuffer, emitter);
    }

    private void flushText(ParseContext context, StringBuilder textBuffer, StreamChunkEmitter emitter) {
        if (textBuffer.isEmpty()) {
            return;
        }
        StreamChunkType type = context.isInThinking() ? StreamChunkType.THINKING : StreamChunkType.CONTENT;
        emitter.emit(new StreamChunk(type, textBuffer.toString()));
        textBuffer.setLength(0);
    }
}
```

**关键点**：

- 使用增量状态机，不能一次性 split
- 处理跨 chunk 的标签（如 `<th` 在一个 chunk，`ink>` 在下一个 chunk）
- 维护 `ParseContext` 状态跟踪思考模式

**覆盖场景**：DeepSeek、一些开源模型

#### 3. MessageContentStrategy（消息内容策略）

**优先级：50**

处理标准的 `content` 字段：

```java
public class MessageContentStrategy implements StreamParseStrategy {
    @Override
    public int priority() {
        return 50;
    }

    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        return chunk.content() != null;
    }

    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        String content = chunk.content();
        if (content != null && !content.isEmpty()) {
            emitter.emit(new StreamChunk(StreamChunkType.CONTENT, content));
        }
    }
}
```

#### 4. FallbackTextStrategy（兜底策略）

**优先级：0（最低）**

永远支持，确保任何情况都能解析：

```java
public class FallbackTextStrategy implements StreamParseStrategy {
    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        return true; // 永远支持
    }

    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        String content = chunk.content();
        if (content != null && !content.isEmpty()) {
            emitter.emit(new StreamChunk(StreamChunkType.CONTENT, content));
        }
        // 在流结束时输出警告提示
        if (chunk.isDone() && !context.isFallbackWarningEmitted()) {
            context.markFallbackWarningEmitted();
            emitter.emit(new StreamChunk(StreamChunkType.CONTENT,
                "\n" + FALLBACK_WARNING));
        }
    }
}
```

**作用**：确保"识别失败"不会变成"功能不可用"。最差情况是不能区分思考，但永远能输出结果。

---

## 使用示例

### 基本使用

在 `StreamRequestExecutor` 中的使用：

```java
private void readStreamResponse(HttpURLConnection connection,
                                @NotNull AIStreamResponseListener listener,
                                @Nullable StreamCancellationToken cancellationToken) throws IOException {
    StringBuilder fullText = new StringBuilder();
    StreamParseEngine parseEngine = StreamParseEngine.createDefault();
    ParseContext parseContext = new ParseContext();
    boolean[] inThinking = {false};
    boolean[] thinkPrefixPrinted = {false};

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }

                JsonObject json = parseSseJson(data);
                if (json == null) {
                    continue;
                }

                RawStreamChunk rawChunk = RawStreamChunk.fromJson(json);

                // 使用解析引擎处理原始数据块
                parseEngine.parse(parseContext, rawChunk, chunk -> {
                    if (chunk.text().isEmpty()) {
                        return;
                    }

                    if (chunk.type() == StreamChunkType.THINKING) {
                        // 处理思考内容
                        printThinking(chunk.text(), inThinking, thinkPrefixPrinted);
                        listener.onThinkingChunk(chunk.text());
                        return;
                    }

                    if (chunk.type() == StreamChunkType.CONTENT) {
                        // 处理正文内容
                        if (inThinking[0]) {
                            AIConsoleLoggerUtil.printStreamPlain(
                                project, "\n══════════════════════════════ 正文内容 ══════════════════════════════\n");
                            inThinking[0] = false;
                            thinkPrefixPrinted[0] = false;
                        }
                        fullText.append(chunk.text());
                        AIConsoleLoggerUtil.printStreamPlain(project, chunk.text());
                        listener.onChunk(chunk.text());
                    }
                });
            }
        }
    }

    listener.onComplete(fullText.toString());
}
```

### 自定义策略

如果需要添加新的解析策略：

```java
public class CustomStrategy implements StreamParseStrategy {
    @Override
    public int priority() {
        return 80; // 介于 ThinkTagStrategy 和 MessageContentStrategy 之间
    }

    @Override
    public boolean supports(@NotNull ParseContext context, @NotNull RawStreamChunk chunk) {
        // 根据特征判断是否支持
        return chunk.hasCustomField();
    }

    @Override
    public void parse(@NotNull ParseContext context,
                      @NotNull RawStreamChunk chunk,
                      @NotNull StreamChunkEmitter emitter) {
        // 解析逻辑
        emitter.emit(new StreamChunk(StreamChunkType.CONTENT, chunk.customContent()));
    }
}

// 创建自定义引擎
StreamParseEngine customEngine = new StreamParseEngine(List.of(
    new ReasoningFieldStrategy(),
    new ThinkTagStrategy(),
    new CustomStrategy(),  // 添加自定义策略
    new MessageContentStrategy(),
    new FallbackTextStrategy()
));
```

---

## 优势对比

### v1 vs v2 对比

| 特性         | v1 方案       | v2 方案      |
|------------|-------------|------------|
| **新模型支持**  | ❌ 需要手动注册    | ✅ 自动识别     |
| **版本兼容性**  | ❌ 版本变化需更新   | ✅ 自动适配     |
| **代理网关**   | ❌ 可能解析失败    | ✅ 自动降级     |
| **私有部署**   | ❌ 需要定制解析器   | ✅ 兜底策略保证可用 |
| **思考模式切换** | ❌ 需要配置      | ✅ 自动识别     |
| **代码维护**   | ❌ 每加一个厂商改一次 | ✅ 策略独立，易扩展 |
| **容错能力**   | ❌ 识别失败即失败   | ✅ 永远有输出    |

### 解决的问题

✅ **新模型无需注册**：根据特征自动识别
✅ **新厂商无需改核心逻辑**：添加策略即可
✅ **协议变动自动降级**：优先尝试精确匹配，失败则降级
✅ **`<think>` / 字段混合可共存**：不同策略处理不同特征
✅ **UI 与厂商彻底解耦**：只关心语义类型
✅ **可统计、可演进、可回溯**：记录未知结构，便于后续优化

---
