package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
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
public final class AIServiceImpl implements AIService {

    private static final AICredentialManager GLOBAL_CREDENTIAL_MANAGER =
        new AICredentialManager("AI Common", "AI_COMMON_");

    /**
     * 扩展点名称：插件 AI 设置提供者
     */
    private static final ExtensionPointName<PluginAISettingsProvider> EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.ai.pluginAISettingsProvider");

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
            performanceMode);

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
     * 通过扩展点查找调用者插件对应的 PluginAISettingsProvider 实现。
     * 通过调用栈识别调用者插件，然后从扩展点中找到对应的实现。
     * 这样可以确保每个插件使用自己的配置，避免多个插件实现同一接口时的冲突。
     */
    @NotNull
    private PluginAISettingsProvider getPluginSettings(@NotNull Project project) {
        // 通过调用栈查找调用者插件的类
        Class<?> callerClass = findCallerPluginClass();
        if (callerClass != null) {
            // 从扩展点中查找所有注册的实现
            for (PluginAISettingsProvider provider : EP_NAME.getExtensionList()) {
                // 检查该实现是否属于调用者插件（通过类加载器判断）
                if (isProviderFromCallerPlugin(provider, callerClass)) {
                    return provider;
                }
            }
        }

        // 如果找不到，返回默认实现（使用全局配置的默认值）
        return new DefaultPluginAISettingsProvider();
    }

    /**
     * 通过调用栈查找调用者插件的类
     * <p>
     * 跳过 AIServiceImpl 和 AIService 相关的类，找到第一个外部调用者。
     *
     * @return 调用者类，如果找不到则返回 null
     */
    @Nullable
    private Class<?> findCallerPluginClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String thisClassName = AIServiceImpl.class.getName();
        String serviceClassName = AIService.class.getName();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            // 跳过当前类、接口类和系统类
            if (!className.equals(thisClassName) &&
                !className.equals(serviceClassName) &&
                !className.startsWith("java.") &&
                !className.startsWith("sun.") &&
                !className.startsWith("com.intellij.")) {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException ignored) {
                    // 继续查找下一个
                }
            }
        }
        return null;
    }

    /**
     * 检查 Provider 是否属于调用者插件
     * <p>
     * 通过比较类加载器来判断 Provider 和调用者是否属于同一个插件。
     *
     * @param provider    Provider 实例
     * @param callerClass 调用者类
     * @return 如果属于同一个插件返回 true，否则返回 false
     */
    private boolean isProviderFromCallerPlugin(@NotNull PluginAISettingsProvider provider,
                                               @NotNull Class<?> callerClass) {
        ClassLoader providerClassLoader = provider.getClass().getClassLoader();
        ClassLoader callerClassLoader = callerClass.getClassLoader();

        // 如果类加载器相同，肯定属于同一个插件
        if (providerClassLoader == callerClassLoader) {
            return true;
        }

        // 如果类加载器不同，检查是否是父子关系（插件类加载器可能有层级关系）
        // 通过检查调用者类是否能被 Provider 的类加载器加载来判断
        try {
            String callerClassName = callerClass.getName();
            Class<?> loadedClass = providerClassLoader.loadClass(callerClassName);
            return loadedClass == callerClass;
        } catch (ClassNotFoundException e) {
            // 如果无法加载，说明不属于同一个插件
            return false;
        }
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

