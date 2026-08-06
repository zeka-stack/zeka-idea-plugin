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
 * 仅写入 {@code thinking.type} 开关的轻量策略
 * <p>
 * 用于上游模型混杂、不宜强加 {@code budget_tokens} / {@code reasoning_effort} 的 Anthropic 兼容网关
 * （如 ModelScope Anthropic）。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ThinkingTypeToggleStrategy implements ThinkingParamStrategy {

    public static final ThinkingTypeToggleStrategy INSTANCE = new ThinkingTypeToggleStrategy();

    private ThinkingTypeToggleStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "thinking_type_toggle";
    }

    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return ThinkingUiCapability.TOGGLE_ONLY;
    }

    @Override
    public void apply(@NotNull JsonObject body,
                      @NotNull ThinkingIntent intent,
                      @NotNull ThinkingContext context) {
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", intent.enabled() ? "enabled" : "disabled");
        body.add("thinking", thinking);
    }

    @Override
    public @NotNull ThinkingProbeResult probe(@NotNull Project project,
                                              @NotNull AIProviderConfig config,
                                              @Nullable String apiKey,
                                              @NotNull BiConsumer<HttpURLConnection, String> connectionTuner) {
        ThinkingProbeResult result = new ThinkingProbeResult();
        result.capability = ThinkingCapability.OPTIONAL;
        result.probedAt = System.currentTimeMillis();
        result.summary = "【思考能力】thinking.type = enabled/disabled（无强度字段）";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.HIGH;
    }
}
