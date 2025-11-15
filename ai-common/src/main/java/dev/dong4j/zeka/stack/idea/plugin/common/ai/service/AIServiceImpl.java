package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * AI 服务实现
 * <p>
 * 自动从插件配置中读取默认供应商，从全局配置中读取供应商详情。
 */
public final class AIServiceImpl implements AIService {

    private static final AICredentialManager GLOBAL_CREDENTIAL_MANAGER =
        new AICredentialManager("AI Common", "AI_COMMON_");

    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                  @NotNull AIChatRequest request,
                                  @Nullable AIProviderConfig config,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        return generateContentWithConfig(project, request, config, listener);
    }


    private String generateContentWithConfig(@NotNull Project project,
                                             @NotNull AIChatRequest request,
                                             @NotNull AIProviderConfig config,
                                             @Nullable AIResponseListener listener) throws AIServiceException {

        final AIProviderSettings instance = AIProviderSettings.getInstance();
        // 创建服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(
            config,
            instance.modelParameters,
            instance.runtimeSettings,
            null,
            false
                                                                    );

        if (provider == null) {
            throw new AIServiceException("Failed to create AI service provider");
        }

        // 生成内容（传递 listener）
        return provider.generateContent(request, GLOBAL_CREDENTIAL_MANAGER.getApiKey(config.credentialId), listener);
    }

    @Override
    @NotNull
    public AIProviderSettings getGlobalSettings() {
        return AIProviderSettings.getInstance();
    }
    /**
     * 获取服务实例
     */
    public static AIService getInstance() {
        return ApplicationManager.getApplication().getService(AIService.class);
    }

}

