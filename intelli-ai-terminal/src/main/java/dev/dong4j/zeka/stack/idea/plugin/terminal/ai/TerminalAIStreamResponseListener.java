package dev.dong4j.zeka.stack.idea.plugin.terminal.ai;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.frontend.view.TerminalView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.terminal.action.TerminalAiGenerateAction;
import dev.dong4j.zeka.stack.idea.plugin.terminal.context.TerminalContextService;
import dev.dong4j.zeka.stack.idea.plugin.terminal.shell.TerminalShellType;
import dev.dong4j.zeka.stack.idea.plugin.terminal.statistics.TerminalStatisticsReporter;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;

/**
 * 终端 AI 流式响应监听器
 * <p>
 * 用于处理终端 AI 命令生成的流式响应，包括接收数据块、处理完成事件、错误处理以及 Token 统计。
 * 该类实现了 {@link AIStreamResponseListener} 接口，负责在流式响应过程中更新进度、记录日志、
 * 应用生成结果并上报统计信息。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22
 * @since 1.0.0
 */
@SuppressWarnings("UnstableApiUsage")
public class TerminalAIStreamResponseListener implements AIStreamResponseListener {

    /** 当前项目对象 */
    private final Project project;
    /** 终端视图实例, 可为 null */
    private final TerminalView terminalView;
    /** 终端组件实例, 可为 null */
    private final JBTerminalWidget jbWidget;
    /** 输入信息封装对象, 包含原始输入内容和是否为多行标识 */
    private final TerminalAiGenerateAction.InputInfo inputInfo;
    /** 用户原始输入内容 */
    private final String input;
    /** 终端上下文服务, 用于记录历史, 可为 null */
    private final TerminalContextService contextService;
    /** 终端 shell 类型 */
    private final TerminalShellType shellType;
    /** 进度指示器, 用于更新进度状态 */
    private final ProgressIndicator indicator;
    /** AI 提供商配置对象 */
    private final AIProviderConfig providerConfig;
    /** AI 聊天请求对象 */
    private final AIChatRequest request;
    /** 请求开始时间 (毫秒) */
    private final long startTimeMs;
    /** 用户操作行为 */
    private final StatisticsUserAction userAction;
    /** 用于等待流式响应完成的 CountDownLatch */
    private final CountDownLatch streamLatch;
    /** 用于缓存流式响应的文本内容, 累积所有分块数据以生成完整响应 */
    private final StringBuilder streamBuffer = new StringBuilder();
    /** 用于记录提示词所消耗的 token 数量 */
    private volatile int promptTokens;
    /** 用于记录完成阶段使用的 token 数量 */
    private volatile int completionTokens;
    /** 用于记录总 token 数量 */
    private volatile int totalTokens;

    /**
     * 初始化终端 AI 流式响应监听器
     * <p>
     * 创建一个用于处理终端 AI 流式响应的监听器实例，绑定所有必要的上下文信息。
     *
     * @param project        当前项目对象
     * @param terminalView   终端视图实例, 可为 null
     * @param jbWidget       终端组件实例, 可为 null
     * @param inputInfo      输入信息封装对象
     * @param input          用户原始输入内容
     * @param contextService 终端上下文服务, 可为 null
     * @param shellType      终端 shell 类型
     * @param indicator      进度指示器
     * @param providerConfig AI 提供商配置对象
     * @param request        AI 聊天请求对象
     * @param startTimeMs    请求开始时间 (毫秒)
     * @param userAction     用户操作行为
     * @param streamLatch    用于等待流式响应完成的 CountDownLatch
     */
    public TerminalAIStreamResponseListener(@NotNull Project project,
                                            @Nullable TerminalView terminalView,
                                            @Nullable JBTerminalWidget jbWidget,
                                            @NotNull TerminalAiGenerateAction.InputInfo inputInfo,
                                            @NotNull String input,
                                            @Nullable TerminalContextService contextService,
                                            @NotNull TerminalShellType shellType,
                                            @NotNull ProgressIndicator indicator,
                                            @NotNull AIProviderConfig providerConfig,
                                            @NotNull AIChatRequest request,
                                            long startTimeMs,
                                            @NotNull StatisticsUserAction userAction,
                                            @NotNull CountDownLatch streamLatch) {
        this.project = project;
        this.terminalView = terminalView;
        this.jbWidget = jbWidget;
        this.inputInfo = inputInfo;
        this.input = input;
        this.contextService = contextService;
        this.shellType = shellType;
        this.indicator = indicator;
        this.providerConfig = providerConfig;
        this.request = request;
        this.startTimeMs = startTimeMs;
        this.userAction = userAction;
        this.streamLatch = streamLatch;
    }

