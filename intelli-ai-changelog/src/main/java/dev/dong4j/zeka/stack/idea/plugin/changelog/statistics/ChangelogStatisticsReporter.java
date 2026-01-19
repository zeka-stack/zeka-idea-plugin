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
 * Changelog 统计上报器
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
public final class ChangelogStatisticsReporter {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FA5]");
    private static final double ENGLISH_CHARS_PER_TOKEN = 4.0;
    private static final double CHINESE_CHARS_PER_TOKEN = 1.5;

    private ChangelogStatisticsReporter() {
    }

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
