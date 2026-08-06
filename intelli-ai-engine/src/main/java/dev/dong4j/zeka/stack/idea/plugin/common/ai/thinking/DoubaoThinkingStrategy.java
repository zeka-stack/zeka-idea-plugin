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
 * 火山方舟 / 豆包思考参数
 * <p>
 * 开关：{@code thinking.type} = enabled/disabled；
 * 强度：开启时写 {@code reasoning_effort}（low/high/max，AUTO 默认 high）。
 * 部分旧模型可能忽略 reasoning_effort，但不影响开关语义。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class DoubaoThinkingStrategy implements ThinkingParamStrategy {

    public static final DoubaoThinkingStrategy INSTANCE = new DoubaoThinkingStrategy();

    private DoubaoThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "doubao";
    }

    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return ThinkingUiCapability.TOGGLE_AND_EFFORT;
    }

    @Override
    public void apply(@NotNull JsonObject body,
                      @NotNull ThinkingIntent intent,
                      @NotNull ThinkingContext context) {
        JsonObject thinking = new JsonObject();
        if (intent.enabled()) {
            thinking.addProperty("type", "enabled");
            body.add("thinking", thinking);
            ThinkingEffort effort = intent.effort() == ThinkingEffort.AUTO ? defaultEffort() : intent.effort();
            body.addProperty("reasoning_effort", effort.toApiValue());
        } else {
            thinking.addProperty("type", "disabled");
            body.add("thinking", thinking);
        }
    }

    @Override
    public @NotNull ThinkingProbeResult probe(@NotNull Project project,
                                              @NotNull AIProviderConfig config,
                                              @Nullable String apiKey,
                                              @NotNull BiConsumer<HttpURLConnection, String> connectionTuner) {
        ThinkingProbeResult result = new ThinkingProbeResult();
        result.capability = ThinkingCapability.OPTIONAL;
        result.probedAt = System.currentTimeMillis();
        result.summary = "【思考能力】豆包：thinking.type + reasoning_effort";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.HIGH;
    }
}
