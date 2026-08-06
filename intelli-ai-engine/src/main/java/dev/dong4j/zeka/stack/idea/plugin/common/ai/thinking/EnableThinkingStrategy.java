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
 * 默认 OpenAI 兼容网关回退：写入厂商扩展字段 {@code enable_thinking}
 * <p>
 * 该字段<strong>不是</strong> OpenAI 官方规范；通义请使用 {@link QianwenThinkingStrategy}。
 * 本策略用于硅基流动等仍兼容该字段、且未单独建模的服务商。
 * 关闭时默认 OMIT（不写 false），避免部分模型对 false 直接 400。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class EnableThinkingStrategy implements ThinkingParamStrategy {

    public static final EnableThinkingStrategy INSTANCE = new EnableThinkingStrategy();

    private EnableThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "enable_thinking";
    }

    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return ThinkingUiCapability.ENABLE_THINKING;
    }

    @Override
    public void apply(@NotNull JsonObject body,
                      @NotNull ThinkingIntent intent,
                      @NotNull ThinkingContext context) {
        ThinkingSendMode sendMode = resolveSendMode(intent, context);
        if (sendMode == ThinkingSendMode.TRUE) {
            body.addProperty("enable_thinking", true);
        } else if (sendMode == ThinkingSendMode.FALSE) {
            body.addProperty("enable_thinking", false);
        }
        // OMIT: 不写字段
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
        return ThinkingEffort.HIGH;
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
            // OPTIONAL 关闭时 OMIT；DEFAULT_ON_NO_PARAM 关闭时也不强行写 false
            return ThinkingSendMode.OMIT;
        }
        if (capability == ThinkingCapability.DEFAULT_ON_NO_PARAM) {
            return ThinkingSendMode.TRUE;
        }
        // OPTIONAL / UNKNOWN / null + enabled
        return ThinkingSendMode.TRUE;
    }
}
