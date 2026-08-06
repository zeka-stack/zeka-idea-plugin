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
 * 智谱 GLM 思考参数（OpenAI 兼容入口）
 * <p>
 * 优先官方形态：{@code thinking.type} + {@code reasoning_effort}；
 * 关闭时显式 {@code thinking.type=disabled}。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://docs.bigmodel.cn/cn/guide/capabilities/thinking">智谱思考模式</a>
 */
public final class ZhipuThinkingStrategy implements ThinkingParamStrategy {

    public static final ZhipuThinkingStrategy INSTANCE = new ZhipuThinkingStrategy();

    private ZhipuThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "zhipu";
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
        result.summary = "【思考能力】智谱 GLM：thinking.type + reasoning_effort";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.MAX;
    }
}
