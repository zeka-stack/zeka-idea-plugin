package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.BlockingRequestExecutor;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.StreamRequestExecutor;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * Azure OpenAI 提供者类
 * <p> 继承自 OpenAILikeProvider, 用于封装与 Azure OpenAI 服务的交互逻辑, 包括内容生成, 流式响应, 配置验证及可用模型获取等功能.
 * 该类专注于基础设施层的请求构建与执行, 不负责请求处理流程, 符合面向对象设计原则, 避免基础设施关注, 封装业务规则.
 * 通过配置验证方法可检查服务连接状态, 支持超时设置与 API 密钥注入, 适用于内部系统集成 Azure OpenAI 服务.
 * 本类不直接暴露给外部调用, 仅作为服务提供者在系统内部使用.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.18
 * @since 1.0.0
 */
public class AzureOpenAIProvider extends OpenAILikeProvider {
    /** 默认 API 版本号, 用于 Azure OpenAI 服务请求 */
    private static final String DEFAULT_API_VERSION = "2024-10-21";

    /**
     * 初始化 Azure OpenAI 提供商实例
     * <p> 用于创建与 Azure OpenAI 服务兼容的 AI 提供商对象, 继承自 OpenAILikeProvider
     *
     * @param project         项目上下文, 用于日志输出和配置管理
     * @param config          提供商配置信息, 包括基础 URL, 模型等
     * @param modelParameters 模型参数, 如温度, 最大长度等
     * @param runtimeSettings 运行时设置, 如超时时间等
     */
    public AzureOpenAIProvider(@NotNull Project project,
                               @NotNull AIProviderConfig config,
                               @NotNull AIModelParameters modelParameters,
                               @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    /**
     * 生成对话内容
     * <p> 通过阻塞式请求执行器发送对话请求, 使用指定的请求体,API 密钥和监听器, 返回服务端响应内容.
     * 该方法适用于需要同步等待响应的场景.
     *
     * @param request  对话请求对象, 包含对话内容, 模型参数等
     * @param apiKey   API 密钥, 可为空, 用于身份验证
     * @param listener 对话响应监听器, 可为空, 用于接收响应回调
     * @return 服务端返回的对话内容字符串
     * @throws AIServiceException 当请求失败或服务端返回错误时抛出
     *
     *                            <pre>{@code
     *                                                       executor.sendRequest(buildRequestBody(request), apiKey, listener, false, url);
     *                                                       }</pre>
     */
    @Override
    @NotNull
    public String generateContent(@NotNull AIChatRequest request,
                                  @Nullable String apiKey,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        BlockingRequestExecutor executor = new BlockingRequestExecutor(project, config, this::tuneConnection);
        String url = buildRequestUrl("/chat/completions");
        return executor.sendRequest(buildRequestBody(request), apiKey, listener, false, url);
    }

    /**
     * 以流式方式生成内容
     * <p> 通过流式请求执行器发送流式请求, 支持实时接收响应数据
     *
     * @param request  AI 聊天请求对象, 包含对话内容和参数
     * @param apiKey   可选的 API 密钥, 用于身份验证
     * @param listener 流式响应监听器, 用于接收逐块返回的响应数据
     * @throws AIServiceException 当服务调用失败时抛出异常
     */
    @Override
    public void generateContentStream(@NotNull AIChatRequest request,
                                      @Nullable String apiKey,
                                      @NotNull AIStreamResponseListener listener) throws AIServiceException {
        StreamRequestExecutor executor = new StreamRequestExecutor(project, config, this::tuneConnection);
        String url = buildRequestUrl("/chat/completions");
        executor.sendStreamRequest(buildRequestBody(request, true), apiKey, listener, url);
    }

    /**
     * 验证当前配置是否有效
     * <p> 通过发送一个简单的 ping 请求到服务端, 检查连接是否正常. 如果响应非空, 则认为配置有效; 否则根据异常类型返回失败结果.
     * <p> 验证过程中会打印日志, 包括提供商名称, 模型名称和基础 URL.
     *
     * @param apiKey API 密钥, 可为空
     * @return 验证结果, 成功时返回包含成功消息的 ValidationResult, 失败时返回包含错误信息的 ValidationResult
     * @since 1.0
     */
    @Override
    @NotNull
    public ValidationResult validateConfiguration(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 开始验证配置 ===");
        AIConsoleLoggerUtil.print(project, "提供商: " + getProviderType().getDisplayName());
        AIConsoleLoggerUtil.print(project, "模型: " + getModelName());
        AIConsoleLoggerUtil.print(project, "Base URL: " + getBaseUrl());
        try {
            AIChatRequest request = new AIChatRequest("i say ping, you say pong", "ping", 0);
            BlockingRequestExecutor executor = new BlockingRequestExecutor(project, config, this::tuneConnection);
            String url = buildRequestUrl("/chat/completions");
            String response = executor.sendRequest(buildRequestBody(request), apiKey, null, true, url);
            if (!response.isEmpty()) {
                AIConsoleLoggerUtil.printSuccess(project, "=== 配置验证成功 ===");
                return ValidationResult.success("连接成功！提供商: " + getProviderType().getDisplayName() +
                                                ", 模型: " + getModelName());
            }
            AIConsoleLoggerUtil.printError(project, "配置验证失败：服务返回空响应");
            return ValidationResult.failure("连接失败：服务返回空响应");
        } catch (AIServiceException e) {
            AIConsoleLoggerUtil.printError(project, "配置验证失败: " + e.getMessage());
            return ValidationResult.failure("配置验证失败", AIServiceException.build(e));
        } catch (Exception e) {
            String details = e.getMessage();
            if (details == null || details.isEmpty()) {
                details = e.getClass().getSimpleName();
            }
            AIConsoleLoggerUtil.printError(project, "配置验证异常: " + details);
            return ValidationResult.failure("配置验证异常", details, e);
        }
    }

    /**
     * 获取当前提供商支持的可用模型列表
     * <p> 从提供商类型中获取其支持的所有模型名称, 并返回为不可为空的字符串列表 </p>
     *
     * @param apiKey 可选的 API 密钥, 用于特定场景下的模型过滤或权限验证, 若为 null 则使用默认配置
     * @return 支持的模型名称列表, 始终返回非空列表, 即使无支持模型时也返回空列表
     */
    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        return new ArrayList<>(getProviderType().getSupportedModels());
    }

