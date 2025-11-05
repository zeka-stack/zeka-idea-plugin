package dev.dong4j.zeka.stack.idea.plugin.ai.provider;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * 硅基流动服务提供商
 *
 * <p>硅基流动提供的 AI 服务，支持多种开源和商业模型。
 * 提供 OpenAI 兼容的 API 接口。
 *
 * <p>特点：
 * <ul>
 *   <li>兼容 OpenAI API</li>
 *   <li>支持多种开源模型</li>
 *   <li>需要 API Key</li>
 *   <li>性能稳定</li>
 *   <li>价格合理</li>
 * </ul>
 *
 * <p>使用要求：
 * <ol>
 *   <li>必须注册硅基流动账号</li>
 *   <li>必须获取 API Key</li>
 *   <li>必须选择支持的模型</li>
 *   <li>需要网络连接</li>
 * </ol>
 *
 * <p>配置步骤：
 * <ol>
 *   <li>访问 <a href="https://siliconflow.cn">硅基流动官网</a> 注册账号</li>
 *   <li>在控制台中获取 API Key</li>
 *   <li>在设置中选择 SILICONFLOW 作为 AI Provider</li>
 *   <li>确认 Base URL 为 https://api.siliconflow.cn/v1</li>
 *   <li>输入 API Key</li>
 *   <li>选择要使用的模型名称</li>
 *   <li>点击"测试连接"验证配置</li>
 * </ol>
 *
 * <p>支持的模型：
 * <ul>
 *   <li>DeepSeek 系列：deepseek-chat, deepseek-coder, deepseek-reasoner</li>
 *   <li>Qwen 系列：qwen-turbo, qwen-plus, qwen-max, qwen2.5-*</li>
 *   <li>Llama 系列：llama-3.1-8b-instruct, llama-3.1-70b-instruct, llama-3.1-405b-instruct</li>
 *   <li>Gemma 系列：gemma-2-9b-it, gemma-2-27b-it</li>
 *   <li>Phi 系列：phi-3-medium-128k-instruct, phi-3-mini-128k-instruct</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>确保 API Key 有效且有足够的余额</li>
 *   <li>模型名称必须与服务提供商支持的模型一致</li>
 *   <li>某些模型可能有使用限制或需要特殊权限</li>
 *   <li>建议根据任务类型选择合适的模型</li>
 *   <li>注意 API 调用频率限制</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see AICompatibleProvider
 * @since 1.0.0
 */
public class SiliconFlowProvider extends AICompatibleProvider {

    /**
     * 构造函数
     *
     * @param settings 配置状态
     */
    public SiliconFlowProvider(@NotNull SettingsState settings, SettingsState.ProviderConfig providerConfig) {
        super(settings, providerConfig);
    }

}
