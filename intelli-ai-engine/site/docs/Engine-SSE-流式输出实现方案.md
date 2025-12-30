# Engine SSE 流式输出实现方案

## 目标

- 在 IntelliAI Engine 中支持 SSE（Server-Sent Events）流式输出
- 保持现有同步接口不变，新增流式接口
- 增量输出可写入 Console，并支持 UI 渐进更新

## 设计原则

- 向后兼容：不破坏现有调用链
- 可扩展：不同 Provider 可按需实现/降级
- 线程安全：UI 更新在 EDT
- 可取消：支持在长时间生成中终止请求

## 总体结构

### 1) API 层（对外接口）

新增流式监听器与流式方法：

- `AIStreamResponseListener`
    - `onStart()`
    - `onChunk(String chunk)`
    - `onComplete(String fullText)`
    - `onError(String error, Throwable exception)`

- `AIService.generateContentStream(...)`
- `AIServiceProvider.generateContentStream(...)`

### 2) Provider 层（协议与解析）

在 `AICompatibleProvider` 中实现 SSE：

- 构建请求体时设置 `stream=true`
- 通过 `HttpRequests` 打开响应流，逐行读取
- 识别 `data:` 行，解析 JSON
- 遇到 `data: [DONE]` 结束
- 将增量内容回调给 listener

### 3) Service 层（线程与生命周期）

- 在 pooled thread 执行流式请求
- 捕获异常并触发 `onError`
- 支持取消：传入 `CancellationToken` 或 `AtomicBoolean`，在读循环中检查

### 4) Console 输出（增量打印）

扩展 `AIConsoleLogger`：

- `printStream(String chunk)`：追加输出，不换行
- `completeStream()`：换行并收口

ConsoleView 内部维护一个缓冲用于统计/收口。

### 5) 兼容与降级

- 若 Provider 不支持 SSE：
    - 退化为同步调用
    - 用 `onChunk` 一次性输出完整内容
    - 保证调用方逻辑一致

## 关键细节

- SSE 解析：只处理 `data:` 行，空行忽略
- JSON 解析：OpenAI 兼容通常为 `choices[0].delta.content`
- 解析失败：记录日志但不中断流
- UI 更新：`invokeLater` 切回 EDT
- 性能：避免在 listener 中无限累积，必要时可配置是否回传 fullText

## 简化落地步骤

1. 定义 `AIStreamResponseListener` 与流式方法接口
2. `AICompatibleProvider` 增加 SSE 读取与解析
3. `AIServiceImpl` 增加流式调度与错误处理
4. Console 支持 `printStream` / `completeStream`
5. 选一个子插件做试点验证

## 具体类与方法清单

### 1) 新增接口与契约

文件建议路径：

- `intelli-ai-engine/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/AIStreamResponseListener.java`

新增接口：

```java
public interface AIStreamResponseListener {
    default void onStart() {}
    default void onChunk(@NotNull String chunk) {}
    default void onComplete(@NotNull String fullText) {}
    default void onError(@NotNull String error, @Nullable Throwable exception) {}
}
```

### 2) 扩展 AIService 接口

文件：

- `intelli-ai-engine/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/service/AIService.java`

新增方法：

```java
void generateContentStream(@NotNull Project project,
                           @NotNull AIChatRequest request,
                           @Nullable AIProviderConfig config,
                           @NotNull AIStreamResponseListener listener) throws AIServiceException;
```

### 3) 扩展 AIServiceProvider 接口

文件：

- `intelli-ai-engine/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/provider/AIServiceProvider.java`

新增方法：

```java
void generateContentStream(@NotNull AIChatRequest request,
                           @Nullable String apiKey,
                           @NotNull AIStreamResponseListener listener) throws AIServiceException;
```

### 4) 实现 AIServiceImpl 流式调度

文件：

- `intelli-ai-engine/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/service/AIServiceImpl.java`

新增实现（伪代码）：

