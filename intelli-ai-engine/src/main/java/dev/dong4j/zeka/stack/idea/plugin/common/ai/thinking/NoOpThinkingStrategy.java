package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import com.google.gson.JsonObject;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.function.BiConsumer;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingCapability;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingProbeResult;

/**
 * 不写入思考扩展字段（如非 DeepSeek 的 Anthropic 路径，本期不做 budget_tokens）
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class NoOpThinkingStrategy implements ThinkingParamStrategy {

    public static final NoOpThinkingStrategy INSTANCE = new NoOpThinkingStrategy();

    private NoOpThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "noop";
    }

    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return ThinkingUiCapability.NONE;
    }

    @Override
    public void apply(@NotNull JsonObject body,
                      @NotNull ThinkingIntent intent,
                      @NotNull ThinkingContext context) {
        // 故意不写任何字段
    }

    @Override
    public @NotNull ThinkingProbeResult probe(@NotNull Project project,
                                              @NotNull AIProviderConfig config,
                                              @Nullable String apiKey,
                                              @NotNull BiConsumer<HttpURLConnection, String> connectionTuner) {
        ThinkingProbeResult result = new ThinkingProbeResult();
        result.capability = ThinkingCapability.UNKNOWN;
        result.probedAt = System.currentTimeMillis();
        result.summary = "【思考能力】当前协议未接入思考参数策略";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.HIGH;
    }
}
