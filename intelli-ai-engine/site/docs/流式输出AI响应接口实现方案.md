# 流式输出 AI 响应接口实现方案

## 📋 目录

- [概述](#概述)
- [技术方案](#技术方案)
- [接口设计](#接口设计)
- [实现细节](#实现细节)
- [Console 集成](#console-集成)
- [向后兼容性](#向后兼容性)
- [使用示例](#使用示例)
- [注意事项](#注意事项)

---

## 概述

### 需求背景

当前 AI 服务接口采用同步方式，等待完整响应返回后再处理。对于长文本生成场景，用户需要等待较长时间才能看到结果。流式输出可以：

- ✅ **实时反馈**：逐块显示 AI 生成的内容，提升用户体验
- ✅ **降低等待感**：用户可以看到生成进度，减少等待焦虑
- ✅ **更好的交互**：支持在生成过程中取消操作
- ✅ **Console 输出**：将流式响应实时输出到 IDEA Console 工具窗口

### 技术挑战

1. **HTTP 流式响应处理**：需要处理 SSE (Server-Sent Events) 格式的流式数据
2. **增量解析**：逐块解析 JSON 响应，提取增量内容
3. **线程安全**：确保 Console 输出在 EDT 线程执行
4. **向后兼容**：保持现有同步接口不变，新增流式接口

---

## 技术方案

### 1. 协议支持

AI 服务提供商通常支持两种流式输出方式：

#### 1.1 Server-Sent Events (SSE)

OpenAI 兼容 API 使用 SSE 格式：

```
data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1234567890,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","created":1234567890,"model":"gpt-3.5-turbo","choices":[{"index":0,"delta":{"content":" World"},"finish_reason":null}]}

data: [DONE]
```

#### 1.2 流式 JSON Lines

部分提供商使用换行分隔的 JSON：

```json
{"content": "Hello"}
{"content": " World"}
{"done": true}
```

### 2. 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     调用层 (Plugin)                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AIService.generateContentStream()                    │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   服务层 (ai-common)                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AIServiceProvider.generateContentStream()            │  │
│  └──────────────────────────────────────────────────────┘  │
│                       │                                      │
│                       ▼                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AICompatibleProvider.sendStreamRequest()             │  │
│  │  - 构建流式请求体 (stream: true)                      │  │
│  │  - 处理 SSE 响应                                      │  │
│  │  - 解析增量内容                                       │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   监听器层                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AIStreamResponseListener                             │  │
│  │  - onChunk(content)      // 增量内容                  │  │
│  │  - onComplete(fullText)  // 完成                      │  │
│  │  - onError(error)        // 错误                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                       │                                      │
│                       ▼                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AIConsoleLogger.printStream()                        │  │
│  │  - 增量输出到 Console                                 │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 接口设计

### 1. 流式响应监听器接口

**文件**: `ai-common/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/AIStreamResponseListener.java`

```java
package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 流式响应监听器接口
 * <p>
 * 用于接收 AI 服务的流式响应数据，支持增量内容回调、完成回调和错误处理。
 * 
 * <p>典型使用场景：
 * <ul>
 *   <li>实时显示 AI 生成的内容到 Console</li>
 *   <li>在 UI 中逐块更新显示内容</li>
 *   <li>处理流式响应过程中的错误</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AIStreamResponseListener {
    
    /**
     * 接收增量内容块
     * <p>
     * 当 AI 服务返回新的内容块时调用此方法。内容块可能是单个字符、单词或句子。
     * 
     * <p>注意：
     * <ul>
     *   <li>此方法可能在后台线程调用，如需更新 UI 请使用 {@code ApplicationManager.getApplication().invokeLater()}</li>
     *   <li>内容块可能为空字符串，应忽略</li>
     *   <li>所有内容块按顺序累积即为完整响应</li>
     * </ul>
     *
     * @param chunk 增量内容块
     */
    default void onChunk(@NotNull String chunk) {}
    
    /**
     * 流式响应完成
     * <p>
     * 当所有内容块接收完成时调用此方法，提供完整的响应文本。
     *
     * @param fullText 完整的响应文本
     */
    default void onComplete(@NotNull String fullText) {}
    
    /**
     * 流式响应过程中发生错误
     * <p>
     * 当流式响应过程中发生错误时调用此方法，错误发生后不会再调用 {@code onChunk()} 或 {@code onComplete()}。
     *
     * @param error 错误信息
     * @param exception 异常对象（可能为 null）
     */
    default void onError(@NotNull String error, @Nullable Throwable exception) {}
    
    /**
     * 流式响应开始
     * <p>
     * 在开始接收流式响应时调用此方法，可用于初始化状态。
     */
    default void onStart() {}
}
```

### 2. AIService 接口扩展

**文件**: `ai-common/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/service/AIService.java`

在现有接口中添加流式方法：

```java
/**
 * 流式生成 AI 内容
 * <p>
 * 与 {@link #generateContent(Project, AIChatRequest, AIProviderConfig, AIResponseListener)} 类似，
 * 但采用流式方式返回响应，通过监听器实时接收增量内容。
 *
 * @param project  项目对象
 * @param request  AI 聊天请求
 * @param config   可选的 AI 提供者配置
 * @param listener 流式响应监听器，用于接收增量内容和完成事件
 * @throws AIServiceException 当 AI 服务调用过程中发生错误时抛出
 * @since 1.0.0
 */
void generateContentStream(@NotNull Project project,
                          @NotNull AIChatRequest request,
                          @Nullable AIProviderConfig config,
                          @NotNull AIStreamResponseListener listener) throws AIServiceException;
```

### 3. AIServiceProvider 接口扩展

**文件**: `ai-common/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/provider/AIServiceProvider.java`

在现有接口中添加流式方法：

```java
/**
 * 流式生成内容
 * <p>
 * 根据传入的 AI 聊天请求对象流式生成相应的内容，通过监听器实时返回增量内容。
 *
 * @param request  AI 聊天请求对象
 * @param apiKey   API 密钥，可选参数
 * @param listener 流式响应监听器，用于接收增量内容
 * @throws AIServiceException 当生成内容过程中发生错误时抛出
 * @since 1.0.0
 */
void generateContentStream(@NotNull AIChatRequest request,
                          @Nullable String apiKey,
                          @NotNull AIStreamResponseListener listener) throws AIServiceException;
```

### 4. AIConsoleLogger 接口扩展

**文件**: `ai-common/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/AIConsoleLogger.java`

添加流式输出方法：

```java
/**
 * 流式输出内容到控制台
 * <p>
 * 用于实时输出 AI 流式响应的增量内容。每次调用会追加内容，不换行。
 * 适合在流式响应过程中逐块输出内容。
 *
 * @param chunk 增量内容块
 */
void printStream(@NotNull String chunk);

/**
 * 完成流式输出
 * <p>
 * 在流式输出完成后调用，会添加换行并可能触发其他完成操作。
 */
void completeStream();
```

---

## 实现细节

### 1. HTTP 流式请求处理

**文件**: `ai-common/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/provider/AICompatibleProvider.java`

#### 1.1 构建流式请求体

在 `buildRequestBody()` 方法中添加 `stream` 参数支持：

```java
private JsonObject buildRequestBody(AIChatRequest request, boolean stream) {
    JsonObject body = new JsonObject();
    // ... 现有代码 ...
    
    // 添加流式参数
    if (stream) {
        body.addProperty("stream", true);
    }
    
    return body;
}
```

#### 1.2 发送流式请求

新增 `sendStreamRequest()` 方法：

```java
/**
 * 发送流式请求到 AI 服务
 * <p>
 * 处理 SSE 格式的流式响应，逐块解析并回调监听器。
 *
 * @param body     请求体
 * @param apiKey   API 密钥
 * @param listener 流式响应监听器
 * @throws AIServiceException 当请求失败时抛出
 */
private void sendStreamRequest(JsonObject body,
                              @Nullable String apiKey,
                              @NotNull AIStreamResponseListener listener) throws AIServiceException {
    if (requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
        throw new AIServiceException("需要 API 密钥但未进行配置",
                                     AIServiceException.ErrorCode.CONFIGURATION_ERROR);
    }

    String url = config.baseUrl + "/chat/completions";
    String requestBody = body.toString();

    try {
        listener.onStart();
        
        HttpRequests.post(url, "application/json")
            .connect(request -> {
                HttpURLConnection connection = (HttpURLConnection) request.getConnection();
                tuneConnection(connection, apiKey);
                request.write(requestBody);
                
                // 读取流式响应
                return readStreamResponse(connection, listener);
            });
            
    } catch (HttpRequests.HttpStatusException e) {
        AIServiceException.ErrorCode code = switch (e.getStatusCode()) {
            case 401 -> AIServiceException.ErrorCode.INVALID_API_KEY;
            case 429 -> AIServiceException.ErrorCode.RATE_LIMIT;
            case 500, 502, 503, 504 -> AIServiceException.ErrorCode.SERVICE_UNAVAILABLE;
            default -> AIServiceException.ErrorCode.INVALID_RESPONSE;
        };
        listener.onError("HTTP error: " + e.getMessage(), e);
        throw new AIServiceException("HTTP error: " + e.getMessage(), code, e);
    } catch (IOException e) {
        listener.onError("网络错误: " + e.getMessage(), e);
        throw new AIServiceException("网络错误: " + e.getMessage(),
                                     AIServiceException.ErrorCode.NETWORK_ERROR, e);
    }
}
```

#### 1.3 解析 SSE 流式响应

```java
/**
 * 读取并解析 SSE 格式的流式响应
 *
 * @param connection HTTP 连接
 * @param listener   流式响应监听器
 * @return 完整的响应文本
 * @throws IOException 当读取失败时抛出
 */
private String readStreamResponse(HttpURLConnection connection,
                                 @NotNull AIStreamResponseListener listener) throws IOException {
    StringBuilder fullText = new StringBuilder();
    
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        
        String line;
        while ((line = reader.readLine()) != null) {
            // 跳过空行
            if (line.trim().isEmpty()) {
                continue;
            }
            
            // SSE 格式: "data: {...}" 或 "data: [DONE]"
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                
                // 检查是否结束
                if ("[DONE]".equals(data)) {
                    break;
                }
                
                // 解析 JSON 并提取内容
                try {
                    String chunk = parseStreamChunk(data);
                    if (chunk != null && !chunk.isEmpty()) {
                        fullText.append(chunk);
                        listener.onChunk(chunk);
                    }
                } catch (Exception e) {
                    // 解析错误，记录但不中断流
                    LOG.warn("Failed to parse stream chunk: " + data, e);
                }
            }
        }
    }
    
    String result = fullText.toString();
    listener.onComplete(result);
    return result;
}

/**
 * 解析流式响应块，提取增量内容
 *
 * @param jsonData JSON 数据字符串
 * @return 增量内容，如果解析失败返回 null
 */
private String parseStreamChunk(String jsonData) {
    try {
        JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices != null && choices.size() > 0) {
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject delta = choice.getAsJsonObject("delta");
            if (delta != null && delta.has("content")) {
                return delta.get("content").getAsString();
            }
        }
        return null;
    } catch (Exception e) {
        LOG.warn("Failed to parse stream chunk JSON", e);
        return null;
    }
}
```

### 2. AIServiceImpl 实现

**文件**: `ai-common/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/service/AIServiceImpl.java`

```java
@Override
public void generateContentStream(@NotNull Project project,
                                 @NotNull AIChatRequest request,
                                 @Nullable AIProviderConfig config,
                                 @NotNull AIStreamResponseListener listener) throws AIServiceException {
    // 获取配置
    AIProviderConfig effectiveConfig = config != null ? config : getDefaultConfig(project);
    AIServiceProvider provider = AIServiceFactory.createProvider(effectiveConfig);
    
    // 获取 API Key
    String apiKey = AICredentialManager.getInstance().getApiKey(
        effectiveConfig.providerType, effectiveConfig.apiKey);
    
    // 在后台线程执行流式请求
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
            provider.generateContentStream(request, apiKey, listener);
        } catch (AIServiceException e) {
            listener.onError("AI 服务调用失败: " + e.getMessage(), e);
        }
    });
}
```

### 3. AICompatibleProvider 实现

**文件**: `ai-common/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/provider/AICompatibleProvider.java`

```java
@Override
public void generateContentStream(@NotNull AIChatRequest request,
                                 @Nullable String apiKey,
                                 @NotNull AIStreamResponseListener listener) throws AIServiceException {
    JsonObject body = buildRequestBody(request, true); // stream = true
    sendStreamRequest(body, apiKey, listener);
}
```

---

## Console 集成

### 1. AIConsoleLogger 实现扩展

**文件**: `intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/ai/AIConsoleLoggerImpl.java`

```java
private final StringBuilder streamBuffer = new StringBuilder();

@Override
public void printStream(@NotNull String chunk) {
    if (verboseLoggingDisable()) {
        return;
    }
    
    // 累积内容到缓冲区
    streamBuffer.append(chunk);
    
    // 在 EDT 线程中更新 Console
    ApplicationManager.getApplication().invokeLater(() -> {
        JavaDocConsoleView consoleView = JavaDocConsoleView.getInstance(project);
        if (consoleView != null) {
            // 追加内容，不换行
            consoleView.printStream(chunk);
        }
    });
}

@Override
public void completeStream() {
    if (verboseLoggingDisable()) {
        return;
    }
    
    // 在 EDT 线程中完成流式输出
    ApplicationManager.getApplication().invokeLater(() -> {
        JavaDocConsoleView consoleView = JavaDocConsoleView.getInstance(project);
        if (consoleView != null) {
            consoleView.completeStream();
        }
        
        // 清空缓冲区
        streamBuffer.setLength(0);
    });
}
```

### 2. JavaDocConsoleView 扩展

**文件**: `intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/console/JavaDocConsoleView.java`

```java
private final StringBuilder currentStream = new StringBuilder();

/**
 * 流式输出内容（追加，不换行）
 * <p>
 * 用于实时输出 AI 流式响应的增量内容。
 *
 * @param chunk 增量内容块
 */
public void printStream(@NotNull String chunk) {
    if (verboseLoggingDisable()) {
        return;
    }
    
    currentStream.append(chunk);
    printInternal(chunk, ConsoleViewContentType.NORMAL_OUTPUT);
}

/**
 * 完成流式输出
 * <p>
 * 在流式输出完成后调用，添加换行并显示完整内容统计。
 */
public void completeStream() {
    if (verboseLoggingDisable()) {
        return;
    }
    
    String fullText = currentStream.toString();
    currentStream.setLength(0);
    
    // 添加换行
    printInternal("\n", ConsoleViewContentType.NORMAL_OUTPUT);
    
    // 可选：显示统计信息
    if (runtimeSettings.verboseLogging && !fullText.isEmpty()) {
        printWithTimestamp(String.format("流式响应完成，总长度: %d 字符", fullText.length()));
    }
}
```

### 3. 流式响应监听器实现

**文件**: `intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/ai/JavaDocAIStreamResponseListener.java`

```java
package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLoggerProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * IntelliAI JavaDoc 的流式响应监听器实现
 * <p>
 * 将流式响应实时输出到 Console。
 */
public class JavaDocAIStreamResponseListener implements AIStreamResponseListener {
    
    private final Project project;
    private final boolean verboseLogging;
    private final AIConsoleLogger consoleLogger;
    
    public JavaDocAIStreamResponseListener(@NotNull Project project) {
        this.project = project;
        this.verboseLogging = AIProviderSettings.getInstance().runtimeSettings.verboseLogging;
        this.consoleLogger = AIConsoleLoggerProvider.getConsoleLogger(project);
    }
    
    @Override
    public void onStart() {
        if (verboseLogging && consoleLogger != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                consoleLogger.printWithTimestamp("=== 开始流式生成 ===");
            });
        }
    }
    
    @Override
    public void onChunk(@NotNull String chunk) {
        if (verboseLogging && consoleLogger != null && !chunk.isEmpty()) {
            consoleLogger.printStream(chunk);
        }
    }
    
    @Override
    public void onComplete(@NotNull String fullText) {
        if (verboseLogging && consoleLogger != null) {
            consoleLogger.completeStream();
            ApplicationManager.getApplication().invokeLater(() -> {
                consoleLogger.printWithTimestamp("=== 流式生成完成 ===");
            });
        }
    }
    
    @Override
    public void onError(@NotNull String error, @Nullable Throwable exception) {
        if (consoleLogger != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                consoleLogger.printError("流式生成错误: " + error);
                if (exception != null && verboseLogging) {
                    consoleLogger.printError(exception.getMessage());
                }
            });
        }
    }
}
```

---

## 向后兼容性

### 1. 保持现有接口不变

- ✅ `AIService.generateContent()` 方法保持不变
- ✅ `AIServiceProvider.generateContent()` 方法保持不变
- ✅ 现有调用代码无需修改

### 2. 新增流式接口

- ✅ 流式接口作为新方法添加，不影响现有功能
- ✅ 可以选择使用同步或流式接口

### 3. 配置兼容

- ✅ 流式接口使用相同的配置（API Key、模型等）
- ✅ 支持相同的重试和错误处理机制

---

## 使用示例

### 1. 基本使用

```java
// 创建流式响应监听器
AIStreamResponseListener listener = new JavaDocAIStreamResponseListener(project);

// 调用流式接口
AIService aiService = AIService.getInstance();
AIChatRequest request = new AIChatRequest("系统提示词", "用户提示词", 0);

try {
    aiService.generateContentStream(project, request, null, listener);
} catch (AIServiceException e) {
    // 处理错误
}
```

### 2. 自定义监听器

```java
AIStreamResponseListener customListener = new AIStreamResponseListener() {
    private final StringBuilder buffer = new StringBuilder();
    
    @Override
    public void onStart() {
        System.out.println("开始接收流式响应...");
    }
    
    @Override
    public void onChunk(@NotNull String chunk) {
        buffer.append(chunk);
        // 实时更新 UI
        ApplicationManager.getApplication().invokeLater(() -> {
            textArea.append(chunk);
        });
    }
    
    @Override
    public void onComplete(@NotNull String fullText) {
        System.out.println("接收完成，总长度: " + fullText.length());
        // 处理完整内容
        processCompleteResponse(fullText);
    }
    
    @Override
    public void onError(@NotNull String error, @Nullable Throwable exception) {
        System.err.println("错误: " + error);
    }
};
```

### 3. 在 Workflow Explainer 中使用

```java
// intelli-ai-tracer 中使用流式输出
public void explainWorkflow(@NotNull Project project, @NotNull PsiMethod method) {
    AIService aiService = AIService.getInstance();
    AIChatRequest request = buildExplainRequest(method);
    
    // 创建流式监听器，输出到 Console
    AIStreamResponseListener listener = new AIStreamResponseListener() {
        @Override
        public void onChunk(@NotNull String chunk) {
            // 输出到 Console
            AIConsoleLogger logger = AIConsoleLoggerProvider.getConsoleLogger(project);
            if (logger != null) {
                logger.printStream(chunk);
            }
        }
        
        @Override
        public void onComplete(@NotNull String fullText) {
            AIConsoleLogger logger = AIConsoleLoggerProvider.getConsoleLogger(project);
            if (logger != null) {
                logger.completeStream();
            }
        }
    };
    
    try {
        aiService.generateContentStream(project, request, null, listener);
    } catch (AIServiceException e) {
        // 错误处理
    }
}
```

---

## 注意事项

### 1. 线程安全

- ⚠️ **流式响应在后台线程接收**：`onChunk()` 可能在后台线程调用
- ✅ **UI 更新必须在 EDT**：使用 `ApplicationManager.getApplication().invokeLater()` 更新 UI
- ✅ **Console 输出已处理**：`AIConsoleLogger.printStream()` 内部已处理线程安全

### 2. 错误处理

- ⚠️ **网络中断**：流式响应过程中网络中断会触发 `onError()`
- ⚠️ **解析错误**：单个块的解析错误不应中断整个流，应记录并继续
- ✅ **异常传播**：严重错误应通过 `onError()` 通知监听器

### 3. 性能考虑

- ⚠️ **缓冲区管理**：避免在监听器中累积大量数据
- ✅ **及时输出**：收到内容块后立即输出，不要等待累积
- ⚠️ **内存使用**：长时间流式响应可能占用较多内存

### 4. 提供商兼容性

- ⚠️ **SSE 格式差异**：不同提供商的 SSE 格式可能略有差异
- ✅ **统一解析**：在 `parseStreamChunk()` 中处理格式差异
- ⚠️ **不支持流式**：部分提供商可能不支持流式输出，需要降级到同步接口

### 5. 测试建议

- ✅ **单元测试**：测试 SSE 解析逻辑
- ✅ **集成测试**：使用 MockWebServer 模拟流式响应
- ✅ **实际测试**：使用真实 API 测试流式输出

---

## 实施步骤

### 阶段 1：核心接口和实现

1. ✅ 创建 `AIStreamResponseListener` 接口
2. ✅ 在 `AIService` 和 `AIServiceProvider` 中添加流式方法
3. ✅ 在 `AICompatibleProvider` 中实现流式请求处理
4. ✅ 实现 SSE 解析逻辑

### 阶段 2：Console 集成

1. ✅ 扩展 `AIConsoleLogger` 接口
2. ✅ 实现 `AIConsoleLoggerImpl.printStream()`
3. ✅ 扩展 `JavaDocConsoleView` 支持流式输出
4. ✅ 创建 `JavaDocAIStreamResponseListener` 实现

### 阶段 3：测试和优化

1. ✅ 编写单元测试
2. ✅ 编写集成测试
3. ✅ 实际环境测试
4. ✅ 性能优化

### 阶段 4：文档和示例

1. ✅ 更新 API 文档
2. ✅ 添加使用示例
3. ✅ 更新用户手册（如需要）

---

## 总结

本方案提供了完整的流式输出 AI 响应接口实现方案，包括：

- ✅ **接口设计**：清晰的流式响应监听器接口
- ✅ **实现细节**：SSE 解析和流式请求处理
- ✅ **Console 集成**：实时输出到 IDEA Console
- ✅ **向后兼容**：不影响现有功能
- ✅ **线程安全**：正确处理 EDT 线程
- ✅ **错误处理**：完善的错误处理机制

该方案可以显著提升用户体验，特别是在长文本生成场景下。


