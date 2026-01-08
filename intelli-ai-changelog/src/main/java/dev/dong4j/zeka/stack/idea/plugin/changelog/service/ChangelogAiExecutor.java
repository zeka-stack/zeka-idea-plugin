package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import dev.dong4j.zeka.stack.idea.plugin.changelog.ai.ChangelogAIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ai.ChangelogAIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ResponseLanguage;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * AI 变更日志执行器类
 * <p> 用于通过 AI 服务生成项目变更日志和提交信息, 支持同步与流式响应模式.
 * <p> 该类封装了与 AI 服务交互的完整流程, 包括系统提示词构建, 请求参数配置, 流式响应监听, 日志记录与错误处理.
 * <p> 支持根据用户输入生成变更日志或提交信息, 并可选择是否启用详细日志输出.
 * <p> 内部使用工具方法处理提示词占位符替换, 文本截断, 请求日志打印等辅助功能.
 * <p> 主要使用场景: 集成 AI 能力到版本控制或发布流程中, 自动生成符合语境的变更说明.
 * <p> 使用示例:
 * <pre>{@code
 * ChangelogAiExecutor executor = new ChangelogAiExecutor(project, 1000);
 * String changelog = executor.callChangelog("请生成最近一次提交的变更日志");
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.07
 * @since 1.0.0
 */
final class ChangelogAiExecutor {

    /** 当前项目上下文, 用于标识和操作当前 IDE 中的项目 */
    private final Project project;
    /** 日志中记录的提示信息最大长度, 用于防止日志内容过长 */
    private final int promptLogMaxLength;

    /**
     * 初始化 Changelog AI 执行器
     * <p> 创建 Changelog AI 执行器实例, 用于处理与 AI 服务交互以生成变更日志和提交信息
     *
     * @param project            当前项目实例, 用于日志记录和上下文管理, 不能为 null
     * @param promptLogMaxLength 最大日志长度, 超过该长度的提示将被截断, 不能为负数
     */
    ChangelogAiExecutor(@NotNull Project project, int promptLogMaxLength) {
        this.project = project;
        this.promptLogMaxLength = promptLogMaxLength;
    }

    /**
     * 调用 AI 生成变更日志内容
     * <p> 根据用户提供的提示信息, 调用 AI 服务生成变更日志内容. 支持流式和非流式两种模式, 根据配置的 verboseLogging 参数决定.
     *
     * @param userPrompt 用户输入的提示信息, 不能为空
     * @return 生成的变更日志内容字符串
     * @throws Exception 当 AI 服务返回空结果或发生异常时抛出
     */
    @NotNull
    String callChangelog(@NotNull String userPrompt) throws Exception {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig config = settings.providerConfig;

        String systemPrompt = settings.systemPrompt;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = SettingsState.getDefaultChangelogSystemPrompt();
        }

        systemPrompt = replaceLanguagePlaceholder(systemPrompt);
        userPrompt = replaceLanguagePlaceholder(userPrompt);

        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);
        boolean verboseLogging = AIProviderSettings.getInstance().verboseLogging;
        AIService aiService = AIServiceImpl.getInstance();

        try {
            String result;
            if (verboseLogging) {
                logChangelogRequest("stream", config, request);
                result = callAIServiceStream(aiService, request, config);
            } else {
                logChangelogRequest("single", config, request);
                AIResponseListener listener = new ChangelogAIResponseListener(project);
                result = aiService.generateContent(project, request, config, listener);
            }

            if (result.trim().isEmpty()) {
                throw new Exception(ChangelogBundle.message("error.ai.service.empty.result"));
            }

            return result;
        } catch (AIServiceException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                throw new Exception(errorMessage);
            } else {
                throw new Exception("未知错误");
            }
        }
    }

    /**
     * 以流式方式调用 AI 服务生成变更日志
     * <p>使用指定的用户提示构建请求, 并通过流式响应监听器处理 AI 返回结果
     *
     * @param userPrompt 用户输入的提示信息, 不能为 null 或空字符串
     * @param listener   流式响应监听器, 用于接收 AI 服务返回的增量内容, 不能为 null
     * @return AI 服务返回的完整结果字符串
     * @throws Exception 如果调用过程中发生错误 (如网络异常,AI 服务返回为空等) 则抛出异常
     */
    @NotNull
    String callChangelogStream(@NotNull String userPrompt,
                               @NotNull AIStreamResponseListener listener) throws Exception {
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        AIChatRequest request = buildChangelogRequest(userPrompt);
        AIService aiService = AIServiceImpl.getInstance();
        return callAIServiceStreamWithListener(aiService, request, config, listener);
    }

    /**
     * 使用 AI 服务生成提交信息
     * <p> 根据用户提供的提示生成符合规范的 Git 提交信息, 支持调用 AI 服务进行内容生成
     * <p> 使用示例:
     * <pre>{@code
     * String commitMessage = callCommitMessage("修复了用户登录失败的问题");
     * }</pre>
     *
     * @param userPrompt 用户输入的提示内容, 用于生成提交信息, 不能为空
     * @return 生成的提交信息文本, 如果生成失败则返回空字符串
     * @throws Exception          当 AI 服务调用失败或返回空结果时抛出异常
     * @throws AIServiceException 当底层 AI 服务发生错误时抛出
     */
    @NotNull
    String callCommitMessage(@NotNull String userPrompt) throws Exception {
        final AIChatRequest request = buildCommitMessageRequest(userPrompt);
        AIService aiService = AIServiceImpl.getInstance();

        try {
            AIProviderConfig config = SettingsState.getInstance().providerConfig;
            String result = callAIServiceStream(aiService, request, config);

            if (result.trim().isEmpty()) {
                throw new Exception(ChangelogBundle.message("error.ai.service.empty.result"));
            }

            return result.trim();
        } catch (AIServiceException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                throw new Exception(ChangelogBundle.message("error.ai.service.failed", errorMessage));
            } else {
                throw new Exception(ChangelogBundle.message("error.ai.service.failed",
                                                            ChangelogBundle.message("error.ai.service.unknown")));
            }
        }
    }

    /**
     * 使用流式响应方式调用 AI 服务生成提交信息
     * <p> 通过指定的用户提示和流式响应监听器, 异步获取 AI 生成的提交信息内容
     * <p> 该方法会将用户提示和系统提示封装为 AIChatRequest, 并通过 AIService 生成流式响应
     * <p> 使用示例:
     * <pre>{@code
     * AIStreamResponseListener listener = new AIStreamResponseListener() {
     *     @Override
     *     public void onStart() {
     *         // 处理开始事件
     *     }
     *
     *     @Override
     *     public void onChunk(@NotNull String chunk) {
     *         // 处理每个数据块
     *     }
     *
     *     @Override
     *     public void onComplete(@NotNull String fullText) {
     *         // 处理完整响应
     *     }
     *
     *     @Override
     *     public void onError(@NotNull String error, @Nullable Throwable exception) {
     *         // 处理错误
     *     }
     * };
     *
     * String result = callCommitMessageStream("请生成一个提交信息", listener);
     * }</pre>
     *
     * @param userPrompt 用户输入的提示内容, 不能为 null
     * @param listener   流式响应监听器, 用于接收 AI 生成的响应数据, 不能为 null
     * @return AI 生成的提交信息内容
     * @throws Exception 当 AI 服务调用失败时抛出异常, 可能包含具体的错误信息
     */
    @NotNull
    String callCommitMessageStream(@NotNull String userPrompt,
                                   @NotNull AIStreamResponseListener listener) throws Exception {
        final AIChatRequest request = buildCommitMessageRequest(userPrompt);
        AIService aiService = AIServiceImpl.getInstance();
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        return callAIServiceStreamWithListener(aiService, request, config, listener);
    }

    /**
     * 构建用于生成变更日志的 AI 对话请求
     * <p> 根据用户提示和系统配置构建 AIChatRequest 实例, 用于调用 AI 服务生成变更日志内容.
     *
     * @param userPrompt 用户提供的输入提示, 用于指导生成变更日志的内容
     * @return 构建完成的 AIChatRequest 实例, 包含系统提示和用户提示
     */
    @NotNull
    private AIChatRequest buildChangelogRequest(@NotNull String userPrompt) {
        SettingsState settings = SettingsState.getInstance();
        return buildRequest(userPrompt,
                            settings.systemPrompt,
                            SettingsState.getDefaultChangelogSystemPrompt());
    }

    /**
     * 构建用于生成提交信息的 AI 对话请求对象
     * <p> 根据当前设置获取系统提示语并构建包含用户提示和系统提示的 AIChatRequest 对象
     *
     * @param userPrompt 用户提供的提示内容, 不能为空
     * @return 包含用户提示和系统提示的 AIChatRequest 对象
     */
    @NotNull
    private static AIChatRequest buildCommitMessageRequest(@NotNull String userPrompt) {
        SettingsState settings = SettingsState.getInstance();
        return buildRequest(userPrompt,
                            settings.commitMessageSystemPrompt,
                            SettingsState.getDefaultCommitMessageSystemPrompt());
    }

    /**
     * 构建 AI 对话请求
     * <p> 根据用户提示和系统提示构建 AI 对话请求对象, 支持语言占位符替换
     * <p> 如果系统提示为空或为空字符串, 则使用默认系统提示
     *
     * @param userPrompt          用户提示, 不能为 null
     * @param systemPrompt        系统提示, 可以为 null 或空字符串
     * @param defaultSystemPrompt 默认系统提示, 不能为 null
     * @return AI 对话请求对象
     */
    @NotNull
    private static AIChatRequest buildRequest(@NotNull String userPrompt,
                                              @Nullable String systemPrompt,
                                              @NotNull String defaultSystemPrompt) {
        String resolvedSystemPrompt = (systemPrompt == null || systemPrompt.trim().isEmpty())
                                      ? defaultSystemPrompt
                                      : systemPrompt;

        resolvedSystemPrompt = replaceLanguagePlaceholder(resolvedSystemPrompt);
        userPrompt = replaceLanguagePlaceholder(userPrompt);

        return new AIChatRequest(resolvedSystemPrompt, userPrompt);
    }

    /**
     * 调用 AI 服务的流式处理方法
     * <p> 此方法使用给定的 AI 服务, 请求和配置来调用 AI 服务的流式接口, 并返回处理结果.
     * <p> 内部会创建一个 {@link ChangelogAIStreamResponseListener} 来监听流式响应.
     *
     * @param aiService AI 服务实例, 不能为 null
     * @param request   AI 请求对象, 不能为 null
     * @param config    AI 提供者配置, 不能为 null
     * @return 处理后的完整文本结果
     * @throws Exception 如果 AI 服务调用失败或发生其他错误
     */
    @NotNull
    private String callAIServiceStream(@NotNull AIService aiService,
                                       @NotNull AIChatRequest request,
                                       @NotNull AIProviderConfig config) throws Exception {
        AIStreamResponseListener streamListener =
            new ChangelogAIStreamResponseListener(project, new StringBuilder(),
                                                  new CountDownLatch(1), new AtomicReference<>());
        return callAIServiceStreamWithListener(aiService, request, config, streamListener);
    }

    /**
     * 调用 AI 服务流式接口并使用自定义监听器处理响应
     * <p> 该方法用于通过流式方式调用 AI 服务, 并将响应分块或完整结果传递给外部监听器.
     * 它会创建内部监听器来缓冲数据, 处理错误和同步等待响应完成.
     *
     * @param aiService        AI 服务实例, 用于生成内容
     * @param request          包含系统提示和用户提示的聊天请求对象
     * @param config           AI 提供商的配置信息
     * @param externalListener 外部响应监听器, 用于接收流式响应事件
     * @return AI 服务返回的完整结果字符串
     * @throws Exception 当调用失败, 发生中断或返回结果为空时抛出异常
     */
    @NotNull
    private String callAIServiceStreamWithListener(@NotNull AIService aiService,
                                                   @NotNull AIChatRequest request,
                                                   @NotNull AIProviderConfig config,
                                                   @NotNull AIStreamResponseListener externalListener) throws Exception {
        StringBuilder buffer = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        AtomicReference<String> resultRef = new AtomicReference<>();

        logChangelogRequest("stream", config, request);

        AIStreamResponseListener listener = new AIStreamResponseListener() {
            /**
             * 启动监听器
             * <p> 在监听器启动时调用此方法, 检查当前线程是否中断, 若中断则直接返回,
             * 否则调用外部监听器的 onStart 方法.
             *
             * @since 1.0
             */
            @Override
            public void onStart() {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                externalListener.onStart();
            }

            /**
             * 处理接收到的文本块数据
             * <p> 当接收到一个文本块时, 检查当前线程是否被中断, 若被中断则直接返回. 否则将文本块追加到缓冲区, 并通知外部监听器处理该文本块.
             *
             * @param chunk 接收到的文本块内容, 不能为空
             */
            @Override
            public void onChunk(@NotNull String chunk) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                buffer.append(chunk);
                externalListener.onChunk(chunk);
            }

            /**
             * 处理完整文本的完成事件
             * <p> 当 AI 流式响应完成时调用此方法, 将完整文本设置到结果引用中, 并通知外部监听器
             * <p> 如果当前线程被中断, 则仅触发计数器减一并返回, 不执行其他操作
             *
             * @param fullText 完整的响应文本, 不能为 null
             */
            public void onComplete(@NotNull String fullText) {
                if (Thread.currentThread().isInterrupted()) {
                    latch.countDown();
                    return;
                }
                resultRef.set(fullText);
                externalListener.onComplete(fullText);
                latch.countDown();
            }

            /**
             * 当发生错误时触发的回调方法
             * <p> 将错误信息和异常封装为一个 Exception 对象并存储, 同时通知外部监听器, 并减少计数器
             *
             * @param error     错误信息, 不可为 null
             * @param exception 与错误相关的异常对象, 可能为 null
             */
            @Override
            public void onError(@NotNull String error, @Nullable Throwable exception) {
                errorRef.set(new Exception(error, exception));
                externalListener.onError(error, exception);
                latch.countDown();
            }

            /**
             * 获取流式取消令牌
             * <p> 委托给外部监听器的 cancellationToken 方法, 返回当前流式操作的取消令牌, 可能为 null
             *
             * @return 流式取消令牌, 如果外部监听器未提供则返回 null
             */
            @Override
            public @Nullable StreamCancellationToken cancellationToken() {
                return externalListener.cancellationToken();
            }
        };

        try {
            aiService.generateContentStream(project, request, config, listener);
        } catch (AIServiceException e) {
            throw new Exception(ChangelogBundle.message("error.ai.service.failed", e.getMessage()));
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception(ChangelogBundle.message("error.ai.service.failed", "Interrupted"));
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }

        String result = resultRef.get();
        if (result == null || result.isEmpty()) {
            result = buffer.toString();
        }
        return result;
    }

    /**
     * 记录 Changelog 请求日志
     * <p> 输出 Changelog 请求的详细信息到控制台, 包括请求模式, 配置参数,System Prompt 和 User Prompt
     * <p> 日志内容包含:
     * <ul>
     *   <li> 请求模式 (如 "stream" 或 "single")</li>
     *   <li>AI 提供商配置信息 (提供者类型, 模型名称, 基础 URL 等)</li>
     *   <li> 模型参数 (温度, 最大 token 数,topP,topK, 存在惩罚等)</li>
     *   <li> 截断后的 System Prompt 和 User Prompt 内容 (长度超过限制时会截断并标记)</li>
     * </ul>
     *
     * @param mode    请求模式, 例如 "stream" 或 "single"
     * @param config  AI 提供商配置, 包含模型和运行时设置
     * @param request AI 聊天请求, 包含系统提示和用户提示
     */
    private void logChangelogRequest(@NotNull String mode,
                                     @NotNull AIProviderConfig config,
                                     @NotNull AIChatRequest request) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("Changelog 请求(%s): %s | %s | %s",
                                                             mode,
                                                             config.providerType,
                                                             config.modelName,
                                                             config.baseUrl));
        AIConsoleLoggerUtil.print(project,
                                  String.format("参数: temp=%s, maxTokens=%s, topP=%s, topK=%s, presencePenalty=%s, " +
                                                "timeout=%s, maxRetries=%d",
                                                config.modelParameters.temperature,
                                                config.modelParameters.maxTokens,
                                                config.modelParameters.topP,
                                                config.modelParameters.topK,
                                                config.modelParameters.presencePenalty,
                                                config.runtimeSettings.timeout,
                                                config.runtimeSettings.maxRetries));
        AIConsoleLoggerUtil.print(project,
                                  "System Prompt (" + request.systemPrompt().length() + " chars):\n" +
                                  truncate(request.systemPrompt(), promptLogMaxLength));
        AIConsoleLoggerUtil.print(project,
                                  "User Prompt (" + request.userPrompt().length() + " chars):\n" +
                                  truncate(request.userPrompt(), promptLogMaxLength));
    }

    /**
     * 对字符串进行截断处理, 防止内容过长
     * <p> 如果字符串长度小于等于指定最大长度, 则直接返回原字符串; 否则截取前 maxLength 个字符, 并添加截断提示信息.
     *
     * @param text      需要截断的字符串, 不能为 null
     * @param maxLength 截断的最大长度, 必须大于 0
     * @return 截断后的字符串, 若原字符串过长则包含截断提示
     */
    private String truncate(@NotNull String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...[truncated " + (text.length() - maxLength) + " chars]";
    }

    /**
     * 替换提示语中的语言占位符
     * <p> 将提示语中的 "简体中文" 占位符替换为当前配置的响应语言描述文本
     *
     * @param prompt 带有语言占位符的提示语, 不能为 null
     * @return 替换后的提示语
     */
    @NotNull
    private static String replaceLanguagePlaceholder(@NotNull String prompt) {
        AIProviderSettings providerSettings = AIProviderSettings.getInstance();
        ResponseLanguage responseLanguage = providerSettings != null && providerSettings.responseLanguage != null
                                            ? providerSettings.responseLanguage
                                            : ResponseLanguage.ZH;
        String languageText = responseLanguage.getDescForPrompt();

        return prompt.replace("${language}", languageText);
    }
}
