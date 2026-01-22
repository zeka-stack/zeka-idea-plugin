package dev.dong4j.zeka.stack.idea.plugin.terminal.statistics;

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
 * Terminal 统计上报器
 *
 * <p>参考 changelog/javadoc 统计实现, 用于在 terminal 模块中上报 AI 命令生成的成功事件.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TerminalStatisticsReporter {

    /** 用于匹配中文字符的正则模式, 范围为 \\u4E00 到 \\u9FA5 */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FA5]");
    /** 英文字符每令牌估算比例, 用于计算文本令牌数 */
    private static final double ENGLISH_CHARS_PER_TOKEN = 4.0;
    /** 中文字符每分词的权重系数, 用于估算 token 数量 */
    private static final double CHINESE_CHARS_PER_TOKEN = 1.5;

    /**
     * 私有构造函数, 禁止外部实例化
     * <p> 该类为工具类, 仅提供静态方法用于上报终端命令执行统计信息, 不允许创建实例
     */
    private TerminalStatisticsReporter() {
    }

    /**
     * 上报终端 AI 命令生成成功的统计事件
     * <p> 根据传入的项目,AI 提供者配置, 请求信息, 响应内容, 延迟时间及用户操作行为, 计算提示词与完成词的总 token 数, 并构造统计事件上报至服务.
     * 该方法用于在终端模块中记录 AI 命令成功执行的统计信息, 便于后续分析与优化.
     *
     * @param project          当前项目对象, 用于标识统计来源
     * @param provider          AI 提供商配置对象, 包含提供商类型和模型名称
     * @param request           AI 聊天请求对象, 用于估算提示 Token 数量
     * @param response          响应内容字符串, 用于估算完成 Token 数量
     * @param latencyMs         生成耗时 (毫秒)
     * @param promptTokens      提示 Token 数量, 若为 0 则使用请求对象估算值
     * @param completionTokens  完成 Token 数量, 若为 0 则使用响应内容估算值
     * @param totalTokens       总 Token 数量, 若为 0 则根据输入输出 Token 计算
     * @param userAction        用户操作行为, 若为 null 则默认为 {@code StatisticsUserAction.UNKNOWN}
     * @since 1.0.0
     */
    public static void reportSuccess(@NotNull Project project,
                                     @NotNull AIProviderConfig provider,
                                     @NotNull AIChatRequest request,
                                     @NotNull String response,
                                     long latencyMs,
                                     int promptTokens,
                                     int completionTokens,
                                     int totalTokens,
                                     @Nullable StatisticsUserAction userAction) {
        int inputToken = promptTokens > 0 ? promptTokens : Math.max(0, request.promptTokenEstimate());
        int outputToken = completionTokens > 0 ? completionTokens : estimateTokens(response);
        int totalToken = totalTokens > 0 ? totalTokens : Math.max(0, inputToken + outputToken);

        StatisticsEvent event = new StatisticsEvent(
            StatisticsPluginId.TERMINAL,
            StatisticsEventType.TERMINAL_COMMAND,
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
     * <p> 根据中文字符与英文字符的比例, 估算输入文本的 Token 总数. 中文字符按每 1.5 个字符估算为 1 个 Token, 英文字符按每 4.0 个字符估算为 1 个 Token.
     * <p> 若输入文本为 null 或空字符串, 则返回 0.
     *
     * @param text 待估算的文本, 允许为 null
     * @return 估算的 Token 数量, 四舍五入取整
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
