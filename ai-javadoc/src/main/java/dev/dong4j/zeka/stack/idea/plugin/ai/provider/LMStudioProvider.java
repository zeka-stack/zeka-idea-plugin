package dev.dong4j.zeka.stack.idea.plugin.ai.provider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * LM Studio 服务提供商
 *
 * <p>LM Studio 是一个本地运行的开源大语言模型服务。
 * 提供 OpenAI 兼容的 API 接口，支持多种开源模型。
 *
 * <p>特点：
 * <ul>
 *   <li>本地运行，数据隐私安全</li>
 *   <li>兼容 OpenAI API</li>
 *   <li>支持多种开源模型</li>
 *   <li>不需要 API Key</li>
 *   <li>免费使用</li>
 * </ul>
 *
 * <p>使用要求：
 * <ol>
 *   <li>必须安装并运行 LM Studio</li>
 *   <li>必须加载一个模型到 LM Studio</li>
 *   <li>必须启动本地服务器（默认端口 1234）</li>
 *   <li>不需要 API Key</li>
 * </ol>
 *
 * <p>配置步骤：
 * <ol>
 *   <li>下载并安装 LM Studio</li>
 *   <li>在 LM Studio 中下载并加载一个模型</li>
 *   <li>启动本地服务器（Local Server）</li>
 *   <li>在设置中选择 LM_STUDIO 作为 AI Provider</li>
 *   <li>确认 Base URL 为 <a href="http://localhost:1234/v1">...</a></li>
 *   <li>选择要使用的模型名称</li>
 *   <li>点击"测试连接"验证配置</li>
 * </ol>
 *
 * <p>支持的模型：
 * <ul>
 *   <li>GPT 系列：gpt-3.5-turbo, gpt-4, gpt-4-turbo</li>
 *   <li>Claude 系列：claude-3-sonnet, claude-3-opus</li>
 *   <li>Llama 系列：llama-2-7b-chat, llama-2-13b-chat, llama-2-70b-chat</li>
 *   <li>Code Llama：codellama-7b, codellama-13b, codellama-34b</li>
 *   <li>Mistral 系列：mistral-7b-instruct, mixtral-8x7b-instruct</li>
 *   <li>其他开源模型</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>确保 LM Studio 正在运行并加载了模型</li>
 *   <li>本地服务器默认运行在端口 1234</li>
 *   <li>模型名称必须与 LM Studio 中加载的模型一致</li>
 *   <li>首次使用可能需要下载模型，需要一定时间</li>
 *   <li>本地运行对硬件要求较高</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see AICompatibleProvider
 * @since 1.0.0
 */
public class LMStudioProvider extends AICompatibleProvider {

    /**
     * 构造函数
     *
     * @param settings 配置状态
     */
    public LMStudioProvider(@NotNull SettingsState settings) {
        super(settings);
    }

    /**
     * 获取当前AI服务提供商的ID
     * <p>
     * 返回AI服务提供商的唯一标识符，该标识符由AIProviderType枚举中的LM_STUDIO类型定义。
     *
     * @return AI服务提供商的ID
     */
    @Override
    @NotNull
    public String getProviderId() {
        return AIProviderType.LM_STUDIO.getProviderId();
    }

    /**
     * 获取当前AI服务提供商的名称
     * <p>
     * 返回AI服务提供商的显示名称，该名称由AIProviderType枚举中的LM_STUDIO值提供。
     *
     * @return AI服务提供商的显示名称
     */
    @Override
    @NotNull
    public String getProviderName() {
        return AIProviderType.LM_STUDIO.getDisplayName();
    }

    /**
     * 获取当前AI提供者支持的模型列表
     * <p>
     * 调用AI提供者类型对应的获取支持模型方法，返回支持的模型名称列表
     *
     * @return 支持的模型名称列表
     */
    @Override
    @NotNull
    public List<String> getSupportedModels() {
        return AIProviderType.LM_STUDIO.getSupportedModels();
    }

    /**
     * 获取默认模型名称
     * <p>
     * 返回当前AI提供者默认的模型名称。
     *
     * @return 默认模型名称
     */
    @Override
    @NotNull
    public String getDefaultModel() {
        return AIProviderType.LM_STUDIO.getDefaultModel();
    }

    /**
     * 获取默认的基础URL
     * <p>
     * 返回AI服务提供商类型LM_STUDIO对应的基础URL。
     *
     * @return 默认的基础URL
     */
    @Override
    @NotNull
    public String getDefaultBaseUrl() {
        return AIProviderType.LM_STUDIO.getDefaultBaseUrl();
    }

    /**
     * 判断当前AI服务提供商是否需要API密钥
     * <p>
     * 调用对应AI服务提供商的requiresApiKey方法，返回是否需要API密钥
     *
     * @return 是否需要API密钥
     */
    @Override
    public boolean requiresApiKey() {
        return AIProviderType.LM_STUDIO.requiresApiKey();
    }
}
