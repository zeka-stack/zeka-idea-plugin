package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * 思考参数策略注册表
 * <p>
 * 按官方服务商类型选择策略；不按 Custom 模型名启发式。
 * {@code enable_thinking} 仅用于通义及仍兼容该扩展字段的默认 OpenAI 兼容网关。
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
            case MOONSHOT -> MoonshotThinkingStrategy.INSTANCE;
            case DOUBAO -> DoubaoThinkingStrategy.INSTANCE;
            case ZHIPU -> ZhipuThinkingStrategy.INSTANCE;
            // Anthropic 兼容入口（非 DeepSeek）：本期仍不写思考扩展，避免字段不兼容
            case ANTHROPIC, MOONSHOT_ANTHROPIC, DOUBAO_ANTHROPIC, HUNYUAN_ANTHROPIC,
                 ZHIPU_ANTHROPIC, MODELSCOPE_ANTHROPIC, ZAI_ANTHROPIC -> NoOpThinkingStrategy.INSTANCE;
            default -> type.isAnthropicCompatible()
                       ? NoOpThinkingStrategy.INSTANCE
                       : EnableThinkingStrategy.INSTANCE;
        };
    }
}