    /**
     * 构建请求 URL
     * <p> 根据基础 URL 和路径, 拼接并添加 API 版本参数, 移除原有查询参数部分
     *
     * @param path 请求路径, 不能为空
     * @return 构建后的完整请求 URL, 包含 API 版本参数
     */
    private String buildRequestUrl(@NotNull String path) {
        String baseUrl = config.baseUrl;
        String apiVersion = resolveApiVersion(baseUrl);
        int queryIndex = baseUrl.indexOf('?');
        if (queryIndex >= 0) {
            baseUrl = baseUrl.substring(0, queryIndex);
        }
        return baseUrl + path + "?api-version=" + apiVersion;
    }

    /**
     * 解析基础 URL 中的 API 版本参数
     * <p> 从给定的 URL 中提取并返回 <code>api-version</code> 查询参数的值, 若未找到则返回默认版本 <code>2024-10-21</code>
     *
     * @param baseUrl 基础 URL 字符串, 可能包含查询参数
     * @return API 版本字符串, 若未指定则返回 {@code DEFAULT_API_VERSION}
     */
    private String resolveApiVersion(@NotNull String baseUrl) {
        int queryIndex = baseUrl.indexOf('?');
        if (queryIndex < 0) {
            return DEFAULT_API_VERSION;
        }
        String query = baseUrl.substring(queryIndex + 1);
        for (String part : query.split("&")) {
            if (part.startsWith("api-version=")) {
                String value = part.substring("api-version=".length());
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return DEFAULT_API_VERSION;
    }

    /**
     * 配置 HTTP 连接参数
     * <p> 设置连接超时和读取超时时间, 并可选设置 API 密钥请求头
     *
     * @param connection HTTP 连接对象
     * @param apiKey     可选的 API 密钥, 若不为空且非空字符串则设置为请求头 "api-key"
     * @since 1.0
     */
    private void tuneConnection(HttpURLConnection connection, @Nullable String apiKey) {
        int timeoutMillis = runtimeSettings.getTimeoutInMillis();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis * 2);
        if (apiKey != null && !apiKey.isEmpty()) {
            connection.setRequestProperty("api-key", apiKey);
        }
        AIConsoleLoggerUtil.print(project, String.format("连接超时: [%ss] 读取超时: [%ss]\n", runtimeSettings.timeout,
                                                         (runtimeSettings.timeout * 2)));
    }
}
