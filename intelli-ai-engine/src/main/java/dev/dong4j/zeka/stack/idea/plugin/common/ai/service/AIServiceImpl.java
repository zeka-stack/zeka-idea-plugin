package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLoggerProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
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
        new AICredentialManager("IntelliAI Engine", "AI_COMMON_");

    /**
     * AI 控制台日志提供者扩展点名称
     */
    private static final ExtensionPointName<AIConsoleLoggerProvider> CONSOLE_LOGGER_PROVIDER_EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.ai.aiConsoleLoggerProvider");

    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                  @NotNull AIChatRequest request,
                                  @NotNull AIProviderConfig config,
                                  @Nullable AIResponseListener listener) throws AIServiceException {
        return generateContentWithConfig(project, request, config, listener);
    }


    private String generateContentWithConfig(@NotNull Project project,
                                             @NotNull AIChatRequest request,
                                             @NotNull AIProviderConfig config,
                                             @Nullable AIResponseListener listener) throws AIServiceException {

        // 从扩展点获取控制台日志记录器
        AIConsoleLogger consoleLogger = getConsoleLogger(project);
        
        // 创建服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(config, consoleLogger);

        if (provider == null) {
            throw new AIServiceException("Failed to create AI service provider");
        }

        // 生成内容（传递 listener）
        return provider.generateContent(request, GLOBAL_CREDENTIAL_MANAGER.getApiKey(config.credentialId), listener);
    }

    /**
     * 从扩展点获取控制台日志记录器
     * <p>
     * 查找所有注册的 AIConsoleLoggerProvider 扩展点实现，返回第一个非空的日志记录器。
     *
     * @param project 项目对象
     * @return 控制台日志记录器，如果没有找到则返回 null
     */
    @Nullable
    private AIConsoleLogger getConsoleLogger(@NotNull Project project) {
        for (AIConsoleLoggerProvider provider : CONSOLE_LOGGER_PROVIDER_EP_NAME.getExtensionList()) {
            AIConsoleLogger logger = provider.getConsoleLogger(project);
            if (logger != null) {
                return logger;
            }
        }
        return null;
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

