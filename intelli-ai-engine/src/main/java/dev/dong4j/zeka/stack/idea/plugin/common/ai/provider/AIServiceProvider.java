package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;

/**
 * AI 服务提供商接口
 * <p>
 * 定义 AI 服务提供商的统一接口规范, 用于集成不同的 AI 服务提供商,
 * 提供模型调用, 配置验证, 可用模型查询等核心功能
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public interface AIServiceProvider {

    /**
     * 获取当前 AI 服务的提供者类型
     * <p>
     * 返回当前配置或实例所使用的 AI 服务提供者类型
     *
     * @return AI 服务提供者类型
     */
    @NotNull
    AIProviderType getProviderType();

    /**
     * 获取模型名称
     * <p>
     * 返回当前模型的名称.
     *
     * @return 模型名称
     */
    @NotNull
    String getModelName();

    /**
     * 获取基础 URL
     * <p>
     * 返回当前应用的基础 URL, 通常用于构建完整的请求地址.
     *
     * @return 不可为空的基础 URL 字符串
     */
    @NotNull
    String getBaseUrl();

    /**
     * 根据 AI 聊天请求生成内容
     * <p>
     * 根据传入的 AI 聊天请求对象生成相应的内容, 支持传入 API 密钥和监听器进行扩展功能.
     *
     * @param request  AI 聊天请求对象, 用于指定生成内容的参数和上下文
     * @param apiKey   API 密钥, 可选参数, 用于身份验证或权限控制
     * @param listener AI 响应监听器, 可选参数, 用于接收生成内容的回调
     * @return 生成的内容字符串
     * @throws AIServiceException 当生成内容过程中发生错误时抛出
     */
    @NotNull
    String generateContent(@NotNull AIChatRequest request,
                           @Nullable String apiKey,
                           @Nullable AIResponseListener listener) throws AIServiceException;

    /**
     * 验证配置信息
     * <p>
     * 使用提供的 API 密钥验证配置的有效性.
     *
     * @param apiKey 可能为 null 的 API 密钥
     * @return 验证结果
     * @throws IllegalArgumentException 如果验证失败
     */
    @NotNull
    ValidationResult validateConfiguration(@Nullable String apiKey);

    /**
     * 获取可用的模型列表
     * <p>
     * 根据提供的 API 密钥获取可用的模型名称列表, 若未提供 API 密钥, 则可能返回默认模型列表.
     *
     * @param apiKey API 密钥, 可为空
     * @return 可用的模型名称列表
     * @since 1.0
     */
    @NotNull
    List<String> getAvailableModels(@Nullable String apiKey);
}
