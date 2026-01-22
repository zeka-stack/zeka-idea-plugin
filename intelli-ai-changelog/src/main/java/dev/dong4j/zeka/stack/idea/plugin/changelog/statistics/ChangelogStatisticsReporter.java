package dev.dong4j.zeka.stack.idea.plugin.changelog.statistics;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsEvent;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsEventType;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsPluginId;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsServiceInitializer;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;

/**
 * 变更日志统计报告器
 * <p> 用于收集并上报 AI 服务调用的统计信息, 包括请求耗时, 输入 / 输出令牌数, 用户行为等, 支持中英文混合文本的令牌估算.
 * 该类通过静态方法将统计事件封装并提交至统计服务, 适用于需要监控 AI 服务性能和使用情况的场景.
 * <p> 令牌估算逻辑: 中文字符按每 1.5 个字符估算为 1 个令牌, 英文字符按每 4.0 个字符估算为 1 个令牌.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22
 * @since x.x.x
 */
public final class ChangelogStatisticsReporter {

    /** 用于匹配中文字符的正则模式, 范围为 \\u4E00 到 \\u9FA5 */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FA5]");
    /** 英文字符每令牌估算系数, 用于计算输入 / 输出令牌数量 */
    private static final double ENGLISH_CHARS_PER_TOKEN = 4.0;
    /** 中文字符每分词对应的 token 数量系数, 用于估算文本 token 数量 */
    private static final double CHINESE_CHARS_PER_TOKEN = 1.5;

    /**
     * 私有构造函数, 防止外部实例化该类
     * <p> 该类为工具类, 仅提供静态方法用于上报变更日志统计信息, 不允许创建实例
     */
    private ChangelogStatisticsReporter() {
    }

    /**
     * 报告成功统计事件
     * <p> 根据传入的参数构建并上报成功状态的统计事件, 自动计算输入, 输出和总 Token 数量.
     * 若指定的 Token 数值为 0 或负数, 则使用估算值或默认值填充.
     *
     * @param project          非空项目对象, 用于获取项目名称
     * @param eventType        非空统计事件类型, 标识事件类别
     * @param provider         非空 AI 提供商配置对象, 用于获取提供者 ID 和模型名称
     * @param request          非空 AI 聊天请求对象, 用于估算提示 Token 数量
     * @param content          非空内容字符串, 用于估算输出 Token 数量
     * @param latencyMs        耗时 (毫秒), 表示请求处理延迟
     * @param promptTokens     指定的提示 Token 数量, 若为 0 或负数则使用估算值
     * @param completionTokens 指定的完成 Token 数量, 若为 0 或负数则使用估算值
     * @param totalTokens      指定的总 Token 数量, 若为 0 或负数则使用输入与输出 Token 之和
     * @param userAction       可空用户操作对象, 若为 null 则默认使用 {@link StatisticsUserAction#UNKNOWN}
     * @since 1.0.0
     */
    public static void reportSuccess(@NotNull Project project,
                                     @NotNull StatisticsEventType eventType,
                                     @NotNull AIProviderConfig provider,
                                     @NotNull AIChatRequest request,
                                     @NotNull String content,
                                     long latencyMs,
                                     int promptTokens,
                                     int completionTokens,
                                     int totalTokens,
                                     @Nullable StatisticsUserAction userAction) {
        int inputToken = promptTokens > 0 ? promptTokens : Math.max(0, request.promptTokenEstimate());
        int outputToken = completionTokens > 0 ? completionTokens : estimateTokens(content);
        int totalToken = totalTokens > 0 ? totalTokens : Math.max(0, inputToken + outputToken);

        StatisticsEvent event = new StatisticsEvent(
            StatisticsPluginId.CHANGELOG,
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
     * 估算文本的 Token 数量
     * <p> 根据中文和英文字符的统计规则, 将输入文本按字符类型分类并计算 Token 数量. 中文字符按每 1.5 个字符计为 1 个 Token, 英文字符按每 4.0 个字符计为 1 个 Token, 最终结果向上取整.
     *
     * @param text 输入的文本, 允许为 null 或空字符串
     * @return 估算的 Token 数量, 若输入为 null 或空字符串则返回 0
     */
    private static int estimateTokens(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int totalChars = text.length();
        int chineseChars = 0;
        Matcher matcher = CHINESE_PATTERN.matcher(text);
        while (matcher.find()) {
            chineseChars++;
        }
        int otherChars = Math.max(0, totalChars - chineseChars);
        double chineseTokens = chineseChars / CHINESE_CHARS_PER_TOKEN;
        double otherTokens = otherChars / ENGLISH_CHARS_PER_TOKEN;
        return (int) Math.ceil(chineseTokens + otherTokens);
    }
}
