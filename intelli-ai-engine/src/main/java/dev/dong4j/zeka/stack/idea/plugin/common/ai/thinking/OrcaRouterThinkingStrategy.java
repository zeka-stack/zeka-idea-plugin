/*
 * OrcaRouter 思考参数模块：把统一的 Think 意图转换为其 OpenAI 兼容 reasoning_effort 字段。
 */
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
 * OrcaRouter 思考参数策略
 * <p>
 * 开启时只写入官方支持的 {@code reasoning_effort}；关闭时省略思考字段，
 * 避免向聚合路由后的不同上游发送非标准 {@code enable_thinking} 或 {@code thinking.type}。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://docs.orcarouter.ai/api-reference/chat/create-a-chat-completion">OrcaRouter Chat API</a>
 */
public final class OrcaRouterThinkingStrategy implements ThinkingParamStrategy {

    /** 无状态策略单例 */
    public static final OrcaRouterThinkingStrategy INSTANCE = new OrcaRouterThinkingStrategy();

    private OrcaRouterThinkingStrategy() {
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull String id() {
        return "orcarouter";
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull ThinkingUiCapability uiCapability(@NotNull ThinkingContext context) {
        return ThinkingUiCapability.TOGGLE_AND_EFFORT;
    }

    /**
     * 写入 OrcaRouter 思考强度
     * <p>
     * 关闭时不写任何扩展字段；AUTO 使用 high，保持与 Engine 其他可调强度策略一致。
     */
    @Override
    public void apply(@NotNull JsonObject body,
                      @NotNull ThinkingIntent intent,
                      @NotNull ThinkingContext context) {
        if (!intent.enabled()) {
            return;
        }
        ThinkingEffort effort = intent.effort() == ThinkingEffort.AUTO ? defaultEffort() : intent.effort();
        body.addProperty("reasoning_effort", effort.toApiValue());
    }

    /**
     * 返回静态能力结论
     * <p>
     * 官方协议已经定义 reasoning_effort，无需发送可能产生计费的探测请求。
     */
    @Override
    public @NotNull ThinkingProbeResult probe(@NotNull Project project,
                                              @NotNull AIProviderConfig config,
                                              @Nullable String apiKey,
                                              @NotNull BiConsumer<HttpURLConnection, String> connectionTuner) {
        ThinkingProbeResult result = new ThinkingProbeResult();
        result.capability = ThinkingCapability.OPTIONAL;
        result.probedAt = System.currentTimeMillis();
        result.summary = "【思考能力】OrcaRouter：reasoning_effort = low/high/max";
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.HIGH;
    }
}