```java
@Override
public void generateContentStream(@NotNull Project project,
                                  @NotNull AIChatRequest request,
                                  @Nullable AIProviderConfig config,
                                  @NotNull AIStreamResponseListener listener) throws AIServiceException {
    AIProviderConfig effective = config != null ? config : getDefaultConfig(project);
    AIServiceProvider provider = AIServiceFactory.createProvider(effective);
    String apiKey = AICredentialManager.getInstance().getApiKey(effective.providerType, effective.apiKey);

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
            provider.generateContentStream(request, apiKey, listener);
        } catch (AIServiceException e) {
            listener.onError("AI 服务调用失败: " + e.getMessage(), e);
        }
    });
}
```

### 5) Provider 层：SSE 读取与解析

文件：

- `intelli-ai-engine/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/provider/AICompatibleProvider.java`

改造点与新增方法：

1. 请求体支持 `stream=true`

```java
protected JsonObject buildRequestBody(AIChatRequest request, boolean stream) {
    JsonObject body = buildRequestBody(request);
    body.addProperty("stream", stream);
    return body;
}
```

2. 新增流式入口

```java
@Override
public void generateContentStream(@NotNull AIChatRequest request,
                                  @Nullable String apiKey,
                                  @NotNull AIStreamResponseListener listener) throws AIServiceException {
    JsonObject body = buildRequestBody(request, true);
    sendStreamRequest(body, apiKey, listener);
}
```

3. 发送与读取 SSE

```java
private void sendStreamRequest(JsonObject body,
                               @Nullable String apiKey,
                               @NotNull AIStreamResponseListener listener) throws AIServiceException {
    String url = config.baseUrl + "/chat/completions";
    String requestBody = body.toString();
    listener.onStart();

    try {
        HttpRequests.post(url, "application/json")
            .connect(request -> {
                HttpURLConnection connection = (HttpURLConnection) request.getConnection();
                tuneConnection(connection, apiKey);
                request.write(requestBody);
                readStreamResponse(connection, listener);
                return null;
            });
    } catch (Exception e) {
        listener.onError("流式请求失败: " + e.getMessage(), e);
        throw new AIServiceException("流式请求失败", AIServiceException.ErrorCode.NETWORK_ERROR, e);
    }
}
```

4. SSE 解析

```java
private void readStreamResponse(HttpURLConnection connection,
                                @NotNull AIStreamResponseListener listener) throws IOException {
    StringBuilder fullText = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                String chunk = parseStreamChunk(data);
                if (chunk != null && !chunk.isEmpty()) {
                    fullText.append(chunk);
                    listener.onChunk(chunk);
                }
            }
        }
    }
    listener.onComplete(fullText.toString());
}
```

5. 增量内容解析

```java
private String parseStreamChunk(String jsonData) {
    JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
    JsonArray choices = json.getAsJsonArray("choices");
    if (choices == null || choices.isEmpty()) {
        return null;
    }
    JsonObject choice = choices.get(0).getAsJsonObject();
    JsonObject delta = choice.getAsJsonObject("delta");
    if (delta != null && delta.has("content")) {
        return delta.get("content").getAsString();
    }
    return null;
}
```

### 6) Console 增量输出扩展

文件：

- `intelli-ai-engine/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/AIConsoleLogger.java`

新增方法：

```java
void printStream(@NotNull String chunk);
void completeStream();
```

说明：

- `printStream` 追加输出，不换行
- `completeStream` 用于收口和换行

### 7) 调用方示例（子插件侧）

调用方只需构建 listener 并调用 `generateContentStream`。若希望输出到 Console，可在 `onChunk` 中调用 `AIConsoleLogger.printStream`，在
`onComplete` 中调用 `completeStream`。

## 备注

- SSE 解析应对不同 Provider 的格式差异保持容错
- 取消能力建议在读循环中频繁检查
- 若需要统一 UI 体验，可在 Engine 提供默认监听器实现
