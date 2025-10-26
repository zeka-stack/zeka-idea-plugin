package dev.dong4j.zeka.stack.idea.plugin.ai.provider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * 通义千问服务提供商
 *
 * <p>阿里云提供的大语言模型服务，支持 OpenAI 兼容模式。
 * 作为阿里云的核心 AI 服务，通义千问提供了多种不同规模和用途的模型，
 * 适用于各种场景的文档生成任务。
 *
 * <p>服务特点：
 * <ul>
 *   <li>多种模型选择，满足不同性能和成本需求</li>
 *   <li>支持中文和多语言处理</li>
 *   <li>高准确性和稳定性</li>
 *   <li>完善的文档和社区支持</li>
 * </ul>
 *
 * <p>使用说明：
 * <ol>
 *   <li>在阿里云申请 API Key：<a href="https://dashscope.console.aliyun.com/">DashScope 控制台</a></li>
 *   <li>在设置中配置 API Key</li>
 *   <li>选择或输入模型名称（可输入官方文档中的任何模型）</li>
 * </ol>
 *
 * <p>常用模型（仅供参考，您可以使用任何官方支持的模型）：
 * <ul>
 *   <li>{@code qwen-max} - 最强大的模型（推荐）</li>
 *   <li>{@code qwen-plus} - 性价比较高</li>
 *   <li>{@code qwen-turbo} - 速度最快</li>
 * </ul>
 *
 * <p><b>更多模型：</b> 请参考官方文档获取完整模型列表和详细说明。
 *
 * @author dong4j
 * @version 1.0.0
 * @see <a href="https://help.aliyun.com/zh/dashscope/">通义千问官方文档</a>
 * @see AICompatibleProvider
 * @since 1.0.0
 */
public class QianWenProvider extends AICompatibleProvider {

    /**
     * 初始化 QianWenProvider 实例
     * <p>
     * 使用给定的 SettingsState 对象初始化 QianWenProvider，调用父类的构造函数进行配置
     *
     * @param settings 用于初始化的设置状态对象
     */
    public QianWenProvider(SettingsState settings) {
        super(settings);
    }

    /**
     * 获取当前AI服务提供商的ID
     * <p>
     * 返回预定义的AI服务提供商类型对应的提供商ID
     *
     * @return 当前AI服务提供商的ID
     */
    @NotNull
    @Override
    public String getProviderId() {
        return AIProviderType.QIANWEN.getProviderId();
    }

    /**
     * 获取当前AI服务提供商的名称
     * <p>
     * 返回AI服务提供商的显示名称，该名称由AIProviderType枚举中的QIANWEN常量提供。
     *
     * @return AI服务提供商的显示名称
     */
    @NotNull
    @Override
    public String getProviderName() {
        return AIProviderType.QIANWEN.getDisplayName();
    }

    /**
     * 返回常用模型列表
     *
     * <p>返回推荐的常用模型列表，供用户在设置界面中选择。
     * 这些模型经过测试，能够很好地支持文档生成任务。
     *
     * <p>注意：这只是常用模型列表，用户可以使用通义千问支持的任何模型。
     * 请参考官方文档获取完整的模型列表。
     *
     * @return 常用模型列表（作为参考）
     */
    @NotNull
    @Override
    public List<String> getSupportedModels() {
        return AIProviderType.QIANWEN.getSupportedModels();
    }

    /**
     * 获取默认模型名称
     * <p>
     * 返回AI服务提供商默认的模型名称
     *
     * @return 默认模型名称
     */
    @NotNull
    @Override
    public String getDefaultModel() {
        return AIProviderType.QIANWEN.getDefaultModel();
    }

    /**
     * 获取默认的基础URL
     * <p>
     * 返回AI服务提供商默认的基础URL地址。
     *
     * @return 默认的基础URL
     */
    @NotNull
    @Override
    public String getDefaultBaseUrl() {
        return AIProviderType.QIANWEN.getDefaultBaseUrl();
    }

    /**
     * 判断当前AI服务提供商是否需要API密钥
     * <p>
     * 调用对应AI服务提供商的requiresApiKey方法，判断是否需要API密钥
     *
     * @return 是否需要API密钥
     */
    @Override
    public boolean requiresApiKey() {
        return AIProviderType.QIANWEN.requiresApiKey();
    }
}

