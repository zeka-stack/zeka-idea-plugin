# AI 生成集成指南：流式与非流式

本文档说明 IntelliAI Engine 的**流式**与**非流式**调用方式、监听器职责、使用规范，并给出子插件的真实集成示例。

## 核心接口

Engine 对外提供统一入口：

- `dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService`

关键方法：

- 非流式：`generateContent(Project, AIChatRequest, AIProviderConfig, AIResponseListener)`
- 流式：`generateContentStream(Project, AIChatRequest, AIProviderConfig, AIStreamResponseListener)`

## 何时选用

- 非流式：需要一次性结果（如 Javadoc 批量生成、代码注释替换）。
- 流式：需要实时展示进度（如提交信息生成、变更日志输出、ToolWindow 文本流）。

## 监听器职责

### AIResponseListener（非流式）

- 用于处理一次性结果的生命周期事件（开始/完成/错误）。
- 适合在日志、统计或 UI 完成回调中使用。

### AIStreamResponseListener（流式）

- `onStart()`：初始化 UI（清空、重置状态）
- `onChunk(String chunk)`：增量内容到达，立即渲染
- `onComplete(String fullText)`：收口与最终替换（可做二次格式化）

> 注意：流式内容已包含换行，不要重复追加多余换行。

## 集成规范

1. **请求构建统一**：先构造 `AIChatRequest`。
2. **UI 更新在 EDT**：listener 内部如需更新 UI，使用 `ApplicationManager.getApplication().invokeLater`。
3. **内容处理策略**：
    - 流式：`onChunk` 只追加，`onComplete` 可做全量替换/格式化。
    - 非流式：直接使用返回值。
4. **异常处理**：统一捕获 `AIServiceException` 并转换成用户可读提示。

## 集成示例一（流式）

使用 `intelli-ai-changelog` 的提交信息生成为例：

文件：`intelli-ai-changelog/src/main/java/dev/dong4j/zeka/stack/idea/plugin/changelog/git/CommitMessageGenerator.java`

```java
AIStreamResponseListener listener = new AIStreamResponseListener() {
    @Override
    public void onStart() {
        buffer.setLength(0);
        ApplicationManager.getApplication().invokeLater(() -> {
            setCommitMessageText("", commitMessageControl);
        });
    }

    @Override
    public void onChunk(@NotNull String chunk) {
        buffer.append(chunk);
        ApplicationManager.getApplication().invokeLater(() -> {
            setCommitMessageText(buffer.toString(), commitMessageControl);
        });
    }

    @Override
    public void onComplete(@NotNull String fullText) {
        ApplicationManager.getApplication().invokeLater(() -> {
            setCommitMessageText(fullText, commitMessageControl);
        });
    }
};

String commitMessage = service.generateCommitMessageFromDiffStream(changes, listener);
```

要点：

- `onChunk` 实时写入提交面板
- `onComplete` 统一替换最终结果
- 业务最终返回值仍可用于二次处理

## 集成示例二（非流式）

使用 `intelli-ai-javadoc` 的单任务生成流程为例：

文件：`intelli-ai-javadoc/src/main/java/dev/dong4j/zeka/stack/idea/plugin/task/SequentialTaskProcessor.java`

```java
AIChatRequest request = AIRequestComposer.compose(settings, task);
AIResponseListener listener = verboseLogging ? new JavadocAIResponseListener(project) : null;

String documentation = aiService.generateContent(project, request, provider, listener);

if (documentation.trim().isEmpty()) {
    // 处理空结果
}

inserterHelper.insertDocumentation(task, documentation, verboseLogging);
```

要点：

- 直接获取完整文本
- 适用于插入/替换类场景
- listener 用于日志与统计

## 注意事项

- 流式输出不要在 UI 线程执行耗时逻辑（格式化/解析放到 `onComplete` 或后台线程）。
- 非流式任务建议在后台任务执行，避免阻塞 UI。
- 统一遵循 Engine 的“AI 服务商”配置和验证逻辑。

