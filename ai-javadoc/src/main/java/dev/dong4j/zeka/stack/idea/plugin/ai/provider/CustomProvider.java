package dev.dong4j.zeka.stack.idea.plugin.ai.provider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * 自定义服务提供商
 *
 * <p>兼容 OpenAI API 接口的自定义服务提供商。
 * 支持任何提供 OpenAI 兼容 API 的服务，包括：
 * <ul>
 *   <li>OpenAI 官方服务</li>
 *   <li>Azure OpenAI 服务</li>
 *   <li>其他兼容 OpenAI API 的第三方服务</li>
 *   <li>自部署的 OpenAI 兼容服务</li>
 * </ul>
 *
 * <p>使用要求：
 * <ol>
 *   <li>必须提供 Base URL（API 服务地址）</li>
 *   <li>必须提供 API Key（用于身份验证）</li>
 *   <li>必须指定模型名称</li>
 *   <li>服务必须支持 OpenAI 兼容的 /chat/completions 接口</li>
 * </ol>
 *
 * <p>配置步骤：
 * <ol>
 *   <li>在设置中选择 CUSTOM 作为 AI Provider</li>
 *   <li>输入 Base URL（如：<a href="https://api.openai.com/v1">...</a>）</li>
 *   <li>输入 API Key</li>
 *   <li>输入模型名称（如：gpt-3.5-turbo, gpt-4 等）</li>
 *   <li>点击"测试连接"验证配置</li>
 * </ol>
 *
 * <p>支持的常见服务：
 * <ul>
 *   <li>OpenAI: https://api.openai.com/v1</li>
 *   <li>Azure OpenAI: https://your-resource.openai.azure.com/openai/deployments/your-deployment</li>
 *   <li>Anthropic Claude (通过兼容层): https://api.anthropic.com/v1</li>
 *   <li>Google Gemini (通过兼容层): https://generativelanguage.googleapis.com/v1beta</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>确保服务支持 OpenAI 兼容的 API 格式</li>
 *   <li>API Key 格式通常为 "Bearer {key}" 或直接使用 key</li>
 *   <li>模型名称必须与服务提供商支持的模型一致</li>
 *   <li>某些服务可能有速率限制或使用配额</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see AICompatibleProvider
 * @since 1.0.0
 */
public class CustomProvider extends AICompatibleProvider {

    /**
     * 初始化 CustomProvider 实例
     * <p>
     * 使用给定的 SettingsState 对象初始化父类，并完成自身的初始化逻辑
     *
     * @param settings 用于初始化的设置状态对象
     */
    public CustomProvider(SettingsState settings) {
        super(settings);
    }

    /**
     * 获取自定义AI提供者的ID
     * <p>
     * 返回自定义AI提供者对应的唯一标识符
     *
     * @return 自定义AI提供者的ID
     */
    @NotNull
    @Override
    public String getProviderId() {
        return AIProviderType.CUSTOM.getProviderId();
    }

    /**
     * 获取当前AI服务提供商的名称
     * <p>
     * 返回AI服务提供商的显示名称，该名称由AIProviderType.CUSTOM对应的枚举项提供。
     *
     * @return AI服务提供商的显示名称
     */
    @NotNull
    @Override
    public String getProviderName() {
        return AIProviderType.CUSTOM.getDisplayName();
    }

    /**
     * 返回推荐的模型列表
     *
     * <p>返回推荐的常用模型列表，供用户在设置界面中选择。
     * 这些模型经过测试，能够很好地支持文档生成任务。
     *
     * <p>注意：这只是推荐列表，用户可以使用任何服务提供商支持的模型。
     * 建议查看服务提供商文档了解支持的模型列表。
     *
     * @return 推荐的模型列表（作为参考）
     */
    @NotNull
    @Override
    public List<String> getSupportedModels() {
        return AIProviderType.CUSTOM.getSupportedModels();
    }

    /**
     * 获取默认模型
     *
     * <p>返回推荐的默认模型。
     *
     * @return 默认模型名称
     */
    @NotNull
    @Override
    public String getDefaultModel() {
        return AIProviderType.CUSTOM.getDefaultModel();
    }

    /**
     * 获取默认 Base URL
     * <p>
     * 返回 OpenAI 官方 API 地址作为默认值。
     * 用户可以根据实际使用的服务提供商修改此地址。
     * <p>
     * 常见 Base URL 示例：
     * <p>
     * - OpenAI: <a href="https://api.openai.com/v1">...</a>
     * - Azure OpenAI: <a href="https://your-resource.openai.azure.com/openai/deployments/your-deployment">...</a>
     * - 自部署服务: <a href="http://localhost:8000/v1">...</a>
     *
     * @return 默认 Base URL
     */
    @NotNull
    @Override
    public String getDefaultBaseUrl() {
        return AIProviderType.CUSTOM.getDefaultBaseUrl();
    }

    /**
     * 是否需要 API Key
     *
     * <p>自定义服务提供商需要 API Key 进行身份验证。
     * 几乎所有兼容 OpenAI API 的服务都需要 API Key。
     *
     * @return 总是返回 true
     */
    @Override
    public boolean requiresApiKey() {
        return AIProviderType.CUSTOM.requiresApiKey();
    }

    /**
     * 验证配置是否正确
     *
     * <p>对自定义服务提供商进行特殊的配置验证。
     * 检查必要的配置项是否完整，然后调用父类的验证方法。
     *
     * <p>验证要求：
     * <ul>
     *   <li>Base URL 不能为空</li>
     *   <li>API Key 不能为空</li>
     *   <li>模型名称不能为空</li>
     *   <li>服务必须可访问</li>
     * </ul>
     *
     * @return 验证结果
     */
    @NotNull
    @Override
    public ValidationResult validateConfiguration() {
        // 检查必要配置项
        if (settings.baseUrl == null || settings.baseUrl.trim().isEmpty()) {
            return ValidationResult.failure(
                "Base URL 不能为空",
                "请提供自定义服务的 API 地址"
                                           );
        }

        if (settings.apiKey == null || settings.apiKey.trim().isEmpty()) {
            return ValidationResult.failure(
                "API Key 不能为空",
                "请提供自定义服务的 API Key"
                                           );
        }

        if (settings.modelName == null || settings.modelName.trim().isEmpty()) {
            return ValidationResult.failure(
                "模型名称不能为空",
                "请指定要使用的模型名称"
                                           );
        }

        // 调用父类验证方法
        return super.validateConfiguration();
    }
}
