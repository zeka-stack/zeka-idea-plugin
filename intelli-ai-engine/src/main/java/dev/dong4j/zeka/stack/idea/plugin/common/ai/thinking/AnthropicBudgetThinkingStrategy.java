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
 * 官方 Anthropic / 混元 Anthropic 思考参数
 * <p>
 * 开启：{@code thinking.type=enabled} + {@code budget_tokens}（按强度映射，并相对 {@code max_tokens} clamp）；
 * 关闭：显式 {@code thinking.type=disabled}。
 * <p>
 * 首期不使用 {@code adaptive}，避免旧 Claude 模型 400。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://docs.anthropic.com/en/docs/build-with-claude/extended-thinking">Extended thinking</a>
 */
public final class AnthropicBudgetThinkingStrategy implements ThinkingParamStrategy {

    public static final AnthropicBudgetThinkingStrategy INSTANCE = new AnthropicBudgetThinkingStrategy();

    private static final int BUDGET_LOW = 1024;
    private static final int BUDGET_HIGH = 4096;
    private static final int BUDGET_MAX = 8192;

    private AnthropicBudgetThinkingStrategy() {
    }

    @Override
    public @NotNull String id() {
        return "anthropic_budget";
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
            ThinkingEffort effort = intent.effort() == ThinkingEffort.AUTO ? defaultEffort() : intent.effort();
            int budget = resolveBudgetTokens(effort, body);
            thinking.addProperty("type", "enabled");
            thinking.addProperty("budget_tokens", budget);
            body.add("thinking", thinking);
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
        result.summary = "【思考能力】Anthropic budget 格式\n"
                         + "  开关: thinking.type = enabled/disabled\n"
                         + "  强度: thinking.budget_tokens（按 low/high/max 映射）\n"
                         + "  结论: 可选 Think（可开关 + 强度）";
        return result;
    }

    @Override
    public @NotNull ThinkingEffort defaultEffort() {
        return ThinkingEffort.HIGH;
    }

    /**
     * 将用户强度映射为 budget_tokens，并保证满足 Anthropic 约束：≥1024 且 &lt; max_tokens。
     */
    static int resolveBudgetTokens(@NotNull ThinkingEffort effort, @NotNull JsonObject body) {
        int budget = switch (effort) {
            case LOW -> BUDGET_LOW;
            case MAX -> BUDGET_MAX;
            case HIGH, AUTO -> BUDGET_HIGH;
        };
        if (body.has("max_tokens") && body.get("max_tokens").isJsonPrimitive()) {
            try {
                int maxTokens = body.get("max_tokens").getAsInt();
                // budget_tokens 必须小于 max_tokens；至少保留 1 token 给最终回复
                int upper = Math.max(BUDGET_LOW, maxTokens - 1);
                budget = Math.min(budget, upper);
                budget = Math.max(budget, BUDGET_LOW);
                // max_tokens 本身过小无法满足官方下限时，仍写 1024，由服务端校验报错更清晰
                if (maxTokens <= BUDGET_LOW) {
                    budget = BUDGET_LOW;
                }
            } catch (RuntimeException ignored) {
                // 非法 max_tokens 时使用未 clamp 的映射值
            }
        }
        return budget;
    }
}
