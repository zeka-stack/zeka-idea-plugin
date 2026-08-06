package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * 思考参数策略注册表
 * <p>
 * 按官方服务商类型选择策略；不按 Custom 模型名启发式。
 * {@code enable_thinking} 仅用于通义及仍兼容该扩展字段的默认 OpenAI 兼容网关；
 * Anthropic 入口按协议写入 budget_tokens / thinking.type / output_config.effort 等。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ThinkingParamStrategyRegistry {

    private ThinkingParamStrategyRegistry() {
    }

    /**
     * 按配置解析策略
     *
     * @param config 服务商配置
     * @return 策略，不为 null
     */
    @NotNull
    public static ThinkingParamStrategy resolve(@NotNull AIProviderConfig config) {
        return resolve(ThinkingContext.from(config));
    }

    /**
     * 按上下文解析策略
     *
     * @param context 上下文
     * @return 策略，不为 null
     */
    @NotNull
    public static ThinkingParamStrategy resolve(@NotNull ThinkingContext context) {
        AIProviderType type = context.providerType();
        return switch (type) {
            case QIANWEN -> QianwenThinkingStrategy.INSTANCE;
            case DEEPSEEK -> DeepSeekOpenAIThinkingStrategy.INSTANCE;
            case DEEPSEEK_ANTHROPIC -> DeepSeekAnthropicThinkingStrategy.INSTANCE;
            case MOONSHOT, MOONSHOT_ANTHROPIC -> MoonshotThinkingStrategy.INSTANCE;
            case DOUBAO, DOUBAO_ANTHROPIC -> DoubaoThinkingStrategy.INSTANCE;
            case ZHIPU, ZHIPU_ANTHROPIC, ZAI_ANTHROPIC -> ZhipuThinkingStrategy.INSTANCE;
            // 官方 Anthropic / 混元 Anthropic：enabled + budget_tokens
            case ANTHROPIC, HUNYUAN_ANTHROPIC -> AnthropicBudgetThinkingStrategy.INSTANCE;
            // 混合上游：仅 thinking.type，避免 budget / effort 误伤
            case MODELSCOPE_ANTHROPIC -> ThinkingTypeToggleStrategy.INSTANCE;
            default -> type.isAnthropicCompatible()
                       ? NoOpThinkingStrategy.INSTANCE
                       : EnableThinkingStrategy.INSTANCE;
        };
    }
}
