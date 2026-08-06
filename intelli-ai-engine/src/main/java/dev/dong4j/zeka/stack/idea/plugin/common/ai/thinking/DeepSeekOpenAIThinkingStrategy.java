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
 * DeepSeek OpenAI 兼容格式思考参数
 * <p>
 * 开启：{@code thinking.type=enabled} + {@code reasoning_effort}；
 * 关闭：显式 {@code thinking.type=disabled}（避免服务端默认开启思考）。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://api-docs.deepseek.com/guides/thinking_mode">DeepSeek Thinking Mode</a>
 */
public final class DeepSeekOpenAIThinkingStrategy implements ThinkingParamStrategy {

    public static final DeepSeekOpenAIThinkingStrategy INSTANCE = new DeepSeekOpenAIThinkingStrategy();

    private DeepSeekOpenAIThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "deepseek_openai";
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
            body.addProperty("reasoning_effort", effort.toApiValue());
        } else {
            // 显式关闭：DeepSeek 默认开启思考，OMIT 会违背开关语义
            thinking.addProperty("type", "disabled");
            body.add("thinking", thinking);
        }
    }

    @Override
    public @NotNull ThinkingProbeResult probe(@NotNull Project project,
                                              @NotNull AIProviderConfig config,
                                              @Nullable String apiKey,
                                              @NotNull BiConsumer<HttpURLConnection, String> connectionTuner) {
        // 官方语义明确，不做 enable_thinking 三探针
        ThinkingProbeResult result = new ThinkingProbeResult();
        result.capability = ThinkingCapability.OPTIONAL;
        result.probedAt = System.currentTimeMillis();
        result.summary = "【思考能力】DeepSeek OpenAI 格式\n"
                         + "  开关: thinking.type = enabled/disabled\n"
                         + "  强度: reasoning_effort = low/high/max\n"
                         + "  结论: 可选 Think（可开关 + 强度）";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.HIGH;
    }
}