    /**
     * 流式响应开始
     * <p>
     * 输出响应头, 便于区分请求与响应, 并更新进度状态为流式接收状态.
     */
    @Override
    public void onStart() {
        AIConsoleLoggerUtil.printSuccess(project, "=== Terminal AI Response ===");
        // 更新为流式接收状态 - 直接调用，ProgressIndicator 是线程安全的
        indicator.setText(TerminalBundle.message("action.terminal.progress.streaming"));
    }

    /**
     * 处理流式响应的片段数据
     * <p>
     * 将接收到的片段内容追加到内部缓冲区中, 用于后续完整响应的拼接, 并更新进度状态.
     *
     * @param chunk 当前接收到的响应片段内容
     */
    @Override
    public void onChunk(@NotNull String chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        streamBuffer.append(chunk);
        // 更新流式接收进度 - 直接调用，ProgressIndicator 是线程安全的
        indicator.setText(TerminalBundle.message("action.terminal.progress.streaming"));
    }

    /**
     * 处理 AI 流式响应完成事件
     * <p>
     * 当 AI 响应完整返回时, 根据是否为空判断使用完整文本或缓冲区内容作为结果, 并调用处理方法.
     * 如果命令成功应用, 则上报统计信息.
     *
     * @param fullText 完整的 AI 响应文本
     */
    @Override
    public void onComplete(@NotNull String fullText) {
        try {
            String result = fullText.isBlank() ? streamBuffer.toString() : fullText;
            // 更新为处理响应状态 - 直接调用，ProgressIndicator 是线程安全的
            indicator.setText(TerminalBundle.message("action.terminal.progress.processing"));
            indicator.checkCanceled();
            boolean applied = TerminalAiGenerateAction.applyAiResult(project, terminalView, jbWidget, inputInfo, result, input,
                                                                     contextService, shellType, indicator);
            if (applied) {
                long latencyMs = System.currentTimeMillis() - startTimeMs;
                TerminalStatisticsReporter.reportSuccess(project,
                                                         providerConfig,
                                                         request,
                                                         result,
                                                         latencyMs,
                                                         promptTokens,
                                                         completionTokens,
                                                         totalTokens,
                                                         userAction);
            }
            // 完成状态
            indicator.setText(TerminalBundle.message("action.terminal.progress.completed"));
        } finally {
            // 释放锁，允许 run 方法继续执行
            streamLatch.countDown();
        }
    }

    /**
     * 处理 AI 流式响应错误情况
     * <p>
     * 当 AI 服务发生错误时, 显示失败提示并弹出错误通知, 并释放等待锁.
     *
     * @param error     错误信息字符串, 用于构建错误提示消息
     * @param exception 可选的异常对象, 用于记录详细错误堆栈信息
     */
    @Override
    public void onError(@NotNull String error, @Nullable Throwable exception) {
        try {
            AIConsoleLoggerUtil.completeStreamPlain(project);
            TerminalAiGenerateAction.showAiFailedHint(terminalView, jbWidget);
            // 更新错误状态 - 直接调用，ProgressIndicator 是线程安全的
            indicator.setText(TerminalBundle.message("action.terminal.progress.completed"));
        } finally {
            // 释放锁，允许 run 方法继续执行
            streamLatch.countDown();
        }
    }

    /**
     * 处理 AI 请求并记录相关信息
     * <p>
     * 在控制台输出请求日志, 包含提供者名称, 模型名称及请求体内容 (如非空), 并更新进度状态为发送请求状态.
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
        // 更新为发送请求状态 - 直接调用，ProgressIndicator 是线程安全的
        indicator.setText(TerminalBundle.message("action.terminal.progress.sending.request"));
    }

    /**
     * 记录 Token 使用情况
     * <p>
     * 将提供者名称, 模型名称,Prompt Token 数,Completion Token 数和总 Token 数格式化后输出到控制台,
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
