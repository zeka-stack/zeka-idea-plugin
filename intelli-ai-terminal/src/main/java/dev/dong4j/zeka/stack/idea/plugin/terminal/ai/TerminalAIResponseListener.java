package dev.dong4j.zeka.stack.idea.plugin.terminal.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.Getter;

/**
 * 终端 AI 响应监听器实现类
 * <p> 用于监听 AI 服务请求与响应过程, 包括请求内容打印, 响应内容打印以及 Token 使用统计信息输出.
 * 该监听器适用于集成在 IDE 或终端环境中的 AI 服务调用场景, 通过日志工具将关键信息输出到控制台.
 * 该类实现了 {@link AIResponseListener} 接口, 提供完整的请求, 响应和用量回调处理.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22
 * @since 1.0.0
 */
public class TerminalAIResponseListener implements AIResponseListener {

    /** 当前项目对象 */
    private final Project project;
    /** 用于记录提示词所消耗的 token 数量 */
    @Getter
    private volatile int promptTokens;
    /** 用于记录完成阶段使用的 token 数量 */
    @Getter
    private volatile int completionTokens;
    /** 用于记录总 token 数量 */
    @Getter
    private volatile int totalTokens;

    /**
     * 初始化 TerminalAIResponseListener
     * <p> 创建一个用于监听终端 AI 请求与响应的监听器实例, 绑定当前项目对象以供后续日志输出使用.
     *
     * @param project 当前项目对象, 用于在日志输出时关联项目上下文
     */
    public TerminalAIResponseListener(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理 AI 请求并记录相关信息
     * <p> 在控制台输出请求日志, 包含提供者名称, 模型名称及请求体内容 (如非空).
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容, 若非空则输出到控制台
     * @param validation   是否进行验证
     */
    @Override
    public void onRequest(String providerName, String modelName, String requestBody, boolean validation) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("请求: %s - %s", providerName, modelName));
        if (requestBody != null && !requestBody.isEmpty()) {
            AIConsoleLoggerUtil.print(project, requestBody);
        }
    }

    /**
     * 处理 AI 响应并记录相关信息
     * <p> 在控制台输出响应日志, 包含提供者名称, 模型名称和响应体内容 (如果非空).
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param responseBody 响应体内容, 若为 null 或空字符串则不输出
     * @param validation   是否进行了验证
     */
    @Override
    public void onResponse(String providerName, String modelName, String responseBody, boolean validation) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("响应: %s - %s", providerName, modelName));
        if (responseBody != null && !responseBody.isEmpty()) {
            AIConsoleLoggerUtil.print(project, responseBody);
        }
    }

    /**
     * 记录 Token 使用情况
     * <p> 将提供者名称, 模型名称,Prompt Token 数,Completion Token 数和总 Token 数格式化后输出到控制台,
     * 并保存 token 信息用于统计上报.
     *
     * @param providerName     提供者名称
     * @param modelName        模型名称
     * @param promptTokens     Prompt 使用 token 数
     * @param completionTokens Completion 使用 token 数
     * @param totalTokens      总 token 数
     */
    @Override
    public void onUsage(String providerName, String modelName,
                        int promptTokens, int completionTokens, int totalTokens) {
        // 保存 token 信息用于统计上报
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        AIConsoleLoggerUtil.print(project,
                                  String.format("Token 使用: %s | %s | Prompt: %d | Completion: %d | Total: %d",
                                                providerName, modelName, promptTokens, completionTokens, totalTokens));
    }
}
