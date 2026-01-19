package dev.dong4j.zeka.stack.idea.javadoc.statistics;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.util.TokenCounter;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsEvent;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsEventType;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsPluginId;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsServiceInitializer;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;

/**
 * Javadoc 统计上报器
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.01
 * @since 1.0.0
 */
public final class JavadocStatisticsReporter {

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 仅提供静态方法用于上报 Javadoc 统计信息, 不可被实例化
     */
    private JavadocStatisticsReporter() {
    }

    /**
     * 报告文档生成成功事件的统计信息
     * <p> 该方法用于将文档生成任务的成功结果上报至统计服务, 包含模型调用耗时,Token 数量, 用户行为等信息.
     * 通过构建 {@code StatisticsEvent} 对象并调用统计服务进行上报.
     *
     * @param project          当前项目对象, 用于标识统计来源
     * @param task             文档生成任务对象, 用于确定事件类型 (如类, 方法, 字段等)
     * @param provider         AI 提供商配置对象, 包含提供商类型和模型名称
     * @param request          AI 聊天请求对象, 用于估算提示 Token 数量
     * @param documentation    生成的文档内容字符串
     * @param latencyMs        生成耗时 (毫秒)
     * @param promptTokens     提示 Token 数量, 若为 0 则使用请求对象估算值
     * @param completionTokens 完成 Token 数量, 若为 0 则使用文档内容估算值
     * @param totalTokens      总 Token 数量, 若为 0 则根据输入输出 Token 计算
     * @param userAction       用户操作行为, 若为 null 则默认为 {@code StatisticsUserAction.UNKNOWN}
     * @since 1.0.0
     */
    public static void reportSuccess(@NotNull Project project,
                                     @NotNull DocumentationTask task,
                                     @NotNull AIProviderConfig provider,
                                     @NotNull AIChatRequest request,
                                     @NotNull String documentation,
                                     long latencyMs,
                                     int promptTokens,
                                     int completionTokens,
                                     int totalTokens,
                                     @Nullable StatisticsUserAction userAction) {
        StatisticsEventType eventType = resolveEventType(task.getType());

        int inputToken = promptTokens > 0 ? promptTokens : Math.max(0, request.promptTokenEstimate());
        int outputToken = completionTokens > 0 ? completionTokens : TokenCounter.estimateTokens(documentation);
        int totalToken = totalTokens > 0 ? totalTokens : Math.max(0, inputToken + outputToken);

        StatisticsEvent event = new StatisticsEvent(
            StatisticsPluginId.JAVADOC,
            eventType,
            provider.providerType != null ? provider.providerType.getProviderId() : "",
            provider.modelName != null ? provider.modelName : "",
            totalToken,
            project.getName(),
            "success",
            latencyMs,
            inputToken,
            outputToken,
            userAction != null ? userAction : StatisticsUserAction.UNKNOWN
        );

        StatisticsServiceInitializer.getService().report(event);
    }

    /**
     * 根据文档任务类型解析对应的统计事件类型
     * <p>根据传入的文档任务类型 (如类, 方法, 字段等) 返回对应的统计事件类型枚举值, 用于后续上报统计信息
     *
     * @param type 文档任务类型, 必须非空
     * @return 对应的统计事件类型, 如果类型不匹配则返回 null
     */
    @NotNull
    private static StatisticsEventType resolveEventType(@NotNull DocumentationTask.TaskType type) {
        return switch (type) {
            case CLASS, INTERFACE, ENUM -> StatisticsEventType.JAVADOC_CLASS;
            case METHOD, TEST_METHOD -> StatisticsEventType.JAVADOC_METHOD;
            case FIELD -> StatisticsEventType.JAVADOC_FIELD;
        };
    }
}
