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
 * DeepSeek Anthropic 兼容格式思考参数
 * <p>
 * 开启：{@code thinking.type=enabled} + {@code output_config.effort}；
 * 关闭：显式 {@code thinking.type=disabled}。不传 {@code budget_tokens}（官方忽略）。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://api-docs.deepseek.com/guides/thinking_mode">DeepSeek Thinking Mode</a>
 */
public final class DeepSeekAnthropicThinkingStrategy implements ThinkingParamStrategy {

    public static final DeepSeekAnthropicThinkingStrategy INSTANCE = new DeepSeekAnthropicThinkingStrategy();

    private DeepSeekAnthropicThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "deepseek_anthropic";
    }

    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return ThinkingUiCapability.DEEPSEEK;
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
            JsonObject outputConfig = new JsonObject();
            outputConfig.addProperty("effort", effort.toApiValue());
            body.add("output_config", outputConfig);
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
        result.summary = "【思考能力】DeepSeek Anthropic 格式\n"
                         + "  开关: thinking.type = enabled/disabled\n"
                         + "  强度: output_config.effort = low/high/max\n"
                         + "  结论: 可选 Think（可开关 + 强度）";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.HIGH;
    }
}
