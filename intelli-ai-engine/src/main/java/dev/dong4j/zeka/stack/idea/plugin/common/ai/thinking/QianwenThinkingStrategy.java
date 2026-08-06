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
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingSendMode;

/**
 * 通义千问 / DashScope OpenAI 兼容思考参数
 * <p>
 * 开关：{@code enable_thinking}（厂商扩展，非 OpenAI 官方字段）；
 * 强度：开启且非 AUTO 时写 {@code reasoning_effort}（low/medium/xhigh）。
 * 不与 {@code thinking_budget} 同时写入，避免部分模型报错。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class QianwenThinkingStrategy implements ThinkingParamStrategy {

    public static final QianwenThinkingStrategy INSTANCE = new QianwenThinkingStrategy();

    private QianwenThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "qianwen";
    }

    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return ThinkingUiCapability.QIANWEN;
    }

    @Override
    public void apply(@NotNull JsonObject body,
                      @NotNull ThinkingIntent intent,
                      @NotNull ThinkingContext context) {
        ThinkingSendMode sendMode = resolveSendMode(intent, context);
        if (sendMode == ThinkingSendMode.TRUE) {
            body.addProperty("enable_thinking", true);
            // AUTO：不写 reasoning_effort，沿用模型默认；显式档位才写入
            if (intent.effort() != ThinkingEffort.AUTO) {
                body.addProperty("reasoning_effort", intent.effort().toDashScopeReasoningEffort());
            }
        } else if (sendMode == ThinkingSendMode.FALSE) {
            body.addProperty("enable_thinking", false);
        }
    }

    @Override
    public @NotNull ThinkingProbeResult probe(@NotNull Project project,
                                              @NotNull AIProviderConfig config,
                                              @Nullable String apiKey,
                                              @NotNull BiConsumer<HttpURLConnection, String> connectionTuner) {
        return ThinkingCapabilityProbe.probe(project, config, apiKey, connectionTuner);
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.MAX;
    }

    @NotNull
    private static ThinkingSendMode resolveSendMode(@NotNull ThinkingIntent intent, @NotNull ThinkingContext context) {
        ThinkingCapability capability = context.probeResult() != null ? context.probeResult().capability : null;
        if (capability == ThinkingCapability.UNSUPPORTED) {
            return ThinkingSendMode.OMIT;
        }
        if (capability == ThinkingCapability.REQUIRED_TRUE) {
            return ThinkingSendMode.TRUE;
        }
        if (!intent.enabled()) {
            return ThinkingSendMode.OMIT;
        }
        return ThinkingSendMode.TRUE;
    }
}
