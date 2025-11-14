package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * AI 服务统一入口
 * <p>
 * 为外部插件提供简洁的 AI 能力，自动从插件配置中读取默认供应商。
 *
 * @author dong4j
 * @version 1.0.0
 */
public interface AIService {

    /**
     * 生成 AI 内容（最简单的方式 - 使用插件配置的默认供应商）
     * <p>
     * 自动从插件的配置中读取默认供应商，从全局配置中读取供应商详情。
     * 外部开发者只需要提供提示词和项目对象即可。
     *
     * @param project      项目对象（用于获取插件配置）
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param listener     响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                           @NotNull String systemPrompt,
                           @NotNull String userPrompt,
                           @Nullable AIResponseListener listener) throws AIServiceException;

    /**
     * 生成 AI 内容（使用插件配置的默认供应商）
     * <p>
     * 自动从插件的配置中读取默认供应商。
     *
     * @param project  项目对象（用于获取插件配置）
     * @param request  AI 聊天请求
     * @param listener 响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                           @NotNull AIChatRequest request,
                           @Nullable AIResponseListener listener) throws AIServiceException;

    /**
     * 生成 AI 内容（指定供应商类型）
     * <p>
     * 临时使用指定的供应商类型，从全局配置中读取该供应商的详情。
     *
     * @param project      项目对象
     * @param providerType 供应商类型（从全局可用列表中选取）
     * @param request      AI 聊天请求
     * @param listener     响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                           @NotNull AIProviderType providerType,
                           @NotNull AIChatRequest request,
                           @Nullable AIResponseListener listener) throws AIServiceException;

    /**
     * 生成 AI 内容（自定义配置）
     * <p>
     * 支持临时覆盖配置，适合特殊场景。
     *
     * @param project  项目对象
     * @param request  AI 聊天请求
     * @param config   临时配置（可选，为 null 时使用插件配置的默认供应商）
     * @param listener 响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                           @NotNull AIChatRequest request,
                           @Nullable AIServiceConfig config,
                           @Nullable AIResponseListener listener) throws AIServiceException;

    /**
     * 获取全局配置（可用供应商列表）
     * <p>
     * 返回全局的 AI 配置，包含所有可用供应商。
     *
     * @return 全局 AI 配置
     */
    @NotNull
    AIProviderSettings getGlobalSettings();

    /**
     * 验证插件配置
     * <p>
     * 验证插件配置的默认供应商是否有效。
     *
     * @param project 项目对象（用于获取插件配置）
     * @return 验证结果
     */
    @NotNull
    ValidationResult validatePluginConfiguration(@NotNull Project project);
}

