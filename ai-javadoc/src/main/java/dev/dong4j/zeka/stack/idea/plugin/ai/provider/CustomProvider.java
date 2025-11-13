// package dev.dong4j.zeka.stack.idea.plugin.ai.provider;
//
// import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
//
// /**
//  * 自定义服务提供商
//  *
//  * <p>兼容 OpenAI API 接口的自定义服务提供商。
//  * 支持任何提供 OpenAI 兼容 API 的服务，包括：
//  * <ul>
//  *   <li>OpenAI 官方服务</li>
//  *   <li>Azure OpenAI 服务</li>
//  *   <li>其他兼容 OpenAI API 的第三方服务</li>
//  *   <li>自部署的 OpenAI 兼容服务</li>
//  * </ul>
//  *
//  * <p>使用要求：
//  * <ol>
//  *   <li>必须提供 Base URL（API 服务地址）</li>
//  *   <li>必须提供 API Key（用于身份验证）</li>
//  *   <li>必须指定模型名称</li>
//  *   <li>服务必须支持 OpenAI 兼容的 /chat/completions 接口</li>
//  * </ol>
//  *
//  * <p>配置步骤：
//  * <ol>
//  *   <li>在设置中选择 CUSTOM 作为 AI Provider</li>
//  *   <li>输入 Base URL（如：<a href="https://api.openai.com/v1">...</a>）</li>
//  *   <li>输入 API Key</li>
//  *   <li>输入模型名称（如：gpt-3.5-turbo, gpt-4 等）</li>
//  *   <li>点击"测试连接"验证配置</li>
//  * </ol>
//  *
//  * <p>支持的常见服务：
//  * <ul>
//  *   <li>OpenAI: https://api.openai.com/v1</li>
//  *   <li>Azure OpenAI: https://your-resource.openai.azure.com/openai/deployments/your-deployment</li>
//  *   <li>Anthropic Claude (通过兼容层): https://api.anthropic.com/v1</li>
//  *   <li>Google Gemini (通过兼容层): https://generativelanguage.googleapis.com/v1beta</li>
//  * </ul>
//  *
//  * <p>注意事项：
//  * <ul>
//  *   <li>确保服务支持 OpenAI 兼容的 API 格式</li>
//  *   <li>API Key 格式通常为 "Bearer {key}" 或直接使用 key</li>
//  *   <li>模型名称必须与服务提供商支持的模型一致</li>
//  *   <li>某些服务可能有速率限制或使用配额</li>
//  * </ul>
//  *
//  * @author dong4j
//  * @version 1.0.0
//  * @see AICompatibleProvider
//  * @since 1.0.0
//  */
// public class CustomProvider extends AICompatibleProvider {
//
//     /**
//      * 初始化 CustomProvider 实例
//      * <p>
//      * 使用给定的 SettingsState 对象初始化父类，并完成自身的初始化逻辑
//      *
//      * @param settings 用于初始化的设置状态对象
//      */
//     public CustomProvider(SettingsState settings, SettingsState.ProviderConfig providerConfig) {
//         super(settings, providerConfig);
//     }
//
// }
