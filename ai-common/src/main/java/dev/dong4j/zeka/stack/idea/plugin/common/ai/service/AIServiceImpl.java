package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * AI 服务实现
 * <p>
 * 自动从插件配置中读取默认供应商，从全局配置中读取供应商详情。
 */
@Service(Service.Level.APP)
public final class AIServiceImpl implements AIService {

    private static final AICredentialManager GLOBAL_CREDENTIAL_MANAGER =
        new AICredentialManager("AI Common", "AI_COMMON_");

    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                  @NotNull String systemPrompt,
                                  @NotNull String userPrompt,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);
        return generateContent(project, request, null, listener);
    }

    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                  @NotNull AIChatRequest request,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        return generateContent(project, request, null, listener);
    }

    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                  @NotNull AIProviderType providerType,
                                  @NotNull AIChatRequest request,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        // 使用指定的供应商类型，从全局配置中读取详情
        return generateContentWithProvider(project, providerType, request, listener);
    }

    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                  @NotNull AIChatRequest request,
                                  @Nullable AIServiceConfig config,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        if (config != null) {
            // 使用临时配置
            return generateContentWithConfig(project, request, config, listener);
        } else {
            // 从插件配置中读取默认供应商
            PluginAISettingsProvider pluginSettings = getPluginSettings(project);
            return generateContentWithProvider(project, pluginSettings.getDefaultProviderType(), request, listener);
        }
    }

    /**
     * 使用指定供应商生成内容
     */
    private String generateContentWithProvider(@NotNull Project project,
                                               @NotNull AIProviderType providerType,
                                               @NotNull AIChatRequest request,
                                               @Nullable AIResponseListener listener) throws AIServiceException {
        // 从全局配置中获取供应商详情
        AIProviderSettings globalSettings = getGlobalSettings();
        AIProviderConfig providerConfig = globalSettings.getDefaultProviderConfig(providerType);

        // 从插件配置中获取模型参数和运行时设置（可选，如果没有则使用全局默认值）
        PluginAISettingsProvider pluginSettings = getPluginSettings(project);
        AIModelParameters modelParams = pluginSettings.getModelParameters();
        AIRuntimeSettings runtimeSettings = pluginSettings.getRuntimeSettings();
        boolean performanceMode = pluginSettings.isPerformanceMode();

        // 从全局凭证管理器获取 API Key
        String apiKey = GLOBAL_CREDENTIAL_MANAGER.getApiKey(providerConfig.credentialId);
        if (apiKey == null) {
            throw new AIServiceException(
                "API Key not configured for provider: " + providerType.getDisplayName() +
                ". Please configure it in Settings → Tools → AI Common");
        }

        // 创建服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(
            providerConfig,
            modelParams,
            runtimeSettings,
            null, // 外部插件可以传入自己的日志器
            performanceMode
                                                                    );

        if (provider == null) {
            throw new AIServiceException("Failed to create AI service provider");
        }

        // 生成内容（传递 listener）
        return provider.generateContent(request, apiKey, listener);
    }

    /**
     * 使用临时配置生成内容
     */
    private String generateContentWithConfig(@NotNull Project project,
                                             @NotNull AIChatRequest request,
                                             @NotNull AIServiceConfig config,
                                             @Nullable AIResponseListener listener) throws AIServiceException {
        // 使用临时配置
        AIProviderConfig providerConfig = convertToProviderConfig(config);
        AIModelParameters modelParams = convertToModelParameters(config);
        AIRuntimeSettings runtimeSettings = convertToRuntimeSettings(config);

        // 创建服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(
            providerConfig,
            modelParams,
            runtimeSettings,
            null,
            false
                                                                    );

        if (provider == null) {
            throw new AIServiceException("Failed to create AI service provider");
        }

        // 生成内容（传递 listener）
        return provider.generateContent(request, config.apiKey, listener);
    }

    /**
     * 获取插件配置
     * <p>
     * 通过 Project 的 ServiceManager 查找实现了 PluginAISettingsProvider 接口的服务。
     * 如果找不到，则使用全局默认配置。
     */
    @NotNull
    private PluginAISettingsProvider getPluginSettings(@NotNull Project project) {
        // 方式1：通过 ServiceManager 查找（推荐）
        PluginAISettingsProvider provider = project.getService(PluginAISettingsProvider.class);
        if (provider != null) {
            return provider;
        }

        // 方式2：如果找不到，返回默认实现（使用全局配置的默认值）
        return new DefaultPluginAISettingsProvider();
    }

    /**
     * 默认插件设置提供者（使用全局默认值）
     */
    private static class DefaultPluginAISettingsProvider implements PluginAISettingsProvider {
        @Override
        @NotNull
        public AIProviderType getDefaultProviderType() {
            return AIProviderType.QIANWEN;
        }

        @Override
        @NotNull
        public AIModelParameters getModelParameters() {
            return AIProviderSettings.getInstance().modelParameters;
        }

        @Override
        @NotNull
        public AIRuntimeSettings getRuntimeSettings() {
            return AIProviderSettings.getInstance().runtimeSettings;
        }

        @Override
        public boolean isPerformanceMode() {
            return AIProviderSettings.getInstance().performanceMode;
        }
    }

    @Override
    @NotNull
    public AIProviderSettings getGlobalSettings() {
        return AIProviderSettings.getInstance();
    }

    @Override
    @NotNull
    public ValidationResult validatePluginConfiguration(@NotNull Project project) {
        PluginAISettingsProvider pluginSettings = getPluginSettings(project);
        AIProviderSettings globalSettings = getGlobalSettings();
        AIProviderConfig config = globalSettings.getDefaultProviderConfig(pluginSettings.getDefaultProviderType());
        String apiKey = GLOBAL_CREDENTIAL_MANAGER.getApiKey(config.credentialId);

        if (apiKey == null) {
            return ValidationResult.failure("API Key not configured for provider: " + pluginSettings.getDefaultProviderType().getDisplayName());
        }

        AIServiceProvider provider = AIServiceFactory.createProvider(
            config,
            pluginSettings.getModelParameters(),
            pluginSettings.getRuntimeSettings(),
            null,
            false
                                                                    );

        if (provider == null) {
            return ValidationResult.failure("Failed to create provider");
        }

        return provider.validateConfiguration(apiKey);
    }

    /**
     * 获取服务实例
     */
    public static AIService getInstance() {
        return ApplicationManager.getApplication().getService(AIService.class);
    }

    // 内部转换方法（仅在需要临时配置时使用）
    private AIProviderConfig convertToProviderConfig(AIServiceConfig config) {
        AIProviderConfig providerConfig = new AIProviderConfig(config.providerType);
        providerConfig.modelName = config.modelName;
        if (config.baseUrl != null) {
            providerConfig.baseUrl = config.baseUrl;
        }
        return providerConfig;
    }

    private AIModelParameters convertToModelParameters(AIServiceConfig config) {
        AIModelParameters params = new AIModelParameters();
        params.temperature = config.temperature;
        params.maxTokens = config.maxTokens;
        return params;
    }

    private AIRuntimeSettings convertToRuntimeSettings(AIServiceConfig config) {
        AIRuntimeSettings settings = new AIRuntimeSettings();
        settings.maxRetries = config.maxRetries;
        settings.timeout = config.timeout;
        return settings;
    }
}

