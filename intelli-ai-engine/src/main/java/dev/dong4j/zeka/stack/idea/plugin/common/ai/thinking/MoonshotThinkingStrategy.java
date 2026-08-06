package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import com.google.gson.JsonObject;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.function.BiConsumer;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingCapability;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingProbeResult;

/**
 * Moonshot / Kimi 思考参数
 * <p>
 * <ul>
 *   <li>K3：始终思考，仅顶层 {@code reasoning_effort}（low/high/max，默认 max）</li>
 *   <li>K2.x：{@code thinking.type} = enabled/disabled；关闭时显式 disabled</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://platform.kimi.com/docs/guide/use-thinking-models">Kimi Thinking Models</a>
 */
public final class MoonshotThinkingStrategy implements ThinkingParamStrategy {

    public static final MoonshotThinkingStrategy INSTANCE = new MoonshotThinkingStrategy();

    private MoonshotThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "moonshot";
    }

    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return isK3(context.modelName()) ? ThinkingUiCapability.EFFORT_ONLY : ThinkingUiCapability.TOGGLE_ONLY;
    }

    @Override
    public void apply(@NotNull JsonObject body,
                      @NotNull ThinkingIntent intent,
                      @NotNull ThinkingContext context) {
        if (isK3(context.modelName())) {
            ThinkingEffort effort = intent.effort() == ThinkingEffort.AUTO ? defaultEffort() : intent.effort();
            body.addProperty("reasoning_effort", effort.toApiValue());
            return;
        }
        // K2.x / 其他：thinking.type
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
        boolean k3 = isK3(config.modelName != null ? config.modelName : "");
        result.summary = k3
                         ? "【思考能力】Kimi K3：始终思考，使用 reasoning_effort（low/high/max）"
                         : "【思考能力】Kimi K2.x：thinking.type = enabled/disabled";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.MAX;
    }

    /**
     * 是否 K3 系列（始终思考 + reasoning_effort）
     */
    static boolean isK3(@NotNull String modelName) {
        String lower = modelName.toLowerCase(Locale.ROOT);
        return lower.contains("kimi-k3") || lower.contains("kimi/k3") || lower.equals("k3");
    }
}
