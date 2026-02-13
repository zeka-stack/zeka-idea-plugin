package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsSettings;

/**
 * AI 服务实现类
 * <p>
 * 提供 AI 内容生成服务的具体实现, 包括 AI 内容生成, 配置管理, 日志记录等功能.
 * 该类实现了 AIService 接口, 通过 AI 服务提供商工厂创建具体的 AI 服务提供商,
 * 并使用全局凭证管理器管理 AI 服务的认证信息.
 * 使用 Engine 的统一控制台进行日志记录.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class AIServiceImpl implements AIService {

    /**
     * 全局凭证管理器实例
     * <p>
     * 用于管理所有与 "IntelliAI Engine" 相关的凭证信息, 使用前缀 "AI_COMMON_" 进行标识
     *
     * @see AICredentialManager
     */
    private static final AICredentialManager GLOBAL_CREDENTIAL_MANAGER =
        new AICredentialManager(EngineContents.PLUGIN_NAME, "AI_COMMON_");

    /**
     * 生成内容
     * <p>
     * 根据提供的项目, 请求, 配置和可选的监听器生成内容.
     *
     * @param project  项目对象
     * @param request  AI 聊天请求
     * @param config   AI 提供者配置
     * @param listener 可选的 AI 响应监听器, 用于处理生成过程中的响应事件
     * @return 生成的内容字符串
     * @throws AIServiceException 当 AI 服务调用过程中发生错误时抛出
     * @since 1.0
     */
    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                  @NotNull AIChatRequest request,
                                  @NotNull AIProviderConfig config,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        return generateContentWithConfig(project, request, config, listener);
    }

    /**
     * 流式生成内容
     *
     * @param project  项目对象
     * @param request  AI 聊天请求
     * @param config   AI 提供者配置
     * @param listener AI 流式响应监听器
     */
    @Override
    public void generateContentStream(@NotNull Project project,
                                      @NotNull AIChatRequest request,
                                      @NotNull AIProviderConfig config,
                                      @NotNull AIStreamResponseListener listener) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                AIServiceProvider provider = AIServiceFactory.createProvider(project, config);
                String apiKey = resolveApiKey(config);
                listener.onStart();
                provider.generateContentStream(request, apiKey, listener);
            } catch (Throwable e) {
                listener.onError("AI 服务调用失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 使用指定配置生成 AI 内容
     * <p>
     * 根据提供的项目, 请求,AI 服务配置和可选的监听器, 创建 AI 服务提供者并生成内容.
     *
     * @param project  项目对象, 用于获取相关上下文信息
     * @param request  AI 请求内容
     * @param config   AI 服务提供者的配置信息
     * @param listener 可选的 AI 响应监听器, 用于处理生成过程中的事件
     * @return 生成的 AI 内容结果
     * @throws AIServiceException 当 AI 服务调用过程中发生错误时抛出
     */
    private String generateContentWithConfig(@NotNull Project project,
                                             @NotNull AIChatRequest request,
                                             @NotNull AIProviderConfig config,
                                             @Nullable AIResponseListener listener) throws AIServiceException {
        // 创建服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(project, config);
        // 生成内容（传递 listener）
        return provider.generateContent(request, resolveApiKey(config), listener);
    }

    /**
     * 获取全局的 AI 提供者设置
     * <p>
     * 返回当前应用中全局使用的 AI 提供者配置实例.
     *
     * @return 全局 AI 提供者设置的实例
     * @throws IllegalStateException 如果无法获取设置实例时抛出
     * @since 1.0.0
     */
    @Override
    @NotNull
    public AIProviderSettings getGlobalSettings() {
        return AIProviderSettings.getInstance();
    }

    /**
     * 获取 AIService 的单例实例
     * <p>
     * 通过 ApplicationManager 获取当前应用上下文中 AIService 的实例.
     *
     * @return AIService 的实例
     * @since 1.0
     */
    public static AIService getInstance() {
        return ApplicationManager.getApplication().getService(AIService.class);
    }

    /**
     * 解析并获取 API 密钥
     * <p> 根据 AI 提供者配置解析对应的 API 密钥.
     * 如果提供者类型为 FREEAI, 会检查统计功能是否已开启, 仅在开启时返回密钥.
     *
     * @param config AI 提供者配置信息
     * @return 解析后的 API 密钥字符串
     * @throws AIServiceException 当提供者类型为 FREEAI 但未开启统计功能时抛出
     */
    private static String resolveApiKey(@NotNull AIProviderConfig config) throws AIServiceException {
        if (config.providerType == AIProviderType.FREEAI) {
            if (!StatisticsSettings.getInstance().isEnableStatistics()) {
                throw new AIServiceException("FREEAI 仅对开启统计功能的设备开放",
                                             AIServiceException.ErrorCode.CONFIGURATION_ERROR);
            }
            return GLOBAL_CREDENTIAL_MANAGER.getApiKey(config.credentialId);
        }
        return GLOBAL_CREDENTIAL_MANAGER.getApiKey(config.credentialId);
    }

}
