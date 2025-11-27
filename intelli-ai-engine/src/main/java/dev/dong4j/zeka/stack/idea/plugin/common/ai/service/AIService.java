package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
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
     * 生成指定项目和请求的 AI 内容
     * <p>
     * 根据提供的项目信息,AI 聊天请求以及可选的配置和监听器生成 AI 响应内容.
     *
     * @param project  项目对象, 用于提供上下文信息
     * @param request  AI 聊天请求, 包含生成内容所需的具体指令或问题
     * @param config   可选的 AI 提供者配置, 用于自定义 AI 行为
     * @param listener 可选的 AI 响应监听器, 用于处理生成过程中的事件或结果
     * @return 生成的 AI 内容字符串
     * @throws AIServiceException 当 AI 服务调用过程中发生错误时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                           @NotNull AIChatRequest request,
                           @NotNull AIProviderConfig config,
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

}

