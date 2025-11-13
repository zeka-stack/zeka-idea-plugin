// package dev.dong4j.zeka.stack.idea.plugin.ai;
//
// import org.jetbrains.annotations.NotNull;
// import org.jetbrains.annotations.Nullable;
//
// import java.util.ArrayList;
// import java.util.List;
//
// import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
// import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
// import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
// import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
// import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
// import dev.dong4j.zeka.stack.idea.plugin.settings.AISettingsBridge;
// import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
//
// /**
//  * 兼容旧接口的 AI 服务工厂封装。
//  */
// public final class AIServiceFactory {
//
//     private AIServiceFactory() {
//     }
//
//     @Nullable
//     public static AIServiceProvider createProvider(@NotNull SettingsState settings) {
//         AIProviderSettings providerSettings = AISettingsBridge.toCommonSettings(settings);
//         AIProviderConfig defaultConfig = providerSettings.getDefaultProviderConfig(providerSettings.providerType);
//         if (!defaultConfig.configurationVerified) {
//             return null;
//         }
//         return dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory.createProvider(defaultConfig,
//                                                                                            providerSettings.modelParameters,
//                                                                                            providerSettings.runtimeSettings);
//     }
//
//     @Nullable
//     public static AIServiceProvider createProvider(@NotNull SettingsState currentSettings,
//                                                    @NotNull SettingsState.ProviderConfig providerConfig) {
//         AIProviderSettings providerSettings = AISettingsBridge.toCommonSettings(currentSettings);
//         AIProviderConfig commonConfig = AISettingsBridge.toCommonConfig(providerConfig);
//         if (commonConfig.providerType == null) {
//             return null;
//         }
//         // 测试链接等场景允许未验证配置通过
//         commonConfig.configurationVerified = true;
//         return dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory.createProvider(commonConfig,
//                                                                                            providerSettings.modelParameters,
//                                                                                            providerSettings.runtimeSettings);
//     }
//
//     @NotNull
//     public static List<String> getSupportedProviders() {
//         return AIProviderType.getAllProviderIds();
//     }
//
//     public static boolean isProviderSupported(@NotNull String providerId) {
//         return AIProviderType.fromProviderId(providerId) != null;
//     }
//
//     @NotNull
//     public static String getProviderName(@NotNull String providerId) {
//         AIProviderType type = AIProviderType.fromProviderId(providerId);
//         return type != null ? type.getDisplayName() : providerId;
//     }
//
//     @NotNull
//     public static List<AIServiceProvider> getAvailableProviders() {
//         SettingsState settings = SettingsState.getInstance();
//         AIProviderSettings providerSettings = AISettingsBridge.toCommonSettings(settings);
//         AICredentialManager credentialManager = AISettingsBridge.getCredentialManager();
//         List<AIServiceProvider> providers = new ArrayList<>();
//         for (AIProviderConfig config : providerSettings.getVerifiedProviders()) {
//             AIServiceProvider provider = dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory.createProvider(
//                 config,
//                 providerSettings.modelParameters,
//                 providerSettings.runtimeSettings
//             );
//             if (provider != null) {
//                 String apiKey = credentialManager.getApiKey(config.credentialId);
//                 // 需要持有 API Key 的 provider 会在调用时注入，此处只判断可创建
//                 providers.add(provider);
//             }
//         }
//         return providers;
//     }
//
//     public static boolean hasAvailableProvider() {
//         SettingsState settings = SettingsState.getInstance();
//         AIProviderSettings providerSettings = AISettingsBridge.toCommonSettings(settings);
//         AIProviderConfig defaultConfig = providerSettings.getDefaultProviderConfig(providerSettings.providerType);
//         return defaultConfig != null && defaultConfig.configurationVerified;
//     }
//
//     public static int getAvailableProviderCount() {
//         return getAvailableProviders().size();
//     }
// }
//
